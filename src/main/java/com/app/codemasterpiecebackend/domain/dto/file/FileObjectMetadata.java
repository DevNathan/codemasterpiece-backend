package com.app.codemasterpiecebackend.domain.dto.file;

import lombok.Builder;

import java.time.Instant;

/**
 * 파일 메타데이터 DTO
 * - size: -1 if unknown
 * - contentType: nullable
 * - etag: nullable (S3 등)
 * - checksumSha256: nullable
 * - createdAt / updatedAt: nullable
 * - storageType: "LOCAL" | "S3" | etc.
 */
@Builder
public record FileObjectMetadata(
        String key,
        long size,
        String contentType,
        String checksumSha256,
        Instant createdAt,
        Instant updatedAt,
        String storageType
) {
    public FileObjectMetadata {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be null or blank");
        }
        if (size == 0) {
            size = -1;
        }
    }
}
