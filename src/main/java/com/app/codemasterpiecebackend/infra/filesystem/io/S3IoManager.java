package com.app.codemasterpiecebackend.infra.filesystem.io;

import com.app.codemasterpiecebackend.domain.dto.file.FileObjectMetadata;
import com.app.codemasterpiecebackend.infra.filesystem.io.cmd.PutCommand;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.*;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3IoManager implements IoManager {

    private final S3Client s3;
    private final Optional<S3Presigner> presigner;

    @Value("${file.s3.bucket}")
    private String bucket;

    @Value("${file.s3.keyPrefix:codemasterpiece}")
    private String keyPrefix;

    @Value("${file.s3.defaultContentType:application/octet-stream}")
    private String defaultContentType;

    // 멀티파트 경계선(이상일 때 멀티파트/스풀 고려)
    @Value("${file.s3.multipartThresholdBytes:5242880}") // 5MB
    private long multipartThreshold;

    @PostConstruct
    void checkBucket() {
        try {
            s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            log.info("S3IoManager ready. bucket={}, prefix='{}'", bucket, keyPrefix);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new IllegalStateException("S3 bucket not found: " + bucket, e);
            }
            throw new IllegalStateException("S3 headBucket failed for: " + bucket, e);
        }
    }

    // ===== API 구현 =====
    @Override
    public FileObjectMetadata put(PutCommand cmd) throws IOException {
        Objects.requireNonNull(cmd.getKey(), "key");
        final String key = normalizeKey(cmd.getKey());
        final String s3key = toS3Key(key);

        InputStream original = cmd.getIn();
        InputStream body = original;
        long len = cmd.getContentLength();
        File tempFile = null;

        try {
            // 길이를 모르면 스풀, 너무 커도 스풀(간단 구현). 실전은 TransferManager 권장.
            if (len <= 0 || len >= multipartThreshold) {
                tempFile = Files.createTempFile("s3-put-", ".bin").toFile();
                try (var out = new BufferedOutputStream(new FileOutputStream(tempFile))) {
                    body.transferTo(out);
                } finally {
                    closeQuietly(original); // 원본은 여기서 끝
                }
                body = new BufferedInputStream(new FileInputStream(tempFile));
                len = tempFile.length();
            }

            PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3key)
                    .contentType(nonBlank(cmd.getContentType(), defaultContentType))
                    .contentLength(len > 0 ? len : null)
                    .build();

            PutObjectResponse res = s3.putObject(req, RequestBody.fromInputStream(body, len > 0 ? len : 0));
            SdkHttpResponse http = res.sdkHttpResponse();
            if (!http.isSuccessful()) {
                throw new IOException("S3 putObject failed: " + http.statusCode() + " / " + http.statusText().orElse(""));
            }
        } catch (S3Exception e) {
            throw new IOException("S3 putObject error: " + safeAwsMsg(e), e);
        } finally {
            closeQuietly(body);
            if (tempFile != null && !tempFile.delete()) {
                log.debug("Temp file delete failed: {}", tempFile.getAbsolutePath());
            }
        }

        long finalLen = len;
        return head(key).orElseGet(() ->
                FileObjectMetadata.builder()
                        .key(key)
                        .size(finalLen > 0 ? finalLen : -1)
                        .contentType(nonBlank(cmd.getContentType(), defaultContentType))
                        .checksumSha256(null)
                        .createdAt(null)
                        .updatedAt(Instant.now())
                        .storageType("S3")
                        .build()
        );
    }

    @Override
    public FileObjectMetadata move(String srcKey, String dstKey) throws IOException {
        final String src = toS3Key(normalizeKey(srcKey));
        final String dst = toS3Key(normalizeKey(dstKey));

        try {
            CopyObjectResponse copyRes = s3.copyObject(
                    CopyObjectRequest.builder()
                            .sourceBucket(bucket)
                            .sourceKey(src)
                            .destinationBucket(bucket)
                            .destinationKey(dst)
                            .metadataDirective(MetadataDirective.COPY)
                            .build()
            );
            if (!copyRes.sdkHttpResponse().isSuccessful()) {
                throw new IOException("S3 copyObject failed: " + copyRes.sdkHttpResponse().statusCode());
            }

            s3.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket).key(src).build());

            String logicalDst = fromS3Key(dst);
            return head(logicalDst).orElseGet(() ->
                    FileObjectMetadata.builder()
                            .key(logicalDst)
                            .size(-1)
                            .contentType(defaultContentType)
                            .checksumSha256(null)
                            .createdAt(null)
                            .updatedAt(Instant.now())
                            .storageType("S3")
                            .build()
            );
        } catch (S3Exception e) {
            throw new IOException("S3 move (copy+delete) error: " + safeAwsMsg(e), e);
        }
    }

    @Override
    public boolean delete(String key) throws IOException {
        final String s3key = toS3Key(normalizeKey(key));
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(s3key).build());
            return true; // 존재하지 않아도 멱등
        } catch (S3Exception e) {
            if (e.statusCode() == 404) return true;
            throw new IOException("S3 deleteObject error: " + safeAwsMsg(e), e);
        }
    }

    @Override
    public Optional<FileObjectMetadata> head(String key) throws IOException {
        final String s3key = toS3Key(normalizeKey(key));
        try {
            HeadObjectResponse head = s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(s3key).build());
            return Optional.of(
                    FileObjectMetadata.builder()
                            .key(key)
                            .size(head.contentLength() != null ? head.contentLength() : -1)
                            .contentType(nonBlank(head.contentType(), defaultContentType))
                            .checksumSha256(head.checksumSHA256())
                            .createdAt(null)
                            .updatedAt(head.lastModified())
                            .storageType("S3")
                            .build()
            );
        } catch (S3Exception e) {
            if (e.statusCode() == 404) return Optional.empty();
            throw new IOException("S3 headObject error: " + safeAwsMsg(e), e);
        }
    }

    @Override
    public InputStream openStream(String key) throws IOException {
        final String s3key = toS3Key(normalizeKey(key));
        try {
            return s3.getObject(GetObjectRequest.builder().bucket(bucket).key(s3key).build());
        } catch (S3Exception e) {
            if (e.statusCode() == 404) throw new FileNotFoundException("S3 object not found: " + key);
            throw new IOException("S3 getObject error: " + safeAwsMsg(e), e);
        }
    }

    @Override
    public InputStream openRange(String key, long startInclusive, long endExclusive) throws IOException {
        final String s3key = toS3Key(normalizeKey(key));
        try {
            String range = "bytes=" + Math.max(0, startInclusive) + "-" + (endExclusive > 0 ? (endExclusive - 1) : "");
            return s3.getObject(GetObjectRequest.builder()
                    .bucket(bucket).key(s3key).range(range).build());
        } catch (S3Exception e) {
            if (e.statusCode() == 404) throw new FileNotFoundException("S3 object not found: " + key);
            throw new IOException("S3 ranged getObject error: " + safeAwsMsg(e), e);
        }
    }

    @Override
    public String presignRead(String key, Duration ttl) throws IOException {
        var p = presigner.orElseThrow(() -> new UnsupportedOperationException("S3Presigner not configured"));
        final String s3key = toS3Key(normalizeKey(key));
        var getReq = GetObjectRequest.builder().bucket(bucket).key(s3key).build();
        var presign = p.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(getReq)
                .build());
        return presign.url().toString();
    }

    @Override
    public PresignWriteResult presignWrite(String key, Duration ttl, String contentType, long maxSize) {
        var p = presigner.orElseThrow(() -> new UnsupportedOperationException("S3Presigner not configured"));
        final String s3key = toS3Key(normalizeKey(key));
        var putReq = PutObjectRequest.builder()
                .bucket(bucket).key(s3key)
                .contentType(nonBlank(contentType, defaultContentType))
                .contentLength(maxSize > 0 ? maxSize : null)
                .build();
        var presign = p.presignPutObject(PutObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .putObjectRequest(putReq)
                .build());
        // AWS SDK v2는 PUT presign이 단일 URL + 헤더 조합
        Map<String, String> headers = new LinkedHashMap<>();
        presign.signedHeaders().forEach((k, v) -> headers.put(k, String.join(",", v)));
        return new PresignWriteResult(presign.url().toString(), headers);
    }

    @Override
    public int deletePrefix(String prefix) throws IOException {
        String normalizedPrefix = normalizePrefix(prefix);
        String s3Prefix = toS3Key(normalizedPrefix);

        int deleted = 0;
        try {
            ListObjectsV2Iterable pages = s3.listObjectsV2Paginator(ListObjectsV2Request.builder()
                    .bucket(bucket).prefix(s3Prefix).build());

            for (ListObjectsV2Response page : pages) {
                List<ObjectIdentifier> ids = new ArrayList<>();
                for (S3Object obj : page.contents()) {
                    ids.add(ObjectIdentifier.builder().key(obj.key()).build());
                    if (ids.size() == 1000) {
                        deleted += bulkDelete(ids);
                        ids.clear();
                    }
                }
                if (!ids.isEmpty()) deleted += bulkDelete(ids);
            }
            return deleted;
        } catch (S3Exception e) {
            throw new IOException("S3 deletePrefix error: " + safeAwsMsg(e), e);
        }
    }

    private int bulkDelete(List<ObjectIdentifier> ids) {
        DeleteObjectsResponse res = s3.deleteObjects(DeleteObjectsRequest.builder()
                .bucket(bucket)
                .delete(Delete.builder().objects(ids).build())
                .build());
        return res.deleted() != null ? res.deleted().size() : 0;
    }

    // ===== 내부 유틸 =====
    private String normalizeKey(String key) {
        if (key == null) throw new IllegalArgumentException("key must not be null");
        // unify slashes
        String cleaned = key.replace('\\', '/');

        // remove leading slashes
        while (cleaned.startsWith("/")) cleaned = cleaned.substring(1);

        // collapse duplicate slashes
        cleaned = cleaned.replaceAll("/{2,}", "/");

        // prohibit parent traversal + control chars
        cleaned = cleaned.replace("../", "")
                .replace("..\\", "")
                .replaceAll("[\\p{Cntrl}]", "_");

        if (cleaned.isBlank()) throw new IllegalArgumentException("key must not be blank");
        return cleaned;
    }

    private String normalizePrefix(String prefix) {
        String p = normalizeKey(prefix);
        if (!p.endsWith("/")) p = p + "/";
        return p;
    }

    private String toS3Key(String normalizedKey) {
        if (keyPrefix == null || keyPrefix.isBlank()) return normalizedKey;
        String p = keyPrefix.replace('\\', '/');
        if (!p.endsWith("/")) p = p + "/";
        return p + normalizedKey;
    }

    private String fromS3Key(String s3key) {
        if (keyPrefix == null || keyPrefix.isBlank()) return s3key;
        String p = keyPrefix.replace('\\', '/');
        if (!p.endsWith("/")) p = p + "/";
        if (s3key.startsWith(p)) return s3key.substring(p.length());
        return s3key;
    }

    private static String nonBlank(String v, String fallback) {
        return (v == null || v.isBlank()) ? fallback : v;
    }

    private static String safeAwsMsg(S3Exception e) {
        String msg = (e.awsErrorDetails() != null) ? e.awsErrorDetails().errorMessage() : e.getMessage();
        return msg != null ? msg : e.toString();
    }

    private static void closeQuietly(InputStream in) {
        if (in == null) return;
        try { in.close(); } catch (Exception ignored) {}
    }
}
