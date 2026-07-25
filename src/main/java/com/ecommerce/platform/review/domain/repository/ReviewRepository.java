package com.ecommerce.platform.review.domain.repository;

import com.ecommerce.platform.review.domain.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Review entity.
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByProductIdAndIsApprovedTrueOrderByCreatedAtDesc(Long productId, Pageable pageable);

    Page<Review> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    Page<Review> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<Review> findByProductIdAndUserId(Long productId, Long userId);

    boolean existsByProductIdAndUserId(Long productId, Long userId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId AND r.isApproved = true")
    Double getAverageRatingByProductId(@Param("productId") Long productId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId AND r.isApproved = true")
    Long countByProductId(@Param("productId") Long productId);

    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.product.id = :productId AND r.isApproved = true GROUP BY r.rating")
    List<Object[]> getRatingDistributionByProductId(@Param("productId") Long productId);

    Page<Review> findByIsApprovedFalseOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT r FROM Review r WHERE r.product.id = :productId AND r.isApproved = true " +
           "AND r.isVerifiedPurchase = true ORDER BY r.helpfulCount DESC, r.createdAt DESC")
    Page<Review> findVerifiedReviewsByProductId(@Param("productId") Long productId, Pageable pageable);
}
