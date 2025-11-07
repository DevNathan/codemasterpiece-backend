package com.app.codemasterpiecebackend.cron;

import com.app.codemasterpiecebackend.domain.repository.analytics.PageViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
@Transactional
public class PageViewRetentionScheduler {

    private final PageViewRepository pvRepo;

    /**
     * 매일 03:30 UTC: 60일 이전 로그 삭제
     * - 집계 대상에서 제외
     * - 디스크/인덱스 최소화
     */
    @Scheduled(cron = "0 30 3 * * *", zone = "UTC")
    public void purgeOldPageViews() {
        LocalDate cutoff = LocalDate.now(ZoneOffset.UTC).minusDays(60);
        Instant cutoffInstant = cutoff.atStartOfDay().toInstant(ZoneOffset.UTC);

        int deleted = pvRepo.deleteOlderThan(cutoffInstant);
        System.out.println("[PV RETENTION] deleted=" + deleted);
    }
}
