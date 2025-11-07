package com.app.codemasterpiecebackend.infra.filesystem.io.cmd;

import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * FileManager.put(...) 호출 파라미터를 한데 묶는 커맨드.
 * AutoCloseable로 감싸 try-with-resources에서 안전하게 닫는다.
 */
@Getter
public final class PutCommand implements AutoCloseable {
    private final String key;
    private final String originalFilename;
    private final InputStream in;
    private final long contentLength;
    private final String contentType;

    private PutCommand(String key, String originalFilename, InputStream in, long contentLength, String contentType) {
        this.key = key;
        this.originalFilename = originalFilename;
        this.in = in;
        this.contentLength = contentLength;
        this.contentType = contentType;
    }

    public String key() {
        return key;
    }

    public String originalFilename() {
        return originalFilename;
    }

    public InputStream in() {
        return in;
    }

    public long contentLength() {
        return contentLength;
    }

    public String contentType() {
        return contentType;
    }

    /**
     * MultipartFile → PutCommand (contentType/filename sanitize 자동)
     */
    public static PutCommand from(String key, MultipartFile file) throws IOException {
        if (file == null) throw new IllegalArgumentException("file must not be null");
        String original = safeFilename(file.getOriginalFilename());
        String ext = extOf(original);
        String ct = safeContentType(file.getContentType(), ext);
        return new PutCommand(
                key,
                original,
                file.getInputStream(),
                file.getSize(),
                ct
        );
    }

    public static PutCommand ofBytes(String key, byte[] bytes, String contentType) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("key must not be blank");
        if (bytes == null) throw new IllegalArgumentException("bytes must not be null");
        return new PutCommand(
                key,
                "bytes", // 다운로드 파일명 힌트가 필요하면 호출부에서 Disposition으로 제어
                new ByteArrayInputStream(bytes),
                bytes.length,
                (contentType != null && !contentType.isBlank()) ? contentType : "application/octet-stream"
        );
    }

    @Override
    public void close() throws IOException {
        if (in != null) in.close(); // FileManager에서 닫더라도 이중 close로 안전장치
    }

    // ====== 내부 유틸(서비스에서 쓰던 로직 이식) ======
    private static String safeFilename(String name) {
        if (name == null || name.isBlank()) return "upload.bin";
        return name.replace('\\', '/').replaceAll("[\\r\\n]", "_");
    }

    private static String extOf(String filename) {
        if (filename == null) return "bin";
        int i = filename.lastIndexOf('.');
        return (i < 0) ? "bin" : filename.substring(i + 1).toLowerCase();
    }

    private static String safeContentType(String ct, String ext) {
        if (ct != null && !ct.isBlank()) return ct;
        return switch (ext) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "webp" -> "image/webp";
            case "avif" -> "image/avif";
            case "gif" -> "image/gif";
            case "pdf" -> "application/pdf";
            default -> "application/octet-stream";
        };
    }
}
