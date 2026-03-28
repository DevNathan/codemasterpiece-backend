package com.app.codemasterpiecebackend.domain.file.variant.processor;

import com.app.codemasterpiecebackend.domain.file.core.entity.StoredFile;
import com.app.codemasterpiecebackend.global.infra.filesystem.io.IoManager;
import com.app.codemasterpiecebackend.domain.file.variant.application.VariantPersister;
import com.app.codemasterpiecebackend.domain.file.variant.dto.VariantProcessCmd;
import com.app.codemasterpiecebackend.global.support.exception.VariantProcessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;

/**
 * 이미지 파일의 변환 작업(리사이징, 포맷 변경 등)을 조율하는 프로세서입니다.
 * * <p>대용량 이미지 처리 시 발생할 수 있는 메모리 고갈(OOM)을 방지하기 위해,
 * S3 원본 스트림을 로컬 임시 파일로 1회만 다운로드하여 다중 변환 타겟에 재사용하며,
 * 처리 완료 후 로컬 디스크의 임시 파일들을 안전하게 삭제(Clean-up)합니다.</p>
 */
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

        File tempOriginal = null;
        try {
            // 1. S3 원본을 로컬 임시 파일로 1회 스트리밍 (메모리 절약)
            tempOriginal = Files.createTempFile("img-orig-", ".bin").toFile();
            try (InputStream in = io.openStream(original.getStorageKey());
                 OutputStream os = new FileOutputStream(tempOriginal)) {
                in.transferTo(os);
            } catch (Exception e) {
                throw new VariantProcessException("Failed to download original stream: " + original.getStorageKey(), e);
            }

            // 2. 요청된 타겟별로 순차적 변환 및 업로드 수행
            for (var t : cmd.targets()) {
                var kind = t.kind();
                File outFile = null;

                try {
                    String mime;
                    Integer w = null, h = null;

                    switch (kind) {
                        case WEBP -> {
                            outFile = transformer.toWebp(tempOriginal);
                            mime = "image/webp";
                        }
                        case AVIF -> {
                            outFile = transformer.toAvif(tempOriginal);
                            mime = "image/avif";
                        }
                        case THUMB_512 -> {
                            var r = transformer.resizeWebp(tempOriginal, 512);
                            outFile = r.file();
                            mime = "image/webp";
                            w = r.width();
                            h = r.height();
                        }
                        case THUMB_256 -> {
                            var r = transformer.resizeWebp(tempOriginal, 256);
                            outFile = r.file();
                            mime = "image/webp";
                            w = r.width();
                            h = r.height();
                        }
                        default -> throw new VariantProcessException("kind not supported: " + kind);
                    }

                    String ext = mimeToExt(mime);
                    String key = buildVariantKey(original, kind.name(), ext);
                    long fileSize = outFile.length();

                    // 3. 변환된 임시 파일을 메모리 버퍼링 없이 S3로 다이렉트 업로드
                    try (InputStream fis = new FileInputStream(outFile)) {
                        io.put(key, fis, fileSize, mime);
                    }

                    // 4. DB 메타데이터 기록 (독립 트랜잭션)
                    persister.saveInNewTx(original, kind, mime, key, w, h, (int) fileSize);

                } catch (Exception ex) {
                    log.warn("Variant target failed and skipped. fileId={}, kind={}", original.getId(), kind, ex);
                } finally {
                    if (outFile != null && outFile.exists()) {
                        outFile.delete();
                    }
                }
            }
        } catch (Exception e) {
            throw new VariantProcessException("Variant processing pipeline failed", e);
        } finally {
            if (tempOriginal != null && tempOriginal.exists()) {
                tempOriginal.delete();
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

    private static String buildVariantKey(StoredFile original, String kindUpper, String ext) {
        String base = normalizeBasePath(original.getStoragePath());
        String stem = preferredStem(original);

        String kind = kindUpper.toUpperCase(Locale.ROOT);
        String fileName;

        if ("WEBP".equals(kind) || "AVIF".equals(kind)) {
            fileName = stem + "." + safe(ext);
        } else if ("THUMB_512".equals(kind)) {
            fileName = stem + ".thumb-512." + safe(ext);
        } else if ("THUMB_256".equals(kind)) {
            fileName = stem + ".thumb-256." + safe(ext);
        } else {
            fileName = stem + "." + kind.toLowerCase(Locale.ROOT).replace('_', '-') + "." + safe(ext);
        }

        return base + "variants/" + fileName;
    }

    private static String normalizeBasePath(String path) {
        if (path == null || path.isBlank()) return "/";
        return path.endsWith("/") ? path : path + "/";
    }

    /**
     * 파일의 스토리지 키 생성에 사용될 식별자(Stem)를 추출합니다.
     * 원본 파일명 -> 스토리지 키 마지막 세그먼트 -> 엔티티 ID 순으로 폴백(Fallback)합니다.
     */
    private static String preferredStem(StoredFile original) {
        String fromName = original.getOriginalFilename();
        if (fromName != null && !fromName.isBlank()) {
            return safe(stripExt(fromName));
        }

        String key = original.getStorageKey();
        if (key != null && !key.isBlank()) {
            String last = key.replace('\\', '/');
            last = last.contains("/") ? last.substring(last.lastIndexOf('/') + 1) : last;
            if (!last.isBlank()) return safe(stripExt(last));
        }

        return safe(String.valueOf(original.getId()));
    }

    private static String stripExt(String s) {
        int i = s.lastIndexOf('.');
        return (i > 0) ? s.substring(0, i) : s;
    }

    private static String safe(String s) {
        if (s == null || s.isBlank()) return "variant";
        String v = s.replace('\\', '/').replaceAll("[\\r\\n]", "_");
        v = v.contains("/") ? v.substring(v.lastIndexOf('/') + 1) : v;
        v = v.replaceAll("\\s+", "_").replaceAll("[^A-Za-z0-9._-]", "");
        if (v.length() > 200) v = v.substring(0, 200);
        return new String(v.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8).trim();
    }
}