package com.app.codemasterpiecebackend.util;

import com.app.codemasterpiecebackend.service.filesystem.file.result.FileInfo;

public final class FileUrlResolver {

    private FileUrlResolver() {
    }

    /**
     * CDN base + keyPrefix + storagePath로 전체 URL 생성.
     * 예: https://cdn.codemasterpiece.com/codemasterpiece/2025/10/29/FL-xxxxx/original
     */
    public static String toFileUrl(CdnProperties props, FileInfo info) {
        String base = normalize(props.getBaseUrl());
        String prefix = normalize(props.getKeyPrefix());
        String path = normalize(info.storagePath());
        String key = info.storageKey();

        return base + prefix + path + extractFilename(key);
    }

    /**
     * baseDir까지만 생성. 폴더 경로 반환용.
     * 예: https://cdn.codemasterpiece.com/codemasterpiece/2025/10/29/FL-xxxxx/
     */
    public static String toBaseDirUrl(CdnProperties props, String storagePath) {
        String base = normalize(props.getBaseUrl());
        String prefix = normalize(props.getKeyPrefix());
        String path = normalize(storagePath);
        return base + prefix + path;
    }

    public static String toCdnUrl(CdnProperties props, String storagePathOrKey) {
        String base = trimTrailingSlash(props.getBaseUrl());
        String prefix = trimSlashes(props.getKeyPrefix());
        String path = trimLeadingSlash(storagePathOrKey);
        if (prefix.isEmpty()) {
            return base + "/" + path;
        }
        return base + "/" + prefix + "/" + path;
    }

    private static String trimTrailingSlash(String s) {
        if (s == null) return "";
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private static String trimLeadingSlash(String s) {
        if (s == null) return "";
        return s.startsWith("/") ? s.substring(1) : s;
    }

    private static String trimSlashes(String s) {
        if (s == null || s.isBlank()) return "";
        String t = s.trim();
        if (t.startsWith("/")) t = t.substring(1);
        if (t.endsWith("/")) t = t.substring(0, t.length() - 1);
        return t;
    }

    private static String normalize(String p) {
        if (p == null || p.isBlank()) return "";
        p = p.trim();
        if (!p.endsWith("/")) p += "/";
        return p;
    }

    private static String extractFilename(String key) {
        if (key == null) return "";
        int slash = key.lastIndexOf('/');
        return slash >= 0 ? key.substring(slash + 1) : key;
    }
}
