package com.app.codemasterpiecebackend.domain.file.ref.application;

import com.app.codemasterpiecebackend.domain.file.ref.entity.FileOwnerType;
import com.app.codemasterpiecebackend.domain.file.ref.entity.FilePurpose;
import lombok.Builder;

import java.util.List;

import static com.app.codemasterpiecebackend.global.util.Stringx.trimToNull;

/**
 * 파일 참조(FileRef) 제어를 위한 외부 요청 커맨드를 모아둔 통합 레코드.
 */
public record FileRefCommand() {

    /**
     * 파일 참조 추가 명령.
     */
    @Builder
    public record Attach(
            String fileId,
            FileOwnerType ownerType,
            String ownerId,
            FilePurpose purpose,
            Integer sortOrder,      // null이면 자동 부여 (맨 뒤)
            String displayName      // 선택 사항
    ) {}

    /**
     * 파일 참조 해제 명령.
     */
    public record Detach(
            String ownerId,
            String fileId
    ) {
        public Detach(String ownerId, String fileId) {
            this.ownerId = trimToNull(ownerId);
            this.fileId = trimToNull(fileId);
        }
    }

    /**
     * 파일 참조 순서 재정렬 명령.
     */
    @Builder
    public record Reorder(
            FileOwnerType ownerType,
            String ownerId,
            FilePurpose purpose,
            List<String> refIdsInOrder // 이 순서대로 재정렬
    ) {}
}