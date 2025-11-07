package com.app.codemasterpiecebackend.service.filesystem.file.result;

import com.app.codemasterpiecebackend.domain.entity.file.FileStatus;
import com.app.codemasterpiecebackend.domain.entity.file.StorageType;
import com.app.codemasterpiecebackend.domain.entity.file.StoredFile;

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
