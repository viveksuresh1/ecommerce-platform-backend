package com.ecommerce.platform.user.domain.model;

/**
 * User account status.
 */
public enum UserStatus {
    ACTIVE,      // Normal active account
    INACTIVE,    // Disabled by admin
    SUSPENDED,   // Temporarily suspended
    DELETED      // Soft deleted
}
