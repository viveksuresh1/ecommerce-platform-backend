package com.ecommerce.platform.payment.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for processing/confirming payment (simulated gateway callback).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessPaymentRequest {

    @NotBlank(message = "Gateway transaction ID is required")
    private String gatewayTransactionId;

    private boolean success;

    private String failureReason;

    private String gatewayResponse;
}
