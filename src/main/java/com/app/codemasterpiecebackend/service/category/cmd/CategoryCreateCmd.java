package com.app.codemasterpiecebackend.service.category.cmd;

import com.app.codemasterpiecebackend.domain.entity.category.CategoryType;
import org.springframework.lang.Nullable;
import org.springframework.web.multipart.MultipartFile;

import static com.app.codemasterpiecebackend.util.Stringx.trimToNull;

public record CategoryCreateCmd(
        String name,
        CategoryType type,
        @Nullable String parentId,
        @Nullable String link,
        @Nullable MultipartFile image
) {
    public CategoryCreateCmd(String name, CategoryType type, @Nullable String parentId, @Nullable String link, @Nullable MultipartFile image) {
        this.name = trimToNull(name);
        this.type = type;
        this.parentId = trimToNull(parentId);

        String cleanedLink = trimToNull(link);
        this.link = cleanedLink != null ? cleanedLink.toLowerCase() : null;

        this.image = image;
    }
}
