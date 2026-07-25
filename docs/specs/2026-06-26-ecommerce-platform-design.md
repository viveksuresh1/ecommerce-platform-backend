# Enterprise E-Commerce Platform Backend - Design Specification

**Date:** 2026-06-26  
**Status:** Approved  
**Author:** Design Session  

---

## 1. Overview

### 1.1 Project Summary

A production-grade B2C e-commerce platform backend built with Java Spring Boot. The platform supports customers purchasing products online, administrators managing the platform, and exposes REST APIs for future web or mobile frontends.

### 1.2 Project Goals

| Goal | Description |
|------|-------------|
| **Learning** | Educational project to learn enterprise backend development |
| **Portfolio** | Demonstrate understanding of enterprise patterns and best practices |
| **Production-Quality** | Follow industry standards, not a simple CRUD application |

### 1.3 Key Decisions

- **Architecture:** Modular Monolith with DDD-lite
- **Target Scale:** Startup/Growth stage (but well-architected)
- **Deployment:** Cloud-agnostic, Docker-based
- **Database:** PostgreSQL
- **Authentication:** JWT-based stateless auth with RBAC

---

## 2. High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENTS                                  │
│              (Web App, Mobile App, Admin Dashboard)              │
└─────────────────────────────────┬───────────────────────────────┘
                                  │ HTTPS/REST
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                   SPRING BOOT APPLICATION                        │
│  ┌───────────┬───────────┬───────────┬───────────┬───────────┐  │
│  │   User    │  Product  │ Inventory │   Cart    │   Order   │  │
│  │  Module   │  Module   │  Module   │  Module   │  Module   │  │
│  └───────────┴───────────┴───────────┴───────────┴───────────┘  │
│  ┌───────────┬───────────┬───────────┐                          │
│  │  Payment  │  Review   │   Admin   │    SHARED / INFRA        │
│  │  Module   │  Module   │  Module   │                          │
│  └───────────┴───────────┴───────────┘                          │
└─────────────────────────────────┬───────────────────────────────┘
                                  │
        ┌─────────────────────────┼─────────────────────────┐
        ▼                         ▼                         ▼
┌───────────────┐       ┌───────────────┐       ┌───────────────┐
│  PostgreSQL   │       │     Redis     │       │ Message Queue │
│  (Primary DB) │       │   (Future)    │       │   (Future)    │
└───────────────┘       └───────────────┘       └───────────────┘
```

### 2.1 Architecture Principles

1. **Modular Monolith** — All modules in one deployable unit, but with strict boundaries
2. **Each Module is Self-Contained** — Own entities, repositories, services, controllers
3. **Modules Communicate via Defined Interfaces** — Through application services or domain events
4. **Shared Infrastructure Layer** — Security, logging, exception handling, utilities

---

## 3. Functional Modules

### 3.1 Module Overview (8 Modules)

| # | Module | Description |
|---|--------|-------------|
| 1 | **User** | Registration, login, profiles, addresses, roles |
| 2 | **Product** | Products, categories, attributes, images |
| 3 | **Inventory** | Stock tracking, reservations, low-stock alerts |
| 4 | **Cart** | Shopping cart (guest + authenticated) |
| 5 | **Order** | Checkout, order lifecycle, history |
| 6 | **Payment** | Payment processing (mocked gateway) |
| 7 | **Review** | Ratings, reviews, moderation |
| 8 | **Admin** | Back-office, audit logging, reporting |

### 3.2 Module Dependencies

```
User ←──────────────────────────────────────────────┐
  │                                                  │
  ▼                                                  │
Product ←── Inventory                                │
  │            │                                     │
  ▼            ▼                                     │
Cart ──────► Order ──────► Payment                   │
  │            │                                     │
  │            ▼                                     │
  │         Review                                   │
  │                                                  │
  └──────────────────► Admin ◄───────────────────────┘
