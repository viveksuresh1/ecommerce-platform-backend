package com.ecommerce.platform.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when user lacks permission for an action.
 * Returns HTTP 403.
 */
public class ForbiddenException extends BaseException {

    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN, "FORBIDDEN");
    }

    public ForbiddenException() {
        super("You don't have permission to perform this action", HttpStatus.FORBIDDEN, "FORBIDDEN");
    }
}
