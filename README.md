# E-Commerce Platform Backend

A production-grade B2C e-commerce platform backend built with **Java Spring Boot**, following **Modular Monolith** architecture with **DDD-lite** patterns. This project demonstrates enterprise-level backend development practices including authentication, authorization, transactional integrity, and clean architecture.

## Highlights

- **125+ Java source files** across 8 business modules
- **7 Flyway migrations** for database versioning
- **50+ REST API endpoints** with Swagger documentation
- **JWT authentication** with access/refresh token flow
- **Role-based access control** (RBAC) with CUSTOMER and ADMIN roles
- **Pessimistic locking** for inventory management
- **Audit trails** for orders, payments, and stock movements

## Tech Stack

| Category | Technology |
|----------|------------|
| Language | Java 21 LTS |
| Framework | Spring Boot 3.3, Spring Security, Spring Data JPA |
| Database | PostgreSQL 16, Flyway Migrations |
| Auth | JWT (jjwt), BCrypt |
| API Docs | SpringDoc OpenAPI (Swagger) |
| Build | Maven, Docker Compose |

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                   SPRING BOOT APPLICATION                    │
│  ┌─────────┬─────────┬───────────┬────────┬───────────────┐ │
│  │  User   │ Product │ Inventory │  Cart  │     Order     │ │
│  │ Module  │ Module  │  Module   │ Module │    Module     │ │
│  └─────────┴─────────┴───────────┴────────┴───────────────┘ │
│  ┌─────────┬─────────┬───────────┐                          │
│  │ Payment │ Review  │   Admin   │      SHARED / INFRA      │
│  │ Module  │ Module  │  Module   │                          │
│  └─────────┴─────────┴───────────┘                          │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │   PostgreSQL    │
                    └─────────────────┘
```

### Module Structure (DDD-lite)

Each module follows a 4-layer architecture:

```
module/
├── api/              # REST controllers, DTOs, request/response objects
├── application/      # Business logic, service orchestration
├── domain/           # Entities, repositories, domain models
└── infrastructure/   # Framework implementations (if needed)
```

**Dependency rule:** `api/` → `application/` → `domain/` ← `infrastructure/`

## Modules Overview

| Module | Description | Key Features |
|--------|-------------|--------------|
| **User** | Authentication & profiles | JWT auth, refresh tokens, address management, RBAC |
| **Product** | Catalog management | Categories, attributes, images, SEO slugs, search |
| **Inventory** | Stock management | Reservations, pessimistic locking, stock movements, low-stock alerts |
| **Cart** | Shopping cart | Add/remove items, inventory validation, price snapshots |
| **Order** | Order processing | Checkout flow, status lifecycle, order history, cancellation |
| **Payment** | Payment handling | Mocked gateway, COD, refunds, transaction logs |
| **Review** | Product reviews | Ratings, verified purchases, helpful votes, moderation |
| **Admin** | Back-office | Dashboard stats, user management, role assignment |

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+ (or use the included `mvnw`)
- Docker + Docker Compose

### Quick Start (local)

1) Start PostgreSQL

```bash
docker compose up -d
```

2) Run the application

```bash
./mvnw spring-boot:run
```

3) Verify

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI spec: http://localhost:8080/api-docs
- Health: http://localhost:8080/actuator/health

> Notes:
> - The app runs on port **8080**.
> - Flyway migrations are enabled and run automatically on startup.

### Database credentials (from docker-compose.yml)

- host: `localhost`
- port: `5432`
- database: `ecommerce`
- username: `ecommerce`
- password: `ecommerce123`

### JWT configuration

The JWT secret is read from `JWT_SECRET`.

- Default dev secret is provided in `src/main/resources/application.yml` (not for production).
- For local override:

```bash
export JWT_SECRET="<your-base64-256-bit-secret>"
./mvnw spring-boot:run
```

### Create an Admin User

After registration, users get the `CUSTOMER` role by default. To grant admin access:

```sql
-- Connect to database
docker exec -it ecommerce-db psql -U ecommerce -d ecommerce

