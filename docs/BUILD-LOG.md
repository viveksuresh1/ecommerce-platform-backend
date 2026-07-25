# E-Commerce Platform Build Log

This document tracks implementation progress and decisions made during development.

**Design Spec:** [2026-06-26-ecommerce-platform-design.md](specs/2026-06-26-ecommerce-platform-design.md)

---

## Phase 1: Project Setup + User Module

### 1A: Project Scaffold + Docker Setup

**Status:** ✅ Complete

**What we're building:**
- Maven project with Spring Boot 3.3+
- Java 21
- Docker Compose for PostgreSQL
- Basic application.yml configuration

**Files to create:**
- `pom.xml` — Maven dependencies
- `docker-compose.yml` — PostgreSQL container
- `src/main/resources/application.yml` — Main config
- `src/main/resources/application-dev.yml` — Dev profile
- `src/main/java/com/ecommerce/platform/EcommercePlatformApplication.java` — Main class

**Dependencies:**
- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Spring Boot Starter Security
- Spring Boot Starter Validation
- Spring Boot Starter Actuator
- PostgreSQL Driver
- Flyway
- Lombok
- MapStruct
- JJWT (JWT library)
- SpringDoc OpenAPI
- JUnit 5 + Mockito + Testcontainers (test scope)

---

### 1B: Shared Infrastructure

**Status:** ✅ Complete

**What we built:**
- `ApiResponse<T>` — Standard response wrapper for all endpoints
- `PagedResponse<T>` — Wrapper for paginated list responses
- `BaseException` — Parent class for all domain exceptions
- `ResourceNotFoundException` — 404 errors
- `BadRequestException` — 400 errors
- `DuplicateResourceException` — 409 conflict errors
- `UnauthorizedException` — 401 errors
- `ForbiddenException` — 403 errors
- `GlobalExceptionHandler` — Catches all exceptions, returns consistent JSON
- `BaseEntity` — Base class with id, createdAt, updatedAt
- `JpaConfig` — Enables JPA auditing
- `HealthController` — Test endpoints to verify everything works

**Files created:**
```
shared/
├── config/
│   ├── SecurityConfig.java
│   └── JpaConfig.java
├── controller/
│   └── HealthController.java
├── domain/
│   └── BaseEntity.java
├── dto/
│   ├── ApiResponse.java
│   └── PagedResponse.java
└── exception/
    ├── BaseException.java
    ├── ResourceNotFoundException.java
    ├── BadRequestException.java
    ├── DuplicateResourceException.java
    ├── UnauthorizedException.java
    ├── ForbiddenException.java
    └── GlobalExceptionHandler.java
```

---

### 1C: User Domain + Database Migrations

**Status:** ✅ Complete

**What we built:**
- `UserStatus` enum — ACTIVE, INACTIVE, SUSPENDED, DELETED
- `Role` entity — Maps to roles table (CUSTOMER, ADMIN)
- `User` entity — Maps to users table with Many-to-Many roles relationship
- `Address` entity — Maps to addresses table with Many-to-One user relationship
- `RoleRepository` — findByName, existsByName
- `UserRepository` — findByEmail, existsByEmail, searchUsers
- `AddressRepository` — findByUserId, clearDefaultAddress

**Files created:**
```
user/domain/
├── model/
│   ├── Role.java
│   ├── User.java
│   ├── UserStatus.java
│   └── Address.java
└── repository/
    ├── RoleRepository.java
    ├── UserRepository.java
    └── AddressRepository.java
```

**Entity Relationships:**
```
User ──< UserRoles >── Role     (Many-to-Many)
User ──< Address                (One-to-Many)
```

---

### 1D: JWT + Spring Security

**Status:** ✅ Complete

**What we built:**
- `JwtService` — Generate access/refresh tokens, validate, extract claims
- `CustomUserDetails` — Wraps User entity for Spring Security
- `CustomUserDetailsService` — Loads users from database for auth
- `JwtAuthenticationFilter` — Checks JWT on every request
- Updated `SecurityConfig` — JWT filter chain, role-based access

