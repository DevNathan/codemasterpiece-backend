package com.app.codemasterpiecebackend.domain.repository.file;

import com.app.codemasterpiecebackend.domain.entity.file.FileStatus;
import com.app.codemasterpiecebackend.domain.entity.file.StoredFile;
import com.app.codemasterpiecebackend.service.filesystem.file.FilePurgeRow;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface StoredFileRepository extends JpaRepository<StoredFile, String> {

    @Modifying(clearAutomatically = false, flushAutomatically = false)
    @Query("update StoredFile f set f.refCount = f.refCount + 1 where f.id = :id")
    int incRef(@Param("id") String fileId);

    @Modifying(clearAutomatically = false, flushAutomatically = false)
    @Query("update StoredFile f set f.refCount = case when f.refCount > 0 then f.refCount - 1 else 0 end where f.id = :id")
    int decRef(@Param("id") String fileId);


    // ACTIVE && ref_count=0 → DELETABLE (행 수만 리턴)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE StoredFile f
            SET f.status = :to,
                f.deletableAt = :now
            WHERE f.status = :from
              AND f.refCount = 0
              AND (f.deletableAt IS NULL)
            """)
    int markDeletableBatch(@Param("from") FileStatus from,
                           @Param("to") FileStatus to,
                           @Param("now") Instant now);

    List<FilePurgeRow> findByStatusAndDeletableAtBefore(
            FileStatus status, Instant cutoff, Pageable pageable);

    // 3) 상태 일괄 전이: DELETED
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE StoredFile f
            SET f.status = :to,
                f.deletedAt = :now
            WHERE f.status = :from
              AND f.id IN (:ids)
            """)
    int bulkMarkDeleted(@Param("from") FileStatus from,
                        @Param("to") FileStatus to,
                        @Param("now") Instant now,
                        @Param("ids") Collection<String> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from StoredFile f where f.id in :ids")
    List<StoredFile> findAllForUpdate(@Param("ids") Collection<String> ids);
}
