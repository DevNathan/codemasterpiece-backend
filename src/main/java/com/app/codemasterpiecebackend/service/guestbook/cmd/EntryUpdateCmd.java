package com.app.codemasterpiecebackend.service.guestbook.cmd;

import static com.app.codemasterpiecebackend.util.Stringx.trimToNull;

public record EntryUpdateCmd(
        String entryId,
        String content,
        boolean elevated,
        String userId,
        String password
) {
    public EntryUpdateCmd(String entryId, String content, boolean elevated, String userId, String password) {
        this.entryId = trimToNull(entryId);
        this.content = trimToNull(content);
        this.elevated = elevated;
        this.userId = trimToNull(userId);
        this.password = trimToNull(password);
    }
}
