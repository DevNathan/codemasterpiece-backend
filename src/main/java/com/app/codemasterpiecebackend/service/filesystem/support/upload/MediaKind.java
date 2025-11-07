package com.app.codemasterpiecebackend.service.filesystem.support.upload;

import java.util.Set;

public enum MediaKind {
    IMAGE("image/", Set.of("png", "jpg", "jpeg", "webp", "avif", "gif", "svg")),
    VIDEO("video/", Set.of("mp4", "webm", "mov", "mkv", "avi")),
    ATTACHMENT("*/*", Set.of());

    private final String mimePrefix;
    private final Set<String> extensions;

    MediaKind(String mimePrefix, Set<String> extensions) {
        this.mimePrefix = mimePrefix;
        this.extensions = extensions;
    }

    public String mimePrefix() {
        return mimePrefix;
    }

    public Set<String> extensions() {
        return extensions;
    }

    public boolean matches(String contentType) {
        if (contentType == null || contentType.isBlank()) return false;
        return contentType.startsWith(mimePrefix.replace("*", ""));
    }
}
