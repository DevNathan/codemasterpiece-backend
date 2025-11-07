package com.app.codemasterpiecebackend.api.v1.request.guestbook;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.lang.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CommentUpdateRequest(
        @NotBlank(message = "validation.comment.content.not_blank")
        @Size(max = 2000, message = "validation.comment.content.max_length")
        String content,

        @Nullable String guestPassword
) {
}