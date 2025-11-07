package com.app.codemasterpiecebackend.service.analytics;

import com.app.codemasterpiecebackend.domain.entity.analytics.BaseRollup;
import com.app.codemasterpiecebackend.domain.repository.analytics.PvDailyRepository;
import com.app.codemasterpiecebackend.domain.repository.analytics.PvMonthlyRepository;
import com.app.codemasterpiecebackend.domain.repository.analytics.PvWeeklyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AnalyticsReadService
 *
 * <p>페이지뷰 통계(일별, 주별, 월별)를 조회하는 서비스.
 * 각 기간별로 조회 시, 조회 구간 내 결측 데이터는 0으로 채운다.</p>
 *
 * <ul>
 *   <li>{@link #getDaily(LocalDate, LocalDate)}: 일별 시리즈 반환</li>
 *   <li>{@link #getWeekly(LocalDate, LocalDate)}: 주별 시리즈 반환</li>
 *   <li>{@link #getMonthly(LocalDate, LocalDate)}: 월별 시리즈 반환</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsReadService {

    /**
     * 프론트엔드로 전달할 단일 데이터 포인트.
     *
     * @param ts       날짜(yyyy-MM-dd)
     * @param views    조회수
     * @param uv       고유 방문자 수
     * @param sessions 세션 수
     */
    public record SeriesPoint(String ts, long views, long uv, long sessions) {
    }

    private final PvDailyRepository dailyRepo;
    private final PvWeeklyRepository weeklyRepo;
    private final PvMonthlyRepository monthlyRepo;

    /**
     * 지정된 기간(from~to)의 일별 통계를 반환한다.
     * 데이터가 없는 일자는 0으로 채운다.
     */
    public List<SeriesPoint> getDaily(LocalDate from, LocalDate to) {
        check(from, to);
        var keys = days(from, to);
        var map = dailyRepo.findAllById(keys).stream().collect(Collectors.toMap(
                BaseRollup::getKeyDate,
                e -> new SeriesPoint(e.getKeyDate().toString(), e.getViews(), e.getUv(), e.getSessions())
        ));
        var out = new ArrayList<SeriesPoint>(keys.size());
        for (var d : keys)
            out.add(map.getOrDefault(d, new SeriesPoint(d.toString(), 0, 0, 0)));
        return out;
    }

    /**
     * 지정된 기간(from~to)의 주별 통계를 반환한다.
     * 주 시작일(월요일 기준)을 키로 사용하며, 데이터가 없는 주는 0으로 채운다.
     */
    public List<SeriesPoint> getWeekly(LocalDate from, LocalDate to) {
        check(from, to);
        var keys = weeks(from, to);
        var map = weeklyRepo.findAllById(keys).stream().collect(Collectors.toMap(
                BaseRollup::getKeyDate,
                e -> new SeriesPoint(e.getKeyDate().toString(), e.getViews(), e.getUv(), e.getSessions())
        ));
        var out = new ArrayList<SeriesPoint>(keys.size());
        for (var d : keys)
            out.add(map.getOrDefault(d, new SeriesPoint(d.toString(), 0, 0, 0)));
        return out;
    }

    /**
     * 지정된 기간(from~to)의 월별 통계를 반환한다.
     * 월의 첫째 날을 키로 사용하며, 데이터가 없는 월은 0으로 채운다.
     */
    public List<SeriesPoint> getMonthly(LocalDate from, LocalDate to) {
        check(from, to);
        var keys = months(from, to);
        var map = monthlyRepo.findAllById(keys).stream().collect(Collectors.toMap(
                BaseRollup::getKeyDate,
                e -> new SeriesPoint(e.getKeyDate().toString(), e.getViews(), e.getUv(), e.getSessions())
        ));
        var out = new ArrayList<SeriesPoint>(keys.size());
        for (var d : keys)
            out.add(map.getOrDefault(d, new SeriesPoint(d.toString(), 0, 0, 0)));
        return out;
    }

    /**
     * 입력된 기간이 유효한지 검사한다.
     *
     * @throws IllegalArgumentException if from/to null or to < from
     */
    private static void check(LocalDate from, LocalDate to) {
        if (from == null || to == null)
            throw new IllegalArgumentException("from/to required");
        if (to.isBefore(from))
            throw new IllegalArgumentException("to must be >= from");
    }

    /**
     * from~to 사이의 모든 날짜 리스트 반환.
     */
    private static List<LocalDate> days(LocalDate from, LocalDate to) {
        var list = new ArrayList<LocalDate>();
        for (var d = from; !d.isAfter(to); d = d.plusDays(1))
            list.add(d);
        return list;
    }

    /**
     * from~to 기간의 모든 주(월요일 기준) 리스트 반환.
     */
    private static List<LocalDate> weeks(LocalDate from, LocalDate to) {
        var list = new ArrayList<LocalDate>();
        for (var d = from.with(DayOfWeek.MONDAY); !d.isAfter(to); d = d.plusWeeks(1))
            list.add(d);
        return list;
    }

    /**
     * from~to 기간의 모든 월(1일 기준) 리스트 반환.
     */
    private static List<LocalDate> months(LocalDate from, LocalDate to) {
        var list = new ArrayList<LocalDate>();
        for (var d = from.withDayOfMonth(1); !d.isAfter(to); d = d.plusMonths(1))
            list.add(d);
        return list;
    }
}
