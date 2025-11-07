package com.app.codemasterpiecebackend.api.v1.request.comment;

import com.app.codemasterpiecebackend.service.comment.cmd.CommentLikeCmd;

public record CommentToggleHideRequest(
        String commentId,
        boolean toggleHide
) {
    public CommentLikeCmd toCmd() {
        return new CommentLikeCmd(commentId, toggleHide);
    }
}
