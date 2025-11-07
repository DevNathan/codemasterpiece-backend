package com.app.codemasterpiecebackend.api.v1.request.comment;

import org.springframework.lang.Nullable;

public record CommentDeleteRequest(
        @Nullable String guestPassword
) {
}
