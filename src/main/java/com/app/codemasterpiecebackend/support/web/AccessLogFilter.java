package com.app.codemasterpiecebackend.support.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * AccessLogFilter
 *
 * <p>
 * HTTP 요청/응답을 가로채서 구조화된 접근 로그를 남기는 서블릿 필터.
 * 운영 환경에서는 JSON 로그로 전송되어, {@code uri}, {@code httpMethod},
 * {@code status}, {@code tookMs}, {@code traceId} 같은 필드를 통해
 * Kibana / Loki 등에서 효율적으로 검색 가능하다.
 * </p>
 *
 * <ul>
 *   <li>정적 리소스(css/js/img)와 헬스체크 엔드포인트는 필터링 대상에서 제외한다.</li>
 *   <li>요청 시작 시 MDC(Mapped Diagnostic Context)에 URI, HTTP 메서드를 저장한다.</li>
 *   <li>응답 완료 후 상태코드, 처리시간(ms)을 MDC에 기록한다.</li>
 *   <li>MDC 필드는 반드시 {@code finally} 블록에서 정리(cleanup)한다.</li>
 *   <li>2xx/3xx 응답은 DEBUG, 4xx/5xx 응답은 INFO 레벨로 기록해 잡음(noise)을 줄인다.</li>
 * </ul>
 *
 * <p>출력 예시(JSON 로그, 운영 서버):</p>
 * <pre>{@code
 * {
 *   "@timestamp": "...",
 *   "level": "INFO",
 *   "logger": "c.a.b.support.web.AccessLogFilter",
 *   "message": "REQ GET /posts/123",
 *   "uri": "/posts/123",
 *   "httpMethod": "GET",
 *   "status": "200",
 *   "tookMs": "15",
 *   "traceId": "250928193758599-195tpe"
 * }
 * }</pre>
 */
@Slf4j
@Component
@Order(SecurityProperties.DEFAULT_FILTER_ORDER)
public class AccessLogFilter extends OncePerRequestFilter {

    /**
     * 필터 제외 대상 패턴 목록.
     * <p>
     * favicon, 이미지, 정적 리소스(css/js), 헬스체크 API 등은 접근 로그에 필요하지 않음.
     */
    private static final String[] EXCLUDES = {
            "/favicon.ico",
            "/**/*.ico", "/**/*.png", "/**/*.jpg", "/**/*.jpeg", "/**/*.gif", "/**/*.svg", "/**/*.webp",
            "/**/*.css", "/**/*.js", "/**/*.map",
            "/actuator/health", "/actuator/info"
    };

    private final AntPathMatcher matcher = new AntPathMatcher();

    /**
     * 요청 URI가 필터링 대상인지 판별.
     * <ul>
     *   <li>Spring Boot 기본 static 리소스 경로(classpath:/static, /public 등)</li>
     *   <li>{@link #EXCLUDES}에 명시된 경로/확장자</li>
     *   <li>OPTIONS 메서드</li>
     * </ul>
     *
     * @param request 현재 요청
     * @return true면 필터를 적용하지 않음
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 1) 공용 정적 리소스
        if (PathRequest.toStaticResources().atCommonLocations().matches(request)) return true;

        // 2) 커스텀 exclude 패턴
        String path = request.getRequestURI();
        for (String p : EXCLUDES) {
            if (matcher.match(p, path)) return true;
        }

        // 3) OPTIONS 메서드
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    /**
     * 요청/응답 접근 로그를 남기는 필터 본체.
     *
     * @param req    HttpServletRequest
     * @param res    HttpServletResponse
     * @param chain  FilterChain
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest req,
            HttpServletResponse res,
            FilterChain chain
    ) throws IOException, ServletException {

        long t0 = System.nanoTime();
        String uri = req.getRequestURI();
        String qs = req.getQueryString();
        if (qs != null && !qs.isBlank()) uri = uri + "?" + qs;
        String method = req.getMethod();

        // 요청 시작 → MDC에 기록
        MDC.put("uri", uri);
        MDC.put("httpMethod", method);

        log.info("REQ {} {}", method, uri);
        try {
            chain.doFilter(req, res);
        } finally {
            long ms = (System.nanoTime() - t0) / 1_000_000;
            int sc = res.getStatus();

            MDC.put("status", Integer.toString(sc));
            MDC.put("tookMs", Long.toString(ms));

            // 레벨 정책: 2xx/3xx → DEBUG, 4xx/5xx → INFO
            if (sc >= 400) {
                log.info("RES {} {} status={} took={}ms", method, uri, sc, ms);
            } else {
                log.debug("RES {} {} status={} took={}ms", method, uri, sc, ms);
            }

            // MDC 정리 (메모리/스레드 로컬 오염 방지)
            MDC.remove("tookMs");
            MDC.remove("status");
            MDC.remove("httpMethod");
            MDC.remove("uri");
        }
    }
}
