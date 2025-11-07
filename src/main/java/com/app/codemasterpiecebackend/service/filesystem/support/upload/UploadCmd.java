package com.app.codemasterpiecebackend.service.filesystem.support.upload;

import lombok.Builder;

import java.io.InputStream;
import java.util.Locale;
import java.util.function.Supplier;

@Builder
public record UploadCmd(
        String originalFilename,
        String contentType,
        long contentLength,
        Supplier<InputStream> content,
        MediaKind kind
) {
    public UploadCmd {
        if (content == null) throw new IllegalArgumentException("content supplier must not be null");
        if (kind == null)    throw new IllegalArgumentException("kind must not be null");

        originalFilename = sanitizeFilename(originalFilename);
        contentType = normalize(contentType, originalFilename);

        if (contentLength < 0) throw new IllegalArgumentException("contentLength must be >= 0");

        if (kind == MediaKind.IMAGE && !contentType.toLowerCase().startsWith("image/"))
            throw new IllegalArgumentException("Invalid IMAGE mime: " + contentType);
        if (kind == MediaKind.VIDEO && !contentType.toLowerCase().startsWith("video/"))
            throw new IllegalArgumentException("Invalid VIDEO mime: " + contentType);
        // ATTACHMENT는 제한 없음
    }

    private static String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) return "download.bin";
        String v = name.replace('\\','/').replaceAll("[\\r\\n]", "_");
        if (v.contains("/")) v = v.substring(v.lastIndexOf('/') + 1);
        return (v.length() > 200) ? v.substring(v.length() - 200) : v;
    }
    private static String normalize(String ct, String filename) {
        if (ct != null && !ct.isBlank()) return ct.trim().toLowerCase(Locale.ROOT);
        String ext = filename != null && filename.contains(".")
                ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        return switch (ext) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "webp" -> "image/webp";
            case "avif" -> "image/avif";
            case "gif" -> "image/gif";
            case "svg" -> "image/svg+xml";
            case "mp4" -> "video/mp4";
            case "webm" -> "video/webm";
            case "pdf" -> "application/pdf";
            case "zip" -> "application/zip";
            default -> "application/octet-stream";
        };
    }
}
