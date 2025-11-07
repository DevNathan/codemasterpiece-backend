package com.app.codemasterpiecebackend.domain.dto.post;

import lombok.Builder;

@Builder
public record PostUpdateResultDTO(
        String postId,
        String slug
) {}
