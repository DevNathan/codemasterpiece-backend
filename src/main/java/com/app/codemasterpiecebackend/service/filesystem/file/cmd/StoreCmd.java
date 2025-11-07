package com.app.codemasterpiecebackend.service.filesystem.file.cmd;

import com.app.codemasterpiecebackend.domain.entity.file.StorageType;
import lombok.Builder;

import java.io.InputStream;
import java.util.function.Supplier;

@Builder
public record StoreCmd(
        String originalFilename,
        String contentType,
        long contentLength,
        Supplier<InputStream> content,
        StorageType storageType,
        String profileHint,
        String logicalPath
) {
}
