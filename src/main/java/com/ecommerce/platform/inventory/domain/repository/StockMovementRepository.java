package com.ecommerce.platform.inventory.domain.repository;

import com.ecommerce.platform.inventory.domain.model.MovementType;
import com.ecommerce.platform.inventory.domain.model.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for StockMovement entity.
 */
@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    Page<StockMovement> findByInventoryIdOrderByCreatedAtDesc(Long inventoryId, Pageable pageable);

    Page<StockMovement> findByInventoryProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    List<StockMovement> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);

    @Query("SELECT sm FROM StockMovement sm WHERE sm.inventory.id = :inventoryId " +
           "AND sm.createdAt BETWEEN :startDate AND :endDate ORDER BY sm.createdAt DESC")
    List<StockMovement> findByInventoryIdAndDateRange(
            @Param("inventoryId") Long inventoryId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT sm FROM StockMovement sm WHERE sm.movementType = :type ORDER BY sm.createdAt DESC")
    Page<StockMovement> findByMovementType(@Param("type") MovementType type, Pageable pageable);
}
