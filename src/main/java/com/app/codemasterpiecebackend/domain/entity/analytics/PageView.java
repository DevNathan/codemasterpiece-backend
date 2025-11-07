package com.app.codemasterpiecebackend.domain.entity.analytics;

import com.app.codemasterpiecebackend.config.jpa.PrefixedUlidId;
import com.app.codemasterpiecebackend.domain.entity.base.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static lombok.AccessLevel.PROTECTED;

@Getter
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor(access = PROTECTED)
@Builder
@Entity
@Table(
        name = "tbl_page_view",
        indexes = {
                @Index(name = "idx_pv_day", columnList = "day"),
                @Index(name = "idx_pv_received", columnList = "received_at"),
                @Index(name = "idx_pv_cid_day", columnList = "cid,day"),
                @Index(name = "idx_pv_sid_day", columnList = "sid,day"),
                @Index(name = "idx_pv_host_day", columnList = "url_host,day"),
                @Index(name = "idx_pv_path_day", columnList = "url_path,day"),
                @Index(name = "idx_pv_ref_host_day", columnList = "ref_host,day"),
                @Index(name = "idx_pv_utm_day", columnList = "utm_source,utm_campaign,day"),
                @Index(name = "idx_pv_device_day", columnList = "device,day"),
                @Index(name = "idx_pv_bot_day", columnList = "is_bot,day")
        }
)
public class PageView extends BaseTimeEntity {

    /**
     * ULID (예: PV-01J7...); CHAR(29) 고정
     */
    @Id
    @PrefixedUlidId("PV")
    @Column(name = "page_view_id", nullable = false, length = 29)
    private String id;

    /**
     * 클라이언트 보고 시각(UTC 가정; 없으면 서버 시각으로 대체)
     */
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    /**
     * 서버가 수집한 시각(UTC)
     */
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    /**
     * yyyy-MM-dd (파티션/그룹핑 키)
     */
    @Column(name = "day", nullable = false, length = 10)
    private String day;

    /**
     * 시간 버킷(0~23) — 카드 차트/히트맵에 바로 쓰기 좋음
     */
    @Column(name = "hour_bucket", nullable = false)
    private short hour;

    /* ───────── 식별/세션 ───────── */
    @Column(length = 40)
    private String cid; // CL-ULID
    @Column(length = 40)
    private String sid; // SE-ULID

    /* ───────── URL 정규화 ───────── */
    @Column(name = "url", length = 1024)
    private String url; // 원문(옵션)
    @Column(name = "url_host", length = 255)
    private String urlHost;
    @Column(name = "url_path", length = 1024)
    private String urlPath;
    @Column(name = "url_query", length = 1024)
    private String urlQuery;

    /* ───────── 리퍼러 정규화 ───────── */
    @Column(name = "ref", length = 1024)
    private String ref; // 원문(옵션)
    @Column(name = "ref_host", length = 255)
    private String refHost;
    @Column(name = "ref_path", length = 1024)
    private String refPath;

    /**
     * 외부 유입인지(자기 호스트와 다른 ref_host)
     */
    @Column(name = "is_external_ref", nullable = false)
    @Builder.Default
    private boolean externalRef = false;

    /* ───────── 컨텐츠/언어 ───────── */
    @Column(length = 255)
    private String title;
    @Column(length = 16)
    private String lang;

    /* ───────── 클라이언트/디바이스 ───────── */
    @Column(length = 32)
    private String device;  // mobile/desktop/tablet/other
    @Column(length = 64)
    private String browser;
    @Column(length = 64)
    private String os;
    @Column(name = "vp_w")
    private Integer viewportW;
    @Column(name = "vp_h")
    private Integer viewportH;

    /* ───────── 네트워크/봇/지역 ───────── */
    @Column(name = "ip_masked", length = 64)
    private String ipMasked; // /24, /48 마스킹
    @Column(length = 2)
    private String country; // ISO-2 (선택)
    @Column(length = 64)
    private String city;    // (선택)
    @Column(name = "is_bot", nullable = false)
    @Builder.Default
    private boolean bot = false;

    /* ───────── UTM (정규화) ───────── */
    @Column(name = "utm_source", length = 80)
    private String utmSource;
    @Column(name = "utm_medium", length = 80)
    private String utmMedium;
    @Column(name = "utm_campaign", length = 120)
    private String utmCampaign;
    @Column(name = "utm_term", length = 120)
    private String utmTerm;
    @Column(name = "utm_content", length = 120)
    private String utmContent;

    /* ───────── 라이프사이클 ───────── */

    @Override
    protected void onPrePersistHook() {
        final Instant now = Instant.now();

        if (occurredAt == null) occurredAt = now;
        if (receivedAt == null) receivedAt = now;

        // UTC로 고정 변환
        final LocalDateTime ldt = LocalDateTime.ofInstant(occurredAt, ZoneOffset.UTC);

        // yyyy-MM-dd 포맷 (인덱스/파티션 키와 정확히 일치)
        this.day = ldt.toLocalDate().toString();

        // 0~23 정수
        this.hour = (short) ldt.getHour();

        normalizeUrls();
        sanitize();
    }


    /* ───────── 유틸/헬퍼 ───────── */

    /**
     * URL/Ref를 파싱해 host/path/query 분리 및 외부 유입 여부 결정
     */
    public void normalizeUrls() {
        try {
            if (url != null) {
                final URI u = URI.create(url);
                this.urlHost = safeLower(u.getHost());
                this.urlPath = safe(u.getPath());
                this.urlQuery = safe(u.getQuery());
            }
        } catch (Exception ignore) {
        }
        try {
            if (ref != null) {
                final URI r = URI.create(ref);
                this.refHost = safeLower(r.getHost());
                this.refPath = safe(r.getPath());
            }
        } catch (Exception ignore) {
        }
        if (urlHost != null && refHost != null) {
            this.externalRef = !urlHost.equalsIgnoreCase(refHost);
        } else {
            this.externalRef = refHost != null && (!refHost.equalsIgnoreCase(urlHost));
        }
    }

    private static String safe(String s) {
        return s == null ? null : trimMax(s, 1024);
    }

    private static String safeLower(String s) {
        return s == null ? null : trimMax(s.toLowerCase(), 255);
    }

    private static String trimMax(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private void sanitize() {
        // 길이 초과 방어(예: 제목/경로), 공백 정리
        if (title != null && title.length() > 255) title = title.substring(0, 255);
        if (lang != null && lang.length() > 16) lang = lang.substring(0, 16);
    }

    /* 편의 세터 */

    public void markBot() {
        this.bot = true;
    }
}
