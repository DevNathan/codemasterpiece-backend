package com.app.codemasterpiecebackend.service.post.cmd;

import com.app.codemasterpiecebackend.util.Stringx;

import java.util.List;
import java.util.Objects;

import static com.app.codemasterpiecebackend.util.Stringx.trimToNull;

public record PostCreateCmd(
        String title,
        String headImageId,
        String headContent,
        List<String> tags,
        String categoryId,
        String mainContent,
        boolean published
) {
    public PostCreateCmd(
            String title,
            String headImageId,
            String headContent,
            List<String> tags,
            String categoryId,
            String mainContent,
            boolean published
    ) {
        this.title = trimToNull(title);
        this.headImageId = trimToNull(headImageId);
        this.headContent = trimToNull(headContent);
        this.categoryId = trimToNull(categoryId);
        this.mainContent = trimToNull(mainContent);
        this.published = published;

        this.tags = (tags == null) ? null :
                tags.stream()
                        .map(Stringx::trimToNull)
                        .filter(Objects::nonNull)
                        .map(String::toLowerCase)
                        .toList();
    }
}
