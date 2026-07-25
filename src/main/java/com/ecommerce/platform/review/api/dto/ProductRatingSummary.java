package com.ecommerce.platform.review.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Summary of product ratings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRatingSummary {
    private Long productId;
    private Double averageRating;
    private Long totalReviews;
    private Map<Integer, Long> ratingDistribution;
}
