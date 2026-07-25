package com.ecommerce.platform.product.application.service;

import com.ecommerce.platform.product.api.dto.CategoryResponse;
import com.ecommerce.platform.product.api.dto.CreateCategoryRequest;
import com.ecommerce.platform.product.api.mapper.ProductMapper;
import com.ecommerce.platform.product.domain.model.Category;
import com.ecommerce.platform.product.domain.model.ProductStatus;
import com.ecommerce.platform.product.domain.repository.CategoryRepository;
import com.ecommerce.platform.product.domain.repository.ProductRepository;
import com.ecommerce.platform.shared.exception.DuplicateResourceException;
import com.ecommerce.platform.shared.exception.ResourceNotFoundException;
import com.ecommerce.platform.shared.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for category operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    /**
     * Get all root categories with their children.
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        List<Category> rootCategories = categoryRepository.findByParentIsNullAndIsActiveTrueOrderBySortOrderAsc();
        return rootCategories.stream()
                .map(cat -> productMapper.toCategoryResponse(cat, true, getProductCount(cat.getId())))
                .collect(Collectors.toList());
    }

    /**
     * Get category by slug with children.
     */
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlugWithChildren(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", slug));
        return productMapper.toCategoryResponse(category, true, getProductCount(category.getId()));
    }

    /**
     * Get category by ID.
     */
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findByIdWithChildren(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        return productMapper.toCategoryResponse(category, true, getProductCount(id));
    }

    /**
     * Create a new category (Admin only).
     */
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        String slug = SlugUtil.toSlug(request.getName());

        // Check slug uniqueness
        if (categoryRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Category", "slug", slug);
        }

        Category category = Category.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();

        // Set parent if provided
        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category", "id", request.getParentId()));
            category.setParent(parent);
        }

        category = categoryRepository.save(category);
        log.info("Category created: {} ({})", category.getName(), category.getSlug());

        return productMapper.toCategoryResponse(category, false, 0L);
    }

    /**
     * Update a category (Admin only).
     */
    @Transactional
    public CategoryResponse updateCategory(Long id, CreateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        if (request.getName() != null) {
            String newSlug = SlugUtil.toSlug(request.getName());
            if (!newSlug.equals(category.getSlug()) && categoryRepository.existsBySlug(newSlug)) {
                throw new DuplicateResourceException("Category", "slug", newSlug);
            }
            category.setName(request.getName());
            category.setSlug(newSlug);
        }

        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        if (request.getImageUrl() != null) {
            category.setImageUrl(request.getImageUrl());
        }
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }
        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category", "id", request.getParentId()));
            category.setParent(parent);
        }

        category = categoryRepository.save(category);
        log.info("Category updated: {}", category.getSlug());

        return productMapper.toCategoryResponse(category, true, getProductCount(id));
    }

    /**
     * Delete a category (Admin only).
     */
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        // Soft delete - just mark as inactive
        category.setIsActive(false);
        categoryRepository.save(category);

        log.info("Category deleted (soft): {}", category.getSlug());
    }

    private Long getProductCount(Long categoryId) {
        return productRepository.countByCategoryIdAndStatus(categoryId, ProductStatus.ACTIVE);
    }
}
