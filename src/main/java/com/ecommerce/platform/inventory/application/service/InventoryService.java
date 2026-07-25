package com.ecommerce.platform.inventory.application.service;

import com.ecommerce.platform.inventory.api.dto.InventoryResponse;
import com.ecommerce.platform.inventory.api.dto.StockCheckResponse;
import com.ecommerce.platform.inventory.api.dto.StockMovementResponse;
import com.ecommerce.platform.inventory.api.dto.UpdateStockRequest;
import com.ecommerce.platform.inventory.domain.model.Inventory;
import com.ecommerce.platform.inventory.domain.model.MovementType;
import com.ecommerce.platform.inventory.domain.model.StockMovement;
import com.ecommerce.platform.inventory.domain.repository.InventoryRepository;
import com.ecommerce.platform.inventory.domain.repository.StockMovementRepository;
import com.ecommerce.platform.product.domain.model.Product;
import com.ecommerce.platform.product.domain.repository.ProductRepository;
import com.ecommerce.platform.shared.dto.PagedResponse;
import com.ecommerce.platform.shared.exception.BadRequestException;
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

/**
 * Service for inventory operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /**
     * Get inventory for a product (public).
     */
    @Transactional(readOnly = true)
    public InventoryResponse getInventoryByProductId(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "productId", productId));
        return toResponse(inventory);
    }

    /**
     * Check stock availability (public).
     */
    @Transactional(readOnly = true)
    public StockCheckResponse checkStock(Long productId, int requestedQuantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "productId", productId));

        return StockCheckResponse.builder()
                .productId(productId)
                .availableQuantity(inventory.getAvailableQuantity())
                .inStock(inventory.isInStock())
                .sufficientStock(inventory.hasAvailableStock(requestedQuantity))
                .requestedQuantity(requestedQuantity)
                .build();
    }

    /**
     * Get all inventory with pagination (admin).
     */
    @Transactional(readOnly = true)
    public PagedResponse<InventoryResponse> getAllInventory(Pageable pageable) {
        Page<Inventory> page = inventoryRepository.findAll(pageable);
        Page<InventoryResponse> responsePage = page.map(this::toResponse);
        return PagedResponse.from(responsePage);
    }

    /**
     * Get low stock items (admin).
     */
    @Transactional(readOnly = true)
    public PagedResponse<InventoryResponse> getLowStockItems(Pageable pageable) {
        Page<Inventory> page = inventoryRepository.findLowStockItems(pageable);
        Page<InventoryResponse> responsePage = page.map(this::toResponse);
        return PagedResponse.from(responsePage);
    }

    /**
     * Get out of stock items (admin).
     */
    @Transactional(readOnly = true)
    public PagedResponse<InventoryResponse> getOutOfStockItems(Pageable pageable) {
        Page<Inventory> page = inventoryRepository.findOutOfStockItems(pageable);
        Page<InventoryResponse> responsePage = page.map(this::toResponse);
        return PagedResponse.from(responsePage);
    }

    /**
     * Update stock level (admin) - sets absolute quantity.
     */
    @Transactional
    public InventoryResponse updateStock(Long productId, UpdateStockRequest request) {
        Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
                .orElseGet(() -> createInventoryForProduct(productId));

        int oldQuantity = inventory.getQuantity();
        int newQuantity = request.getQuantity();
        int difference = newQuantity - oldQuantity;

        inventory.setQuantity(newQuantity);
        if (request.getLowStockThreshold() != null) {
            inventory.setLowStockThreshold(request.getLowStockThreshold());
        }

        inventory = inventoryRepository.save(inventory);

        // Record movement
        recordMovement(inventory, MovementType.ADJUSTMENT, difference, "MANUAL", null, request.getNotes());

        log.info("Stock updated for product {}: {} -> {}", productId, oldQuantity, newQuantity);
        return toResponse(inventory);
    }

    /**
     * Add stock (admin) - increment by amount.
     */
    @Transactional
    public InventoryResponse addStock(Long productId, int quantity, String notes) {
        if (quantity <= 0) {
            throw new BadRequestException("Quantity must be positive");
        }

        Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
                .orElseGet(() -> createInventoryForProduct(productId));

        inventory.addStock(quantity);
        inventory = inventoryRepository.save(inventory);

        recordMovement(inventory, MovementType.RESTOCK, quantity, "MANUAL", null, notes);

        log.info("Stock added for product {}: +{}", productId, quantity);
        return toResponse(inventory);
    }

    /**
     * Reserve stock for cart/checkout (internal use).
     */
    @Transactional
    public void reserveStock(Long productId, int quantity, String referenceType, Long referenceId) {
        Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "productId", productId));

        if (!inventory.hasAvailableStock(quantity)) {
            throw new BadRequestException("Insufficient stock available");
        }

        inventory.reserve(quantity);
        inventoryRepository.save(inventory);

        recordMovement(inventory, MovementType.RESERVATION, quantity, referenceType, referenceId, null);

        log.info("Stock reserved for product {}: {} units for {} #{}", productId, quantity, referenceType, referenceId);
    }

    /**
     * Release reserved stock (internal use).
     */
    @Transactional
    public void releaseStock(Long productId, int quantity, String referenceType, Long referenceId) {
        Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "productId", productId));

        inventory.release(quantity);
        inventoryRepository.save(inventory);

        recordMovement(inventory, MovementType.RELEASE, quantity, referenceType, referenceId, null);

        log.info("Stock released for product {}: {} units from {} #{}", productId, quantity, referenceType, referenceId);
    }

    /**
     * Deduct stock on sale (internal use).
     */
    @Transactional
    public void deductStock(Long productId, int quantity, String referenceType, Long referenceId) {
        Inventory inventory = inventoryRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "productId", productId));

        inventory.deduct(quantity);
        inventoryRepository.save(inventory);

        recordMovement(inventory, MovementType.SALE, -quantity, referenceType, referenceId, null);

        log.info("Stock deducted for product {}: {} units for {} #{}", productId, quantity, referenceType, referenceId);
    }

    /**
     * Get stock movement history for a product (admin).
     */
    @Transactional(readOnly = true)
    public PagedResponse<StockMovementResponse> getStockMovements(Long productId, Pageable pageable) {
        Page<StockMovement> page = stockMovementRepository.findByInventoryProductIdOrderByCreatedAtDesc(productId, pageable);
        Page<StockMovementResponse> responsePage = page.map(this::toMovementResponse);
        return PagedResponse.from(responsePage);
    }

    // ==================== Helper Methods ====================

    private Inventory createInventoryForProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        Inventory inventory = Inventory.builder()
                .product(product)
                .quantity(0)
                .reservedQuantity(0)
                .lowStockThreshold(10)
                .build();

        return inventoryRepository.save(inventory);
    }

    private void recordMovement(Inventory inventory, MovementType type, int quantity,
                                String referenceType, Long referenceId, String notes) {
        User currentUser = getCurrentUser();

        StockMovement movement = StockMovement.builder()
                .inventory(inventory)
                .movementType(type)
                .quantity(quantity)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .notes(notes)
                .createdBy(currentUser)
                .build();

        stockMovementRepository.save(movement);
    }

    private User getCurrentUser() {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            return userRepository.findByEmail(email).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private InventoryResponse toResponse(Inventory inventory) {
        Product product = inventory.getProduct();
        return InventoryResponse.builder()
                .id(inventory.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productSku(product.getSku())
                .quantity(inventory.getQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .availableQuantity(inventory.getAvailableQuantity())
                .lowStockThreshold(inventory.getLowStockThreshold())
                .inStock(inventory.isInStock())
                .lowStock(inventory.isLowStock())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }

    private StockMovementResponse toMovementResponse(StockMovement movement) {
        return StockMovementResponse.builder()
                .id(movement.getId())
                .productId(movement.getInventory().getProduct().getId())
                .productName(movement.getInventory().getProduct().getName())
                .movementType(movement.getMovementType())
                .quantity(movement.getQuantity())
                .referenceType(movement.getReferenceType())
                .referenceId(movement.getReferenceId())
                .notes(movement.getNotes())
                .createdByEmail(movement.getCreatedBy() != null ? movement.getCreatedBy().getEmail() : null)
                .createdAt(movement.getCreatedAt())
                .build();
    }
}
