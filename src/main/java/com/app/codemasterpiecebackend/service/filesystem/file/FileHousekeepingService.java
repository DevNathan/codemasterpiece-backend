package com.app.codemasterpiecebackend.service.filesystem.file;

import com.app.codemasterpiecebackend.domain.entity.file.FileStatus;
import com.app.codemasterpiecebackend.domain.repository.file.StoredFileRepository;
import com.app.codemasterpiecebackend.infra.filesystem.io.IoManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileHousekeepingService {

    private final StoredFileRepository storedFiles;
    private final IoManager io;

    // 배치 크기 (퍼지 시 한 번에 지울 후보 수)
    private static final int PURGE_BATCH = 300;

    @Transactional
    public int sweepMarkDeletable() {
        var now = Instant.now();
        int updated = storedFiles.markDeletableBatch(
                FileStatus.ACTIVE, FileStatus.DELETABLE, now
        );
        if (updated > 0) {
            log.info("Sweep: marked {} files as DELETABLE", updated);
        }
        return updated;
    }

    @Transactional // 트랜잭션 경계 내에서 루프 돌되, I/O는 try-catch로 보호
    public int purgeExpiredDeletables(Duration grace) {
        var cutoff = Instant.now().minus(grace);
        int total = 0;

        while (true) {
            var rows = storedFiles.findByStatusAndDeletableAtBefore(
                    FileStatus.DELETABLE, cutoff, PageRequest.of(0, PURGE_BATCH)
            );
            if (rows.isEmpty()) break;

            // 1) 스토리지에서 실제 삭제 (prefix 단위)
            List<String> ids = new ArrayList<>(rows.size());
            for (var r : rows) {
                var prefix = ensureTrailingSlash(r.storagePath());
                try {
                    int deleted = io.deletePrefix(prefix);
                    log.debug("Purge: prefix={} deletedObjects={}", prefix, deleted);
                    ids.add(r.id());
                } catch (Exception e) {
                    // 실패해도 배치 진행. 다음 라운드에서 다시 시도 가능.
                    log.warn("Purge failed for prefix={}", prefix, e);
                }
            }

            if (!ids.isEmpty()) {
                // 2) 상태 전이: DELETED
                int changed = storedFiles.bulkMarkDeleted(
                        FileStatus.DELETABLE, FileStatus.DELETED, Instant.now(), ids
                );
                total += changed;
                log.info("Purge: marked {} files as DELETED", changed);
            }

            if (rows.size() < PURGE_BATCH) break;
        }
        return total;
    }

    private static String ensureTrailingSlash(String p) {
        if (p == null || p.isBlank()) return "/";
        return p.endsWith("/") ? p : (p + "/");
    }
}
