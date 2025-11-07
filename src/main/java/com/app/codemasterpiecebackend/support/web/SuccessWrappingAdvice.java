package com.app.codemasterpiecebackend.support.web;

import com.app.codemasterpiecebackend.domain.dto.response.ErrorResponse;
import com.app.codemasterpiecebackend.domain.dto.response.SuccessPayload;
import com.app.codemasterpiecebackend.domain.dto.response.SuccessResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Set;

import static com.app.codemasterpiecebackend.support.web.TraceIdFilter.TRACE_ID;

/**
 * {@code SuccessWrappingAdvice}
 *
 * <p><b>목적:</b> 모든 <em>성공(2xx)</em> 컨트롤러 응답을 표준 {@link SuccessResponse} 포맷으로 일관되게 래핑한다.
 * 컨트롤러는 성공 포맷(타임스탬프/트레이스/경로/메시지)을 신경 쓰지 않고 <em>의도</em>(데이터/메시지코드)만 반환한다.
 *
 * <h3>핵심 동작</h3>
 * <ul>
 *   <li><b>래핑 대상:</b> 2xx 응답. (상태 코드는 {@link ResponseStatus}, {@link ResponseEntity} 로부터 유지)</li>
 *   <li><b>메시지 처리:</b> 컨트롤러가 {@link SuccessPayload}를 반환하면 {@code messageCode/messageArgs}로
 *       {@link MessageSource}를 통해 로케일별 메시지를 해석한다. 없으면 상태코드에 따른 기본 코드
 *       ({@code success.ok}, {@code success.created}, {@code success.accepted})를 사용한다.</li>
 *   <li><b>메타 삽입:</b> {@code traceId}와 {@code path}는 여기서 주입한다. (컨트롤러 보일러 제거)</li>
 *   <li><b>문자열 응답:</b> {@code String} 응답은 JSON 문자열로 변환하여 반환(컨텐트 타입 {@code application/json})</li>
 *   <li><b>패스 스루:</b> HTML/이미지/바이너리 등은 래핑 없이 그대로 통과</li>
 *   <li><b>예외 케이스 제외:</b> 이미 표준 포맷({@link SuccessResponse}/{@link ErrorResponse})인 경우는 재래핑하지 않음
 *       <!-- 변경: ResponseEntity는 이제 래핑 대상. 상태/헤더는 보존하면서 body만 래핑한다. --></li>
 *   <li><b>Opt-out:</b> {@code @SkipWrap}가 클래스/메서드에 있으면 래핑 제외</li>
 * </ul>
 *
 * <h3>주의사항</h3>
 * <ul>
 *   <li><b>제네릭 일관성:</b> 내부 빌더는 {@code SuccessResponse<?>}로 귀결되도록 설계하여
 *       {@code Void} 등 분기별 제네릭 차이를 흡수한다.</li>
 *   <li><b>JDK 기능:</b> 본 구현은 <i>record pattern matching</i> 문법을 사용한다.
 *       구버전 JDK 또는 프리뷰 비활성 환경에서는
 *       {@code if (body instanceof SuccessPayload<?> p) { ... }} 형태로 대체하라.</li>
 * </ul>
 *
 * <h3>사용 예</h3>
 * <pre>{@code
 * @PostMapping
 * @ResponseStatus(HttpStatus.CREATED)
 * public SuccessPayload<Void> createUser(@Valid UserCreate req) {
 *     service.create(...);
 *     return SuccessPayload.msgOnly("success.user.created");
 *     // → Advice가 SuccessResponse로 래핑하면서 traceId/path/timestamp/message를 채운다.
 * }
 *
 * @SkipWrap
 * @GetMapping("/raw")
 * public ResponseEntity<byte[]> rawFile() { ... } // 래핑 제외
 * }</pre>
 *
 * @author Nathan
 * @since 2025-10
 */
