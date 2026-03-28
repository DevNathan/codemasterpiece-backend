package com.app.codemasterpiecebackend.domain.guestbook.application;

import com.app.codemasterpiecebackend.domain.guestbook.dto.EntryDTO;
import com.app.codemasterpiecebackend.domain.guestbook.entity.GuestbookEntry;

public final class GuestbookDTOMapper {

    private GuestbookDTOMapper() {
    }

    /**
     * 방명록 엔티티를 DTO로 변환합니다.
     *
     * @param e          변환할 방명록 엔티티
     * @param myGithubId 작성자 본인 확인을 위한 GitHub ID
     * @return 변환된 EntryDTO
     */
    public static EntryDTO toDto(GuestbookEntry e, String myGithubId) {
        return new EntryDTO(
                e.getId(),
                e.getActorId(),
                e.getActorProvider(),
                e.getActorSnapshot() != null ? e.getActorSnapshot().getImageUrl() : null,
                e.getActorSnapshot() != null ? e.getActorSnapshot().getDisplayName() : null,
                e.getContent(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getActorId().equals("GITHUB" + myGithubId)
        );
    }
}