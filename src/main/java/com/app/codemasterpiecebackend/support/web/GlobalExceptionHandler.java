package com.app.codemasterpiecebackend.support.web;

import com.app.codemasterpiecebackend.domain.dto.response.ErrorResponse;
import com.app.codemasterpiecebackend.support.exception.AppException;
import com.app.codemasterpiecebackend.support.exception.FieldValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import java.nio.charset.StandardCharsets;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.*;
import java.util.stream.Collectors;

import static com.app.codemasterpiecebackend.support.web.TraceIdFilter.TRACE_ID;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    @Value("${logging.includeStackFor4xx:false}")
    private boolean includeStackFor4xx;

    private final MessageSource messageSource;

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(
            AppException ex, HttpServletRequest req, Locale locale) {
        logWarn(req, ex.getStatus(), "AppException", ex);
        return build(req, ex.getStatus(), ex.getMessage(), locale, "Internal error", Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest req, Locale locale) {

        Map<String, String> validation = new LinkedHashMap<>();

        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            String key = Optional.ofNullable(fe.getDefaultMessage()).orElse("invalid");
            String resolved = resolve(key, locale, key);
            validation.put(fe.getField(), resolved);
        }

        Map<String, String> finalValidation = validation;

        ex.getBindingResult().getGlobalErrors().forEach(err -> {
            String key = Optional.ofNullable(err.getDefaultMessage()).orElse("invalid");
            String resolved = resolve(key, locale, key);
            finalValidation.merge("_", resolved, (a, b) -> a + "; " + b);
        });

        if (validation.isEmpty() && ex.getBindingResult().hasErrors()) {
            validation = Map.of("_", resolve("error.validation", locale, "Validation failed"));
        }

        logWarn(req, HttpStatus.BAD_REQUEST, "Validation", ex);
        return build(req, HttpStatus.BAD_REQUEST, "error.validation", locale, "Validation failed", validation);
    }

    @ExceptionHandler(FieldValidationException.class)
    public ResponseEntity<ErrorResponse> handleFieldValidation(
            FieldValidationException ex, HttpServletRequest req, Locale locale) {

        Map<String, String> validation = new LinkedHashMap<>();
        ex.getErrors().forEach((field, keyOrMsg) -> {
            String resolved = resolve(keyOrMsg, locale, keyOrMsg);
            validation.put(field, resolved);
        });

        logWarn(req, HttpStatus.BAD_REQUEST, "FieldValidation", ex);
        return build(req, HttpStatus.BAD_REQUEST, "error.validation", locale, "Validation failed", validation);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleBodyMissing(
            HttpMessageNotReadableException ex, HttpServletRequest req, Locale locale) {

        logWarn(req, HttpStatus.BAD_REQUEST, "BodyNotReadable", ex);
        Map<String, String> detail = extractJsonParseDetail(ex);
        return build(req, HttpStatus.BAD_REQUEST, "error.request.body", locale,
                "Invalid or missing request body", detail);
    }

    @ExceptionHandler({
            BindException.class,
            MissingServletRequestParameterException.class,
            MissingPathVariableException.class,
            MissingRequestHeaderException.class,
            MethodArgumentTypeMismatchException.class,
            ConstraintViolationException.class
    })
    public ResponseEntity<ErrorResponse> handleBind(
            Exception ex, HttpServletRequest req, Locale locale) {

        HttpStatus status = HttpStatus.BAD_REQUEST;
        String code = codeForBind(ex);
        String fallback = fallbackForBind(ex);

        logWarn(req, status, "Bind", ex);
        return build(req, status, code, locale, fallback, Map.of());
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            Exception ex, HttpServletRequest req, Locale locale) {

        logWarn(req, HttpStatus.FORBIDDEN, "AccessDenied", ex);
        return build(req, HttpStatus.FORBIDDEN, "error.forbidden", locale, "Access denied", Map.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethod(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest req, Locale locale) {

        logWarn(req, HttpStatus.METHOD_NOT_ALLOWED, "MethodNotSupported", ex);
        HttpHeaders headers = new HttpHeaders();

        if (ex.getSupportedHttpMethods() != null && !ex.getSupportedHttpMethods().isEmpty()) {
            headers.setAllow(ex.getSupportedHttpMethods());
        }

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .headers(headers)
                .contentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
                .body(errorBody(req, "error.method_not_allowed", locale,
                        "Method not allowed", Map.of()));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMedia(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest req, Locale locale) {

        logWarn(req, HttpStatus.UNSUPPORTED_MEDIA_TYPE, "MediaTypeNotSupported", ex);
        HttpHeaders headers = new HttpHeaders();

        if (!ex.getSupportedMediaTypes().isEmpty()) {
            headers.setAccept(ex.getSupportedMediaTypes());
        }

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .headers(headers)
                .contentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
                .body(errorBody(req, "error.unsupported_media_type", locale,
                        "Unsupported media type", Map.of()));
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErrorResponse> handleNotAcceptable(
            HttpMediaTypeNotAcceptableException ex, HttpServletRequest req, Locale locale) {

        logWarn(req, HttpStatus.NOT_ACCEPTABLE, "NotAcceptable", ex);
        return build(req, HttpStatus.NOT_ACCEPTABLE,
                "error.not_acceptable", locale, "Not acceptable", Map.of());
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ErrorResponse> handleNoHandler(
            Exception ex, HttpServletRequest req, Locale locale) {

        logWarn(req, HttpStatus.NOT_FOUND, "NotFound", ex);
        return build(req, HttpStatus.NOT_FOUND,
                "error.not_found", locale, "Resource not found", Map.of());
    }

    @ExceptionHandler({MaxUploadSizeExceededException.class, MultipartException.class})
    public ResponseEntity<ErrorResponse> handleMultipart(
            Exception ex, HttpServletRequest req, Locale locale) {

        logWarn(req, HttpStatus.PAYLOAD_TOO_LARGE, "Multipart", ex);
        return build(req, HttpStatus.PAYLOAD_TOO_LARGE,
                "error.payload_too_large", locale, "Payload too large", Map.of());
    }

    @ExceptionHandler(OAuth2AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleOAuth2(
            OAuth2AuthenticationException ex, HttpServletRequest req, Locale locale) {

        String codeKey = Optional.ofNullable(ex.getError())
                .map(OAuth2Error::getErrorCode)
                .orElse("error.oauth.failed");

        logWarn(req, HttpStatus.UNAUTHORIZED, "OAuth2", ex);
        return build(req, HttpStatus.UNAUTHORIZED, "error.oauth.failed",
                locale, "OAuth2 authentication failed", Map.of("reason", codeKey));
    }

    @ExceptionHandler({
            SQLIntegrityConstraintViolationException.class,
            DataIntegrityViolationException.class
    })
    public ResponseEntity<ErrorResponse> handleSqlConstraint(
            Exception ex, HttpServletRequest req, Locale locale) {

        logWarn(req, HttpStatus.CONFLICT, "SQLIntegrity", ex);
        return build(req, HttpStatus.CONFLICT,
                "error.db.duplicate", locale, "Duplicate value", Map.of());
    }

    @ExceptionHandler({ResponseStatusException.class, ErrorResponseException.class})
    public ResponseEntity<ErrorResponse> handleResponseStatus(
            Exception ex, HttpServletRequest req, Locale locale) {

        HttpStatus status;
        String reason;

        if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.resolve(rse.getStatusCode().value());
            reason = Optional.ofNullable(rse.getReason()).orElse("error.internal");
        } else {
            ErrorResponseException ere = (ErrorResponseException) ex;
            status = HttpStatus.resolve(ere.getStatusCode().value());
            reason = Optional.of(ere.getMessage()).orElse("error.internal");
        }

        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;

        logWarn(req, status, "ResponseStatus", ex);
        return build(req, status, reason, locale, "Request failed", Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAny(
            Exception ex, HttpServletRequest req, Locale locale) {

        log.error("500 WebError traceId={} uri={}",
                req.getAttribute(TRACE_ID), req.getRequestURI(), ex);

        return build(req, HttpStatus.INTERNAL_SERVER_ERROR,
                "error.internal", locale, "An unexpected server error occurred", Map.of());
    }

    private ResponseEntity<ErrorResponse> build(
            HttpServletRequest req, HttpStatus status, String codeKeyOrLiteral,
            Locale locale, String fallbackMsg, Map<String, String> validation) {

        ErrorResponse body = errorBody(req, codeKeyOrLiteral, locale, fallbackMsg, validation);
        return ResponseEntity.status(status)
                .contentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
                .body(body);
    }

    private ErrorResponse errorBody(
            HttpServletRequest req, String codeKeyOrLiteral,
            Locale locale, String fallbackMsg, Map<String, String> validation) {

        String traceId = (String) req.getAttribute(TRACE_ID);
        String path = req.getRequestURI();
        String message = resolve(codeKeyOrLiteral, locale, fallbackMsg);

        return ErrorResponse.of(traceId, path, codeKeyOrLiteral, message, validation);
    }

    private String resolve(String keyOrMsg, Locale locale, String fallback) {
        try {
            return messageSource.getMessage(keyOrMsg, null, locale);
        } catch (Exception ignore) {
            return (keyOrMsg != null && !keyOrMsg.isBlank()) ? keyOrMsg : fallback;
        }
    }

    private void logWarn(HttpServletRequest req, HttpStatus status, String tag, Exception ex) {
        String traceId = (String) req.getAttribute(TRACE_ID);
        String uri = req.getRequestURI();

        if (status.is4xxClientError() && !includeStackFor4xx) {
            log.warn("{} {} traceId={} uri={} msg={}",
                    status.value(), tag, traceId, uri, ex.toString());
            return;
        }

        log.warn("{} {} traceId={} uri={}", status.value(), tag, traceId, uri, ex);
    }

    private String codeForBind(Exception ex) {
        if (ex instanceof MissingServletRequestParameterException ||
                ex instanceof MissingPathVariableException ||
                ex instanceof MissingRequestHeaderException) {
            return "error.missing_param";
        }
        if (ex instanceof MethodArgumentTypeMismatchException) {
            return "error.type_mismatch";
        }
        if (ex instanceof ConstraintViolationException || ex instanceof BindException) {
            return "error.validation";
        }
        return "error.validation";
    }

    private String fallbackForBind(Exception ex) {
        if (ex instanceof MissingServletRequestParameterException ||
                ex instanceof MissingPathVariableException ||
                ex instanceof MissingRequestHeaderException) {
            return "Missing required parameter";
        }
        if (ex instanceof MethodArgumentTypeMismatchException) {
            return "Type mismatch";
        }
        if (ex instanceof ConstraintViolationException || ex instanceof BindException) {
            return "Validation failed";
        }
        return "Bad request";
    }

    private Map<String, String> extractJsonParseDetail(HttpMessageNotReadableException ex) {
        Throwable root = ex.getMostSpecificCause();
        Map<String, String> out = new LinkedHashMap<>();

        if (root instanceof InvalidFormatException ife) {
            out.put("field", jsonPath(ife));
            out.put("rejected", String.valueOf(ife.getValue()));
            out.put("expectedType", ife.getTargetType().getSimpleName());
        } else if (root instanceof MismatchedInputException mie) {
            out.put("field", jsonPath(mie));
            out.put("expectedType",
                    mie.getTargetType() != null ? mie.getTargetType().getSimpleName() : "Unknown");
        }
        return out;
    }

    private String jsonPath(JsonMappingException jme) {
        return jme.getPath().stream()
                .map(ref -> ref.getFieldName() != null
                        ? ref.getFieldName()
                        : "[" + ref.getIndex() + "]")
                .collect(Collectors.joining("."));
    }
}
