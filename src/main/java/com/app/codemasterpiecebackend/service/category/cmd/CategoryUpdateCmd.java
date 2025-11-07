package com.app.codemasterpiecebackend.service.category.cmd;

import org.springframework.lang.Nullable;
import org.springframework.web.multipart.MultipartFile;

import static com.app.codemasterpiecebackend.util.Stringx.trimToNull;

public record CategoryUpdateCmd(
        String categoryId,
        @Nullable String name,
        @Nullable String link,
        @Nullable MultipartFile image,
        boolean removeImage
) {
    public CategoryUpdateCmd(String categoryId, @Nullable String name, @Nullable String link, @Nullable MultipartFile image, boolean removeImage) {
        this.categoryId = trimToNull(categoryId);
        this.name = trimToNull(name);
        this.link = trimToNull(link);
        this.image = image;
        this.removeImage = removeImage;
    }
}

