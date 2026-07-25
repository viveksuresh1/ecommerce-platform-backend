package com.ecommerce.platform.product.api.dto;

import com.ecommerce.platform.product.domain.model.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for product.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private String name;
    private String slug;
    private String description;
    private String shortDescription;
    private BigDecimal price;
    private BigDecimal compareAtPrice;
    private String sku;
    private ProductStatus status;
    private Boolean isFeatured;
    private Boolean isOnSale;
    private BigDecimal discountPercentage;

    // Category info
    private Long categoryId;
    private String categoryName;
    private String categorySlug;

    // Images
    private String primaryImageUrl;
    private List<ProductImageResponse> images;

    // Attributes
    private List<ProductAttributeResponse> attributes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductImageResponse {
        private Long id;
        private String url;
        private String altText;
        private Boolean isPrimary;
        private Integer sortOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductAttributeResponse {
        private Long id;
        private String name;
        private String value;
    }
}
