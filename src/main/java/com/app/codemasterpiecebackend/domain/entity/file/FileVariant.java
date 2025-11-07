package com.app.codemasterpiecebackend.domain.entity.file;

import com.app.codemasterpiecebackend.config.jpa.PrefixedUlidId;
import com.app.codemasterpiecebackend.domain.entity.base.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * FileVariant — 원본 파일(StoredFile)의 파생 자산
 * 예: 썸네일(THUMB_256, THUMB_512), webp, pdf-preview 등
 * <p>
 * - 원본 파일 삭제 시 ON DELETE CASCADE 로 함께 제거
 * - 상태는 원본과 동일하게 ACTIVE / DELETABLE / DELETED 로 관리
 */
@Entity
@Table(name = "tbl_file_variant")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class FileVariant extends BaseTimeEntity {

    /**
     * 변형 파일 식별자 ("FV-xxxxxxxx...")
     */
    @Id
    @PrefixedUlidId("FV")
    @Column(name = "variant_id", length = 29, nullable = false, updatable = false)
    private String id;

    /**
     * 원본 파일
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_variant_file"))
    private StoredFile original;

    /**
     * 변형 종류 (THUMB_256, THUMB_512, WEBP, AVIF, PDF_PREVIEW 등)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "kind", length = 32, nullable = false)
    private FileVariantKind kind;

    /**
     * 저장소 타입 (LOCAL, S3 등)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", length = 16, nullable = false)
    private StorageType storageType;

    /**
     * 파일 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 12, nullable = false)
    private FileStatus status;

    /**
     * 실제 스토리지 키
     */
    @Column(name = "storage_key", length = 512, nullable = false)
    private String storageKey;

    /**
     * 포맷 / MIME 타입 (image/webp, image/png 등)
     */
    @Column(name = "content_type", length = 255, nullable = false)
    private String contentType;

    /**
     * 가로/세로 (nullable — 영상/문서 썸네일 등은 존재 안할 수 있음)
     */
    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    /**
     * 파일 크기(byte)
     */
    @Column(name = "byte_size", nullable = false)
    private long byteSize;

    // === helper ===
    public void markActive() {
        this.status = FileStatus.ACTIVE;
    }

    public void markDeletable() {
        this.status = FileStatus.DELETABLE;
    }

    public void markDeleted() {
        this.status = FileStatus.DELETED;
    }
}
