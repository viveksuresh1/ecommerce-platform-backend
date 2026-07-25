package com.ecommerce.platform.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested resource doesn't exist.
 * Returns HTTP 404.
 */
public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(
            String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue),
            HttpStatus.NOT_FOUND,
            resourceName.toUpperCase() + "_NOT_FOUND"
        );
    }

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }
}
