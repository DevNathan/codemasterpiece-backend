package com.app.codemasterpiecebackend.cron;

import com.app.codemasterpiecebackend.service.filesystem.file.FileHousekeepingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileCleaner {

    private final FileHousekeepingService housekeeping;

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void nightlyCleanup() {
        int marked = housekeeping.sweepMarkDeletable();

        int purged = housekeeping.purgeExpiredDeletables(Duration.ofHours(24));

        log.info("Nightly cleanup done. markedDeletable={}, purgedDeleted={}", marked, purged);
    }
}
