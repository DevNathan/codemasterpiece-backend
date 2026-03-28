package com.app.codemasterpiecebackend.domain.file.core.application;

import com.app.codemasterpiecebackend.domain.file.core.entity.FileStatus;
import com.app.codemasterpiecebackend.domain.file.core.entity.StorageType;
import com.app.codemasterpiecebackend.domain.file.core.entity.StoredFile;
import com.app.codemasterpiecebackend.domain.file.core.repository.StoredFileRepository;
import com.app.codemasterpiecebackend.global.infra.filesystem.io.IoManager;
import com.app.codemasterpiecebackend.domain.file.core.dto.FileInfo;
import com.app.codemasterpiecebackend.domain.file.core.support.FilePathStrategy;
import com.app.codemasterpiecebackend.global.util.ULIDs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Optional;

/**
 * 파일 업로드 및 메타데이터 생명주기를 관리하는 핵심 서비스 구현체.
 * 스트리밍 I/O는 IoManager에게 완벽히 위임하고, 본 서비스는 트랜잭션과 메타데이터만 통제한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultFileServiceImpl implements FileService {
    private final IoManager ioManager;
    private final StoredFileRepository storedFileRepository;
    private final FilePathStrategy filePathStrategy;

    @Override
    @Transactional
    public FileInfo store(StoreCmd cmd) {
        String id = ULIDs.newMonotonicUlid("FL");
        var base = filePathStrategy.allocateFor(id, Instant.now());
        String key = base.originalKey();

        try (InputStream in = cmd.content().get()) {
            ioManager.put(key, in, cmd.contentLength(), safeType(cmd.contentType()));
        } catch (IOException e) {
            // I/O 실패 시 트랜잭션 롤백 (StoredFile 저장 안 됨)
            throw new RuntimeException("Upload pipeline failed for key: " + key, e);
        }

        // I/O 성공 시에만 DB 메타데이터 확정
        var sf = StoredFile.builder()
                .id(id)
                .status(FileStatus.ACTIVE)
                .storagePath(base.path())
                .storageKey(key)
                .storageType(StorageType.S3)
                .originalFilename(cmd.originalFilename())
                .byteSize(cmd.contentLength())
                .contentType(safeType(cmd.contentType()))
                .refCount(0)
                .build();
        storedFileRepository.save(sf);

        return FileInfo.from(sf);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FileInfo> getFile(String fileId) {
        return storedFileRepository.findById(fileId).map(FileInfo::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FileInfo> getFileHeadMeta(String fileId) {
        return storedFileRepository.findById(fileId).map(FileInfo::from);
    }

    private static String safeType(String ct) {
        return (ct == null || ct.isBlank()) ? "application/octet-stream" : ct;
    }
}