**Files created:**
```
shared/security/
├── JwtService.java              # Token generation/validation
├── CustomUserDetails.java       # UserDetails implementation
├── CustomUserDetailsService.java # Loads user from DB
└── JwtAuthenticationFilter.java  # Request filter
```

**Security flow:**
```
Request → JwtAuthenticationFilter → Extract token → Validate → Set auth context → Controller
```

**Endpoint access:**
- `/api/v1/auth/**` — Public (no auth needed)
- `/api/v1/test/**` — Public
- `/api/v1/admin/**` — Admin role required
- Everything else — Authenticated user required

---

### 1E: Auth Endpoints

**Status:** ✅ Complete

**What we built:**
- `RegisterRequest` / `LoginRequest` / `RefreshTokenRequest` — Input DTOs with validation
- `AuthResponse` — Response with tokens and user info
- `AuthService` — Business logic for register, login, refresh
- `AuthController` — REST endpoints

**Files created:**
```
user/
├── api/
│   ├── controller/
│   │   └── AuthController.java
│   └── dto/
│       ├── RegisterRequest.java
│       ├── LoginRequest.java
│       ├── RefreshTokenRequest.java
│       └── AuthResponse.java
└── application/
    └── service/
        └── AuthService.java
```

**Endpoints:**
```
POST /api/v1/auth/register  → Create user, return tokens
POST /api/v1/auth/login     → Authenticate, return tokens
POST /api/v1/auth/refresh   → Refresh access token
```

**Tested scenarios:**
- ✅ Register new user → Returns tokens
- ✅ Duplicate email → Returns 409 Conflict
- ✅ Login success → Returns tokens
- ✅ Wrong password → Returns 401 Unauthorized
- ✅ Validation errors → Returns field-level errors

---

### 1F: User Profile + Address Endpoints

**Status:** ✅ Complete

**What we built:**
- `UserProfileResponse` / `UpdateProfileRequest` / `ChangePasswordRequest` — Profile DTOs
- `AddressResponse` / `CreateAddressRequest` / `UpdateAddressRequest` — Address DTOs
- `UserMapper` — Entity to DTO conversion
- `UserService` — Profile operations (get, update, change password)
- `AddressService` — Address CRUD + default address logic
- `UserController` — REST endpoints

**Files created:**
```
user/
├── api/
│   ├── controller/
│   │   └── UserController.java
│   ├── dto/
│   │   ├── UserProfileResponse.java
│   │   ├── UpdateProfileRequest.java
│   │   ├── ChangePasswordRequest.java
│   │   ├── AddressResponse.java
│   │   ├── CreateAddressRequest.java
│   │   └── UpdateAddressRequest.java
│   └── mapper/
│       └── UserMapper.java
└── application/
    └── service/
        ├── UserService.java
        └── AddressService.java
```

**Endpoints:**
```
GET    /api/v1/users/me                      → Get profile
PUT    /api/v1/users/me                      → Update profile
POST   /api/v1/users/me/change-password      → Change password
GET    /api/v1/users/me/addresses            → List addresses
GET    /api/v1/users/me/addresses/{id}       → Get address
POST   /api/v1/users/me/addresses            → Add address
PUT    /api/v1/users/me/addresses/{id}       → Update address
DELETE /api/v1/users/me/addresses/{id}       → Delete address
POST   /api/v1/users/me/addresses/{id}/set-default → Set default
```

**Features:**
- First address automatically becomes default
- Only one default address per user
- Deleting default promotes another address
- All endpoints require JWT authentication

---

## Decisions Log

| Date | Decision | Rationale |
|------|----------|-----------|
| 2026-06-26 | Modular monolith architecture | Balance between clean separation and simplicity |
| 2026-06-26 | 8 modules (removed 5 from original 13) | Focus on core learning, avoid redundancy |
| 2026-06-26 | JWT stateless auth | Standard for REST APIs, simpler than OAuth2 server |
| 2026-06-26 | PostgreSQL | Feature-rich, industry standard |

---

## Progress Commits

| Date | What | Files |
|------|------|-------|
| 2026-06-27 | Phase 1A: Project scaffold complete | pom.xml, docker-compose.yml, application.yml, EcommercePlatformApplication.java, SecurityConfig.java, V1__create_user_tables.sql |

