package com.ecommerce.platform.product.domain.repository;

import com.ecommerce.platform.product.domain.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Category entity.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    // Find all root categories (no parent)
    List<Category> findByParentIsNullAndIsActiveTrueOrderBySortOrderAsc();

    // Find children of a category
    List<Category> findByParentIdAndIsActiveTrueOrderBySortOrderAsc(Long parentId);

    // Find all active categories
    List<Category> findByIsActiveTrueOrderBySortOrderAsc();

    // Find category with children eagerly loaded
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.children WHERE c.id = :id")
    Optional<Category> findByIdWithChildren(Long id);

    // Find category by slug with children
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.children WHERE c.slug = :slug")
    Optional<Category> findBySlugWithChildren(String slug);
}
