// src/main/java/.../variant/image/VariantPersister.java
package com.app.codemasterpiecebackend.service.filesystem.variant;

import com.app.codemasterpiecebackend.domain.entity.file.FileStatus;
import com.app.codemasterpiecebackend.domain.entity.file.FileVariant;
import com.app.codemasterpiecebackend.domain.entity.file.FileVariantKind;
import com.app.codemasterpiecebackend.domain.entity.file.StoredFile;
import com.app.codemasterpiecebackend.domain.repository.file.FileVariantRepository;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VariantPersister {

    private final FileVariantRepository variantRepo;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveInNewTx(
            StoredFile original,
            FileVariantKind kind,
            String mime,
            String storageKey,
            @Nullable Integer width,
            @Nullable Integer height,
            int byteSize
    ) {
        if (variantRepo.existsByOriginal_idAndKind(original.getId(), kind)) return;

        var v = FileVariant.builder()
                .original(original)
                .kind(kind)
                .storageType(original.getStorageType())
                .status(FileStatus.ACTIVE)
                .storageKey(storageKey)
                .contentType(mime)
                .width(width)
                .height(height)
                .byteSize(byteSize)
                .build();

        try {
            variantRepo.saveAndFlush(v);
        } catch (DataIntegrityViolationException ignore) {
            // 레이스 시 멱등
        }
    }
}
