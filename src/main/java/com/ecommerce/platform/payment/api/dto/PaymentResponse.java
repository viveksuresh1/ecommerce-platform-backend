package com.ecommerce.platform.payment.api.dto;

import com.ecommerce.platform.payment.domain.model.PaymentTransactionStatus;
import com.ecommerce.platform.payment.domain.model.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for payment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private String paymentNumber;
    private Long orderId;
    private String orderNumber;
    private BigDecimal amount;
    private String currency;
    private PaymentTransactionStatus status;
    private String paymentMethod;
    private String gatewayName;
    private String gatewayTransactionId;
    private String failureReason;
    private BigDecimal refundAmount;
    private String refundReason;
    private LocalDateTime refundedAt;
    private List<TransactionResponse> transactions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionResponse {
        private Long id;
        private TransactionType transactionType;
        private PaymentTransactionStatus status;
        private BigDecimal amount;
        private LocalDateTime createdAt;
    }
}
