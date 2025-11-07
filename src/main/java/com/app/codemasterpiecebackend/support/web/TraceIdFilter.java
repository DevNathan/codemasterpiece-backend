package com.app.codemasterpiecebackend.support.web;

import com.app.codemasterpiecebackend.util.ULIDs;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SecureRandom;

/**
 * <h2>TraceIdFilter</h2>
 *
 * <p>모든 HTTP 요청에 대해 고유한 traceId를 생성하거나,
 * 클라이언트/프록시가 전달한 {@code X-Trace-Id}/{@code X-Request-Id} 값을 재사용한다.</p>
 *
 * <ul>
 *   <li>생성된 traceId는 {@link MDC} 에 저장되어 로그 패턴에서 %X{traceId}로 찍힌다.</li>
 *   <li>요청 attribute에도 저장되어 예외 핸들러나 컨트롤러에서 접근 가능하다.</li>
 *   <li>응답 헤더 {@code X-Trace-Id}로 내려보내 클라이언트와 서버 로그를 상관관계 지을 수 있다.</li>
 * </ul>
 *
 * <p>traceId 포맷: {@code ULIDs} </p>
 *
 * <p>SecurityFilterChain보다 먼저 동작하도록 {@code @Order(SecurityProperties.DEFAULT_FILTER_ORDER - 1)}로 설정됨.</p>
 */
@Component
@Order(SecurityProperties.DEFAULT_FILTER_ORDER - 1) // Security(-100)보다 먼저
public class TraceIdFilter extends OncePerRequestFilter {

    /**
     * MDC 키 및 request attribute 키
     */
    public static final String TRACE_ID = "traceId";

    /**
     * 난수 생성기 (traceId 접미사용)
     */
    private static final SecureRandom RNG = new SecureRandom();

    /**
     * 요청당 실행.
     * <ol>
     *   <li>기존 {@code X-Trace-Id}/{@code X-Request-Id} 헤더 있으면 우선 사용.</li>
     *   <li>없으면 새 traceId 생성.</li>
     *   <li>MDC, request attribute, response header에 traceId 주입.</li>
     *   <li>요청 처리 후 MDC 정리.</li>
     * </ol>
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // 들어온 헤더 우선적 처리
        String incoming = firstNonBlank(request.getHeader("X-Trace-Id"), request.getHeader("X-Request-Id"));
        String traceId = (incoming != null) ? incoming : ULIDs.newMonotonicUlid();

        // 요청 컨텍스트 세팅
        MDC.put(TRACE_ID, traceId);
        request.setAttribute(TRACE_ID, traceId);

        // 응답 헤더에 traceId 심기 (프런트/모바일에서 보고 서버 로그 매칭)
        response.setHeader("X-Trace-Id", traceId);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID);
        }
    }

    /**
     * 첫 번째로 null/blank 아닌 문자열을 반환한다.
     */
    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return null;
    }
}
