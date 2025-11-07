package com.app.codemasterpiecebackend.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        String timestamp,
        String traceId,
        String path,
        ErrorDetail error,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) Map<String, String> validation
) {
    public static ErrorResponse of(String traceId, String path, String code, String message, Map<String, String> validation) {
        return new ErrorResponse(Instant.now().toString(), traceId, path, new ErrorDetail(code, message), validation);
    }

    public record ErrorDetail(String code, String message) {}
}
