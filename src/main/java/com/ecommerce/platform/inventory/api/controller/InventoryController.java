package com.ecommerce.platform.inventory.api.controller;

import com.ecommerce.platform.inventory.api.dto.InventoryResponse;
import com.ecommerce.platform.inventory.api.dto.StockCheckResponse;
import com.ecommerce.platform.inventory.api.dto.StockMovementResponse;
import com.ecommerce.platform.inventory.api.dto.UpdateStockRequest;
import com.ecommerce.platform.inventory.application.service.InventoryService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inventory endpoints.
 * Public: Stock check for products
 * Admin: Full inventory management
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Stock management")
public class InventoryController {

    private final InventoryService inventoryService;

    // ==================== Public Endpoints ====================

    @GetMapping("/products/{productId}/stock")
    @Operation(summary = "Get stock for product", description = "Returns stock availability info")
    public ApiResponse<InventoryResponse> getProductStock(@PathVariable Long productId) {
        InventoryResponse inventory = inventoryService.getInventoryByProductId(productId);
        return ApiResponse.success(inventory);
    }

    @GetMapping("/products/{productId}/stock/check")
    @Operation(summary = "Check stock availability", description = "Check if requested quantity is available")
    public ApiResponse<StockCheckResponse> checkStock(
            @PathVariable Long productId,
            @Parameter(description = "Quantity to check") @RequestParam(defaultValue = "1") int quantity) {
        StockCheckResponse check = inventoryService.checkStock(productId, quantity);
        return ApiResponse.success(check);
    }

    // ==================== Admin Endpoints ====================

    @GetMapping("/admin/inventory")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get all inventory", description = "Admin only - paginated inventory list")
    public ApiResponse<PagedResponse<InventoryResponse>> getAllInventory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("product.name").ascending());
        PagedResponse<InventoryResponse> inventory = inventoryService.getAllInventory(pageable);
        return ApiResponse.success(inventory);
    }

    @GetMapping("/admin/inventory/low-stock")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get low stock items", description = "Admin only - items at or below threshold")
    public ApiResponse<PagedResponse<InventoryResponse>> getLowStockItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<InventoryResponse> inventory = inventoryService.getLowStockItems(pageable);
        return ApiResponse.success(inventory);
    }

    @GetMapping("/admin/inventory/out-of-stock")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get out of stock items", description = "Admin only - items with no available stock")
    public ApiResponse<PagedResponse<InventoryResponse>> getOutOfStockItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<InventoryResponse> inventory = inventoryService.getOutOfStockItems(pageable);
        return ApiResponse.success(inventory);
    }

    @PutMapping("/admin/inventory/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update stock", description = "Admin only - set absolute stock level")
    public ApiResponse<InventoryResponse> updateStock(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateStockRequest request) {
        InventoryResponse inventory = inventoryService.updateStock(productId, request);
        return ApiResponse.success(inventory, "Stock updated successfully");
    }

    @PostMapping("/admin/inventory/{productId}/add")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Add stock", description = "Admin only - increment stock by amount")
    public ApiResponse<InventoryResponse> addStock(
            @PathVariable Long productId,
            @Parameter(description = "Quantity to add") @RequestParam int quantity,
            @Parameter(description = "Notes") @RequestParam(required = false) String notes) {
        InventoryResponse inventory = inventoryService.addStock(productId, quantity, notes);
        return ApiResponse.success(inventory, "Stock added successfully");
    }

    @GetMapping("/admin/inventory/{productId}/movements")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get stock movements", description = "Admin only - stock change history")
    public ApiResponse<PagedResponse<StockMovementResponse>> getStockMovements(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<StockMovementResponse> movements = inventoryService.getStockMovements(productId, pageable);
        return ApiResponse.success(movements);
    }
}