-- Add ADMIN role to a user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.email = 'your-email@example.com' AND r.name = 'ADMIN';
```


## API Reference

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/register` | Register new user |
| POST | `/api/v1/auth/login` | Login, receive JWT tokens |
| POST | `/api/v1/auth/refresh` | Refresh access token |

### Products (Public)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/products` | List products (paginated, searchable) |
| GET | `/api/v1/products/{slug}` | Get product by slug |
| GET | `/api/v1/categories` | List all categories |

### Cart (Authenticated)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/cart` | Get current user's cart |
| POST | `/api/v1/cart/items` | Add item to cart |
| PUT | `/api/v1/cart/items/{productId}` | Update item quantity |
| DELETE | `/api/v1/cart/items/{productId}` | Remove item from cart |

### Orders (Authenticated)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/orders` | Create order from cart |
| GET | `/api/v1/orders` | List user's orders |
| GET | `/api/v1/orders/{id}` | Get order details |
| POST | `/api/v1/orders/{id}/cancel` | Cancel order |

### Reviews
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/reviews/product/{id}` | Get product reviews (public) |
| GET | `/api/v1/reviews/product/{id}/summary` | Get rating summary (public) |
| POST | `/api/v1/reviews` | Create review (authenticated) |
| POST | `/api/v1/reviews/{id}/helpful` | Mark as helpful (authenticated) |

### Admin (ADMIN role required)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/admin/dashboard` | Dashboard statistics |
| GET | `/api/v1/admin/users` | List all users |
| PUT | `/api/v1/admin/users/{id}/status` | Update user status |
| POST | `/api/v1/admin/users/{id}/roles` | Add role to user |

## Key Implementation Details

### Authentication Flow
- **Access Token:** 15-minute expiry, used for API requests
- **Refresh Token:** 7-day expiry, used to obtain new access tokens
- **Password Storage:** BCrypt hashing with salt

### Order Lifecycle
```
PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
    ↓         ↓           ↓
 CANCELLED  CANCELLED   CANCELLED
```
- Order creation deducts inventory
- Cancellation restores inventory
- Payment success auto-confirms order

### Inventory Management
- **Pessimistic locking** prevents overselling during concurrent checkouts
- **Stock movements** track all inventory changes with audit trail
- **Reservations** hold stock during checkout process

## Project Structure

```
ecommerce-platform/
├── src/main/java/com/ecommerce/platform/
│   ├── user/                    # User module (auth, profiles, addresses)
│   ├── product/                 # Product module (catalog, categories)
│   ├── inventory/               # Inventory module (stock management)
│   ├── cart/                    # Cart module (shopping cart)
│   ├── order/                   # Order module (checkout, lifecycle)
│   ├── payment/                 # Payment module (transactions)
│   ├── review/                  # Review module (ratings, moderation)
│   ├── admin/                   # Admin module (dashboard, management)
│   └── shared/                  # Cross-cutting concerns
│       ├── config/              # Spring configuration
│       ├── security/            # JWT filter, authentication
│       ├── exception/           # Global exception handling
│       └── dto/                 # ApiResponse, PagedResponse
├── src/main/resources/
│   ├── application.yml          # Main configuration
│   ├── application-dev.yml      # Development profile
│   └── db/migration/            # Flyway migrations (V1-V7)
├── docs/
│   ├── specs/                   # Design specification
│   └── BUILD-LOG.md             # Development progress log
├── docker-compose.yml           # PostgreSQL container
└── pom.xml                      # Maven dependencies
```

## Design Documentation

- **[Design Specification](docs/specs/2026-06-26-ecommerce-platform-design.md)** - Full system design with database schemas, API contracts, and architecture decisions
- **[Build Log](docs/BUILD-LOG.md)** - Phase-by-phase implementation progress

## Future Enhancements

- [ ] Unit and integration tests
- [ ] Redis caching for products/sessions
- [ ] Elasticsearch for full-text search
- [ ] Email notifications
- [ ] Wishlist module
- [ ] Promotions/coupons

## License

This project is for educational and portfolio demonstration purposes.
