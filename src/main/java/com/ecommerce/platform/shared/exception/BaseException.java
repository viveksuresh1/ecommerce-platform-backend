package com.ecommerce.platform.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base exception for all domain exceptions.
 * Carries HTTP status and error code for consistent error responses.
 */
@Getter
public abstract class BaseException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    protected BaseException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    protected BaseException(String message, HttpStatus status, String errorCode, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
    }
}
