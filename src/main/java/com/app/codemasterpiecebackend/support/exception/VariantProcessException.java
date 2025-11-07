package com.app.codemasterpiecebackend.support.exception;

public class VariantProcessException extends RuntimeException {
    public VariantProcessException(String message) {
        super(message);
    }

    public VariantProcessException(String message, Throwable cause) {
        super(message, cause);
    }
}
