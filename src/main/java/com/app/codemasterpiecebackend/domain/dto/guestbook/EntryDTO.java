package com.app.codemasterpiecebackend.domain.dto.guestbook;

import com.app.codemasterpiecebackend.domain.types.ActorProvider;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntryDTO {
    private String entryId;
    private String actorId;
    private ActorProvider provider;
    private String profileImage;
    private String nickname;
    private String content;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean author;
}
