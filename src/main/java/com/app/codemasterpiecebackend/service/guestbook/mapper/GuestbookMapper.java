package com.app.codemasterpiecebackend.service.guestbook.mapper;

import com.app.codemasterpiecebackend.domain.dto.guestbook.EntryDTO;
import com.app.codemasterpiecebackend.domain.entity.guestbook.GuestbookEntry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GuestbookMapper {
    @Value("${app.auth.author-github-id}")
    private String myGithubId;

    public EntryDTO toDto(GuestbookEntry e) {
        return new EntryDTO(
                e.getId(),
                e.getActorId(),
                e.getActorProvider(),
                e.getActorSnapshot() != null ? e.getActorSnapshot().getImageUrl() : null,
                e.getActorSnapshot() != null ? e.getActorSnapshot().getDisplayName() : null,
                e.getContent(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getActorId().equals(myGithubId)
        );
    }
}