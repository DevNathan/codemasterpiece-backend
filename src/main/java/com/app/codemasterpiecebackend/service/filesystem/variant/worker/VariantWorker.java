package com.app.codemasterpiecebackend.service.filesystem.variant.worker;

import com.app.codemasterpiecebackend.domain.repository.file.StoredFileRepository;
import com.app.codemasterpiecebackend.infra.messaging.variant.VariantJob;
import com.app.codemasterpiecebackend.service.filesystem.variant.VariantProcessor;
import com.app.codemasterpiecebackend.service.filesystem.variant.cmd.VariantProcessCmd;
import com.app.codemasterpiecebackend.service.filesystem.variant.preset.VariantPresetResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VariantWorker {

    private final StoredFileRepository files;
    private final VariantPresetResolver presets;

    // 핵심: 모든 VariantProcessor를 주입
    private final List<VariantProcessor> processors;

    @Transactional(readOnly = true)
    public void handle(VariantJob job) {
        var original = files.findById(job.fileId())
                .orElseThrow(() -> new IllegalArgumentException("file not found: " + job.fileId()));

        // 컨텐츠 타입 인지하는 프리셋(선택사항)
        var targets = (job.targets() != null && !job.targets().isEmpty())
                ? job.targets()
                : presets.resolve(job.preset(), original.getContentType());

        if (targets == null || targets.isEmpty()) {
            log.debug("No targets resolved. fileId={}, preset={}", job.fileId(), job.preset());
            return;
        }

        var processor = processors.stream()
                .filter(p -> p.supports(original.getContentType()))
                .findFirst()
                .orElse(null);

        if (processor == null) {
            log.debug("No processor supports contentType={}, fileId={}",
                    original.getContentType(), original.getId());
            return;
        }

        // 변환 실행
        processor.process(
                original,
                VariantProcessCmd.builder().targets(targets).build()
        );
    }
}