@RestControllerAdvice
@Component
@RequiredArgsConstructor
public class SuccessWrappingAdvice implements ResponseBodyAdvice<Object> {

    /**
     * 래핑을 건너뛸 미디어 타입 집합.
     * HTML/이미지/바이너리는 표준 JSON 포맷 대상이 아니므로 그대로 전달한다.
     * <p>
     * <!-- 확장: 실제 운영에서 빈번한 타입들을 추가 -->
     */
    private static final Set<MediaType> PASS_THROUGH = Set.of(
            MediaType.TEXT_HTML,
            MediaType.IMAGE_PNG,
            MediaType.IMAGE_JPEG,
            new MediaType("image", "gif"),
            new MediaType("image", "webp"),
            new MediaType("image", "svg+xml"),
            MediaType.APPLICATION_OCTET_STREAM,
            new MediaType("application", "pdf"),
            MediaType.MULTIPART_FORM_DATA,
            new MediaType("text", "event-stream"),
            new MediaType("application", "x-ndjson")
    );

    private final ObjectMapper objectMapper;
    private final MessageSource messageSource;

    /**
     * 래핑 적용 여부 결정.
     * <ul>
     *   <li>{@code @SkipWrap} 있으면 제외</li>
     *   <li>이미 {@link SuccessResponse}/{@link ErrorResponse}면 제외</li>
     *   <!-- 변경: ResponseEntity는 이제 래핑 대상이므로 제외 조건에서 제거 -->
     *   <li>그 외는 래핑</li>
     * </ul>
     */
    @Override
    public boolean supports(MethodParameter returnType, @Nullable Class converterType) {
        if (returnType.hasMethodAnnotation(SkipWrap.class)
                || returnType.getContainingClass().isAnnotationPresent(SkipWrap.class)) {
            return false;
        }
        Class<?> type = returnType.getParameterType();
        return !(SuccessResponse.class.isAssignableFrom(type)
                || ErrorResponse.class.isAssignableFrom(type));
    }

    /**
     * 바디를 가로채 표준 {@link SuccessResponse}로 조립한다.
     * <p>상태코드는 {@link ResponseStatus} 또는 {@link ResponseEntity}에서 유도한다.</p>
     * <p>문자열 응답은 JSON 문자열로 직렬화하여 반환한다.</p>
     */
    @Override
    public Object beforeBodyWrite(
            Object body,
            @Nullable MethodParameter returnType,
            @Nullable MediaType contentType,
            @Nullable Class selectedConverterType,
            @Nullable ServerHttpRequest req,
            @Nullable ServerHttpResponse res
    ) {
        // 파일/이미지/HTML은 래핑 없이 패스
        if (contentType != null && PASS_THROUGH.stream().anyMatch(contentType::isCompatibleWith)) {
            return body;
        }

        // 요청 메타
        HttpServletRequest servletReq = (req instanceof ServletServerHttpRequest s) ? s.getServletRequest() : null;
        String traceId = (servletReq != null) ? (String) servletReq.getAttribute(TRACE_ID) : null;
        String path = (servletReq != null) ? servletReq.getRequestURI()
                : (req != null ? req.getURI().getPath() : "");

        // 현재 응답 status (이미 세팅돼 있을 수 있음) → 없으면 @ResponseStatus → 200
        HttpStatus status = resolveStatus(returnType);
        try {
            var servletRes = (org.springframework.http.server.ServletServerHttpResponse) res;
            int current = (servletRes != null) ? servletRes.getServletResponse().getStatus() : -1;
            if (current > 0) status = HttpStatus.valueOf(current);
        } catch (Exception ignore) {
            // no-op
        }

        // [1] 바디 자체가 ErrorResponse면 절대 래핑 금지
        if (body instanceof ErrorResponse) {
            return body;
        }

        // [2] ResponseEntity 처리: 에러 상태면 패스, 204면 바디 만들지 않음, 그 외만 래핑
        if (body instanceof ResponseEntity<?> entity) {
            HttpStatus st = HttpStatus.valueOf(entity.getStatusCode().value());
            Object inner = entity.getBody();

            // 에러 상태면 무조건 패스
            if (st.isError()) {
                return entity;
            }
            // 이미 표준 포맷 존중
            if (inner instanceof SuccessResponse<?> || inner instanceof ErrorResponse) {
                return entity;
            }
            // 204 No Content 보장
            if (st == HttpStatus.NO_CONTENT) {
                return ResponseEntity.noContent().headers(entity.getHeaders()).build();
            }

            SuccessResponse<?> wrapped = build(st, traceId, path, inner);
            return ResponseEntity.status(st).headers(entity.getHeaders()).body(wrapped);
        }

        // [3] 일반 경로: 상태가 에러면 절대 래핑하지 않는다 (혹시 컨트롤러에서 4xx를 직접 세팅한 경우)
        if (status.isError()) {
            return body;
        }

        // 204 No Content 보장
        if (status == HttpStatus.NO_CONTENT) {
            return null;
        }

        // [4] 성공만 래핑
        SuccessResponse<?> wrapped = build(status, traceId, path, body);

        // String 응답은 JSON 문자열로 변환
        boolean isStringConverter = (selectedConverterType != null)
                && StringHttpMessageConverter.class.isAssignableFrom(selectedConverterType);
        boolean isReturnTypeString = (returnType != null)
                && String.class.isAssignableFrom(returnType.getParameterType());
        if (isStringConverter || isReturnTypeString || body instanceof String) {
            try {
                if (res != null) res.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                return objectMapper.writeValueAsString(wrapped);
            } catch (Exception e) {
                return body;
            }
        }

        return wrapped;
    }

