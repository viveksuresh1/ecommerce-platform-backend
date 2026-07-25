package com.ecommerce.platform.inventory.api.dto;

import com.ecommerce.platform.inventory.domain.model.MovementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for stock movement history.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementResponse {
    private Long id;
    private Long productId;
    private String productName;
    private MovementType movementType;
    private Integer quantity;
    private String referenceType;
    private Long referenceId;
    private String notes;
    private String createdByEmail;
    private LocalDateTime createdAt;
}
