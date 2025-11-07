package com.app.codemasterpiecebackend.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record SuccessResponse<T>(
        String timestamp,
        String traceId,
        String path,
        SuccessDetail detail,
        @JsonInclude(JsonInclude.Include.NON_NULL) T data
) {
    public static <T> SuccessResponse<T> of(String traceId, String path, String code, String message, T data) {
        return new SuccessResponse<>(
                OffsetDateTime.now(ZoneOffset.UTC).toString(),
                traceId,
                path,
                new SuccessDetail(code, message),
                data
        );
    }
    public static <T> SuccessResponse<T> ok(String traceId, String path, String code, String message, T data) {
        return of(traceId, path, code != null ? code : "success.ok", message, data);
    }
    public static <T> SuccessResponse<T> created(String traceId, String path, String code, String message, T data) {
        return of(traceId, path, code != null ? code : "success.created", message, data);
    }
    public static SuccessResponse<Void> accepted(String traceId, String path, String code, String message) {
        return of(traceId, path, code != null ? code : "success.accepted", message, null);
    }


    public record SuccessDetail(String code, String message) {}
}
