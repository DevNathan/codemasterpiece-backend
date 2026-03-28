package com.app.codemasterpiecebackend.domain.file.ref.application;

import com.app.codemasterpiecebackend.domain.file.ref.entity.FileOwnerType;
import com.app.codemasterpiecebackend.domain.file.ref.entity.FilePurpose;
import lombok.Builder;

import java.util.List;

/**
 * 시스템 내 파일의 논리적 참조(Reference) 생명주기를 관리하는 서비스입니다.
 * * <p>게시글, 카테고리 등 도메인 엔티티가 파일을 참조하거나 해제할 때 호출되며,
 * 내부적으로 StoredFile의 참조 카운트(ref_count)를 조절하여 가비지 컬렉터(GC)의
 * 타겟을 결정하는 핵심 통제소 역할을 합니다.</p>
 */
public interface FileRefService {

    /**
     * 파일 참조를 추가하고 대상 파일의 참조 카운트를 1 증가시킵니다. (멱등성 보장)
     */
    void attach(FileRefCommand.Attach cmd);

    /**
     * 파일 참조를 해제하고 대상 파일의 참조 카운트를 1 감소시킵니다.
     */
    void detach(FileRefCommand.Detach cmd);

    /**
     * 특정 소유자(Owner)가 가진 파일 참조들의 정렬 순서를 변경합니다.
     */
    void reorder(FileRefCommand.Reorder cmd);

    /**
     * 특정 소유자의 모든 파일 참조를 해제하고 연관된 파일들의 참조 카운트를 감소시킵니다.
     */
    void detachAll(FileOwnerType ownerType, String ownerId);

    /**
     * 특정 소유자 및 목적(Purpose)에 해당하는 모든 파일 참조를 해제합니다.
     */
    void detachAll(FileOwnerType ownerType, String ownerId, FilePurpose purpose);

    /**
     * 주어진 (ownerType, ownerId, purpose) 조건의 파일 참조를
     * 제공된 orderedFileIds와 정확히 동일한 상태로 원자적 교체(Sync)합니다.
     * * <p>불필요한 쿼리를 최소화하기 위해 삽입, 삭제, 정렬 순서 업데이트를
     * 일괄(Batch) 처리로 수행합니다.</p>
     *
     * @param cmd 교체할 대상 및 파일 ID 목록
     * @return 정렬이 보장된 최종 참조 상태
     */
    ReplaceAllResult replaceAll(ReplaceAllCmd cmd);

    /**
     * 현재 참조 상태를 정렬 순서대로 반환합니다. (읽기 전용)
     */
    List<FileRefEntry> list(ListQuery q);

    record ReplaceAllCmd(
            FileOwnerType ownerType,
            String ownerId,
            FilePurpose purpose,
            List<String> orderedFileIds,
            String displayNamePrefix
    ) {
    }

    record ReplaceAllResult(
            List<FileRefEntry> entriesInOrder
    ) {
    }

    record FileRefEntry(
            String refId,
            String fileId,
            Integer sortOrder
    ) {
    }

    @Builder
    record ListQuery(
            FileOwnerType ownerType,
            String ownerId,
            FilePurpose purpose
    ) {
    }
}