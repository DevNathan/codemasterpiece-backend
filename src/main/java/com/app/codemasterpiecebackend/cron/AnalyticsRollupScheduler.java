package com.app.codemasterpiecebackend.cron;

import com.app.codemasterpiecebackend.service.analytics.RollupService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
public class AnalyticsRollupScheduler {

    private final RollupService rollup;

    /**
     * 매 시 정각 + 1분(UTC): 오늘/어제(UTC) 일별 재계산
     * 지연 도착 이벤트 커버.
     */
    @Scheduled(cron = "0 1 * * * *", zone = "Asia/Seoul")
    public void hourlyDailyRollupUtc() {
        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
        rollup.rollupDay(todayUtc);
        rollup.rollupDay(todayUtc.minusDays(1));
    }

    /**
     * 매일 00:05(UTC): 주/월 롤업 갱신 (이번/이전)
     * - 주: ISO 월요일 시작(UTC)
     * - 월: 1일(UTC)
     */
    @Scheduled(cron = "0 5 0 * * *", zone = "UTC")
    public void dailyWeekMonthRollupUtc() {
        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);

        // 주간: 이번 주, 지난 주
        rollup.rollupWeekOf(todayUtc);
        rollup.rollupWeekOf(todayUtc.minusWeeks(1));

        // 월간: 이번 달, 지난 달
        rollup.rollupMonthOf(todayUtc);
        rollup.rollupMonthOf(todayUtc.minusMonths(1));
    }
}
