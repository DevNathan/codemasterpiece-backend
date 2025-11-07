package com.app.codemasterpiecebackend.service.filesystem.ref;

import com.app.codemasterpiecebackend.domain.entity.file.FileOwnerType;
import com.app.codemasterpiecebackend.domain.entity.file.FilePurpose;
import com.app.codemasterpiecebackend.service.filesystem.ref.cmd.AttachCmd;
import com.app.codemasterpiecebackend.service.filesystem.ref.cmd.DetachCmd;
import com.app.codemasterpiecebackend.service.filesystem.ref.cmd.ReorderCmd;
import lombok.Builder;

import java.util.List;

public interface FileRefService {
    void attach(AttachCmd cmd);

    void detach(DetachCmd cmd);

    void reorder(ReorderCmd cmd);

    void detachAll(FileOwnerType ownerType, String ownerId);
    void detachAll(FileOwnerType ownerType, String ownerId, FilePurpose purpose);

    /**
     * 주어진 (ownerType, ownerId, purpose)의 파일 참조를
     * orderedFileIds와 '정확히 동일한 상태'로 원자적 교체한다.
     * - 누락된 것은 삭제
     * - 새로운 것은 생성
     * - 정렬은 orderedFileIds 순서대로 1..N 보장
     * - idempotent
     *
     * @return 교체 이후의 참조들(정렬 보장)
     */
    ReplaceAllResult replaceAll(ReplaceAllCmd cmd);

    /**
     * 읽기 전용. 현재 상태를 정렬 순서대로 반환한다.
     * (디버깅/관리용; 업무 로직은 replaceAll만 사용해도 충분)
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
            List<FileRefEntry> entriesInOrder // 최종 상태(정렬된) 반환
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

