package com.app.codemasterpiecebackend.domain.file.ref.repository;

import com.app.codemasterpiecebackend.domain.file.ref.entity.FileOwnerType;
import com.app.codemasterpiecebackend.domain.file.ref.entity.FilePurpose;
import com.app.codemasterpiecebackend.domain.file.ref.entity.FileRef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FileRefRepository extends JpaRepository<FileRef, String> {

    boolean existsByStoredFileIdAndOwnerTypeAndOwnerIdAndPurpose(
            String fileId, FileOwnerType ownerType, String ownerId, FilePurpose purpose);

    @Query("""
            select coalesce(max(fr.sortOrder), 0)
            from FileRef fr
            where fr.ownerType = :ownerType and fr.ownerId = :ownerId and fr.purpose = :purpose
            """)
    Integer findMaxSort(FileOwnerType ownerType, String ownerId, FilePurpose purpose);

    List<FileRef> findByOwnerTypeAndOwnerIdAndPurposeOrderBySortOrderAsc(
            FileOwnerType ownerType, String ownerId, FilePurpose purpose);

    Optional<FileRef> findByOwnerIdAndStoredFileId(String ownerId, String fileId);

    List<FileRef> findByOwnerTypeAndOwnerId(FileOwnerType ownerType, String ownerId);
}
