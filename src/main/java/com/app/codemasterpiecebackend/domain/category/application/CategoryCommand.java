package com.app.codemasterpiecebackend.domain.category.application;

import com.app.codemasterpiecebackend.domain.category.entity.CategoryType;
import org.springframework.lang.Nullable;
import org.springframework.web.multipart.MultipartFile;

import static com.app.codemasterpiecebackend.global.util.Stringx.trimToNull;

/**
 * 카테고리(Category) 도메인의 모든 유즈케이스 입력을 정의하는 통합 Command 클래스입니다.
 */
public final class CategoryCommand {

    private CategoryCommand() {}

    public record Create(
            String name,
            CategoryType type,
            @Nullable String parentId,
            @Nullable String link,
            @Nullable MultipartFile image
    ) {
        public Create(String name, CategoryType type, @Nullable String parentId, @Nullable String link, @Nullable MultipartFile image) {
            this.name = trimToNull(name);
            this.type = type;
            this.parentId = trimToNull(parentId);

            String cleanedLink = trimToNull(link);
            this.link = cleanedLink != null ? cleanedLink.toLowerCase() : null;

            this.image = image;
        }
    }

    public record Update(
            String categoryId,
            @Nullable String name,
            @Nullable String link,
            @Nullable MultipartFile image,
            boolean removeImage
    ) {
        public Update(String categoryId, @Nullable String name, @Nullable String link, @Nullable MultipartFile image, boolean removeImage) {
            this.categoryId = trimToNull(categoryId);
            this.name = trimToNull(name);
            this.link = trimToNull(link);
            this.image = image;
            this.removeImage = removeImage;
        }
    }

    public record Move(
            String categoryId,
            String newParentId,
            Integer newIndex,
            String beforeId,
            String afterId
    ) {
        public Move(String categoryId, String newParentId, Integer newIndex, String beforeId, String afterId) {
            this.categoryId = trimToNull(categoryId);
            this.newParentId = trimToNull(newParentId);
            this.newIndex = newIndex;
            this.beforeId = trimToNull(beforeId);
            this.afterId = trimToNull(afterId);
        }
    }
}