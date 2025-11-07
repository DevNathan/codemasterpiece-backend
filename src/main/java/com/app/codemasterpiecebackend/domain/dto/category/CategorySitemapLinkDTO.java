package com.app.codemasterpiecebackend.domain.dto.category;

import java.time.Instant;

public record CategorySitemapLinkDTO(
        String link,
        Instant lastModified
) {
}
