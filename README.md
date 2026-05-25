# Personal Finance Manager API

A RESTful backend service for managing personal finances — built with **Kotlin**, **Spring Boot 3.x**, and **H2** (in-memory / file-based). Supports transaction tracking, category management, savings goals, and financial reports, all secured via session-based authentication.

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Running Locally](#running-locally)
- [Running Tests](#running-tests)
- [Project Structure](#project-structure)
- [API Reference](#api-reference)
  - [Authentication](#1-authentication)
  - [Categories](#2-categories)
  - [Transactions](#3-transactions)
  - [Savings Goals](#4-savings-goals)
  - [Reports](#5-reports)
- [Error Responses](#error-responses)
- [Deploying to Render](#deploying-to-render)
- [Default Categories](#default-categories)

---

## Tech Stack

| Layer          | Technology                        |
|----------------|-----------------------------------|
| Language       | Kotlin 1.9                        |
| Framework      | Spring Boot 3.2                   |
| Security       | Spring Security 6 (session-based) |
| Persistence    | Spring Data JPA + Hibernate       |
| Database       | H2 (in-memory dev / file prod)    |
| Build          | Gradle Kotlin DSL                 |
| Testing        | JUnit 5 + Mockito-Kotlin          |
| Coverage       | JaCoCo (≥ 80%)                    |
| Deployment     | Docker + Render                   |

---

## Prerequisites

- **JDK 17+**
- **Gradle 8+** (or use the `./gradlew` wrapper — no installation needed)
- Docker (optional, for containerised runs)

---

## Running Locally

### 1. Clone and build

```bash
git clone <repository-url>
cd finance-manager
./gradlew build -x test
```

### 2. Run

```bash
./gradlew bootRun
```

The API starts on **http://localhost:8080**.

### 3. H2 Console (dev only)

Navigate to **http://localhost:8080/h2-console**

| Setting | Value |
|---------|-------|
| JDBC URL | `jdbc:h2:mem:financedb` |
| Username | `sa` |
| Password | `password` |

### 4. Run with Docker

```bash
docker build -t finance-manager .
docker run -p 8080:8080 finance-manager
```

---

## Running Tests

```bash
# Run all tests
./gradlew test

# Run tests + generate coverage report
./gradlew test jacocoTestReport

# View coverage report
open build/reports/jacoco/test/html/index.html

# Enforce 80% coverage gate
./gradlew jacocoTestCoverageVerification
```

---

## Project Structure

```
src/
├── main/kotlin/com/financemanager/
│   ├── FinanceManagerApplication.kt
│   ├── config/
│   │   ├── SecurityConfig.kt          # Spring Security 6 config
│   │   └── DataInitializer.kt         # Seeds default categories
│   ├── controller/
│   │   ├── AuthController.kt
│   │   ├── CategoryController.kt
│   │   ├── TransactionController.kt
│   │   ├── GoalController.kt
│   │   └── ReportController.kt
│   ├── service/
│   │   ├── AuthService.kt
│   │   ├── CategoryService.kt
│   │   ├── TransactionService.kt
│   │   ├── GoalService.kt
│   │   └── ReportService.kt
│   ├── repository/
│   │   ├── UserRepository.kt
│   │   ├── CategoryRepository.kt
│   │   ├── TransactionRepository.kt
│   │   └── GoalRepository.kt
│   ├── entity/
│   │   ├── User.kt
│   │   ├── Category.kt
│   │   ├── Transaction.kt
│   │   └── Goal.kt
│   ├── dto/
│   │   ├── AuthDtos.kt
│   │   ├── CategoryDtos.kt
│   │   ├── TransactionDtos.kt
│   │   ├── GoalDtos.kt
│   │   ├── ReportDtos.kt
│   │   └── ErrorResponse.kt
│   ├── security/
│   │   └── UserDetailsServiceImpl.kt
│   └── exception/
│       ├── CustomExceptions.kt
│       └── GlobalExceptionHandler.kt
├── main/resources/
│   └── application.yml
└── test/kotlin/com/financemanager/
    ├── controller/
    │   ├── AuthControllerTest.kt
    │   ├── CategoryControllerTest.kt
    │   ├── TransactionControllerTest.kt
    │   ├── GoalControllerTest.kt
    │   ├── ReportControllerTest.kt
    │   └── SecurityAccessTest.kt
    └── service/
        ├── AuthServiceTest.kt
        ├── CategoryServiceTest.kt
        ├── TransactionServiceTest.kt
        ├── GoalServiceTest.kt
        └── ReportServiceTest.kt
```

---

## API Reference

> All endpoints except `/api/auth/register` and `/api/auth/login` require an active session.  
> After login, the server sets a `FINANCE_SESSION` cookie — include it in subsequent requests.

---

### 1. Authentication

#### POST `/api/auth/register`

Register a new user.

**Request body:**
```json
{
  "username": "user@example.com",
  "password": "securepassword",
  "fullName": "Jane Doe",
  "phoneNumber": "+1234567890"
}
```

**Responses:**

| Status | Description |
|--------|-------------|
| `201 Created` | Registration successful |
| `400 Bad Request` | Validation error |
| `409 Conflict` | Email already registered |

**201 Response:**
```json
{
  "id": 1,
  "username": "user@example.com",
  "fullName": "Jane Doe",
  "phoneNumber": "+1234567890"
}
```

---

#### POST `/api/auth/login`

Authenticate and start a session.

**Request body:**
```json
{
  "username": "user@example.com",
  "password": "securepassword"
}
```

**Responses:**

| Status | Description |
|--------|-------------|
| `200 OK` | Login successful; `FINANCE_SESSION` cookie set |
| `400 Bad Request` | Missing fields |
| `401 Unauthorized` | Invalid credentials |

---

#### POST `/api/auth/logout`

End the current session. **Requires authentication.**

**Responses:**

| Status | Description |
|--------|-------------|
| `200 OK` | Logged out |
| `401 Unauthorized` | Not authenticated |

```json
{ "message": "Logged out successfully" }
```

---

### 2. Categories

#### GET `/api/categories`

Returns all default categories plus the user's custom categories.

**Response:**
```json
[
  { "id": 1, "name": "Salary",      "type": "INCOME",  "isDefault": true  },
  { "id": 2, "name": "Food",        "type": "EXPENSE", "isDefault": true  },
  { "id": 9, "name": "Freelance",   "type": "INCOME",  "isDefault": false }
]
```

---

#### POST `/api/categories`

Create a custom category.

**Request body:**
```json
{
  "name": "Freelance",
  "type": "INCOME"
}
```

| Status | Description |
|--------|-------------|
| `201 Created` | Category created |
| `400 Bad Request` | Validation error |
| `409 Conflict` | Name already exists |

---

#### DELETE `/api/categories/{name}`

Delete a custom category by name.

| Status | Description |
|--------|-------------|
| `200 OK` | Deleted |
| `400 Bad Request` | Attempt to delete a default category |
| `404 Not Found` | Category not found |
| `409 Conflict` | Category has existing transactions |

---

### 3. Transactions

#### POST `/api/transactions`

Create a transaction.

**Request body:**
```json
{
  "amount": 1500.00,
  "date": "2024-03-15",
  "categoryId": 1,
  "description": "Monthly salary"
}
```

Rules:
- `amount` must be > 0
- `date` must be today or in the past (format: `YYYY-MM-DD`)
- `categoryId` must reference a valid category accessible to the user
- `description` is optional (max 500 chars)

| Status | Description |
|--------|-------------|
| `201 Created` | Transaction created |
| `400 Bad Request` | Validation / future date |
| `404 Not Found` | Category not found |

---

#### GET `/api/transactions`

List transactions, newest first. Supports optional filters.

**Query parameters:**

| Parameter | Type | Example | Description |
|-----------|------|---------|-------------|
| `startDate` | `YYYY-MM-DD` | `2024-01-01` | Filter from date (inclusive) |
| `endDate` | `YYYY-MM-DD` | `2024-01-31` | Filter to date (inclusive) |
| `categoryId` | `Long` | `2` | Filter by category |

**Response:**
```json
[
  {
    "id": 1,
    "amount": 1500.00,
    "date": "2024-03-15",
    "categoryId": 1,
    "categoryName": "Salary",
    "categoryType": "INCOME",
    "description": "Monthly salary"
  }
]
```

---

#### PUT `/api/transactions/{id}`

Update a transaction (date cannot be changed).

**Request body:**
```json
{
  "amount": 1600.00,
  "categoryId": 1,
  "description": "Updated salary"
}
```

| Status | Description |
|--------|-------------|
| `200 OK` | Updated |
| `404 Not Found` | Transaction not found |

---

#### DELETE `/api/transactions/{id}`

Soft-delete a transaction (excluded from reports and goals).

| Status | Description |
|--------|-------------|
| `200 OK` | Deleted |
| `404 Not Found` | Transaction not found |

---

### 4. Savings Goals

#### POST `/api/goals`

Create a savings goal.

**Request body:**
```json
{
  "goalName": "Emergency Fund",
  "targetAmount": 5000.00,
  "targetDate": "2025-12-31",
  "startDate": "2024-01-01"
}
```

- `targetDate` must be a future date
- `startDate` defaults to today if omitted
- Progress = (Total Income − Total Expenses) since `startDate`

---

#### GET `/api/goals`

List all goals with progress.

#### GET `/api/goals/{id}`

Get a single goal with progress.

**Response (both):**
```json
{
  "id": 1,
  "goalName": "Emergency Fund",
  "targetAmount": 5000.00,
  "targetDate": "2025-12-31",
  "startDate": "2024-01-01",
  "currentProgress": 2500.00,
  "progressPercentage": 50.00,
  "remainingAmount": 2500.00
}
```

---

#### PUT `/api/goals/{id}`

Update a goal (name, target amount, target date).

#### DELETE `/api/goals/{id}`

Delete a goal permanently.

---

### 5. Reports

#### GET `/api/reports/monthly/{year}/{month}`

Monthly income/expense summary.

**Example:** `GET /api/reports/monthly/2024/3`

**Response:**
```json
{
  "year": 2024,
  "month": 3,
  "totalIncome": 3500.00,
  "totalExpenses": 1000.00,
  "netSavings": 2500.00,
  "incomeByCategory": [
    { "categoryId": 1, "categoryName": "Salary",    "total": 3000.00 },
    { "categoryId": 9, "categoryName": "Freelance",  "total": 500.00  }
  ],
  "expensesByCategory": [
    { "categoryId": 2, "categoryName": "Food",  "total": 600.00 },
    { "categoryId": 5, "categoryName": "Rent",  "total": 400.00 }
  ]
}
```

---

#### GET `/api/reports/yearly/{year}`

Yearly summary with monthly breakdown.

**Example:** `GET /api/reports/yearly/2024`

**Response:**
```json
{
  "year": 2024,
  "totalIncome": 42000.00,
  "totalExpenses": 12000.00,
  "netSavings": 30000.00,
  "monthlyBreakdown": [
    { "month": 1,  "totalIncome": 3500.00, "totalExpenses": 1000.00, "netSavings": 2500.00 },
    { "month": 2,  "totalIncome": 3500.00, "totalExpenses": 1000.00, "netSavings": 2500.00 },
    ...
  ],
  "incomeByCategory":   [ ... ],
  "expensesByCategory": [ ... ]
}
```

---

## Error Responses

All errors follow a consistent JSON structure:

```json
{
  "timestamp": "2024-03-15T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Category 'Unknown' not found",
  "path": "/api/categories/Unknown"
}
```

Validation errors return field-level details:

```json
{
  "timestamp": "2024-03-15T10:30:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "One or more fields are invalid",
  "fieldErrors": {
    "username": "Must be a valid email address",
    "password": "Password must be at least 8 characters"
  }
}
```

| HTTP Status | When |
|-------------|------|
| `400` | Validation error, business rule violation |
| `401` | Missing or invalid session |
| `403` | Authenticated but forbidden |
| `404` | Resource not found |
| `409` | Duplicate resource or constraint violation |
| `500` | Unexpected server error |

---

## Default Categories

Seeded automatically on startup. Cannot be modified or deleted.

| Name | Type |
|------|------|
| Salary | INCOME |
| Food | EXPENSE |
| Rent | EXPENSE |
| Transportation | EXPENSE |
| Entertainment | EXPENSE |
| Healthcare | EXPENSE |
| Utilities | EXPENSE |

---

## Deploying to Render

### Option A — Docker (recommended)

1. Push your code to GitHub.
2. In the [Render Dashboard](https://dashboard.render.com), create a **New Web Service**.
3. Connect your GitHub repository.
4. Render auto-detects the `render.yaml` and `Dockerfile`.
5. Set environment variable `SPRING_PROFILES_ACTIVE=production` if not already in `render.yaml`.
6. Click **Deploy**.

The production profile uses an H2 **file-based** database persisted to `/data` via a Render disk (configured in `render.yaml`).

### Option B — Native (no Docker)

1. In Render, select **Environment: Java**.
2. Set **Build Command:**
   ```
   ./gradlew bootJar -x test
   ```
3. Set **Start Command:**
   ```
   java -jar build/libs/finance-manager-0.0.1-SNAPSHOT.jar
   ```
4. Add environment variable `SPRING_PROFILES_ACTIVE=production`.

### Health Check

Render uses `GET /actuator/health` to monitor the service. It returns `{ "status": "UP" }` when healthy.

---

## Notes

- Passwords are stored as **BCrypt** hashes — never in plain text.
- Sessions are stored server-side; the client holds only a `FINANCE_SESSION` cookie (`HttpOnly`, `Secure` in production, `SameSite=Strict`).
- Deleted transactions are soft-deleted and excluded from all reports and goal calculations.
- All monetary values use `BigDecimal` with 2 decimal places for precision.
