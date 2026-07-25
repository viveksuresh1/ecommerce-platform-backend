package com.ecommerce.platform.order.domain.model;

/**
 * Payment status enum.
 */
public enum PaymentStatus {
    PENDING,        // Awaiting payment
    PAID,           // Payment received
    FAILED,         // Payment failed
    REFUNDED        // Payment refunded
}
