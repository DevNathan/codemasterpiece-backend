package com.app.codemasterpiecebackend.api.v1.request.post;

import com.app.codemasterpiecebackend.service.post.cmd.PostUpdateCmd;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static com.app.codemasterpiecebackend.util.Stringx.trimToNull;

@JsonIgnoreProperties(ignoreUnknown = true)
@Jacksonized
@Builder
public record PostUpdateRequest(

        @NotBlank(message = "{validation.post.title.notBlank}")
        @Size(max = 255, message = "{validation.post.title.size}")
        String title,

        // create와 동일하게 문자열로 받되, 내부에선 fileId로 사용
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
    public PostUpdateCmd toCmd(String postId) {
        return new PostUpdateCmd(
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
