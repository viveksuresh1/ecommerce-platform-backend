package com.ecommerce.platform.product.domain.repository;

import com.ecommerce.platform.product.domain.model.Product;
import com.ecommerce.platform.product.domain.model.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Product entity.
 * Extends JpaSpecificationExecutor for dynamic queries.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySku(String sku);

    // Find products by status
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    // Find products by category
    Page<Product> findByCategoryIdAndStatus(Long categoryId, ProductStatus status, Pageable pageable);

    // Find featured products
    Page<Product> findByIsFeaturedTrueAndStatus(ProductStatus status, Pageable pageable);

    // Find products in price range
    Page<Product> findByPriceBetweenAndStatus(BigDecimal minPrice, BigDecimal maxPrice,
                                               ProductStatus status, Pageable pageable);

    // Search products by name or description
    @Query("SELECT p FROM Product p WHERE p.status = :status AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Product> searchProducts(@Param("status") ProductStatus status,
                                 @Param("search") String search,
                                 Pageable pageable);

    // Find product with images eagerly loaded
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.images WHERE p.id = :id")
    Optional<Product> findByIdWithImages(@Param("id") Long id);

    // Find product with category loaded
    @Query("SELECT p FROM Product p " +
           "LEFT JOIN FETCH p.category " +
           "WHERE p.slug = :slug")
    Optional<Product> findBySlugWithDetails(@Param("slug") String slug);

    // Count products by category
    long countByCategoryIdAndStatus(Long categoryId, ProductStatus status);

    // Find products by multiple category IDs
    Page<Product> findByCategoryIdInAndStatus(List<Long> categoryIds, ProductStatus status, Pageable pageable);
}