```

---

## 4. Package Structure

```
src/main/java/com/ecommerce/platform/
│
├── user/                           # USER MODULE
│   ├── api/                        # REST layer
│   │   ├── controller/             # REST endpoints
│   │   ├── dto/                    # Request/Response objects
│   │   └── mapper/                 # Entity ↔ DTO conversion
│   ├── domain/                     # Core business logic
│   │   ├── model/                  # Entities, Value Objects
│   │   ├── repository/             # Repository interfaces
│   │   └── service/                # Domain services
│   ├── application/                # Use cases, orchestration
│   │   └── service/                # Application services
│   └── infrastructure/             # Framework implementations
│       ├── persistence/            # JPA repository implementations
│       └── security/               # Module-specific security
│
├── product/                        # PRODUCT MODULE
│   ├── api/
│   ├── domain/
│   ├── application/
│   └── infrastructure/
│
├── inventory/                      # INVENTORY MODULE
│   └── ... (same structure)
│
├── cart/                           # CART MODULE
│   └── ...
│
├── order/                          # ORDER MODULE
│   └── ...
│
├── payment/                        # PAYMENT MODULE
│   └── ...
│
├── review/                         # REVIEW MODULE
│   └── ...
│
├── admin/                          # ADMIN MODULE
│   └── ...
│
└── shared/                         # CROSS-CUTTING CONCERNS
    ├── config/                     # Global Spring configuration
    ├── security/                   # JWT, authentication, authorization
    ├── exception/                  # Global exception handling
    ├── validation/                 # Custom validators
    ├── audit/                      # Audit logging
    └── util/                       # Common utilities

src/main/resources/
├── application.yml                 # Main config
├── application-dev.yml             # Dev profile
├── application-prod.yml            # Production profile
└── db/migration/                   # Flyway migrations
    ├── V1__create_user_tables.sql
    ├── V2__create_product_tables.sql
    └── ...
```

### 4.1 Layer Responsibilities

| Layer | Purpose | Depends On |
|-------|---------|------------|
| `api/` | REST endpoints, DTOs, validation | `application/` |
| `application/` | Use case orchestration, transactions | `domain/` |
| `domain/` | Business logic, entities, rules | Nothing (pure) |
| `infrastructure/` | Framework glue, DB access | `domain/` |

**Dependency Rule:** Dependencies only point inward. `api/` → `application/` → `domain/` ← `infrastructure/`

---

## 5. Database Design

### 5.1 User Module Tables

```sql
-- users
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    phone           VARCHAR(20),
    status          VARCHAR(20) DEFAULT 'ACTIVE',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- roles
CREATE TABLE roles (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(50) NOT NULL UNIQUE,
    description     VARCHAR(255)
);

-- user_roles (join table)
CREATE TABLE user_roles (
    user_id         BIGINT REFERENCES users(id),
    role_id         BIGINT REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);

