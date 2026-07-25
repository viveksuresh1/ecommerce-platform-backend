package com.ecommerce.platform.product.application.service;

import com.ecommerce.platform.product.api.dto.CreateProductRequest;
import com.ecommerce.platform.product.api.dto.ProductListResponse;
import com.ecommerce.platform.product.api.dto.ProductResponse;
import com.ecommerce.platform.product.api.dto.UpdateProductRequest;
import com.ecommerce.platform.product.api.mapper.ProductMapper;
import com.ecommerce.platform.product.domain.model.Category;
import com.ecommerce.platform.product.domain.model.Product;
import com.ecommerce.platform.product.domain.model.ProductAttribute;
import com.ecommerce.platform.product.domain.model.ProductStatus;
import com.ecommerce.platform.product.domain.repository.CategoryRepository;
import com.ecommerce.platform.product.domain.repository.ProductRepository;
import com.ecommerce.platform.shared.dto.PagedResponse;
import com.ecommerce.platform.shared.exception.DuplicateResourceException;
import com.ecommerce.platform.shared.exception.ResourceNotFoundException;
import com.ecommerce.platform.shared.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for product operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    /**
     * Get all active products with pagination.
     */
    @Transactional(readOnly = true)
    public PagedResponse<ProductListResponse> getAllProducts(Pageable pageable) {
        Page<Product> products = productRepository.findByStatus(ProductStatus.ACTIVE, pageable);
        Page<ProductListResponse> responsePage = products.map(productMapper::toProductListResponse);
        return PagedResponse.from(responsePage);
    }

    /**
     * Get products by category.
     */
    @Transactional(readOnly = true)
    public PagedResponse<ProductListResponse> getProductsByCategory(String categorySlug, Pageable pageable) {
        Category category = categoryRepository.findBySlug(categorySlug)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", categorySlug));

        Page<Product> products = productRepository.findByCategoryIdAndStatus(
                category.getId(), ProductStatus.ACTIVE, pageable);
        Page<ProductListResponse> responsePage = products.map(productMapper::toProductListResponse);
        return PagedResponse.from(responsePage);
    }

    /**
     * Get featured products.
     */
    @Transactional(readOnly = true)
    public PagedResponse<ProductListResponse> getFeaturedProducts(Pageable pageable) {
        Page<Product> products = productRepository.findByIsFeaturedTrueAndStatus(ProductStatus.ACTIVE, pageable);
        Page<ProductListResponse> responsePage = products.map(productMapper::toProductListResponse);
        return PagedResponse.from(responsePage);
    }

    /**
     * Search products.
     */
    @Transactional(readOnly = true)
    public PagedResponse<ProductListResponse> searchProducts(String query, Pageable pageable) {
        Page<Product> products = productRepository.searchProducts(ProductStatus.ACTIVE, query, pageable);
        Page<ProductListResponse> responsePage = products.map(productMapper::toProductListResponse);
        return PagedResponse.from(responsePage);
    }

    /**
     * Get product by slug (full details).
     */
    @Transactional(readOnly = true)
    public ProductResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlugWithDetails(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "slug", slug));
        // Initialize lazy collections within transaction
        product.getImages().size();
        product.getAttributes().size();
        return productMapper.toProductResponse(product);
    }

    /**
     * Get product by ID (full details).
     */
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        // Initialize lazy collections within transaction
        product.getImages().size();
        product.getAttributes().size();
        if (product.getCategory() != null) {
            product.getCategory().getName();
        }
        return productMapper.toProductResponse(product);
    }

    /**
     * Create a new product (Admin only).
     */
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        String slug = SlugUtil.toSlug(request.getName());

        // Ensure slug is unique
        if (productRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis();
        }

        // Check SKU uniqueness if provided
        if (request.getSku() != null && productRepository.existsBySku(request.getSku())) {
            throw new DuplicateResourceException("Product", "sku", request.getSku());
        }

        Product product = Product.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .shortDescription(request.getShortDescription())
                .price(request.getPrice())
                .compareAtPrice(request.getCompareAtPrice())
                .sku(request.getSku())
                .status(request.getStatus() != null ? request.getStatus() : ProductStatus.DRAFT)
                .isFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : false)
                .build();

        // Set category if provided
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
            product.setCategory(category);
        }

        // Add attributes if provided
        if (request.getAttributes() != null) {
            for (CreateProductRequest.ProductAttributeRequest attr : request.getAttributes()) {
                ProductAttribute attribute = ProductAttribute.builder()
                        .name(attr.getName())
                        .value(attr.getValue())
                        .build();
                product.addAttribute(attribute);
            }
        }

        product = productRepository.save(product);
        log.info("Product created: {} ({})", product.getName(), product.getSlug());

        return productMapper.toProductResponse(product);
    }

    /**
     * Update a product (Admin only).
     */
    @Transactional
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        if (request.getName() != null) {
            product.setName(request.getName());
            // Optionally update slug
            String newSlug = SlugUtil.toSlug(request.getName());
            if (!newSlug.equals(product.getSlug()) && !productRepository.existsBySlug(newSlug)) {
                product.setSlug(newSlug);
            }
        }

        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getShortDescription() != null) {
            product.setShortDescription(request.getShortDescription());
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getCompareAtPrice() != null) {
            product.setCompareAtPrice(request.getCompareAtPrice());
        }
        if (request.getSku() != null) {
            if (!request.getSku().equals(product.getSku()) && productRepository.existsBySku(request.getSku())) {
                throw new DuplicateResourceException("Product", "sku", request.getSku());
            }
            product.setSku(request.getSku());
        }
        if (request.getStatus() != null) {
            product.setStatus(request.getStatus());
        }
        if (request.getIsFeatured() != null) {
            product.setIsFeatured(request.getIsFeatured());
        }
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
            product.setCategory(category);
        }

        product = productRepository.save(product);
        log.info("Product updated: {}", product.getSlug());

        return productMapper.toProductResponse(product);
    }

    /**
     * Delete a product (Admin only).
     */
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        // Soft delete - mark as inactive
        product.setStatus(ProductStatus.INACTIVE);
        productRepository.save(product);

        log.info("Product deleted (soft): {}", product.getSlug());
    }

    /**
     * Get all products for admin (includes drafts and inactive).
     */
    @Transactional(readOnly = true)
    public PagedResponse<ProductListResponse> getAllProductsAdmin(Pageable pageable) {
        Page<Product> products = productRepository.findAll(pageable);
        Page<ProductListResponse> responsePage = products.map(productMapper::toProductListResponse);
        return PagedResponse.from(responsePage);
    }
}
