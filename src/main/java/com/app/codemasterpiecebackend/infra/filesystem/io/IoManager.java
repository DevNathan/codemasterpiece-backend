package com.app.codemasterpiecebackend.infra.filesystem.io;

import com.app.codemasterpiecebackend.domain.dto.file.FileObjectMetadata;
import com.app.codemasterpiecebackend.infra.filesystem.io.cmd.PutCommand;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

public interface IoManager {
    // 기본 I/O
    FileObjectMetadata put(PutCommand cmd) throws IOException;
    FileObjectMetadata move(String srcKey, String dstKey) throws IOException;
    boolean delete(String key) throws IOException;
    Optional<FileObjectMetadata> head(String key) throws IOException;
    InputStream openStream(String key) throws IOException;

    // 추가: Range 읽기
    default InputStream openRange(String key, long startInclusive, long endExclusive) throws IOException {
        throw new UnsupportedOperationException("Range read not supported");
    }

    // 추가: 프리사인 URL
    String presignRead(String key, Duration ttl) throws IOException;
    PresignWriteResult presignWrite(String key, Duration ttl, String contentType, long maxSize) throws IOException;

    // 추가: 접두어 일괄 삭제(variants 정리용)
    int deletePrefix(String prefix) throws IOException;

    record PresignWriteResult(String url, Map<String, String> headersOrFormFields) {}
}
