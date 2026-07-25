package com.ecommerce.platform.product.api.mapper;

import com.ecommerce.platform.product.api.dto.CategoryResponse;
import com.ecommerce.platform.product.api.dto.ProductListResponse;
import com.ecommerce.platform.product.api.dto.ProductResponse;
import com.ecommerce.platform.product.domain.model.Category;
import com.ecommerce.platform.product.domain.model.Product;
import com.ecommerce.platform.product.domain.model.ProductAttribute;
import com.ecommerce.platform.product.domain.model.ProductImage;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for Product and Category entities to DTOs.
 */
@Component
public class ProductMapper {

    // ==================== Category Mapping ====================

    public CategoryResponse toCategoryResponse(Category category) {
        return toCategoryResponse(category, false, null);
    }

    public CategoryResponse toCategoryResponse(Category category, boolean includeChildren, Long productCount) {
        CategoryResponse.CategoryResponseBuilder builder = CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .isActive(category.getIsActive())
                .sortOrder(category.getSortOrder())
                .productCount(productCount);

        if (category.getParent() != null) {
            builder.parentId(category.getParent().getId())
                   .parentName(category.getParent().getName());
        }

        if (includeChildren && category.getChildren() != null) {
            builder.children(category.getChildren().stream()
                    .filter(Category::getIsActive)
                    .map(c -> toCategoryResponse(c, false, null))
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }

    public List<CategoryResponse> toCategoryResponseList(List<Category> categories) {
        return categories.stream()
                .map(this::toCategoryResponse)
                .collect(Collectors.toList());
    }

    // ==================== Product Mapping ====================

    public ProductResponse toProductResponse(Product product) {
        ProductResponse.ProductResponseBuilder builder = ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .shortDescription(product.getShortDescription())
                .price(product.getPrice())
                .compareAtPrice(product.getCompareAtPrice())
                .sku(product.getSku())
                .status(product.getStatus())
                .isFeatured(product.getIsFeatured())
                .isOnSale(product.isOnSale())
                .discountPercentage(product.getDiscountPercentage())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt());

        // Category info
        if (product.getCategory() != null) {
            builder.categoryId(product.getCategory().getId())
                   .categoryName(product.getCategory().getName())
                   .categorySlug(product.getCategory().getSlug());
        }

        // Primary image
        ProductImage primaryImage = product.getPrimaryImage();
        if (primaryImage != null) {
            builder.primaryImageUrl(primaryImage.getUrl());
        }

        // All images
        if (product.getImages() != null) {
            builder.images(product.getImages().stream()
                    .map(this::toImageResponse)
                    .collect(Collectors.toList()));
        }

        // Attributes
        if (product.getAttributes() != null) {
            builder.attributes(product.getAttributes().stream()
                    .map(this::toAttributeResponse)
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }

    public ProductListResponse toProductListResponse(Product product) {
        ProductListResponse.ProductListResponseBuilder builder = ProductListResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .shortDescription(product.getShortDescription())
                .price(product.getPrice())
                .compareAtPrice(product.getCompareAtPrice())
                .status(product.getStatus())
                .isFeatured(product.getIsFeatured())
                .isOnSale(product.isOnSale())
                .discountPercentage(product.getDiscountPercentage());

        if (product.getCategory() != null) {
            builder.categoryName(product.getCategory().getName())
                   .categorySlug(product.getCategory().getSlug());
        }

        ProductImage primaryImage = product.getPrimaryImage();
        if (primaryImage != null) {
            builder.primaryImageUrl(primaryImage.getUrl());
        }

        return builder.build();
    }

    public List<ProductListResponse> toProductListResponseList(List<Product> products) {
        return products.stream()
                .map(this::toProductListResponse)
                .collect(Collectors.toList());
    }

    private ProductResponse.ProductImageResponse toImageResponse(ProductImage image) {
        return ProductResponse.ProductImageResponse.builder()
                .id(image.getId())
                .url(image.getUrl())
                .altText(image.getAltText())
                .isPrimary(image.getIsPrimary())
                .sortOrder(image.getSortOrder())
                .build();
    }

    private ProductResponse.ProductAttributeResponse toAttributeResponse(ProductAttribute attr) {
        return ProductResponse.ProductAttributeResponse.builder()
                .id(attr.getId())
                .name(attr.getName())
                .value(attr.getValue())
                .build();
    }
}
