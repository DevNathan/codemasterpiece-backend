package com.app.codemasterpiecebackend.service.post.cmd;

import com.app.codemasterpiecebackend.util.Stringx;

import java.util.List;
import java.util.Objects;

import static com.app.codemasterpiecebackend.util.Stringx.trimToNull;

public record PostUpdateCmd(
        String postId,
        String title,
        String headImageId,
        String headContent,
        List<String> tags,
        String categoryId,
        String mainContent,
        boolean published
) {
    public PostUpdateCmd(
            String postId,
            String title,
            String headImageId,
            String headContent,
            List<String> tags,
            String categoryId,
            String mainContent,
            boolean published
    ) {
        this.postId = trimToNull(postId);
        this.title = trimToNull(title);
        this.headImageId = trimToNull(headImageId);
        this.headContent = trimToNull(headContent);
        this.categoryId = trimToNull(categoryId);

        this.tags = (tags == null)
                ? List.of()
                : tags.stream()
                .map(Stringx::trimToNull)
                .filter(Objects::nonNull)
                .map(s -> s.toLowerCase(java.util.Locale.ROOT))
                .toList();

        this.mainContent = (mainContent == null) ? "" : mainContent;

        this.published = published;
    }
}
