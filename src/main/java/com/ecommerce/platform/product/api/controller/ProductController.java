package com.ecommerce.platform.product.api.controller;

import com.ecommerce.platform.product.api.dto.CreateProductRequest;
import com.ecommerce.platform.product.api.dto.ProductListResponse;
import com.ecommerce.platform.product.api.dto.ProductResponse;
import com.ecommerce.platform.product.api.dto.UpdateProductRequest;
import com.ecommerce.platform.product.application.service.ProductService;
import com.ecommerce.platform.shared.dto.ApiResponse;
import com.ecommerce.platform.shared.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Product endpoints.
 * Public: GET endpoints for browsing products
 * Admin only: POST, PUT, DELETE for product management
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product browsing and management")
public class ProductController {

    private final ProductService productService;

    // ==================== Public Endpoints ====================

    @GetMapping("/products")
    @Operation(summary = "Get all products", description = "Returns paginated list of active products")
    public ApiResponse<PagedResponse<ProductListResponse>> getAllProducts(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PagedResponse<ProductListResponse> products = productService.getAllProducts(pageable);
        return ApiResponse.success(products);
    }

    @GetMapping("/products/featured")
    @Operation(summary = "Get featured products", description = "Returns paginated list of featured products")
    public ApiResponse<PagedResponse<ProductListResponse>> getFeaturedProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PagedResponse<ProductListResponse> products = productService.getFeaturedProducts(pageable);
        return ApiResponse.success(products);
    }

    @GetMapping("/products/search")
    @Operation(summary = "Search products", description = "Search products by name or description")
    public ApiResponse<PagedResponse<ProductListResponse>> searchProducts(
            @Parameter(description = "Search query") @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<ProductListResponse> products = productService.searchProducts(q, pageable);
        return ApiResponse.success(products);
    }

    @GetMapping("/products/{slug}")
    @Operation(summary = "Get product by slug", description = "Returns full product details")
    public ApiResponse<ProductResponse> getProductBySlug(@PathVariable String slug) {
        ProductResponse product = productService.getProductBySlug(slug);
        return ApiResponse.success(product);
    }

    @GetMapping("/categories/{categorySlug}/products")
    @Operation(summary = "Get products by category", description = "Returns products in a specific category")
    public ApiResponse<PagedResponse<ProductListResponse>> getProductsByCategory(
            @PathVariable String categorySlug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PagedResponse<ProductListResponse> products = productService.getProductsByCategory(categorySlug, pageable);
        return ApiResponse.success(products);
    }

    // ==================== Admin Endpoints ====================

    @GetMapping("/admin/products")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get all products (admin)", description = "Returns all products including drafts and inactive")
    public ApiResponse<PagedResponse<ProductListResponse>> getAllProductsAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PagedResponse<ProductListResponse> products = productService.getAllProductsAdmin(pageable);
        return ApiResponse.success(products);
    }

    @GetMapping("/admin/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get product by ID (admin)", description = "Returns full product details by ID")
    public ApiResponse<ProductResponse> getProductById(@PathVariable Long id) {
        ProductResponse product = productService.getProductById(id);
        return ApiResponse.success(product);
    }

    @PostMapping("/admin/products")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create product", description = "Admin only")
    public ApiResponse<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse product = productService.createProduct(request);
        return ApiResponse.success(product, "Product created successfully");
    }

    @PutMapping("/admin/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update product", description = "Admin only")
    public ApiResponse<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request) {
        ProductResponse product = productService.updateProduct(id, request);
        return ApiResponse.success(product, "Product updated successfully");
    }

    @DeleteMapping("/admin/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete product", description = "Admin only - soft delete")
    public ApiResponse<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ApiResponse.success(null, "Product deleted successfully");
    }
}
