package com.app.codemasterpiecebackend.domain.pageView.api.v1;

import com.app.codemasterpiecebackend.global.support.response.SuccessPayload;
import com.app.codemasterpiecebackend.domain.pageView.application.AnalyticsReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/analytics")
public class AnalyticsQueryV1Controller {

    private final AnalyticsReadService service;

    @GetMapping("/visitors/day")
    public SuccessPayload<?> byDay(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to
    ) {
        var result = service.getDaily(from, to);
        return SuccessPayload.of(result);
    }

    @GetMapping("/visitors/week")
    public SuccessPayload<?> byWeek(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to
    ) {
        var result = service.getWeekly(from, to);
        return SuccessPayload.of(result);
    }

    @GetMapping("/visitors/month")
    public SuccessPayload<?> byMonth(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to
    ) {
        var result = service.getMonthly(from, to);
        return SuccessPayload.of(result);
    }
}
