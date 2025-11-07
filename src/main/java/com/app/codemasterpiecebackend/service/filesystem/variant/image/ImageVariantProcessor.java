package com.app.codemasterpiecebackend.service.filesystem.variant.image;

import com.app.codemasterpiecebackend.domain.entity.file.StoredFile;
import com.app.codemasterpiecebackend.infra.filesystem.io.IoManager;
import com.app.codemasterpiecebackend.infra.filesystem.io.cmd.PutCommand;
import com.app.codemasterpiecebackend.service.filesystem.variant.VariantPersister;
import com.app.codemasterpiecebackend.service.filesystem.variant.VariantProcessor;
import com.app.codemasterpiecebackend.service.filesystem.variant.cmd.VariantProcessCmd;
import com.app.codemasterpiecebackend.support.exception.VariantProcessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Slf4j
@Service("imageProcessor")
@RequiredArgsConstructor
public class ImageVariantProcessor implements VariantProcessor {

    private final IoManager io;
    private final ImageTransformer transformer;
    private final VariantPersister persister;

    @Override
    public void process(StoredFile original, VariantProcessCmd cmd) {
        if (!supports(original.getContentType())) {
            throw new VariantProcessException("Unsupported contentType: " + original.getContentType());
        }
        if (cmd.targets() == null || cmd.targets().isEmpty()) return;

        byte[] originalBytes;
        try (InputStream in = io.openStream(original.getStorageKey())) {
            originalBytes = in.readAllBytes();
        } catch (Exception e) {
            throw new VariantProcessException("open original failed: " + original.getStorageKey(), e);
        }

        for (var t : cmd.targets()) {
            var kind = t.kind();

            try {
                byte[] out;
                String mime;
                Integer w = null, h = null;

                switch (kind) {
                    case WEBP -> {
                        out = transformer.toWebp(originalBytes);
                        mime = "image/webp";
                    }
                    case AVIF -> {
                        out = transformer.toAvif(originalBytes);
                        mime = "image/avif";
                    }
                    case THUMB_512 -> {
                        var r = transformer.resizeWebp(originalBytes, 512);
                        out = r.bytes();
                        mime = "image/webp";
                        w = r.width();
                        h = r.height();
                    }
                    case THUMB_256 -> {
                        var r = transformer.resizeWebp(originalBytes, 256);
                        out = r.bytes();
                        mime = "image/webp";
                        w = r.width();
                        h = r.height();
                    }
                    default -> throw new VariantProcessException("kind not supported: " + kind);
                }

                String ext = mimeToExt(mime);

                String key = buildVariantKey(original, kind.name(), ext);

                try (var put = PutCommand.ofBytes(key, out, mime)) {
                    io.put(put);
                }

                persister.saveInNewTx(original, kind, mime, key, w, h, out.length);

            } catch (Exception ex) {
                log.warn("variant target failed and skipped. fileId={}, kind={}", original.getId(), kind, ex);
            }
        }
    }

    @Override
    public boolean supports(String contentType) {
        if (contentType == null) return false;
        String ct = contentType.toLowerCase(Locale.ROOT);
        if (!ct.startsWith("image/")) return false;
        return !(ct.contains("svg") || ct.contains("icon") || ct.contains("svg+xml"));
    }

    private static String mimeToExt(String mime) {
        String m = mime.toLowerCase(Locale.ROOT);
        return switch (m) {
            case "image/webp" -> "webp";
            case "image/avif" -> "avif";
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            default -> (m.contains("/") ? m.substring(m.indexOf('/') + 1) : "bin");
        };
    }

    // ====== 여기부터 신규/변경 유틸 ======

    private static String buildVariantKey(StoredFile original, String kindUpper, String ext) {
        String base = normalizeBasePath(original.getStoragePath());
        String stem = preferredStem(original);

        String kind = kindUpper.toUpperCase(Locale.ROOT);
        String fileName;

        // 포맷 변환은 확장자 교체만
        if ("WEBP".equals(kind) || "AVIF".equals(kind)) {
            fileName = stem + "." + safe(ext);
        } else if ("THUMB_512".equals(kind)) {
            fileName = stem + ".thumb-512." + safe(ext);
        } else if ("THUMB_256".equals(kind)) {
            fileName = stem + ".thumb-256." + safe(ext);
        } else {
            // 알 수 없는 kind는 방어적으로 kind를 하이픈 소문자 붙임
            fileName = stem + "." + kind.toLowerCase(Locale.ROOT).replace('_', '-') + "." + safe(ext);
        }

        return base + "variants/" + fileName;
    }

    private static String normalizeBasePath(String path) {
        if (path == null || path.isBlank()) return "/";
        return path.endsWith("/") ? path : path + "/";
    }

    /**
     * 선호 stem 추출 규칙:
     * 1) original.getOriginalFilename() 있으면 확장자 제거 후 사용
     * 2) 없으면 storageKey 마지막 세그먼트의 확장자 제거
     * 3) 그마저도 없으면 original.getId()
     */
    private static String preferredStem(StoredFile original) {
        String fromName = null;
        try {
            // Optional: 도메인에 따라 메서드 명이 다를 수 있음. 없으면 catch.
            var m = original.getClass().getMethod("getOriginalFilename");
            Object val = m.invoke(original);
            if (val instanceof String s && !s.isBlank()) fromName = s;
        } catch (Exception ignore) { /* 메서드 없을 수 있음 */ }

        if (fromName != null) {
            return safe(stripExt(fromName));
        }

        String key = original.getStorageKey();
        if (key != null && !key.isBlank()) {
            String last = key.replace('\\', '/'); // 윈도우 방어
            last = last.contains("/") ? last.substring(last.lastIndexOf('/') + 1) : last;
            if (!last.isBlank()) return safe(stripExt(last));
        }

        // 최후의 보루: 엔티티 ID
        try {
            String id = String.valueOf(original.getId());
            if (id != null && !id.isBlank()) return safe(id);
        } catch (Exception ignore) { /* getId()가 없을 리는 없지만 방어 */ }

        return "file";
    }

    private static String stripExt(String s) {
        int i = s.lastIndexOf('.');
        return (i > 0) ? s.substring(0, i) : s;
    }

    private static String safe(String s) {
        if (s == null || s.isBlank()) return "variant";
        String v = s.replace('\\', '/').replaceAll("[\\r\\n]", "_");
        v = v.contains("/") ? v.substring(v.lastIndexOf('/') + 1) : v;
        // 파일명 불량문자 정리(대체): 공백→_, 나머지 위험문자 제거
        v = v.replaceAll("[\\s]+", "_").replaceAll("[^A-Za-z0-9._-]", "");
        if (v.length() > 200) v = v.substring(0, 200);
        return new String(v.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8).trim();
    }
}
