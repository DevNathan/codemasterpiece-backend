package com.app.codemasterpiecebackend.service.comment.cmd;

import static com.app.codemasterpiecebackend.util.Stringx.trimToNull;

public record CommentDeleteCmd(
        String commentId,
        boolean elevated,
        String userId,
        String password
) {
    public CommentDeleteCmd(String commentId, boolean elevated, String userId, String password) {
        this.commentId = trimToNull(commentId);
        this.elevated = elevated;
        this.userId = trimToNull(userId);
        this.password = trimToNull(password);
    }
}
