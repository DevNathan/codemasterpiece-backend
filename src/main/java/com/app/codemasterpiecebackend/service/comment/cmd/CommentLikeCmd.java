package com.app.codemasterpiecebackend.service.comment.cmd;

import com.app.codemasterpiecebackend.util.Stringx;

public record CommentLikeCmd(
        String commentId,
        boolean setHidden
) {
    public CommentLikeCmd(String commentId, boolean setHidden) {
        this.commentId = Stringx.trimToNull(commentId);
        this.setHidden = setHidden;
    }
}
