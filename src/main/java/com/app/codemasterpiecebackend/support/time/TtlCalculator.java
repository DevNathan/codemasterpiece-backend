package com.app.codemasterpiecebackend.support.time;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class TtlCalculator {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    public long secondsUntilNext3AM() {
        ZonedDateTime now = ZonedDateTime.now(KST);
        ZonedDateTime next3 = now.withHour(3).withMinute(0).withSecond(0).withNano(0);
        if (!now.isBefore(next3)) next3 = next3.plusDays(1);
        return Duration.between(now, next3).getSeconds();
    }

    public String todayStr() {
        return LocalDate.now(KST).format(DateTimeFormatter.BASIC_ISO_DATE);
    }
}
