package com.app.codemasterpiecebackend.domain.file.variant.application;

import com.app.codemasterpiecebackend.domain.file.core.repository.StoredFileRepository;
import com.app.codemasterpiecebackend.global.infra.messaging.variant.VariantJob;
import com.app.codemasterpiecebackend.domain.file.variant.processor.VariantProcessor;
import com.app.codemasterpiecebackend.domain.file.variant.dto.VariantProcessCmd;
import com.app.codemasterpiecebackend.domain.file.variant.dto.VariantPreset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 비동기 메시지 큐로부터 파일 변환 작업(Variant Job)을 수신하여 처리하는 워커(Worker) 클래스입니다.
 *
 * <p>본 클래스는 트랜잭션이 분리된 비동기 환경에서 동작하며, 다음의 책임을 수행합니다:</p>
 * <ul>
 * <li>수신된 작업의 대상 원본 파일 메타데이터 조회</li>
 * <li>요청된 프리셋(Preset)을 기반으로 현재 미디어 타입에 맞는 변환 타겟(Target) 산출</li>
 * <li>파일의 콘텐츠 타입(MIME)을 지원하는 적절한 {@link VariantProcessor} 탐색 및 실행 위임</li>
 * </ul>
 * <p>이 클래스 자체는 물리적인 변환 로직을 포함하지 않으며, 작업의 흐름을 제어하는 라우터 역할을 담당합니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VariantWorker {

    private final StoredFileRepository files;

    /** * 시스템에 등록된 모든 도메인별 변환 프로세서(Image, Video 등)
     */
    private final List<VariantProcessor> processors;

    /**
     * 큐로부터 전달받은 변환 작업(Job)을 실행합니다.
     *
     * @param job 변환 대상 파일 ID와 프리셋 정보를 포함하는 작업 명세 객체
     * @throws IllegalArgumentException 대상 파일을 데이터베이스에서 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public void handle(VariantJob job) {
        var original = files.findById(job.fileId())
                .orElseThrow(() -> new IllegalArgumentException("file not found: " + job.fileId()));

        List<VariantProcessCmd.Target> targets = resolveTargets(job, original.getContentType());

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

        // 선택된 프로세서에게 실제 변환 작업 위임
        processor.process(
                original,
                VariantProcessCmd.builder().targets(targets).build()
        );
    }

    /**
     * 작업 명세서의 요청에 따라 최종적으로 생성해야 할 변환 타겟 목록을 산출합니다.
     * 명시적인 타겟 목록이 우선하며, 없을 경우 프리셋 설정을 따릅니다.
     *
     * @param job 수신된 작업 명세 객체
     * @param contentType 원본 파일의 MIME 타입
     * @return 생성해야 할 변환 타겟 목록
     */
    private List<VariantProcessCmd.Target> resolveTargets(VariantJob job, String contentType) {
        if (job.targets() != null && !job.targets().isEmpty()) {
            return job.targets();
        }
        if (job.preset() != null && !job.preset().isBlank()) {
            return VariantPreset.valueOf(job.preset()).resolveTargets(contentType);
        }
        return List.of();
    }
}