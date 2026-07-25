package com.ecommerce.platform.inventory.domain.repository;

import com.ecommerce.platform.inventory.domain.model.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Inventory entity.
 */
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(Long productId);

    /**
     * Find with pessimistic lock for concurrent updates
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.product.id = :productId")
    Optional<Inventory> findByProductIdWithLock(@Param("productId") Long productId);

    /**
     * Find low stock items
     */
    @Query("SELECT i FROM Inventory i WHERE i.quantity <= i.lowStockThreshold")
    Page<Inventory> findLowStockItems(Pageable pageable);

    /**
     * Find out of stock items
     */
    @Query("SELECT i FROM Inventory i WHERE i.quantity - i.reservedQuantity <= 0")
    Page<Inventory> findOutOfStockItems(Pageable pageable);

    /**
     * Find inventory for multiple products
     */
    @Query("SELECT i FROM Inventory i WHERE i.product.id IN :productIds")
    List<Inventory> findByProductIdIn(@Param("productIds") List<Long> productIds);

    /**
     * Check if product exists in inventory
     */
    boolean existsByProductId(Long productId);
}
