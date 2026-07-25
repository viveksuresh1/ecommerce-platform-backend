package com.ecommerce.platform.inventory.domain.model;

/**
 * Types of inventory movements.
 */
public enum MovementType {
    RESTOCK,      // Stock added (purchase, return)
    SALE,         // Stock sold (order completed)
    ADJUSTMENT,   // Manual correction
    RESERVATION,  // Reserved for cart/checkout
    RELEASE       // Released from reservation (cart abandoned, order cancelled)
}
