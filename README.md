# Warehouse Inventory & Stock Management System

A production-ready REST API built with **Spring Boot 3.2**, **Java 21**, and **MySQL** for managing warehouse inventory, stock movements, reservations, threshold breach alerting, and async email notifications.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Environment Variables](#environment-variables)
- [API Overview](#api-overview)
- [Roles & Permissions](#roles--permissions)
- [Seed Users](#seed-users)
- [Running Tests](#running-tests)
- [CSV Bulk Upload Format](#csv-bulk-upload-format)

---

## Features

- **Product Management** — Create, update, and list products with SKU, thresholds, and assigned Product Managers
- **Stock Operations** — ADD, REMOVE, RESERVE, RELEASE with full audit trail
- **Threshold Breach Detection** — Automatic BELOW_MIN / ABOVE_MAX detection on every stock change
- **Async Email Notifications** — Thymeleaf HTML breach alert emails sent via Gmail SMTP with retry logic
- **Stock Reservations** — Time-limited reservations with automatic expiry scheduler
- **Breach Monitor** — Dedicated `/products/breached` endpoint for real-time breach dashboard
- **Time-Range Metrics** — Business-level operational metrics over any custom time window
- **Admin Retrigger** — Manually retrigger failed email notifications per alert
- **CSV Bulk Upload** — Upload products in bulk via CSV; async job processing with per-row results
- **CSV Export** — Export products and stock movements as downloadable CSV files
- **JWT Authentication** — Stateless auth with role-based access control (ADMIN / STAFF / PRODUCT_MANAGER)
- **JPA Specification Filtering** — Dynamic multi-criteria filtering with pagination and sorting
- **Micrometer Metrics** — Prometheus-compatible metrics exposed via Spring Actuator
- **Health Endpoint** — Custom `/api/v1/health` plus `/actuator/health`
- **Swagger / OpenAPI** — Interactive API docs at `/docs`

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.2.3 |
| Security | Spring Security + JWT (JJWT 0.12.3) |
| Database | MySQL 8.0 |
| ORM | Spring Data JPA / Hibernate |
| Validation | Jakarta Bean Validation |
| Email | Spring Mail + Thymeleaf templates |
| Cache | Caffeine (in-memory) |
| Metrics | Micrometer + Prometheus |
| CSV | OpenCSV 5.9 |
| API Docs | SpringDoc OpenAPI 2.5 (Swagger UI) |
| Build | Maven (Maven Wrapper included) |
| Containerisation | Docker Compose (MySQL only) |

---

## Project Structure

```
inventory/
├── src/main/java/com/warehouse/inventory/
│   ├── config/             # Security, async, Swagger, metrics, cache config
│   ├── controller/         # REST controllers (Auth, Product, Stock, Alerts, etc.)
│   ├── dto/
│   │   ├── request/        # Validated request payloads
│   │   └── response/       # Typed response wrappers
│   ├── entity/             # JPA entities (Product, User, StockMovement, etc.)
│   ├── exception/          # Global exception handler + custom exceptions
│   ├── repository/         # Spring Data JPA repositories
│   ├── scheduler/          # Reservation expiry + notification retry schedulers
│   ├── security/           # JWT filter, UserDetails, JwtService
│   ├── service/            # Service interfaces + implementations
│   └── specification/      # JPA Specification builders for dynamic filtering
├── src/main/resources/
│   ├── application.yml     # App configuration
│   ├── schema.sql          # DDL — auto-run on startup
│   ├── data.sql            # Seed users — auto-run on startup
│   └── templates/email/    # Thymeleaf HTML email templates
├── docker-compose.yml      # MySQL container
└── pom.xml
```

---

## Getting Started

### Prerequisites

- Java 21+
- Docker Desktop (for MySQL)
- Maven (or use `./mvnw`)

### 1. Clone the repository

```bash
git clone https://github.com/DISHANK-PATEL/Warehouse_Inventory_and_Stock_Management.git
cd Warehouse_Inventory_and_Stock_Management/inventory
```

### 2. Start MySQL

```bash
docker compose up -d
```

### 3. Set environment variables

```bash
export MYSQL_ROOT_PASSWORD=secret
export MYSQL_DATABASE=warehouse_db
export MYSQL_USER=root
export MYSQL_PASSWORD=secret
export MAIL_USERNAME=your_gmail@gmail.com
export MAIL_PASSWORD=your_gmail_app_password
```

> **Gmail App Password:** Go to myaccount.google.com → Security → 2-Step Verification → App Passwords → Generate.
> Never commit credentials to Git.

### 4. Build & run

```bash
chmod +x mvnw
./mvnw clean spring-boot:run
```

### 5. Open Swagger UI

```
http://localhost:8080/docs
```

---

## Environment Variables

| Variable | Description | Default (dev only) |
|---|---|---|
| `MYSQL_ROOT_PASSWORD` | MySQL root password | — |
| `MYSQL_DATABASE` | Database name | `warehouse_db` |
| `MYSQL_USER` | MySQL user | `root` |
| `MYSQL_PASSWORD` | MySQL password | — |
| `MAIL_USERNAME` | Gmail address for sending alerts | — |
| `MAIL_PASSWORD` | Gmail App Password (16 chars) | — |
| `JWT_SECRET` | JWT signing secret (64+ chars) | set in application.yml |

---

## API Overview

All endpoints are prefixed with `/api/v1`. Full interactive docs available at **`/docs`**.

### Auth
| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/auth/login` | Public | Get JWT token |
| POST | `/auth/signup` | ADMIN | Create new user |

### Products
| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/products` | ADMIN, PM | Create product |
| GET | `/products` | ALL | List with filters & pagination |
| GET | `/products/{id}` | ALL | Get product by ID |
| PUT | `/products/{id}` | ADMIN | Update product |
| GET | `/products/breached` | ALL | Products with active threshold breaches |

### Stock
| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/stock/update` | ALL | ADD / REMOVE / RESERVE / RELEASE |
| GET | `/stock/history` | ALL | Paginated movement history |
| GET | `/stock/history/{id}` | ALL | Movement by ID |
| GET | `/stock/reservations` | ALL | Active reservations |

### Stock Alerts
| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/alerts` | ALL | List alerts with filters |
| GET | `/alerts/{id}` | ALL | Get alert by ID |
| POST | `/alerts/{id}/retrigger` | ADMIN | Retrigger failed notifications |

### Metrics
| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/metrics` | ADMIN | Time-range operational metrics |

**Query params:** `?hours=24` (last N hours) or `?from=2025-01-01T00:00:00&to=2025-01-31T23:59:59`

### Bulk Operations
| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/bulk/upload` | ADMIN | Upload products CSV |
| GET | `/bulk/{jobId}` | ALL | Get job status & row results |
| GET | `/bulk` | ALL | List all bulk jobs |

### Export
| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/export/products` | ALL | Download products as CSV |
| GET | `/export/movements` | ALL | Download stock movements as CSV |

### Health
| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/health` | Public | App + DB health status |

---

## Roles & Permissions

| Permission | ADMIN | STAFF | PRODUCT_MANAGER |
|---|:---:|:---:|:---:|
| Create user | ✅ | ❌ | ❌ |
| Create product | ✅ | ❌ | ✅ |
| Update product | ✅ | ❌ | ❌ |
| View products | ✅ | ✅ | ✅ (own only) |
| Stock ADD / REMOVE | ✅ | ✅ | ✅ |
| Stock RESERVE / RELEASE | ✅ | ✅ | ✅ |
| View alerts | ✅ | ✅ | ✅ (own products) |
| Retrigger notifications | ✅ | ❌ | ❌ |
| View metrics | ✅ | ❌ | ❌ |
| Bulk CSV upload | ✅ | ❌ | ❌ |
| CSV export | ✅ | ✅ | ✅ (own scope) |

> **Product Manager scope:** PMs automatically see only products assigned to them — no extra filter needed.

---

## Seed Users

These users are auto-inserted on first startup via `data.sql`:

| Role | Email | Password |
|---|---|---|
| ADMIN | admin@warehouse.com | admin123 |
| STAFF | staff@warehouse.com | password |
| PRODUCT_MANAGER | pm@warehouse.com | productmanager123 |

---

## Running Tests

```bash
./mvnw test
```

Unit tests are in `src/test/java/com/warehouse/inventory/`.

---

## CSV Bulk Upload Format

Upload a `.csv` file to `POST /api/v1/bulk/upload` with the following columns:

```csv
name,sku,description,minThreshold,maxThreshold,productManagerEmail
Widget A,SKU-001,Blue widget,10,500,pm@warehouse.com
Widget B,SKU-002,Red widget,5,200,
```

- `productManagerEmail` is optional — leave blank to create unassigned products
- `minThreshold` and `maxThreshold` are optional
- Rows with duplicate name or SKU are skipped with a per-row error message
- Job results are accessible via `GET /api/v1/bulk/{jobId}`

---

## Notification Retry Logic

When a breach email fails to send:
- Status is set to `FAILED` in `notification_logs`
- The `RetryScheduler` retries every 5 minutes (up to 3 attempts)
- Admins can manually force a retry via `POST /api/v1/alerts/{id}/retrigger`

