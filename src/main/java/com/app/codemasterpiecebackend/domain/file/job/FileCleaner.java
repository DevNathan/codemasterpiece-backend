package com.app.codemasterpiecebackend.domain.file.core.job;

import com.app.codemasterpiecebackend.domain.file.core.application.FileHousekeepingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 파일 시스템의 무결성을 유지하기 위해 참조되지 않는 파일을 주기적으로 정리하는 스케줄러입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileCleaner {

    private final FileHousekeepingService housekeeping;

    /**
     * 매일 새벽 3시(KST)에 가비지 컬렉션을 실행합니다.
     * 1. 참조 카운트가 0인 파일을 DELETABLE로 마킹
     * 2. 마킹 후 24시간이 경과한 파일을 물리적 삭제
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void nightlyCleanup() {
        log.info("Starting scheduled file system cleanup...");

        try {
            int marked = housekeeping.sweepMarkDeletable();
            int purged = housekeeping.purgeExpiredDeletables(Duration.ofHours(24));

            log.info("Nightly cleanup finished. [Marked Deletable: {}], [Physically Purged: {}]", marked, purged);
        } catch (Exception e) {
            log.error("Critical error during nightly file cleanup", e);
        }
    }
}