## Verified Working

- ✅ Maven build: `./mvnw compile` 
- ✅ PostgreSQL: `docker compose up -d`
- ✅ Spring Boot: `./mvnw spring-boot:run`
- ✅ Flyway migrations run automatically
- ✅ Health check: http://localhost:8080/actuator/health
- ✅ Swagger UI: http://localhost:8080/swagger-ui.html

---

## Phase 2: Product Module

### 2A: Product Database Schema

**Status:** ✅ Complete

**What we built:**
- Flyway migration V2 for product tables
- Categories with self-referencing hierarchy (parent/children)
- Products with status, pricing, and SEO slugs
- Product images with primary flag
- Product attributes (key-value pairs)
- Sample category data seeded

**Files created:**
```
db/migration/
└── V2__create_product_tables.sql
```

**Tables:**
- `categories` — Hierarchical categories with parent_id
- `products` — Main product info with category_id FK
- `product_images` — Multiple images per product
- `product_attributes` — Flexible key-value attributes

---

### 2B: Product Domain Entities

**Status:** ✅ Complete

**What we built:**
- `Category` entity — Self-referencing for hierarchy
- `Product` entity — With price, compareAtPrice, status, isFeatured
- `ProductImage` entity — With isPrimary and sortOrder
- `ProductAttribute` entity — Flexible key-value pairs
- `ProductStatus` enum — DRAFT, ACTIVE, INACTIVE

**Files created:**
```
product/domain/
├── model/
│   ├── Category.java
│   ├── Product.java
│   ├── ProductImage.java
│   ├── ProductAttribute.java
│   └── ProductStatus.java
└── repository/
    ├── CategoryRepository.java
    ├── ProductRepository.java
    └── ProductImageRepository.java
```

**Entity Relationships:**
```
Category ──< Category (self-ref)  (parent/children)
Category ──< Product              (One-to-Many)
Product ──< ProductImage          (One-to-Many)
Product ──< ProductAttribute      (One-to-Many)
```

---

### 2C: Product DTOs + Mapper

**Status:** ✅ Complete

**What we built:**
- `CategoryResponse` — With children list and productCount
- `CreateCategoryRequest` — Admin create/update category
- `ProductResponse` — Full product details with images & attributes
- `ProductListResponse` — Lightweight for lists
- `CreateProductRequest` / `UpdateProductRequest` — Admin product management
- `ProductMapper` — Entity to DTO conversion

**Files created:**
```
product/api/
├── dto/
│   ├── CategoryResponse.java
│   ├── CreateCategoryRequest.java
│   ├── ProductResponse.java
│   ├── ProductListResponse.java
│   ├── CreateProductRequest.java
│   └── UpdateProductRequest.java
└── mapper/
    └── ProductMapper.java
```

---

### 2D: Product Services

**Status:** ✅ Complete

**What we built:**
- `CategoryService` — CRUD + hierarchical tree building
- `ProductService` — CRUD + search + filtering + pagination
- `SlugUtil` — URL-friendly slug generation

**Files created:**
```
product/application/service/
├── CategoryService.java
└── ProductService.java

shared/util/
└── SlugUtil.java
```

---

### 2E: Product Controllers

**Status:** ✅ Complete

**What we built:**
- `CategoryController` — Public GET, Admin POST/PUT/DELETE
- `ProductController` — Public browsing, Admin management

**Files created:**
```
product/api/controller/
├── CategoryController.java
└── ProductController.java
```

**Public Endpoints:**
```
GET /api/v1/categories                      → List all categories (hierarchical)
GET /api/v1/categories/{slug}               → Get category by slug
GET /api/v1/products                        → List products (paginated)
GET /api/v1/products/featured               → Featured products
GET /api/v1/products/search?q=...           → Search products
GET /api/v1/products/{slug}                 → Product details
GET /api/v1/categories/{slug}/products      → Products in category
```

