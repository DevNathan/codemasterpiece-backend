package com.app.codemasterpiecebackend.service.analytics;

import com.app.codemasterpiecebackend.domain.dto.analytics.PvAggDto;
import com.app.codemasterpiecebackend.domain.entity.analytics.PvDaily;
import com.app.codemasterpiecebackend.domain.entity.analytics.PvMonthly;
import com.app.codemasterpiecebackend.domain.entity.analytics.PvWeekly;
import com.app.codemasterpiecebackend.domain.repository.analytics.PageViewRepository;
import com.app.codemasterpiecebackend.domain.repository.analytics.PvDailyRepository;
import com.app.codemasterpiecebackend.domain.repository.analytics.PvMonthlyRepository;
import com.app.codemasterpiecebackend.domain.repository.analytics.PvWeeklyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;

@Service
@RequiredArgsConstructor
@Transactional
public class RollupService {

    private final PageViewRepository pvRepo;
    private final PvDailyRepository dailyRepo;
    private final PvWeeklyRepository weeklyRepo;
    private final PvMonthlyRepository monthlyRepo;

    // ===== 일(day) 집계: key = UTC 날짜 =====
    public void rollupDay(LocalDate dayUtc) {
        Instant from = dayUtc.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to   = dayUtc.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        PvAggDto agg = pvRepo.aggregateRange(from, to);
        long views = agg.views();
        long uv = agg.uv();
        long sessions = agg.sessions();

        dailyRepo.findById(dayUtc)
                .map(e -> {
                    e.setRollup(views, uv, sessions);
                    return e;
                })
                .orElseGet(() -> dailyRepo.save(PvDaily.of(dayUtc, views, uv, sessions)));
    }

    // ===== 주(week) 집계: key = UTC 기준 ISO 주 시작(월요일) =====
    public void rollupWeek(LocalDate weekStartUtc) {
        Instant from = weekStartUtc.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to   = weekStartUtc.plusWeeks(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        PvAggDto agg = pvRepo.aggregateRange(from, to);
        long views = agg.views();
        long uv = agg.uv();
        long sessions = agg.sessions();

        weeklyRepo.findById(weekStartUtc)
                .map(e -> {
                    e.setRollup(views, uv, sessions);
                    return e;
                })
                .orElseGet(() -> weeklyRepo.save(PvWeekly.of(weekStartUtc, views, uv, sessions)));
    }

    // ===== 월(month) 집계: key = UTC 기준 해당 월 1일 =====
    public void rollupMonth(YearMonth ymUtc) {
        LocalDate monthStart = ymUtc.atDay(1);
        Instant from = monthStart.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to   = ymUtc.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        PvAggDto agg = pvRepo.aggregateRange(from, to);
        long views = agg.views();
        long uv = agg.uv();
        long sessions = agg.sessions();

        monthlyRepo.findById(monthStart)
                .map(e -> {
                    e.setRollup(views, uv, sessions);
                    return e;
                })
                .orElseGet(() -> monthlyRepo.save(PvMonthly.of(monthStart, views, uv, sessions)));
    }

    // ===== 범위 유틸 (모두 UTC 기준) =====

    /**
     * 아무 UTC 날짜가 속한 ISO 주(월요일 시작) 재집계
     */
    public void rollupWeekOf(LocalDate anyDayUtc) {
        LocalDate weekStart = anyDayUtc.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        rollupWeek(weekStart);
    }

    /**
     * 아무 UTC 날짜가 속한 월 재집계
     */
    public void rollupMonthOf(LocalDate anyDayUtc) {
        rollupMonth(YearMonth.from(anyDayUtc));
    }
}
