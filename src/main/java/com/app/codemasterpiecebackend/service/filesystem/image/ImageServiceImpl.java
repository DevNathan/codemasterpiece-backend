package com.app.codemasterpiecebackend.service.filesystem.image;

import com.app.codemasterpiecebackend.infra.messaging.variant.VariantJob;
import com.app.codemasterpiecebackend.infra.messaging.variant.VariantProducer;
import com.app.codemasterpiecebackend.service.filesystem.file.FileService;
import com.app.codemasterpiecebackend.service.filesystem.file.cmd.StoreCmd;
import com.app.codemasterpiecebackend.service.filesystem.file.result.FileInfo;
import com.app.codemasterpiecebackend.service.filesystem.support.upload.MediaKind;
import com.app.codemasterpiecebackend.service.filesystem.support.upload.UploadCmd;
import com.app.codemasterpiecebackend.service.filesystem.variant.cmd.VariantProcessCmd;
import com.app.codemasterpiecebackend.support.tx.TxHooks;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ImageServiceImpl implements ImageService {
    private final FileService fileService;
    private final VariantProducer variantProducer;
    private final TxHooks txHooks;

    @Override
    public FileInfo upload(UploadCmd cmd) {
        // 1) 원본 저장 (같은 트랜잭션)
        FileInfo saved = fileService.store(StoreCmd.builder()
                .originalFilename(cmd.originalFilename())
                .contentType(cmd.contentType())
                .contentLength(cmd.contentLength())
                .content(cmd.content())
                .build());

        var targets = List.of(
                VariantProcessCmd.Target.THUMB_512(),
                VariantProcessCmd.Target.THUMB_256(),
                VariantProcessCmd.Target.WEBP(),
                VariantProcessCmd.Target.AVIF()
        );

        // 2) 커밋 후에만 비동기 실행
        txHooks.afterCommit(() -> variantProducer.publish(
                VariantJob.builder().fileId(saved.fileId()).targets(targets).build()
        ));

        return saved;
    }
}

