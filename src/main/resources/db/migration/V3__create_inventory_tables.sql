-- V3: Inventory tables
-- Tracks stock levels and reservations for products

-- Inventory table - tracks stock per product
CREATE TABLE inventory (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL UNIQUE REFERENCES products(id) ON DELETE CASCADE,
    quantity INT NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    reserved_quantity INT NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    low_stock_threshold INT NOT NULL DEFAULT 10,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_reserved_not_exceed CHECK (reserved_quantity <= quantity)
);

-- Stock movements - audit trail for inventory changes
CREATE TABLE stock_movements (
    id BIGSERIAL PRIMARY KEY,
    inventory_id BIGINT NOT NULL REFERENCES inventory(id) ON DELETE CASCADE,
    movement_type VARCHAR(20) NOT NULL, -- RESTOCK, SALE, ADJUSTMENT, RESERVATION, RELEASE
    quantity INT NOT NULL,
    reference_type VARCHAR(50), -- ORDER, CART, MANUAL
    reference_id BIGINT, -- order_id, cart_id, etc.
    notes TEXT,
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_inventory_product ON inventory(product_id);
CREATE INDEX idx_inventory_low_stock ON inventory(quantity) WHERE quantity <= low_stock_threshold;
CREATE INDEX idx_stock_movements_inventory ON stock_movements(inventory_id);
CREATE INDEX idx_stock_movements_reference ON stock_movements(reference_type, reference_id);
CREATE INDEX idx_stock_movements_created ON stock_movements(created_at);

-- Initialize inventory for existing products (quantity 0)
INSERT INTO inventory (product_id, quantity, reserved_quantity, low_stock_threshold)
SELECT id, 0, 0, 10 FROM products
ON CONFLICT (product_id) DO NOTHING;
