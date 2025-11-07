package com.app.codemasterpiecebackend.service.filesystem.support.path;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 기본 구현:
 * yyyy/MM/dd/ULID/ 형태 경로를 생성.
 * ULID/시간은 주입된 Clock 기반으로 결정(테스트 용이).
 */
@Component
@RequiredArgsConstructor
public class UlidPathStrategy implements FilePathStrategy {
    private final Clock clock;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneId.of("UTC"));

    @Override
    public BasePath allocateFor(String ulid, Instant createdAt) {
        Instant ts = (createdAt != null) ? createdAt : clock.instant();
        String path = DATE_FMT.format(ts) + "/" + ulid + "/";
        return new BasePath(path, ulid, ts);
    }

    @Override
    public BasePath of(String path, String ulid, Instant timestamp) {
        if (path == null || path.isBlank()) throw new IllegalArgumentException("path blank");
        if (!path.endsWith("/")) path = path + "/";
        return new BasePath(path, ulid, timestamp != null ? timestamp : clock.instant());
    }
}
