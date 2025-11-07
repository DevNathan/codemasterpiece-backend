package com.app.codemasterpiecebackend.domain.dto.post;

import lombok.Builder;

import java.util.List;

@Builder
public record PostEditDTO(
        String id,
        String title,
        String categoryId,
        String headImage,
        String headImageUrl,
        String headContent,
        List<String> tags,
        String mainContent,
        boolean published
) {
}
