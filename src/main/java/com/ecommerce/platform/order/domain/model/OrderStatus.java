package com.ecommerce.platform.order.domain.model;

/**
 * Order status enum.
 */
public enum OrderStatus {
    PENDING,        // Order placed, awaiting payment
    CONFIRMED,      // Payment confirmed
    PROCESSING,     // Being prepared
    SHIPPED,        // Shipped to customer
    DELIVERED,      // Delivered
    CANCELLED,      // Cancelled by user or admin
    REFUNDED        // Refunded
}
