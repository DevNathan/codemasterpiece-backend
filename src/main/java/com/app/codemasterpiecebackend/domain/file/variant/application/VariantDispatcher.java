package com.app.codemasterpiecebackend.domain.file.variant.application;

import com.app.codemasterpiecebackend.domain.file.variant.dto.VariantPreset;
import com.app.codemasterpiecebackend.global.infra.messaging.variant.VariantJob;
import com.app.codemasterpiecebackend.global.infra.messaging.variant.VariantProducer;
import com.app.codemasterpiecebackend.global.support.tx.TxHooks;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 파일 변환(Variant) 작업을 비동기 큐로 전달하는 중앙 디스패처입니다.
 * 트랜잭션 훅(TxHooks)을 내장하여, 호출자의 트랜잭션이 성공적으로 커밋된 이후에만 메시지를 발행합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VariantDispatcher {

    private final VariantProducer variantProducer;
    private final TxHooks txHooks;

    /**
     * 파일 변환 작업을 예약합니다. 트랜잭션이 커밋된 후 큐로 전송됩니다.
     *
     * @param fileId 원본 파일의 ID
     * @param preset 적용할 변환 프리셋
     */
    public void dispatch(String fileId, VariantPreset preset) {
        txHooks.afterCommit(() -> {
            log.debug("Dispatching variant job for fileId: {}, preset: {}", fileId, preset);
            variantProducer.publish(
                    VariantJob.builder()
                            .fileId(fileId)
                            .preset(preset.name())
                            .build()
            );
        });
    }
}