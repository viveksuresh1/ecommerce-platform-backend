package com.ecommerce.platform.review.application.service;

import com.ecommerce.platform.order.domain.model.Order;
import com.ecommerce.platform.order.domain.model.OrderStatus;
import com.ecommerce.platform.order.domain.repository.OrderRepository;
import com.ecommerce.platform.product.domain.model.Product;
import com.ecommerce.platform.product.domain.repository.ProductRepository;
import com.ecommerce.platform.review.api.dto.CreateReviewRequest;
import com.ecommerce.platform.review.api.dto.ProductRatingSummary;
import com.ecommerce.platform.review.api.dto.ReviewResponse;
import com.ecommerce.platform.review.api.dto.UpdateReviewRequest;
import com.ecommerce.platform.review.domain.model.Review;
import com.ecommerce.platform.review.domain.model.ReviewHelpfulVote;
import com.ecommerce.platform.review.domain.repository.ReviewHelpfulVoteRepository;
import com.ecommerce.platform.review.domain.repository.ReviewRepository;
import com.ecommerce.platform.shared.dto.PagedResponse;
import com.ecommerce.platform.shared.exception.BadRequestException;
import com.ecommerce.platform.shared.exception.DuplicateResourceException;
import com.ecommerce.platform.shared.exception.ForbiddenException;
import com.ecommerce.platform.shared.exception.ResourceNotFoundException;
import com.ecommerce.platform.user.domain.model.User;
import com.ecommerce.platform.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for review operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewHelpfulVoteRepository helpfulVoteRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    /**
     * Create a review.
     */
    @Transactional
    public ReviewResponse createReview(CreateReviewRequest request) {
        User user = getCurrentUser();
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        // Check if already reviewed
        if (reviewRepository.existsByProductIdAndUserId(product.getId(), user.getId())) {
            throw new DuplicateResourceException("Review", "product", product.getId());
        }

        // Check if verified purchase
        boolean isVerified = false;
        Order order = null;
        if (request.getOrderId() != null) {
            order = orderRepository.findById(request.getOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Order", "id", request.getOrderId()));
            if (!order.getUser().getId().equals(user.getId())) {
                throw new ForbiddenException("Order does not belong to user");
            }
            if (order.getStatus() == OrderStatus.DELIVERED) {
                isVerified = order.getItems().stream()
                        .anyMatch(item -> item.getProduct().getId().equals(product.getId()));
            }
        }

        Review review = Review.builder()
                .product(product)
                .user(user)
                .order(order)
                .rating(request.getRating())
                .title(request.getTitle())
                .comment(request.getComment())
                .isVerifiedPurchase(isVerified)
                .isApproved(true)
                .helpfulCount(0)
                .build();

        review = reviewRepository.save(review);
        log.info("Review created for product {} by user {}", product.getSlug(), user.getEmail());

        return toReviewResponse(review, false);
    }

    /**
     * Update a review.
     */
    @Transactional
    public ReviewResponse updateReview(Long reviewId, UpdateReviewRequest request) {
        User user = getCurrentUser();
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        if (!review.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Cannot update another user's review");
        }

        if (request.getRating() != null) {
            review.setRating(request.getRating());
        }
        if (request.getTitle() != null) {
            review.setTitle(request.getTitle());
        }
        if (request.getComment() != null) {
            review.setComment(request.getComment());
        }

        review = reviewRepository.save(review);
        log.info("Review {} updated", reviewId);

        return toReviewResponse(review, false);
    }

    /**
     * Delete a review.
     */
    @Transactional
    public void deleteReview(Long reviewId) {
        User user = getCurrentUser();
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        if (!review.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Cannot delete another user's review");
        }

        reviewRepository.delete(review);
        log.info("Review {} deleted", reviewId);
    }

    /**
     * Get reviews for a product (public).
     */
    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> getProductReviews(Long productId, Pageable pageable) {
        User currentUser = getCurrentUserOptional();
        Page<Review> reviews = reviewRepository.findByProductIdAndIsApprovedTrueOrderByCreatedAtDesc(productId, pageable);
        Page<ReviewResponse> responsePage = reviews.map(review -> {
            boolean markedHelpful = currentUser != null &&
                    helpfulVoteRepository.existsByReviewIdAndUserId(review.getId(), currentUser.getId());
            return toReviewResponse(review, markedHelpful);
        });
        return PagedResponse.from(responsePage);
    }

    /**
     * Get rating summary for a product.
     */
    @Transactional(readOnly = true)
    public ProductRatingSummary getProductRatingSummary(Long productId) {
        Double avgRating = reviewRepository.getAverageRatingByProductId(productId);
        Long totalReviews = reviewRepository.countByProductId(productId);
        List<Object[]> distribution = reviewRepository.getRatingDistributionByProductId(productId);

        Map<Integer, Long> ratingDist = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            ratingDist.put(i, 0L);
        }
        for (Object[] row : distribution) {
            Integer rating = (Integer) row[0];
            Long count = (Long) row[1];
            ratingDist.put(rating, count);
        }

        return ProductRatingSummary.builder()
                .productId(productId)
                .averageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0)
                .totalReviews(totalReviews)
                .ratingDistribution(ratingDist)
                .build();
    }

    /**
     * Get user's reviews.
     */
    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> getMyReviews(Pageable pageable) {
        User user = getCurrentUser();
        Page<Review> reviews = reviewRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        Page<ReviewResponse> responsePage = reviews.map(review -> toReviewResponse(review, false));
        return PagedResponse.from(responsePage);
    }

    /**
     * Mark review as helpful.
     */
    @Transactional
    public ReviewResponse markHelpful(Long reviewId) {
        User user = getCurrentUser();
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        if (review.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Cannot mark your own review as helpful");
        }

        if (helpfulVoteRepository.existsByReviewIdAndUserId(reviewId, user.getId())) {
            throw new BadRequestException("Already marked as helpful");
        }

        ReviewHelpfulVote vote = ReviewHelpfulVote.builder()
                .review(review)
                .user(user)
                .build();
        helpfulVoteRepository.save(vote);

        review.incrementHelpful();
        review = reviewRepository.save(review);

        return toReviewResponse(review, true);
    }

    /**
     * Unmark review as helpful.
     */
    @Transactional
    public ReviewResponse unmarkHelpful(Long reviewId) {
        User user = getCurrentUser();
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        ReviewHelpfulVote vote = helpfulVoteRepository.findByReviewIdAndUserId(reviewId, user.getId())
                .orElseThrow(() -> new BadRequestException("Not marked as helpful"));

        helpfulVoteRepository.delete(vote);
        review.decrementHelpful();
        review = reviewRepository.save(review);

        return toReviewResponse(review, false);
    }

    // ==================== Admin Methods ====================

    /**
     * Get pending reviews (admin).
     */
    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> getPendingReviews(Pageable pageable) {
        Page<Review> reviews = reviewRepository.findByIsApprovedFalseOrderByCreatedAtDesc(pageable);
        Page<ReviewResponse> responsePage = reviews.map(review -> toReviewResponse(review, false));
        return PagedResponse.from(responsePage);
    }

    /**
     * Approve/reject review (admin).
     */
    @Transactional
    public ReviewResponse moderateReview(Long reviewId, boolean approve) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        review.setIsApproved(approve);
        review = reviewRepository.save(review);

        log.info("Review {} {}", reviewId, approve ? "approved" : "rejected");
        return toReviewResponse(review, false);
    }

    /**
     * Delete review (admin).
     */
    @Transactional
    public void deleteReviewAdmin(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));
        reviewRepository.delete(review);
        log.info("Review {} deleted by admin", reviewId);
    }

    // ==================== Helper Methods ====================

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private User getCurrentUserOptional() {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            if ("anonymousUser".equals(email)) {
                return null;
            }
            return userRepository.findByEmail(email).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private ReviewResponse toReviewResponse(Review review, boolean markedHelpful) {
        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .productName(review.getProduct().getName())
                .productSlug(review.getProduct().getSlug())
                .userId(review.getUser().getId())
                .userName(review.getUser().getFirstName())
                .rating(review.getRating())
                .title(review.getTitle())
                .comment(review.getComment())
                .isVerifiedPurchase(review.getIsVerifiedPurchase())
                .isApproved(review.getIsApproved())
                .helpfulCount(review.getHelpfulCount())
                .markedHelpfulByCurrentUser(markedHelpful)
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
