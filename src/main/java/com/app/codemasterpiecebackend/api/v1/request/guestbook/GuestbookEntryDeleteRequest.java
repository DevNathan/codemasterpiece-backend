package com.app.codemasterpiecebackend.api.v1.request.guestbook;

import org.springframework.lang.Nullable;

public record GuestbookEntryDeleteRequest(
        @Nullable String guestPassword
) {
}