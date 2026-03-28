package com.app.codemasterpiecebackend.domain.category.api.v1;

import com.app.codemasterpiecebackend.domain.category.application.CategoryCommand;
import com.app.codemasterpiecebackend.domain.category.entity.CategoryType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import org.springframework.lang.Nullable;
import org.springframework.web.multipart.MultipartFile;

import static com.app.codemasterpiecebackend.global.util.Stringx.trimToNull;

/**
 * 카테고리(Category) 도메인의 API 요청(Request) 규격을 통합 관리하는 레코드입니다.
 */
public record CategoryRequest() {

    /**
     * 카테고리 생성 요청 규격.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Jacksonized
    @Builder
    public record Create(
            @NotBlank(message = "validation.category.name.notBlank")
            @Size(min = 2, max = 20, message = "{validation.category.name.size}")
            String name,

            @NotNull(message = "validation.category.type.notNull")
            CategoryType type,

            String parentId,

            @Pattern(regexp = "^[a-z-]+$", message = "validation.category.link.pattern")
            @Size(min = 2, max = 200, message = "{validation.category.link.size}")
            String link,

            MultipartFile image
    ) {
        public CategoryCommand.Create toCmd() {
            MultipartFile effectiveImage = (image != null && !image.isEmpty()) ? image : null;
            return new CategoryCommand.Create(name, type, parentId, link, effectiveImage);
        }
    }

    /**
     * 카테고리 수정 요청 규격.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Jacksonized
    @Builder
    public record Update(
            @Nullable
            @Size(min = 2, max = 20, message = "{validation.category.name.size}")
            String name,

            @Nullable
            @Pattern(regexp = "^[a-z-]+$", message = "{validation.category.link.pattern}")
            @Size(min = 2, max = 200, message = "{validation.category.link.size}")
            String link,

            @Nullable MultipartFile image,

            boolean removeImage
    ) {
        public CategoryCommand.Update toCmd(String categoryId) {
            MultipartFile effectiveImage = (!removeImage && image != null && !image.isEmpty()) ? image : null;
            String normalizedLink = (trimToNull(link) != null) ? trimToNull(link).toLowerCase() : null;

            return new CategoryCommand.Update(
                    categoryId,
                    trimToNull(name),
                    normalizedLink,
                    effectiveImage,
                    removeImage
            );
        }
    }

    /**
     * 카테고리 이동 및 정렬 요청 규격.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Move(
            @NotBlank(message = "validation.category.id.notBlank")
            String categoryId,

            String newParentId,

            @PositiveOrZero(message = "validation.category.move.indexPositiveOrZero")
            Integer newIndex,

            String beforeId,

            String afterId
    ) {
        public CategoryCommand.Move toCmd() {
            return new CategoryCommand.Move(categoryId, newParentId, newIndex, beforeId, afterId);
        }
    }
}