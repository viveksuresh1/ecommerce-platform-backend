package com.ecommerce.platform.review.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for review.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productSlug;
    private Long userId;
    private String userName;
    private Integer rating;
    private String title;
    private String comment;
    private Boolean isVerifiedPurchase;
    private Boolean isApproved;
    private Integer helpfulCount;
    private Boolean markedHelpfulByCurrentUser;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
