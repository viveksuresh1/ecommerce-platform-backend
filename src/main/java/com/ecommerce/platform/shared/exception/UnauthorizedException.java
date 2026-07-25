package com.ecommerce.platform.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when authentication fails.
 * Returns HTTP 401.
 */
public class UnauthorizedException extends BaseException {

    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }

    public UnauthorizedException() {
        super("Authentication required", HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }
}
