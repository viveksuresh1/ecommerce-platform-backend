package com.ecommerce.platform.review.api.controller;

import com.ecommerce.platform.review.api.dto.CreateReviewRequest;
import com.ecommerce.platform.review.api.dto.ProductRatingSummary;
import com.ecommerce.platform.review.api.dto.ReviewResponse;
import com.ecommerce.platform.review.api.dto.UpdateReviewRequest;
import com.ecommerce.platform.review.application.service.ReviewService;
import com.ecommerce.platform.shared.dto.ApiResponse;
import com.ecommerce.platform.shared.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Product review management")
public class ReviewController {

    private final ReviewService reviewService;

    // ==================== Public Endpoints ====================

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get reviews for a product")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<ReviewResponse> reviews = reviewService.getProductReviews(productId, pageable);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    @GetMapping("/product/{productId}/summary")
    @Operation(summary = "Get rating summary for a product")
    public ResponseEntity<ApiResponse<ProductRatingSummary>> getProductRatingSummary(
            @PathVariable Long productId) {
        ProductRatingSummary summary = reviewService.getProductRatingSummary(productId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    // ==================== Authenticated User Endpoints ====================

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Create a review")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @Valid @RequestBody CreateReviewRequest request) {
        ReviewResponse review = reviewService.createReview(request);
        return ResponseEntity.ok(ApiResponse.success(review, "Review created successfully"));
    }

    @PutMapping("/{reviewId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Update your review")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody UpdateReviewRequest request) {
        ReviewResponse review = reviewService.updateReview(reviewId, request);
        return ResponseEntity.ok(ApiResponse.success(review, "Review updated successfully"));
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Delete your review")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.ok(ApiResponse.<Void>success(null, "Review deleted successfully"));
    }

    @GetMapping("/my-reviews")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Get my reviews")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> getMyReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<ReviewResponse> reviews = reviewService.getMyReviews(pageable);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    @PostMapping("/{reviewId}/helpful")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Mark review as helpful")
    public ResponseEntity<ApiResponse<ReviewResponse>> markHelpful(@PathVariable Long reviewId) {
        ReviewResponse review = reviewService.markHelpful(reviewId);
        return ResponseEntity.ok(ApiResponse.success(review, "Marked as helpful"));
    }

    @DeleteMapping("/{reviewId}/helpful")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Unmark review as helpful")
    public ResponseEntity<ApiResponse<ReviewResponse>> unmarkHelpful(@PathVariable Long reviewId) {
        ReviewResponse review = reviewService.unmarkHelpful(reviewId);
        return ResponseEntity.ok(ApiResponse.success(review, "Removed helpful mark"));
    }

    // ==================== Admin Endpoints ====================

    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get pending reviews (Admin)")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> getPendingReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<ReviewResponse> reviews = reviewService.getPendingReviews(pageable);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    @PutMapping("/admin/{reviewId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve a review (Admin)")
    public ResponseEntity<ApiResponse<ReviewResponse>> approveReview(@PathVariable Long reviewId) {
        ReviewResponse review = reviewService.moderateReview(reviewId, true);
        return ResponseEntity.ok(ApiResponse.success(review, "Review approved"));
    }

    @PutMapping("/admin/{reviewId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject a review (Admin)")
    public ResponseEntity<ApiResponse<ReviewResponse>> rejectReview(@PathVariable Long reviewId) {
        ReviewResponse review = reviewService.moderateReview(reviewId, false);
        return ResponseEntity.ok(ApiResponse.success(review, "Review rejected"));
    }

    @DeleteMapping("/admin/{reviewId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete any review (Admin)")
    public ResponseEntity<ApiResponse<Void>> deleteReviewAdmin(@PathVariable Long reviewId) {
        reviewService.deleteReviewAdmin(reviewId);
        return ResponseEntity.ok(ApiResponse.<Void>success(null, "Review deleted"));
    }
}
