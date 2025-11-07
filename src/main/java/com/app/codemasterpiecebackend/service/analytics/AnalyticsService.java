package com.app.codemasterpiecebackend.service.analytics;

import com.app.codemasterpiecebackend.domain.dto.analytics.PageViewEvent;
import com.app.codemasterpiecebackend.domain.entity.analytics.PageView;
import com.app.codemasterpiecebackend.domain.repository.analytics.PageViewRepository;
import com.app.codemasterpiecebackend.support.net.IpResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 페이지 뷰 이벤트를 수집/정규화/저장하는 서비스.
 * <p>
 * 프론트엔드에서 들어온 {@link PageViewEvent}를 분석해
 * {@link PageView} 엔티티로 저장한다.
 * <ul>
 *   <li>Bot 필터링 (User-Agent 기반)</li>
 *   <li>IP 해석 및 마스킹/해시 (via {@link IpResolver})</li>
 *   <li>User-Agent 파싱 (브라우저/OS/디바이스)</li>
 *   <li>UTC 기준 타임라인 보정</li>
 *   <li>UTM 파라미터 정규화</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private final PageViewRepository repo;
    private final IpResolver ipResolver;
    private final UserAgentAnalyzer uaa;

    public void ingest(PageViewEvent e, HttpServletRequest req) {
        // 1) IP
        var ip = ipResolver.resolve(req);

        // 2) UA 파싱
        UserAgent ua = uaa.parse(e.ua());
        String device = ua.getValue("DeviceClass");
        boolean isBot = "Robot".equalsIgnoreCase(device);

        // 3) 타임라인: 모두 UTC Instant로
        Instant occurred = (e.ts() != null)
                ? Instant.ofEpochMilli(e.ts())
                : Instant.now();
        Instant received = Instant.now();

        PageView pv = PageView.builder()
                .occurredAt(occurred)
                .receivedAt(received)
                .cid(e.cid())
                .sid(e.sid())
                .url(e.url())
                .ref(e.ref())
                .title(e.title())
                .lang(e.lang())
                .device(device)
                .browser(ua.getValue("AgentName"))
                .os(ua.getValue("OperatingSystemName"))
                .viewportW(e.vp() != null ? e.vp().w() : null)
                .viewportH(e.vp() != null ? e.vp().h() : null)
                .ipMasked(ip.maskedIp())
                .utmSource(val(e.utm(), "utm_source"))
                .utmMedium(val(e.utm(), "utm_medium"))
                .utmCampaign(val(e.utm(), "utm_campaign"))
                .utmTerm(val(e.utm(), "utm_term"))
                .utmContent(val(e.utm(), "utm_content"))
                .build();

        if (isBot) pv.markBot();

        repo.save(pv);
    }

    private static String val(java.util.Map<String, String> m, String k) {
        return m != null ? m.get(k) : null;
    }
}

