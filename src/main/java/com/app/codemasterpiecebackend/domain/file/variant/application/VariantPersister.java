package com.app.codemasterpiecebackend.domain.file.variant.application;

import com.app.codemasterpiecebackend.domain.file.core.entity.FileStatus;
import com.app.codemasterpiecebackend.domain.file.variant.entity.FileVariant;
import com.app.codemasterpiecebackend.domain.file.variant.entity.FileVariantKind;
import com.app.codemasterpiecebackend.domain.file.core.entity.StoredFile;
import com.app.codemasterpiecebackend.domain.file.variant.repository.FileVariantRepository;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 생성된 파일 파생 자산(Variant)의 메타데이터를 데이터베이스에 영속화하는 전용 컴포넌트입니다.
 *
 * <p>이 클래스는 비동기 워커의 처리 흐름 내에서 개별 Variant의 저장 상태를 격리하기 위해 사용됩니다.
 * 다중 타겟 변환 작업 중 일부가 실패하더라도, 이미 성공한 변환 결과의 커밋은 보장되어야 합니다.</p>
 */
@Service
@RequiredArgsConstructor
public class VariantPersister {

    private final FileVariantRepository variantRepo;

    /**
     * 개별 변환 결과를 독립된 새로운 트랜잭션(REQUIRES_NEW)으로 데이터베이스에 기록합니다.
     *
     * <p>호출자(Caller)의 트랜잭션 상태와 무관하게 즉시 커밋을 시도하며,
     * 다중 워커 환경에서 발생할 수 있는 동시 삽입(Race Condition)에 의한
     * 중복 키 예외(DataIntegrityViolationException)를 내부적으로 삼켜 멱등성(Idempotency)을 보장합니다.</p>
     *
     * @param original 변환의 기준이 된 원본 파일 엔티티
     * @param kind 생성된 파생 자산의 종류 (예: WEBP, THUMB_512 등)
     * @param mime 생성된 파일의 MIME 타입
     * @param storageKey 스토리지에 실제 저장된 객체의 키 경로
     * @param width 이미지의 가로 픽셀 크기 (해당하지 않는 경우 null)
     * @param height 이미지의 세로 픽셀 크기 (해당하지 않는 경우 null)
     * @param byteSize 생성된 파일의 실제 바이트 크기
     */
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
        // 1차 방어선: 애플리케이션 레벨의 중복 검사
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
            // 2차 방어선: 데이터베이스 유니크 제약 조건을 활용한 멱등성 보장
            variantRepo.saveAndFlush(v);
        } catch (DataIntegrityViolationException ignore) {
            // 다른 워커나 스레드가 찰나의 순간에 먼저 데이터를 넣었을 경우, 예외를 무시하고 정상 처리로 간주합니다.
        }
    }
}