    /**
     * 메서드에 선언된 {@link ResponseStatus}로부터 상태 코드를 해석한다.
     * 없으면 {@link HttpStatus#OK}.
     */
    private HttpStatus resolveStatus(@Nullable MethodParameter returnType) {
        if (returnType == null) return HttpStatus.OK; // 널 세이프티
        ResponseStatus rs = returnType.getMethodAnnotation(ResponseStatus.class);
        return (rs != null) ? rs.value() : HttpStatus.OK;
    }

    /**
     * 상태/메시지/데이터를 바탕으로 최종 {@link SuccessResponse}를 조립한다.
     * <p>컨트롤러가 {@link SuccessPayload}를 반환하면 messageCode/args를 사용해 메시지를 해석한다.
     * 없으면 상태코드 기반 기본 메시지 코드를 사용한다.</p>
     *
     * @implNote 반환 타입은 {@code SuccessResponse<?>}로 고정하여
     * 분기별 제네릭 차이(예: {@code Void} vs {@code Object})로 인한 컴파일 오류를 방지한다.
     */
    private SuccessResponse<?> build(HttpStatus status, String traceId, String path, Object body) {
        String code = null;
        Object[] args = null;
        Object data = body;

        if (body instanceof SuccessPayload<?>(Object data1, String messageCode, Object[] messageArgs)) {
            code = messageCode;
            args = messageArgs;
            data = data1;
        }

        // 메시지 코드가 없으면 상태코드에 따른 기본 코드 할당
        if (code == null) {
            code = switch (status) {
                case CREATED -> "success.created";
                case ACCEPTED -> "success.accepted";
                default -> "success.ok";
            };
        }

        String msg = messageSource.getMessage(code, args, code, LocaleContextHolder.getLocale());

        // 상태코드에 맞는 팩토리 호출. 모두 SuccessResponse<?>로 통일.
        return switch (status) {
            case CREATED -> SuccessResponse.created(traceId, path, code, msg, data);
            case ACCEPTED -> SuccessResponse.accepted(traceId, path, code, msg);
            default -> SuccessResponse.ok(traceId, path, code, msg, data);
        };
    }
}
