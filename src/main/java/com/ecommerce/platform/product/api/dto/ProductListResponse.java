package com.ecommerce.platform.product.api.dto;

import com.ecommerce.platform.product.domain.model.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Lightweight response DTO for product lists.
 * Excludes full description and attributes for performance.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductListResponse {

    private Long id;
    private String name;
    private String slug;
    private String shortDescription;
    private BigDecimal price;
    private BigDecimal compareAtPrice;
    private ProductStatus status;
    private Boolean isFeatured;
    private Boolean isOnSale;
    private BigDecimal discountPercentage;
    private String primaryImageUrl;
    private String categoryName;
    private String categorySlug;
}
