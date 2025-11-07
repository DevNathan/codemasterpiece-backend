package com.app.codemasterpiecebackend.service.guestbook.cmd;

import com.app.codemasterpiecebackend.util.ActorUtil;

import static com.app.codemasterpiecebackend.util.Stringx.trimToNull;

public record EntryCreateCommand(
        String content,
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

    public EntryCreateCommand(String content, ActorUtil.Actor actor, String displayName, GuestPayload guest) {
        this.content = trimToNull(content);
        this.actor = actor;
        this.displayName = trimToNull(displayName);
        this.guest = guest;
    }
}
