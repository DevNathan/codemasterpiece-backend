package com.app.codemasterpiecebackend.service.guestbook.cmd;

import static com.app.codemasterpiecebackend.util.Stringx.trimToNull;

public record EntryDeleteCmd(
        String entryId,
        boolean elevated,
        String userId,
        String password
) {
    public EntryDeleteCmd(String entryId, boolean elevated, String userId, String password) {
        this.entryId = trimToNull(entryId);
        this.elevated = elevated;
        this.userId = trimToNull(userId);
        this.password = trimToNull(password);
    }
}
