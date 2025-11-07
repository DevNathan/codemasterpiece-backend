package com.app.codemasterpiecebackend.api.v1.request.post;

public record PostToggleLikeRequest(
        String postId,
        boolean toggleLike
) {
}