**Admin Endpoints (require ADMIN role):**
```
GET    /api/v1/admin/products               → All products (inc. drafts)
GET    /api/v1/admin/products/{id}          → Product by ID
POST   /api/v1/admin/products               → Create product
PUT    /api/v1/admin/products/{id}          → Update product
DELETE /api/v1/admin/products/{id}          → Soft delete product
POST   /api/v1/admin/categories             → Create category
PUT    /api/v1/admin/categories/{id}        → Update category
DELETE /api/v1/admin/categories/{id}        → Soft delete category
```

**Features:**
- SEO-friendly slugs auto-generated from names
- Pagination with sorting on all list endpoints
- Product search across name and description
- Featured products endpoint for homepage
- Discount percentage calculated from compareAtPrice
- Soft delete (status → INACTIVE)

---

## Phase 2 Verified Working

- ✅ Categories hierarchy loads correctly
- ✅ Products list with pagination
- ✅ Featured products filter
- ✅ Product search works
- ✅ Product details with attributes
- ✅ Products by category
- ✅ Admin create product (tested with JWT)

---

## Phase 3: Inventory Module

### 3A: Inventory Database Schema

**Status:** ✅ Complete

**What we built:**
- Flyway migration V3 for inventory tables
- Inventory table with quantity, reserved, threshold
- Stock movements audit trail
- Check constraint preventing over-reservation

**Files created:**
```
db/migration/
└── V3__create_inventory_tables.sql
```

**Tables:**
- `inventory` — Stock levels per product (1:1 with products)
- `stock_movements` — Audit trail of all stock changes

---

### 3B: Inventory Domain Entities

**Status:** ✅ Complete

**What we built:**
- `Inventory` entity — With reserve/release/deduct/addStock methods
- `StockMovement` entity — Audit record
- `MovementType` enum — RESTOCK, SALE, ADJUSTMENT, RESERVATION, RELEASE

**Files created:**
```
inventory/domain/
├── model/
│   ├── Inventory.java
│   ├── StockMovement.java
│   └── MovementType.java
└── repository/
    ├── InventoryRepository.java
    └── StockMovementRepository.java
```

**Business Logic in Entity:**
- `getAvailableQuantity()` — total - reserved
- `isLowStock()` — at or below threshold
- `hasAvailableStock(qty)` — check if can fulfill
- `reserve(qty)` / `release(qty)` — for cart operations
- `deduct(qty)` — for completed sales

---

### 3C: Inventory Service + Controller

**Status:** ✅ Complete

**What we built:**
- `InventoryService` — Full CRUD + stock operations + audit
- `InventoryController` — Public stock check + Admin management
- Pessimistic locking for concurrent updates
- Automatic audit trail on all changes

**Files created:**
```
inventory/
├── api/
│   ├── controller/
│   │   └── InventoryController.java
│   └── dto/
│       ├── InventoryResponse.java
│       ├── UpdateStockRequest.java
│       ├── StockMovementResponse.java
│       └── StockCheckResponse.java
└── application/
    └── service/
        └── InventoryService.java
```

**Public Endpoints:**
```
GET /api/v1/products/{id}/stock           → Stock info
GET /api/v1/products/{id}/stock/check     → Check availability
```

**Admin Endpoints:**
```
GET  /api/v1/admin/inventory              → All inventory (paginated)
GET  /api/v1/admin/inventory/low-stock    → Low stock items
GET  /api/v1/admin/inventory/out-of-stock → Out of stock items
PUT  /api/v1/admin/inventory/{productId}  → Set stock level
POST /api/v1/admin/inventory/{productId}/add → Add stock
GET  /api/v1/admin/inventory/{productId}/movements → Stock history
```

**Features:**
- Real-time stock availability check
- Reserved quantity for cart/checkout
- Low stock threshold alerts
- Full audit trail of all changes
- Pessimistic locking prevents race conditions

## Phase 3 Verified Working

- ✅ Public stock check endpoints
- ✅ Admin stock management
- ✅ Low stock / out of stock queries
- ✅ Stock movement history
- ✅ Audit trail records all changes

---

## Phase 4: Cart Module

### 4A: Cart Database Schema

**Status:** ✅ Complete

