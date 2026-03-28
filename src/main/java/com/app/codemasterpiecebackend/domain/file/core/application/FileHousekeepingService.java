package com.app.codemasterpiecebackend.domain.file.core.application;

import com.app.codemasterpiecebackend.domain.file.core.entity.FileStatus;
import com.app.codemasterpiecebackend.domain.file.core.repository.StoredFileRepository;
import com.app.codemasterpiecebackend.global.infra.filesystem.io.IoManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 참조가 끊어진 고아 파일(Orphan Files)의 상태를 변경하고 물리적 스토리지를 정리하는 가비지 컬렉터 서비스입니다.
 * * <p>외부 네트워크 I/O(S3 연동) 대기 시간 동안 데이터베이스 커넥션 풀이 고갈되는 현상을 원천 차단하기 위해,
 * 물리 삭제 배치 로직에서는 클래스 및 메서드 레벨의 글로벌 트랜잭션을 엄격히 배제하고
 * 리포지토리(Repository) 단일 쿼리 레벨의 트랜잭션만 활용합니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileHousekeepingService {

    private final StoredFileRepository storedFiles;
    private final IoManager io;

    private static final int PURGE_BATCH = 300;

    /**
     * 참조 카운트(ref_count)가 0인 ACTIVE 상태의 파일들을 찾아 DELETABLE 상태로 일괄 전이시킵니다.
     * 이 작업은 단순 DB 업데이트이므로 트랜잭션 내에서 안전하게 수행됩니다.
     *
     * @return 상태가 변경된 파일의 총 개수
     */
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

    /**
     * 유예 기간이 지난 DELETABLE 파일들을 찾아 스토리지에서 물리적으로 삭제하고 DELETED 상태로 전이시킵니다.
     *
     * @param grace 유예 기간 (이 기간이 지난 파일만 삭제 대상이 됨)
     * @return 물리적 삭제 및 상태 변경이 완료된 파일의 총 개수
     */
    public int purgeExpiredDeletables(Duration grace) {
        var cutoff = Instant.now().minus(grace);
        int total = 0;

        while (true) {
            // 1) 단순 읽기 (DB 락 없음)
            var rows = storedFiles.findByStatusAndDeletableAtBefore(
                    FileStatus.DELETABLE, cutoff, PageRequest.of(0, PURGE_BATCH)
            );
            if (rows.isEmpty()) break;

            List<String> ids = new ArrayList<>(rows.size());

            // 2) 외부 I/O (DB 커넥션을 물고 있지 않으므로 병목 발생 안 함)
            for (var r : rows) {
                var prefix = ensureTrailingSlash(r.storagePath());
                try {
                    int deleted = io.deletePrefix(prefix);
                    log.debug("Purge: prefix={} deletedObjects={}", prefix, deleted);
                    ids.add(r.id());
                } catch (Exception e) {
                    log.warn("Purge failed for prefix={}", prefix, e);
                }
            }

            // 3) 상태 전이 (Repository 내부의 짧은 트랜잭션만 사용)
            if (!ids.isEmpty()) {
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