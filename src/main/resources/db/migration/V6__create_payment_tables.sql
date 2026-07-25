-- V6: Payment tables
-- Payment records and transaction history

-- Payments table
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    payment_number VARCHAR(50) NOT NULL UNIQUE,
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(50) NOT NULL,

    -- Payment gateway details
    gateway_name VARCHAR(50),
    gateway_transaction_id VARCHAR(255),
    gateway_response TEXT,

    -- Failure tracking
    failure_reason TEXT,
    retry_count INT NOT NULL DEFAULT 0,

    -- Refund tracking
    refund_amount DECIMAL(12,2),
    refund_reason TEXT,
    refunded_at TIMESTAMP,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Payment transactions log (for audit)
CREATE TABLE payment_transactions (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    transaction_type VARCHAR(30) NOT NULL, -- INITIATE, AUTHORIZE, CAPTURE, REFUND, VOID
    status VARCHAR(30) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    gateway_response TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_payments_order ON payments(order_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_number ON payments(payment_number);
CREATE INDEX idx_payments_gateway_txn ON payments(gateway_transaction_id);
CREATE INDEX idx_payment_transactions_payment ON payment_transactions(payment_id);
