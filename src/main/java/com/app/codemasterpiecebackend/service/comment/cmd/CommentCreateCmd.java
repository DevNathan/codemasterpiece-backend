package com.app.codemasterpiecebackend.service.comment.cmd;

import com.app.codemasterpiecebackend.util.ActorUtil;

import static com.app.codemasterpiecebackend.util.Stringx.trimToNull;

public record CommentCreateCmd(
        String postId,
        String content,
        String parentId,
        ActorUtil.Actor actor,
        String displayName,
        GuestPayload guest
) {
    public record GuestPayload(
            String imageUrl,
            String pin
    ) {
        public GuestPayload(String imageUrl, String pin) {
            this.imageUrl = trimToNull(imageUrl);
            this.pin = trimToNull(pin);
        }
    }

    public CommentCreateCmd(String postId, String content, String parentId, ActorUtil.Actor actor, String displayName, GuestPayload guest) {
        this.postId = trimToNull(postId);
        this.content = trimToNull(content);
        this.parentId = trimToNull(parentId);
        this.actor = actor;
        this.displayName = trimToNull(displayName);
        this.guest = guest;
    }
}
