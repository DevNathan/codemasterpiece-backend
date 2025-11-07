package com.app.codemasterpiecebackend.infra.messaging.variant;

import com.app.codemasterpiecebackend.service.filesystem.variant.worker.VariantWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VariantProducer {
    private final TaskExecutor variantExecutor;
    private final VariantWorker worker;

    public void publish(VariantJob job) {
        variantExecutor.execute(() -> {
            try {
                worker.handle(job); // 비동기 처리
            } catch (Exception e) {
                // 실패는 버린다(요구사항). 로그만 남김.
                log.warn("Variant processing failed. fileId={}, preset={}", job.fileId(), job.preset(), e);
            }
        });
    }
}
