package com.app.codemasterpiecebackend.domain.category.dto;

import java.time.Instant;

public record CategorySitemapLinkDTO(
        String link,
        Instant lastModified
) {
}
