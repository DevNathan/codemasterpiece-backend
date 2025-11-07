package com.app.codemasterpiecebackend.support.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class FieldValidationException extends RuntimeException {
    private final Map<String, String> errors;

    public FieldValidationException(Map<String, String> errors) {
        super("VALIDATION_ERROR");
        this.errors = errors;
    }
}