package com.app.codemasterpiecebackend.domain.file.core.dto;

import com.app.codemasterpiecebackend.domain.file.core.entity.FileStatus;
import com.app.codemasterpiecebackend.domain.file.core.entity.StorageType;
import com.app.codemasterpiecebackend.domain.file.core.entity.StoredFile;

public record FileInfo(
        String fileId,
        String storagePath,
        String storageKey,
        StorageType storageType,
        String originalFilename,
        long byteSize,
        String contentType,
        FileStatus status
) {
    public static FileInfo from(StoredFile f) {
        return new FileInfo(
                f.getId(),
                f.getStoragePath(),
                f.getStorageKey(),
                f.getStorageType(),
                f.getOriginalFilename(),
                f.getByteSize(),
                f.getContentType(),
                f.getStatus()
        );
    }
}
