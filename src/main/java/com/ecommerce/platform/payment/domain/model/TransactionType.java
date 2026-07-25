package com.ecommerce.platform.payment.domain.model;

/**
 * Payment transaction types.
 */
public enum TransactionType {
    INITIATE,       // Payment initiated
    AUTHORIZE,      // Payment authorized (for cards)
    CAPTURE,        // Payment captured/completed
    REFUND,         // Full or partial refund
    VOID            // Transaction voided
}
