package com.app.codemasterpiecebackend.service.filesystem.support.path;

import java.time.Instant;

/**
 * S3 keyPrefix 이후의 base path(yyyy/MM/dd/ULID/)를 생성/복원하는 전략.
 * <p>
 * - allocate(): 업로드 시 신규 베이스 경로 할당<br>
 * - of(): 워커/큐에서 기존 경로 복원(메타에 담긴 값으로 재조립)<br>
 * - BasePath: original/variant 키 빌더 제공
 * </p>
 */
public interface FilePathStrategy {

    /**
     * 업로드 시 신규 path 할당.
     * <p>예) 2025/10/27/FL-01HX.../</p>
     *
     * @param originalFilename 원본 파일명(확장자 사용 안 함. 키 결정은 확장자 독립)
     */
    BasePath allocateFor(String ulid, Instant createdAt);


    /**
     * 기존(저장된) path/ulid로 복원 (큐/워커에서 사용).
     * <p>이미 DB/메시지에 담겨있는 값으로 재조립할 때 사용.</p>
     */
    BasePath of(String path, String ulid, Instant timestamp);

    /**
     * yyyy/MM/dd/ULID/ 형태의 베이스 경로 보유 객체.
     */
    record BasePath(String path, String ulid, Instant createdAt) {

        /**
         * 원본 키.
         * <p>예) yyyy/MM/dd/ULID/original</p>
         */
        public String originalKey() {
            return ensureSlash(path) + "original";
        }

        /**
         * 배리언트 키(범용).
         * <p>예) yyyy/MM/dd/ULID/variants/{name}.{ext}</p>
         *
         * @param variantName 예: "webp_2048", "thumb_512"
         * @param ext         예: "webp", "avif", "mp4"
         */
        public String variantKey(String variantName, String ext) {
            return ensureSlash(path) + "variants/" + safe(variantName) + "." + safeExt(ext);
        }

        /**
         * 배리언트 공통 프리픽스.
         * <p>예) yyyy/MM/dd/ULID/variants/</p>
         */
        public String variantsPrefix() {
            return ensureSlash(path) + "variants/";
        }

        private static String ensureSlash(String p) {
            if (p == null || p.isBlank()) throw new IllegalArgumentException("base path blank");
            return p.endsWith("/") ? p : (p + "/");
        }

        private static String safe(String s) {
            if (s == null || s.isBlank()) return "variant";
            String v = s.replace('\\','/').replaceAll("[\\r\\n]", "_");
            // 슬래시는 허용하지 않음
            if (v.contains("/")) v = v.replace("/", "_");
            return v;
        }

        private static String safeExt(String ext) {
            if (ext == null || ext.isBlank()) return "bin";
            String e = ext.startsWith(".") ? ext.substring(1) : ext;
            return e.toLowerCase();
        }
    }
}
