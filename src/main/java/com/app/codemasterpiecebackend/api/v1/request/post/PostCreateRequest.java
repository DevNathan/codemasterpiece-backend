package com.app.codemasterpiecebackend.api.v1.request.post;

import com.app.codemasterpiecebackend.service.post.cmd.PostCreateCmd;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Objects;

import static com.app.codemasterpiecebackend.util.Stringx.trimToNull;

@JsonIgnoreProperties(ignoreUnknown = true)
@Jacksonized
@Builder
public record PostCreateRequest(
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
    public PostCreateCmd toCmd() {
        List<String> normalizedTags = tags.stream()
                .map(tag -> {
                    String t = trimToNull(tag);
                    return (t != null) ? t.toLowerCase() : null;
                })
                .filter(Objects::nonNull)
                .toList();

        return new PostCreateCmd(
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