**What we built:**
- Flyway migration V4 for cart tables
- One cart per user (unique constraint)
- Cart items with product reference and quantity

**Files created:**
```
db/migration/
└── V4__create_cart_tables.sql
```

**Tables:**
- `carts` — One per user, FK to users
- `cart_items` — Product, quantity, unit_price, unique(cart_id, product_id)

---

### 4B: Cart Domain Entities

**Status:** ✅ Complete

**What we built:**
- `Cart` entity — With addItem/removeItem/clear business methods
- `CartItem` entity — Product, quantity, price snapshot

**Files created:**
```
cart/domain/
├── model/
│   ├── Cart.java
│   └── CartItem.java
└── repository/
    ├── CartRepository.java
    └── CartItemRepository.java
```

**Business Logic in Entity:**
- `addItem()` — Add or merge if product exists
- `removeItem()` — Remove by product ID
- `getSubtotal()` — Sum of line totals
- `getTotalItems()` — Sum of quantities

---

### 4C: Cart Service + Controller

**Status:** ✅ Complete

**What we built:**
- `CartService` — CRUD with inventory validation
- `CartController` — REST endpoints (all authenticated)
- Stock availability check on add/update
- Price snapshot at time of add

**Files created:**
```
cart/
├── api/
│   ├── controller/
│   │   └── CartController.java
│   └── dto/
│       ├── CartResponse.java
│       ├── AddToCartRequest.java
│       └── UpdateCartItemRequest.java
└── application/
    └── service/
        └── CartService.java
```

**Endpoints (all require auth):**
```
GET    /api/v1/cart                  → Get cart with items
POST   /api/v1/cart/items            → Add product to cart
PUT    /api/v1/cart/items/{productId} → Update quantity
DELETE /api/v1/cart/items/{productId} → Remove product
DELETE /api/v1/cart                  → Clear entire cart
GET    /api/v1/cart/count            → Get item count
```

**Features:**
- Auto-create cart on first access
- Stock validation on add/update
- Price captured at add time
- Available stock shown in response
- Quantity merging for same product

## Phase 4 Verified Working

- ✅ Get/create cart
- ✅ Add items with stock validation
- ✅ Update item quantity
- ✅ Remove items
- ✅ Clear cart
- ✅ Stock exceeded returns error

---

## Phase 5: Order Module

### 5A: Order Database Schema

**Status:** ✅ Complete

**What we built:**
- Flyway migration V5 for order tables
- Orders with shipping address, totals, status
- Order items (product snapshot at order time)
- Order status history for audit trail

**Files created:**
```
db/migration/
└── V5__create_order_tables.sql
```

**Tables:**
- `orders` — Main order with shipping, totals, status
- `order_items` — Product snapshot (name, sku, price at order time)
- `order_status_history` — Status change audit trail

---

### 5B: Order Domain Entities

**Status:** ✅ Complete

**What we built:**
- `Order` entity — With shipping, totals, items
- `OrderItem` entity — Product snapshot
- `OrderStatusHistory` entity — Audit trail
- `OrderStatus` enum — PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED, REFUNDED
- `PaymentStatus` enum — PENDING, PAID, FAILED, REFUNDED

**Files created:**
```
order/domain/
├── model/
│   ├── Order.java
│   ├── OrderItem.java
│   ├── OrderStatusHistory.java
│   ├── OrderStatus.java
│   └── PaymentStatus.java
└── repository/
    ├── OrderRepository.java
    └── OrderItemRepository.java
```

---

### 5C: Order Service + Controller

**Status:** ✅ Complete

**What we built:**
- `OrderService` — Create from cart, cancel, status updates
- `OrderController` — User and admin endpoints
- Cart cleared after order
- Inventory deducted on order, restored on cancel
- Order number generation (ORD-YYYYMMDD-XXXXXXXX)

**Files created:**
```
order/
├── api/
│   ├── controller/
│   │   └── OrderController.java
│   └── dto/
│       ├── OrderResponse.java
│       ├── OrderListResponse.java
│       ├── CreateOrderRequest.java
│       └── UpdateOrderStatusRequest.java
└── application/
    └── service/
        └── OrderService.java
```

