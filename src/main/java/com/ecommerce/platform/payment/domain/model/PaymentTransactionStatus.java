package com.ecommerce.platform.payment.domain.model;

/**
 * Payment/transaction status.
 */
public enum PaymentTransactionStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    CANCELLED,
    REFUNDED
}
