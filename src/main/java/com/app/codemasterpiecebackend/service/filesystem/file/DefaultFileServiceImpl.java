package com.app.codemasterpiecebackend.service.filesystem.file;

import com.app.codemasterpiecebackend.domain.entity.file.FileStatus;
import com.app.codemasterpiecebackend.domain.entity.file.StorageType;
import com.app.codemasterpiecebackend.domain.entity.file.StoredFile;
import com.app.codemasterpiecebackend.domain.repository.file.StoredFileRepository;
import com.app.codemasterpiecebackend.infra.filesystem.io.IoManager;
import com.app.codemasterpiecebackend.infra.filesystem.io.cmd.PutCommand;
import com.app.codemasterpiecebackend.service.filesystem.file.cmd.StoreCmd;
import com.app.codemasterpiecebackend.service.filesystem.file.result.FileInfo;
import com.app.codemasterpiecebackend.service.filesystem.support.path.FilePathStrategy;
import com.app.codemasterpiecebackend.util.ULIDs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

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

        try (var in = cmd.content().get();
             var put = PutCommand.ofBytes(key, in.readAllBytes(), safeType(cmd.contentType()))) {
            ioManager.put(put);
        } catch (IOException e) {
            throw new RuntimeException("upload failed: " + key, e);
        }

        var sf = StoredFile.builder()
                .id(id)
                .status(FileStatus.ACTIVE)
                .storagePath(base.path())
                .storageKey(key)
                .storageType(StorageType.S3)
                .originalFilename(cmd.originalFilename())
                .byteSize(cmd.contentLength())
                .contentType(cmd.contentType())
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
        // 현재는 DB 메타로 반환. 필요 시 IoManager.head()로 보강.
        return storedFileRepository.findById(fileId).map(FileInfo::from);
    }

    private static String safeType(String ct) {
        return (ct == null || ct.isBlank()) ? "application/octet-stream" : ct;
    }
}