**User Endpoints:**
```
POST   /api/v1/orders                    → Create order from cart
GET    /api/v1/orders                    → Get my orders
GET    /api/v1/orders/{id}               → Get order details
GET    /api/v1/orders/number/{number}    → Get by order number
POST   /api/v1/orders/{id}/cancel        → Cancel order
```

**Admin Endpoints:**
```
GET    /api/v1/admin/orders              → All orders (with status filter)
GET    /api/v1/admin/orders/{id}         → Any order details
PUT    /api/v1/admin/orders/{id}/status  → Update status
```

**Features:**
- Create order from cart (clears cart)
- Use saved address or provide inline
- Inventory deducted on order creation
- Inventory restored on cancellation
- Status history audit trail
- Order number format: ORD-YYYYMMDD-XXXXXXXX

## Phase 5 Verified Working

- ✅ Create order from cart
- ✅ Cart cleared after order
- ✅ Inventory deducted
- ✅ View order details
- ✅ List user's orders
- ✅ Cancel order (restores inventory)
- ✅ Admin status updates

---

## Phase 6: Payment Module

### 6A: Payment Database Schema

**Status:** ✅ Complete

**What we built:**
- Flyway migration V6 for payment tables
- Payments with gateway integration fields
- Payment transactions for audit trail
- Refund tracking

**Files created:**
```
db/migration/
└── V6__create_payment_tables.sql
```

**Tables:**
- `payments` — Payment records with gateway details
- `payment_transactions` — Transaction audit log

---

### 6B: Payment Domain Entities

**Status:** ✅ Complete

**What we built:**
- `Payment` entity — With gateway, refund tracking
- `PaymentTransaction` entity — Transaction log
- `PaymentMethodType` enum — COD, CREDIT_CARD, DEBIT_CARD, UPI, etc.
- `PaymentTransactionStatus` enum — PENDING, SUCCESS, FAILED, REFUNDED
- `TransactionType` enum — INITIATE, CAPTURE, REFUND, VOID

**Files created:**
```
payment/domain/
├── model/
│   ├── Payment.java
│   ├── PaymentTransaction.java
│   ├── PaymentMethodType.java
│   ├── PaymentTransactionStatus.java
│   └── TransactionType.java
└── repository/
    ├── PaymentRepository.java
    └── PaymentTransactionRepository.java
```

---

### 6C: Payment Service + Controller

**Status:** ✅ Complete

**What we built:**
- `PaymentService` — Initiate, process, COD, refund
- `PaymentController` — User and admin endpoints
- Order status auto-update on payment success
- Transaction audit trail
- Simulated gateway callback

**Files created:**
```
payment/
├── api/
│   ├── controller/
│   │   └── PaymentController.java
│   └── dto/
│       ├── PaymentResponse.java
│       ├── InitiatePaymentRequest.java
│       ├── ProcessPaymentRequest.java
│       └── RefundRequest.java
└── application/
    └── service/
        └── PaymentService.java
```

**User Endpoints:**
```
POST   /api/v1/payments/initiate       → Start payment for order
GET    /api/v1/payments/{id}           → Get payment details
GET    /api/v1/payments/order/{orderId} → Get payment for order
GET    /api/v1/payments                → Get my payments
```

**Admin Endpoints:**
```
GET    /api/v1/admin/payments          → All payments
GET    /api/v1/admin/payments/{id}     → Any payment details
POST   /api/v1/admin/payments/{id}/process → Simulate gateway callback
POST   /api/v1/admin/payments/cod/{orderId} → Mark COD as paid
POST   /api/v1/admin/payments/{id}/refund → Process refund
```

**Features:**
- Payment number generation (PAY-YYYYMMDD-XXXXXXXX)
- Gateway transaction ID tracking
- Order status auto-update (PENDING → CONFIRMED on payment)
- COD payment collection
- Full/partial refund support
- Transaction audit trail

## Phase 6 Verified Working

- ✅ Initiate payment
- ✅ Process payment (success/failure)
- ✅ Order auto-confirmed on payment
- ✅ COD payment processing
- ✅ Refund processing
- ✅ Transaction history
