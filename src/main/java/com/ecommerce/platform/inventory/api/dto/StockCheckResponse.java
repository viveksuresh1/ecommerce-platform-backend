package com.ecommerce.platform.inventory.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simple response for stock availability check.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockCheckResponse {
    private Long productId;
    private Integer availableQuantity;
    private Boolean inStock;
    private Boolean sufficientStock;
    private Integer requestedQuantity;
}
