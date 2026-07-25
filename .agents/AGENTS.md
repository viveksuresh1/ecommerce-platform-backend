# Workspace AGENTS.md — E-Commerce Platform Backend

This file serves as the project-scoped memory for the **E-Commerce Platform Backend** repository.

---

## 📌 Project Summary & Remote

- **Project Name**: E-Commerce Platform Backend
- **GitHub Repository**: [https://github.com/viveksuresh1/ecommerce-platform-backend](https://github.com/viveksuresh1/ecommerce-platform-backend)
- **Local Path**: `/Users/sureshkrishnamurthy/Documents/projects/E-commerce platform`
- **Main Branch**: `main`

---

## 🛠️ Technology Stack & Architecture

- **Language & Framework**: Java 21, Spring Boot 3.3.0, Spring Data JPA, Spring Security
- **Database**: PostgreSQL 16 (via Docker Compose on port 5432)
- **Migrations**: Flyway DB (`V1` to `V7` scripts in `src/main/resources/db/migration/`)
- **Architecture Pattern**: Modular Monolith with DDD-lite
  - 4 Layers per module: `api/` → `application/` → `domain/` ← `infrastructure/`
- **Key Modules**: `user`, `product`, `inventory`, `cart`, `order`, `payment`, `review`, `admin`, `shared`

---

## 🔑 Critical Domain Rules & Concurrency

1. **Inventory Concurrency**: Use `PESSIMISTIC_WRITE` locking when querying stock during cart checkout to prevent race conditions and overselling.
2. **Order Lifecycle Machine**:
   `PENDING` → `CONFIRMED` → `PROCESSING` → `SHIPPED` → `DELIVERED`
   *(Cancelling an order automatically restores inventory via stock movements).*
3. **Authentication**: JWT Access Token (15m expiration) + Refresh Token (7d expiration). Passwords hashed using BCrypt.
4. **API Responses**: Always wrap REST API responses using `ApiResponse<T>` or `PagedResponse<T>` from `shared/dto`.

---

## 🚀 Local Development Commands

- **Start PostgreSQL**: `docker compose up -d`
- **Run Application**: `./mvnw spring-boot:run`
- **Swagger Documentation**: `http://localhost:8080/swagger-ui.html`
- **Actuator Health**: `http://localhost:8080/actuator/health`

---

## 📝 Recent Progress & History

- ✅ **Project Setup**: Spring Boot 3.3, Java 21, Docker Compose.
- ✅ **Shared Infrastructure**: Base entities, global exception handler, security filter chain.
- ✅ **User & Auth Module**: DB Migration V1, AuthController, UserController, JWT token service.
- ✅ **Product Module**: DB Migration V2, ProductController, CategoryController.
- ✅ **Inventory Module**: DB Migration V3, stock reservations, pessimistic locking.
- ✅ **Cart Module**: DB Migration V4, CartController, item quantity validation.
- ✅ **Order Module**: DB Migration V5, OrderController, order state machine.
- ✅ **Payment Module**: DB Migration V6, mock gateway, COD, refund logging.
- ✅ **Review Module**: DB Migration V7, rating calculation, review votes.
- ✅ **Admin Module**: Admin dashboard stats, role management endpoints.
- ✅ **Documentation**: Design specs, build log, OpenAPI documentation.
- ✅ **GitHub Release**: Initialized Git repository and pushed 12-commit history to `viveksuresh1/ecommerce-platform-backend`.
