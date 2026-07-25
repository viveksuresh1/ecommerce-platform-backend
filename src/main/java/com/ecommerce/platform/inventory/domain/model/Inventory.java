package com.ecommerce.platform.inventory.domain.model;

import com.ecommerce.platform.product.domain.model.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Inventory entity tracking stock levels for products.
 */
@Entity
@Table(name = "inventory")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 0;

    @Column(name = "reserved_quantity", nullable = false)
    @Builder.Default
    private Integer reservedQuantity = 0;

    @Column(name = "low_stock_threshold", nullable = false)
    @Builder.Default
    private Integer lowStockThreshold = 10;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Available quantity = total - reserved
     */
    public Integer getAvailableQuantity() {
        return quantity - reservedQuantity;
    }

    /**
     * Check if stock is low (at or below threshold)
     */
    public boolean isLowStock() {
        return quantity <= lowStockThreshold;
    }

    /**
     * Check if item is in stock
     */
    public boolean isInStock() {
        return getAvailableQuantity() > 0;
    }

    /**
     * Check if requested quantity is available
     */
    public boolean hasAvailableStock(int requestedQty) {
        return getAvailableQuantity() >= requestedQty;
    }

    /**
     * Reserve stock for a cart/order
     */
    public void reserve(int qty) {
        if (!hasAvailableStock(qty)) {
            throw new IllegalStateException("Insufficient stock to reserve");
        }
        this.reservedQuantity += qty;
    }

    /**
     * Release reserved stock (cart abandoned, order cancelled)
     */
    public void release(int qty) {
        this.reservedQuantity = Math.max(0, this.reservedQuantity - qty);
    }

    /**
     * Deduct from total stock (order completed)
     */
    public void deduct(int qty) {
        if (this.quantity < qty) {
            throw new IllegalStateException("Cannot deduct more than available stock");
        }
        this.quantity -= qty;
        // Also reduce reserved if this was from a reservation
        this.reservedQuantity = Math.max(0, this.reservedQuantity - qty);
    }

    /**
     * Add stock (restock, return)
     */
    public void addStock(int qty) {
        this.quantity += qty;
    }
}