-- addresses
CREATE TABLE addresses (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT REFERENCES users(id),
    street          VARCHAR(255) NOT NULL,
    city            VARCHAR(100) NOT NULL,
    state           VARCHAR(100) NOT NULL,
    postal_code     VARCHAR(20) NOT NULL,
    country         VARCHAR(100) NOT NULL,
    is_default      BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 5.2 Product Module Tables

```sql
-- categories
CREATE TABLE categories (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    slug            VARCHAR(100) NOT NULL UNIQUE,
    description     TEXT,
    parent_id       BIGINT REFERENCES categories(id),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- products
CREATE TABLE products (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(255) NOT NULL UNIQUE,
    description     TEXT,
    price           DECIMAL(12,2) NOT NULL,
    category_id     BIGINT REFERENCES categories(id),
    status          VARCHAR(20) DEFAULT 'ACTIVE',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- product_images
CREATE TABLE product_images (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT REFERENCES products(id),
    url             VARCHAR(500) NOT NULL,
    is_primary      BOOLEAN DEFAULT FALSE,
    sort_order      INT DEFAULT 0
);

-- product_attributes
CREATE TABLE product_attributes (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT REFERENCES products(id),
    name            VARCHAR(100) NOT NULL,
    value           VARCHAR(255) NOT NULL
);
```

### 5.3 Inventory Module Tables

```sql
-- inventory
CREATE TABLE inventory (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT REFERENCES products(id) UNIQUE,
    quantity        INT NOT NULL DEFAULT 0,
    reserved        INT NOT NULL DEFAULT 0,
    version         BIGINT DEFAULT 0,  -- optimistic locking
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- inventory_history
CREATE TABLE inventory_history (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT REFERENCES products(id),
    adjustment      INT NOT NULL,
    reason          VARCHAR(255),
    created_by      BIGINT REFERENCES users(id),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 5.4 Cart Module Tables

```sql
-- carts
CREATE TABLE carts (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT REFERENCES users(id),
    session_id      VARCHAR(100),  -- for guest users
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at      TIMESTAMP
);

-- cart_items
CREATE TABLE cart_items (
    id              BIGSERIAL PRIMARY KEY,
    cart_id         BIGINT REFERENCES carts(id),
    product_id      BIGINT REFERENCES products(id),
    quantity        INT NOT NULL,
    price_at_add    DECIMAL(12,2) NOT NULL,
    UNIQUE(cart_id, product_id)
);
```

### 5.5 Order Module Tables

```sql
-- orders
CREATE TABLE orders (
    id              BIGSERIAL PRIMARY KEY,
    order_number    VARCHAR(50) NOT NULL UNIQUE,
    user_id         BIGINT REFERENCES users(id),
    status          VARCHAR(30) NOT NULL,
    subtotal        DECIMAL(12,2) NOT NULL,
    tax             DECIMAL(12,2) DEFAULT 0,
    total           DECIMAL(12,2) NOT NULL,
    shipping_address_id BIGINT,
    notes           TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- order_items
CREATE TABLE order_items (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT REFERENCES orders(id),
    product_id      BIGINT REFERENCES products(id),
    product_name    VARCHAR(255) NOT NULL,  -- snapshot
    quantity        INT NOT NULL,
    unit_price      DECIMAL(12,2) NOT NULL,
    subtotal        DECIMAL(12,2) NOT NULL
);

-- order_history
CREATE TABLE order_history (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT REFERENCES orders(id),
    status          VARCHAR(30) NOT NULL,
    notes           TEXT,
    changed_by      BIGINT REFERENCES users(id),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 5.6 Payment Module Tables

```sql
-- payments
CREATE TABLE payments (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT REFERENCES orders(id),
    amount          DECIMAL(12,2) NOT NULL,
    method          VARCHAR(50) NOT NULL,
    status          VARCHAR(30) NOT NULL,
    external_id     VARCHAR(255),  -- payment gateway reference
    idempotency_key VARCHAR(100) UNIQUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- payment_logs
CREATE TABLE payment_logs (
    id              BIGSERIAL PRIMARY KEY,
    payment_id      BIGINT REFERENCES payments(id),
    event_type      VARCHAR(50) NOT NULL,
    payload         JSONB,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 5.7 Review Module Tables

```sql
-- reviews
CREATE TABLE reviews (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT REFERENCES products(id),
    user_id         BIGINT REFERENCES users(id),
    rating          INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    title           VARCHAR(255),
    comment         TEXT,
    is_verified     BOOLEAN DEFAULT FALSE,  -- verified purchase
    status          VARCHAR(20) DEFAULT 'PENDING',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(product_id, user_id)
);
```

### 5.8 Admin Module Tables

```sql
-- audit_logs
CREATE TABLE audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT REFERENCES users(id),
    action          VARCHAR(100) NOT NULL,
    entity_type     VARCHAR(100) NOT NULL,
    entity_id       BIGINT,
    old_value       JSONB,
    new_value       JSONB,
    ip_address      VARCHAR(45),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 6. Tech Stack

### 6.1 Core Framework

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 LTS | Programming language |
| Spring Boot | 3.3+ | Application framework |
| Spring Web | - | REST API support |
| Spring Data JPA | - | Database access |
| Spring Security | - | Authentication/Authorization |
| Spring Validation | - | Input validation |

### 6.2 Database & Persistence

| Technology | Purpose |
|------------|---------|
| PostgreSQL | Primary database |
| Flyway | Database migrations |
| HikariCP | Connection pooling |

### 6.3 Security

| Technology | Purpose |
|------------|---------|
| JWT (jjwt) | Token-based authentication |
| BCrypt | Password hashing |

### 6.4 Developer Productivity

| Technology | Purpose |
|------------|---------|
| Lombok | Reduce boilerplate |
| MapStruct | Object mapping |
| SpringDoc OpenAPI | API documentation |

### 6.5 Testing

| Technology | Purpose |
|------------|---------|
| JUnit 5 | Test framework |
| Mockito | Mocking |
| Testcontainers | Integration tests |
| REST Assured | API testing |

### 6.6 Observability

| Technology | Purpose |
|------------|---------|
| Spring Actuator | Health checks, metrics |
| SLF4J + Logback | Logging |
| Micrometer | Metrics collection |

### 6.7 Build & Infrastructure

| Technology | Purpose |
|------------|---------|
| Maven | Build tool |
| Docker | Containerization |
| Docker Compose | Local dev environment |

---

## 7. Design Patterns

### 7.1 Architectural Patterns

| Pattern | Where Used | Description |
|---------|------------|-------------|
| Modular Monolith | Overall structure | Independent modules in one deployment |
| Layered Architecture | Per module | api → application → domain → infrastructure |
| Repository Pattern | Data access | Abstract database operations |
| DTO Pattern | API layer | Separate external contracts from internal entities |

### 7.2 Domain Patterns

| Pattern | Where Used | Description |
|---------|------------|-------------|
| Entity | All modules | Objects with identity (has an ID) |
| Value Object | Addresses, Money | Objects without identity, immutable |
| Aggregate | Order + OrderItems | Cluster treated as one unit |
| Domain Service | Complex logic | Logic that doesn't belong to one entity |
| Application Service | Use cases | Orchestrates domain objects |

### 7.3 Behavioral Patterns

| Pattern | Where Used | Description |
|---------|------------|-------------|
| Builder | Complex objects | Clean object construction |
| Factory | Object creation | Create objects based on input |
| Strategy | Payment, Shipping | Interchangeable algorithms |
| State Machine | Order status | Controlled state transitions |

### 7.4 Infrastructure Patterns

| Pattern | Where Used | Description |
|---------|------------|-------------|
| Mapper | DTO ↔ Entity | Dedicated conversion classes |
| Exception Handler | Global | Consistent error responses |
| Specification | Queries | Composable query criteria |

---

## 8. Coding Standards

### 8.1 Naming Conventions

| Element | Convention | Example |
|---------|------------|---------|
| Classes | PascalCase | `OrderService` |
| Methods | camelCase | `createOrder()` |
| Variables | camelCase | `orderTotal` |
| Constants | SCREAMING_SNAKE | `MAX_CART_ITEMS` |
| Packages | lowercase, singular | `com.ecommerce.platform.order` |
| DB tables | snake_case, plural | `order_items` |
| DB columns | snake_case | `created_at` |
| REST endpoints | kebab-case, plural | `/api/v1/order-items` |

### 8.2 Class Naming by Layer

| Layer | Suffix | Example |
|-------|--------|---------|
| Controller | `Controller` | `ProductController` |
| Application Service | `Service` | `OrderService` |
| Domain Service | `DomainService` | `PricingDomainService` |
| Repository Interface | `Repository` | `ProductRepository` |
| DTO (request) | `Request` | `CreateProductRequest` |
| DTO (response) | `Response` | `ProductResponse` |
| Mapper | `Mapper` | `ProductMapper` |
| Exception | `Exception` | `ProductNotFoundException` |

### 8.3 REST API Standards

**URL Structure:**
```
GET    /api/v1/products          → List (paginated)
GET    /api/v1/products/{id}     → Get single
POST   /api/v1/products          → Create
PUT    /api/v1/products/{id}     → Full update
PATCH  /api/v1/products/{id}     → Partial update
DELETE /api/v1/products/{id}     → Delete
```

**Response Format:**
```json
// Success
{
  "success": true,
  "data": { ... },
  "message": null
}

// Success with pagination
{
  "success": true,
  "data": [ ... ],
  "pagination": {
    "page": 0,
    "size": 20,
    "totalElements": 156,
    "totalPages": 8
  }
}

// Error
{
  "success": false,
  "data": null,
  "message": "Product not found",
  "errorCode": "PRODUCT_NOT_FOUND",
  "timestamp": "2026-06-26T10:30:00Z"
}
```

### 8.4 Code Rules

1. **Method length:** Max ~20 lines
2. **Class length:** Max ~300 lines
3. **Parameter count:** Max 3-4, use objects for more
4. **Return early:** Avoid deep nesting

### 8.5 Testing Standards

| Test Type | Naming | Example |
|-----------|--------|---------|
| Unit test | `{Class}Test` | `OrderServiceTest.java` |
| Integration test | `{Class}IT` | `OrderControllerIT.java` |

**Method naming:** `should_ExpectedBehavior_When_Condition`

---

## 9. Development Roadmap

### Phase 1: Project Setup + User Module (Week 1-2)

**Deliverables:**
- Spring Boot project scaffold
- PostgreSQL + Flyway setup
- User registration & login
- JWT authentication
- User profile & addresses
- Role-based authorization

**Endpoints:**
```
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh
GET    /api/v1/users/me
PUT    /api/v1/users/me
GET    /api/v1/users/me/addresses
POST   /api/v1/users/me/addresses
PUT    /api/v1/users/me/addresses/{id}
DELETE /api/v1/users/me/addresses/{id}
```

**Learning Objectives:**
- Spring Boot project structure
- Spring Data JPA basics
- Spring Security configuration
- JWT token flow
- Password hashing with BCrypt
- Database migrations with Flyway
- Input validation
- Global exception handling

---

### Phase 2: Product Module (Week 3-4)

**Deliverables:**
- Categories (hierarchical)
- Products with attributes
- Product images
- Pagination, sorting, filtering

**Endpoints:**
```
GET    /api/v1/categories
GET    /api/v1/categories/{slug}
GET    /api/v1/categories/{slug}/products
GET    /api/v1/products
GET    /api/v1/products/{slug}
POST   /api/v1/admin/products
PUT    /api/v1/admin/products/{id}
DELETE /api/v1/admin/products/{id}
POST   /api/v1/admin/products/{id}/images
```

**Learning Objectives:**
- Entity relationships (OneToMany, ManyToOne)
- Self-referencing entities
- DTOs and MapStruct mappers
- Pagination with Spring Data
- Specification pattern

---

### Phase 3: Inventory Module (Week 5)

**Deliverables:**
- Stock tracking
- Stock reservation
- Low stock alerts
- Adjustment history

**Endpoints:**
```
GET    /api/v1/admin/inventory
GET    /api/v1/admin/inventory/{productId}
PUT    /api/v1/admin/inventory/{productId}
POST   /api/v1/admin/inventory/{productId}/adjust
GET    /api/v1/admin/inventory/low-stock
```

**Learning Objectives:**
- Optimistic locking
- Transactional integrity
- Domain events
- Business rule enforcement

---

### Phase 4: Cart Module (Week 6)

**Deliverables:**
- Shopping cart (guest + authenticated)
- Add/remove/update items
- Cart merging
- Cart expiration

**Endpoints:**
```
GET    /api/v1/cart
POST   /api/v1/cart/items
PUT    /api/v1/cart/items/{productId}
DELETE /api/v1/cart/items/{productId}
DELETE /api/v1/cart
POST   /api/v1/cart/merge
```

**Learning Objectives:**
- Session management
- Guest user handling
- Data merging
- Scheduled tasks

---

### Phase 5: Order Module (Week 7-8)

**Deliverables:**
- Checkout flow
- Order creation from cart
- Order status lifecycle
- Order history
- Order cancellation

**Order States:**
```
PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
    ↓         ↓           ↓
 CANCELLED  CANCELLED   CANCELLED
```

**Endpoints:**
```
POST   /api/v1/orders
GET    /api/v1/orders
GET    /api/v1/orders/{id}
POST   /api/v1/orders/{id}/cancel
GET    /api/v1/admin/orders
GET    /api/v1/admin/orders/{id}
PUT    /api/v1/admin/orders/{id}/status
```

**Learning Objectives:**
- State machine pattern
- Complex transactions
- Aggregate design
- Workflow orchestration

---

### Phase 6: Payment Module (Week 9)

**Deliverables:**
- Payment processing (mocked)
- Payment status tracking
- Refund handling
- Payment logs

**Endpoints:**
```
POST   /api/v1/payments
GET    /api/v1/payments/{id}
POST   /api/v1/payments/{id}/refund
POST   /api/v1/webhooks/payment
```

**Learning Objectives:**
- External API integration
- Idempotency
- Strategy pattern
- Webhook handling

---

### Phase 7: Review Module (Week 10)

**Deliverables:**
- Product reviews & ratings
- Verified purchase badge
- Review moderation
- Rating aggregation

**Endpoints:**
```
GET    /api/v1/products/{id}/reviews
POST   /api/v1/products/{id}/reviews
PUT    /api/v1/reviews/{id}
DELETE /api/v1/reviews/{id}
GET    /api/v1/admin/reviews/pending
PUT    /api/v1/admin/reviews/{id}/approve
PUT    /api/v1/admin/reviews/{id}/reject
```

**Learning Objectives:**
- User-generated content
- Aggregation queries
- Moderation workflows
- Abuse prevention

---

### Phase 8: Admin Module (Week 11-12)

**Deliverables:**
- Admin dashboard APIs
- User management
- Audit logging
- Basic reporting

**Endpoints:**
```
GET    /api/v1/admin/users
GET    /api/v1/admin/users/{id}
PUT    /api/v1/admin/users/{id}/status
PUT    /api/v1/admin/users/{id}/role
GET    /api/v1/admin/audit-logs
GET    /api/v1/admin/reports/sales
GET    /api/v1/admin/reports/top-products
GET    /api/v1/admin/dashboard/stats
```

**Learning Objectives:**
- Fine-grained RBAC
- Audit trail implementation
- Reporting queries
- Admin security

---

## 10. Summary Timeline

| Phase | Module | Duration | Cumulative |
|-------|--------|----------|------------|
| 1 | Setup + User | 2 weeks | Week 2 |
| 2 | Product | 2 weeks | Week 4 |
| 3 | Inventory | 1 week | Week 5 |
| 4 | Cart | 1 week | Week 6 |
| 5 | Order | 2 weeks | Week 8 |
| 6 | Payment | 1 week | Week 9 |
| 7 | Review | 1 week | Week 10 |
| 8 | Admin | 2 weeks | Week 12 |

**Total:** ~12 weeks for complete backend

---

## Appendix A: Future Enhancements (Out of Scope)

These were intentionally excluded but can be added later:

- Wishlist module
- Shipping integration
- Notifications (email/SMS)
- Full-text search (Elasticsearch)
- Promotions/Coupons
- Redis caching
- Message queues (async processing)
