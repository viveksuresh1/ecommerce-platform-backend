package com.ecommerce.platform.order.api.dto;

import com.ecommerce.platform.order.domain.model.OrderStatus;
import com.ecommerce.platform.order.domain.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lightweight order DTO for lists.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderListResponse {
    private Long id;
    private String orderNumber;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private Integer itemCount;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
}
