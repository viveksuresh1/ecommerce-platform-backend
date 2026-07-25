-- V2: Create Product Module Tables
-- Categories, Products, Product Images, Product Attributes

-- Categories table (self-referencing for hierarchy)
CREATE TABLE categories (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    slug            VARCHAR(100) NOT NULL UNIQUE,
    description     TEXT,
    parent_id       BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    image_url       VARCHAR(500),
    is_active       BOOLEAN DEFAULT TRUE,
    sort_order      INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Products table
CREATE TABLE products (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(255) NOT NULL UNIQUE,
    description     TEXT,
    short_description VARCHAR(500),
    price           DECIMAL(12,2) NOT NULL,
    compare_at_price DECIMAL(12,2),  -- Original price for showing discounts
    sku             VARCHAR(100) UNIQUE,
    category_id     BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    status          VARCHAR(20) DEFAULT 'DRAFT',  -- DRAFT, ACTIVE, INACTIVE
    is_featured     BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Product images table
CREATE TABLE product_images (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    url             VARCHAR(500) NOT NULL,
    alt_text        VARCHAR(255),
    is_primary      BOOLEAN DEFAULT FALSE,
    sort_order      INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Product attributes table (flexible key-value pairs)
CREATE TABLE product_attributes (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    value           VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_categories_parent_id ON categories(parent_id);
CREATE INDEX idx_categories_slug ON categories(slug);
CREATE INDEX idx_categories_is_active ON categories(is_active);

CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_products_slug ON products(slug);
CREATE INDEX idx_products_status ON products(status);
CREATE INDEX idx_products_is_featured ON products(is_featured);
CREATE INDEX idx_products_price ON products(price);

CREATE INDEX idx_product_images_product_id ON product_images(product_id);
CREATE INDEX idx_product_attributes_product_id ON product_attributes(product_id);

-- Insert sample categories
INSERT INTO categories (name, slug, description, sort_order) VALUES
    ('Electronics', 'electronics', 'Electronic devices and gadgets', 1),
    ('Clothing', 'clothing', 'Fashion and apparel', 2),
    ('Home & Garden', 'home-garden', 'Home decor and garden supplies', 3),
    ('Books', 'books', 'Books and literature', 4);

-- Insert subcategories
INSERT INTO categories (name, slug, description, parent_id, sort_order) VALUES
    ('Smartphones', 'smartphones', 'Mobile phones and accessories', 1, 1),
    ('Laptops', 'laptops', 'Notebook computers', 1, 2),
    ('Men''s Clothing', 'mens-clothing', 'Clothing for men', 2, 1),
    ('Women''s Clothing', 'womens-clothing', 'Clothing for women', 2, 2);
