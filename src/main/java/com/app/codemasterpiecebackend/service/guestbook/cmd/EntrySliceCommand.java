package com.app.codemasterpiecebackend.service.guestbook.cmd;

public record EntrySliceCommand(
        String cursor,
        int size
) {
    public int safeSize() {
        return (size <= 0 || size > 100) ? 20 : size; // 가드
    }
}
