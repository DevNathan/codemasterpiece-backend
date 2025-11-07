package com.app.codemasterpiecebackend.service.comment.cmd;

import static com.app.codemasterpiecebackend.util.Stringx.trimToNull;

public record CommentUpdateCmd(
        String commentId,
        String content,
        boolean elevated,
        String userId,
        String password
) {
    public CommentUpdateCmd(String commentId, String content, boolean elevated, String userId, String password) {
        this.commentId = trimToNull(commentId);
        this.content = trimToNull(content);
        this.elevated = elevated;
        this.userId = trimToNull(userId);
        this.password = trimToNull(password);
    }
}
