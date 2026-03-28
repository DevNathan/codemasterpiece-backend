package com.app.codemasterpiecebackend.domain.file.media.application;

import com.app.codemasterpiecebackend.domain.file.core.application.FileService;
import com.app.codemasterpiecebackend.domain.file.core.application.StoreCmd;
import com.app.codemasterpiecebackend.domain.file.core.dto.FileInfo;
import com.app.codemasterpiecebackend.domain.file.variant.application.VariantDispatcher;
import com.app.codemasterpiecebackend.domain.file.variant.dto.VariantPreset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이미지 전용 미디어 업로드 및 변환 서비스.
 * 기반 규격인 MediaUploadUseCase를 상속받아 이미지 도메인에 맞는 구현을 제공한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ImageServiceImpl implements ImageService {

    private final FileService fileService;
    private final VariantDispatcher variantDispatcher;

    @Override
    public FileInfo upload(StoreCmd cmd) {
        // 1. 단순 스토리지 저장 및 DB 메타데이터 기록 (FileService 엔진 사용)
        FileInfo saved = fileService.store(cmd);

        // 2. 프로필 힌트를 프리셋으로 변환하여 비동기 처리 지시
        VariantPreset preset = resolvePreset(cmd.profileHint());
        variantDispatcher.dispatch(saved.fileId(), preset);

        return saved;
    }

    @Override
    public void process(String fileId) {
        // 기존 파일에 대한 변환 작업 수동 트리거 (또는 재처리)
        // 멱등성은 워커 단에서 보장되므로 주저 없이 큐로 던진다.
        variantDispatcher.dispatch(fileId, VariantPreset.BLOG_DEFAULT);
    }

    /**
     * 클라이언트가 전달한 힌트 문자열을 안전하게 Enum으로 변환한다.
     * 매칭되지 않으면 DEFAULT로 강제 폴백(Fallback) 처리한다.
     */
    private VariantPreset resolvePreset(String hint) {
        if (hint == null || hint.isBlank()) {
            return VariantPreset.DEFAULT;
        }
        try {
            return VariantPreset.valueOf(hint.toUpperCase());
        } catch (IllegalArgumentException e) {
            return VariantPreset.DEFAULT;
        }
    }
}