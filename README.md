# Warehouse Inventory and Stock Management System

A secure, role-based backend system for managing warehouse products, stock operations, and audit history using a clean layered architecture.
---

## Problem Statement

The retail company currently tracks warehouse inventory manually, leading to:

- Stock count inaccuracies
- No accountability for stock changes
- Lack of audit history
- Poor visibility across roles

This system digitizes and controls all product and stock operations with full audit tracking.

---

## System Architecture
<img width="1323" height="793" alt="image" src="https://github.com/user-attachments/assets/d13fee08-de44-477f-a028-10584fe2455f" />
<img width="1365" height="840" alt="image" src="https://github.com/user-attachments/assets/f826ab6c-9e95-4b7f-8778-9a366a66447d" />

The system follows a layered clean architecture:

Client  
↓  
Controller Layer (REST APIs)  
↓  
Service Layer (Business Logic)  
↓  
Repository Layer (JPA)  
↓  
MySQL Database  

### Core Modules

- Authentication Module (JWT-based)
- User & Role Module
- Product Module
- Stock Module
- Movement History Module

---

## User Roles

### Admin
- Create products
- Update products
- View products
- Add stock
- Remove stock
- View stock history

### Warehouse Staff
- View products
- Add stock
- Remove stock
- View stock history

---

## Tech Stack

- Java
- Spring Boot
- Spring Security (JWT)
- Spring Data JPA
- MySQL
- Maven
- Docker 

---

## Security

- JWT-based authentication
- Role-based authorization
- BCrypt password hashing
- Token expiry validation
- No sensitive data returned in API responses

---
## Role Permission Matrix

| Endpoint | Admin | Staff |
|----------|--------|--------|
| `POST /auth/login` | ✅ | ✅ |
| `POST /products` | ✅ | ❌ |
| `GET /products` | ✅ | ✅ |
| `GET /products/:id` | ✅ | ✅ |
| `PUT /products/:id` | ✅ | ❌ |
| `POST /stock/add` | ✅ | ✅ |
| `POST /stock/remove` | ✅ | ✅ |
| `GET /stock/history` | ✅ | ✅ |
| `GET /stock/history/:productId` | ✅ | ✅ |

---

## HTTP Status Code Summary

| Status | When Used |
|--------|------------|
| `200 OK` | Successful read or update |
| `201 Created` | Resource successfully created |
| `400 Bad Request` | Invalid input — missing fields, wrong types |
| `401 Unauthorized` | Missing or invalid JWT token |
| `403 Forbidden` | Token valid but role lacks permission |
| `404 Not Found` | Resource does not exist |
| `409 Conflict` | Duplicate resource or idempotency key already used |
| `422 Unprocessable Entity` | Input valid but business rule violated (e.g., insufficient stock) |
| `500 Internal Server Error` | Unexpected server failure |

---

---

# Setup & Running the Application

## Prerequisites

Make sure you have installed:

- Java 17+
- Maven 3.8+
- Docker
- Docker Compose

---

## Step 1: Start Database Using Docker

From the project root directory:

```bash
docker-compose up -d
```

This will:

- Start MySQL container
- Create required database
- Expose configured ports
- Prepare environment for the Inventory application

To verify containers:

```bash
docker ps
```

To stop containers:

```bash
docker-compose down
```

---

## Step 2: Run Inventory Application

After Docker services are running, start the Spring Boot application.

### Option A: Using Maven

```bash
mvn clean install
mvn spring-boot:run
```

### Option B: Run Packaged JAR

Build the project:

```bash
mvn clean package
```

Run the generated JAR:

```bash
java -jar target/inventory-0.0.1-SNAPSHOT.jar
```

---

## Application Access

Application URL:

```
http://localhost:8080
```

Base API URL:

```
http://localhost:8080/api/v1
```

---

# Running Tests

## Run All Tests

```bash
mvn test
```

## Run Only StockService Tests

```bash
mvn -Dtest=StockServiceTest test
```

This runs only the `StockServiceTest` class.

---

#  Design Decisions

- JWT for stateless authentication
- Append-only StockMovement for audit integrity
- Stock quantity stored in Product for performance
- Role-based middleware authorization
- Layered architecture for maintainability

---

# Assumptions

- Single warehouse system
- No ERP integration
- Admin-managed users
- Stock stored as running total

---

## Sample Screenshots
![img.png](img.png)
### 1. Signup as New Staff User
<img src="https://github.com/user-attachments/assets/9a80c153-4d06-4367-92a7-97f2af5bd5ca" width="700"/>

### 2. Login as Warehouse Admin
<img src="https://github.com/user-attachments/assets/a118ba3c-5047-4656-89e2-0d2003024e4d" width="900"/>

### 3. Add New Product
<img src="https://github.com/user-attachments/assets/d37f7426-cda0-4b7b-ac72-ffbb10dbec32" width="900"/>

### 4. View All Available Products
<img src="https://github.com/user-attachments/assets/5fbfd085-5211-42a2-9764-a9d0fe2bded2" width="700"/>

### 5. Add Stock
<img src="https://github.com/user-attachments/assets/833a36d7-7092-4631-96c1-8af669fac834" width="700"/>

### 6. Remove Stock (Validation Error Example)
<img src="https://github.com/user-attachments/assets/f9357694-dbe4-4887-bb4f-8c94fc101231" width="900"/>

### 7. View Product Stock History
<img src="https://github.com/user-attachments/assets/e70cfe0f-eb15-4543-b594-be72ee4b3eb6" width="900"/>

### 8. Filter stock movement by date
![img_1.png](img_1.png)
---






