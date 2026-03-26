package com.app.codemasterpiecebackend.service.comment.cmd;

public record CommentRawCmd(
        String commentId,
        boolean elevated,
        String actorId
) {
}
