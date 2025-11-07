package com.app.codemasterpiecebackend.domain.entity.file;

import com.app.codemasterpiecebackend.domain.entity.base.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "tbl_file")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class StoredFile extends BaseTimeEntity {
    @Id
    @Column(length = 29, name = "file_id", nullable = false, updatable = false)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 12, nullable = false)
    private FileStatus status;

    @Column(name = "storage_path", nullable = false, length = 512)
    private String storagePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", length = 16, nullable = false)
    private StorageType storageType;

    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "byte_size", nullable = false)
    private long byteSize;

    @Column(name = "content_type", length = 255)
    private String contentType;

    @Column(name = "ref_count", nullable = false)
    private int refCount;

    @Column(name = "deletable_at")
    private Instant deletableAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // === helper ===
    public void assignStorage(String storagePath, String storageKey, StorageType storageType) {
        this.storagePath = storagePath;
        this.storageKey = storageKey;
        this.storageType = storageType;
    }

    public void activate() {
        this.status = FileStatus.ACTIVE;
    }

    public void pending() {
        this.status = FileStatus.PENDING;
    }

    public void markDeletable() {
        this.status = FileStatus.DELETABLE;
        this.deletableAt = Instant.now();
    }

    public void markDeleted() {
        this.status = FileStatus.DELETED;
        this.deletedAt = Instant.now();
    }
}
