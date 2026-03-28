package com.app.codemasterpiecebackend.domain.post.api.v1;

import com.app.codemasterpiecebackend.domain.post.application.PostCommand;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Objects;

import static com.app.codemasterpiecebackend.global.util.Stringx.trimToNull;

/**
 * 게시글(Post) 도메인의 API 요청(Request) 규격을 통합 관리하는 레코드입니다.
 */
public record PostRequest() {

    /**
     * 게시글 생성 요청 규격.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Jacksonized
    @Builder
    public record Create(
            @NotBlank(message = "{validation.post.title.notBlank}")
            @Size(max = 255, message = "{validation.post.title.size}")
            String title,

            @NotBlank(message = "{validation.post.headImage.notBlank}")
            String headImage,

            @Size(max = 1000, message = "{validation.post.headContent.size}")
            String headContent,

            @NotNull(message = "{validation.post.tags.notNull}")
            @Size(max = 20, message = "{validation.post.tags.size}")
            List<
                    @NotBlank(message = "{validation.tag.name.notBlank}")
                    @Size(max = 60, message = "{validation.tag.name.size}")
                            String
                    > tags,

            @NotNull(message = "{validation.post.categoryId.notNull}")
            String categoryId,

            @NotBlank(message = "{validation.post.mainContent.notBlank}")
            String mainContent,

            boolean published
    ) {
        public PostCommand.Create toCmd() {
            List<String> normalizedTags = tags.stream()
                    .map(tag -> {
                        String t = trimToNull(tag);
                        return (t != null) ? t.toLowerCase() : null;
                    })
                    .filter(Objects::nonNull)
                    .toList();

            return new PostCommand.Create(
                    trimToNull(title),
                    headImage,
                    trimToNull(headContent),
                    normalizedTags,
                    categoryId,
                    trimToNull(mainContent),
                    published
            );
        }
    }

    /**
     * 게시글 수정 요청 규격.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Jacksonized
    @Builder
    public record Update(
            @NotBlank(message = "{validation.post.title.notBlank}")
            @Size(max = 255, message = "{validation.post.title.size}")
            String title,

            @Size(max = 255, message = "{validation.post.headImage.size}")
            String headImage,

            @Size(max = 1000, message = "{validation.post.headContent.size}")
            String headContent,

            @NotNull(message = "{validation.post.tags.notNull}")
            @Size(max = 6, message = "{validation.post.tags.size}")
            List<
                    @NotBlank(message = "{validation.tag.name.notBlank}")
                    @Size(max = 60, message = "{validation.tag.name.size}")
                            String
                    > tags,

            @NotNull(message = "{validation.post.categoryId.notNull}")
            String categoryId,

            @NotBlank(message = "{validation.post.mainContent.notBlank}")
            String mainContent,

            boolean published
    ) {
        public PostCommand.Update toCmd(String postId) {
            return new PostCommand.Update(
                    postId,
                    title,
                    headImage,
                    headContent,
                    tags,
                    categoryId,
                    mainContent,
                    published
            );
        }
    }

    /**
     * 게시글 좋아요 토글 요청 규격.
     */
    public record ToggleLike(
            String postId,
            boolean toggleLike
    ) {
    }
}