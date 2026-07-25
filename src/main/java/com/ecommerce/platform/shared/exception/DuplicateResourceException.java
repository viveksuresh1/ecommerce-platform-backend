package com.ecommerce.platform.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when trying to create a resource that already exists.
 * Returns HTTP 409 Conflict.
 */
public class DuplicateResourceException extends BaseException {

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(
            String.format("%s already exists with %s: '%s'", resourceName, fieldName, fieldValue),
            HttpStatus.CONFLICT,
            resourceName.toUpperCase() + "_ALREADY_EXISTS"
        );
    }
}
