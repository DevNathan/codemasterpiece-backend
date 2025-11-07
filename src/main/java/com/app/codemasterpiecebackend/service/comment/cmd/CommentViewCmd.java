package com.app.codemasterpiecebackend.service.comment.cmd;

import org.springframework.data.domain.Pageable;

import static com.app.codemasterpiecebackend.util.Stringx.trimToNull;

public record CommentViewCmd(
        String postId,
        boolean elevated,
        String actorId,
        Pageable pageable
) {
    public CommentViewCmd(String postId, boolean elevated, String actorId, Pageable pageable) {
        this.postId = trimToNull(postId);
        this.elevated = elevated;
        this.actorId = trimToNull(actorId);
        this.pageable = pageable;
    }
}
