package com.ecommerce.platform.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when request data is invalid.
 * Returns HTTP 400.
 */
public class BadRequestException extends BaseException {

    public BadRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "BAD_REQUEST");
    }

    public BadRequestException(String message, String errorCode) {
        super(message, HttpStatus.BAD_REQUEST, errorCode);
    }
}
