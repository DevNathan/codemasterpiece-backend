package com.app.codemasterpiecebackend.domain.dto.post;

import java.time.Instant;

public record PostSitemapDTO(
        String slug,
        Instant updatedAt
) {
}
