# FeatureFlagLite ⚑

A **minimal, clean, production-style Feature Flag and Configuration Service** inspired by the core concept of [Flagsmith](https://flagsmith.com/), intentionally simplified for academic and project use.

---

## Project Overview

FeatureFlagLite allows developers to:

- **Create and manage** feature flags with names, descriptions, and default states
- **Configure flags independently** for `dev`, `test`, and `prod` environments
- **Gradual rollout** with percentage-based feature enablement (0–100%)
- **Deterministic evaluation** — same user always gets the same flag result
- **Audit trail** — track who changed what, when, and the before/after values
- **Read API** — lightweight endpoint for client applications to consume flag states
- **Visual dashboard** — demo website to interact with flags in real-time

---

## Features

| Feature                       | Description                                          |
|-------------------------------|------------------------------------------------------|
| Feature Flag CRUD             | Create, read, update, delete flags                   |
| Environment Configuration     | Independent flag states for dev / test / prod        |
| Percentage Rollout            | Gradual 0–100% rollout with deterministic hashing    |
| Flag Evaluation API           | Per-user evaluation with consistent results          |
| Change History                | Full audit trail with old/new state tracking         |
| Input Validation              | Bean Validation with structured error responses      |
| Centralized Error Handling    | `@RestControllerAdvice` with custom exceptions       |
| Caching                       | Caffeine cache for frequently read flag states       |
| Structured Logging            | SLF4J/Logback with contextual log messages           |
| Dashboard UI                  | Glassmorphism-styled test website                    |

---

## Architecture

```
Controller  →  Service  →  Repository  →  Database
     ↑             ↑
   DTOs      Business Logic
   Validation   Caching
```

### Package Structure

```
com.FeatureFlagLite.FeartureFlagSmasher
├── controller/     — REST API endpoints
├── service/        — Business logic & caching
├── repository/     — Spring Data JPA repositories
├── entity/         — JPA entities
├── dto/            — Request/Response DTOs
├── exception/      — Custom exceptions & global handler
├── config/         — Cache, CORS, data initialization
├── mapper/         — Entity ↔ DTO conversion
└── util/           — Rollout evaluator utility
```

---

## Tech Stack

| Layer      | Technology                    |
|------------|-------------------------------|
| Language   | Java 25                       |
| Framework  | Spring Boot 4.1.1             |
| Web        | Spring Web (REST)             |
| Data       | Spring Data JPA + MySQL       |
| Validation | Jakarta Bean Validation       |
| Caching    | Spring Cache + Caffeine       |
| Logging    | SLF4J + Logback               |
| Testing    | JUnit 5 + MockMvc + H2       |
| Frontend   | HTML + CSS + JavaScript       |

---

## Database Schema

### Entity Relationship

```
FeatureFlag (1) ──── (N) FlagState (N) ──── (1) Environment
     │                       │
     │                       │
     └── (1) ─── (N) ChangeLog (N) ─── (1) ┘
```

### Tables

| Table           | Key Columns                                             | Constraints                        |
|-----------------|----------------------------------------------------------|------------------------------------|
| `feature_flags` | id, name, description, default_state, created/updated_at | name UNIQUE                        |
| `environments`  | id, name                                                 | name UNIQUE                        |
| `flag_states`   | id, feature_flag_id, environment_id, enabled, rollout_%  | (flag_id, env_id) UNIQUE, 0 ≤ % ≤ 100 |
| `change_logs`   | id, flag_id, env_id, old/new state, changed_by/at       | FK to flags & environments         |

---

## API Documentation

### Feature Flag Management

| Method   | Endpoint                           | Description               | Status  |
|----------|------------------------------------|---------------------------|---------|
| `POST`   | `/api/v1/flags`                    | Create a feature flag     | `201`   |
| `GET`    | `/api/v1/flags`                    | Get all flags             | `200`   |
| `GET`    | `/api/v1/flags/{id}`               | Get flag by ID            | `200`   |
| `GET`    | `/api/v1/flags/{flagName}`         | Get flag by name          | `200`   |
| `PUT`    | `/api/v1/flags/{id}`               | Update flag metadata      | `200`   |
| `DELETE` | `/api/v1/flags/{id}`               | Delete a flag             | `204`   |

### Flag State & Evaluation

| Method   | Endpoint                              | Description                     | Status  |
|----------|---------------------------------------|---------------------------------|---------|
| `GET`    | `/api/v1/flags?environment=dev`       | Get all flags for environment   | `200`   |
| `GET`    | `/api/v1/flags/{name}/states`         | Get states across environments  | `200`   |
| `PUT`    | `/api/v1/flags/{name}/states`         | Update state for environment    | `200`   |
| `GET`    | `/api/v1/flags/{name}/evaluate?environment=dev&userId=123` | Evaluate flag | `200`   |
| `GET`    | `/api/v1/flags/{name}/history`        | Get change history              | `200`   |
| `GET`    | `/api/v1/environments`               | List all environments           | `200`   |

---

## Example Requests

### Create a Feature Flag

```bash
curl -X POST http://localhost:8080/api/v1/flags \
  -H "Content-Type: application/json" \
  -d '{
    "name": "newDashboard",
    "description": "Enable the redesigned dashboard",
    "defaultState": false
  }'
```

**Response (201):**
```json
{
  "id": 1,
  "name": "newDashboard",
  "description": "Enable the redesigned dashboard",
  "defaultState": false,
  "createdAt": "2026-09-28T10:30:15",
  "updatedAt": "2026-09-28T10:30:15"
}
```

### Update Flag State for an Environment

```bash
curl -X PUT http://localhost:8080/api/v1/flags/newDashboard/states \
  -H "Content-Type: application/json" \
  -d '{
    "environment": "dev",
    "enabled": true,
    "rolloutPercentage": 50,
    "changedBy": "admin"
  }'
```

### Get All Flags for an Environment

```bash
curl http://localhost:8080/api/v1/flags?environment=dev
```

**Response:**
```json
{
  "environment": "dev",
  "flags": {
    "newDashboard": true,
    "darkMode": false
  }
}
```

### Evaluate a Flag for a User

```bash
curl "http://localhost:8080/api/v1/flags/newDashboard/evaluate?environment=dev&userId=user-123"
```

**Response:**
```json
{
  "flagName": "newDashboard",
  "environment": "dev",
  "enabled": true,
  "rolloutPercentage": 50
}
```

### Get Change History

```bash
curl http://localhost:8080/api/v1/flags/newDashboard/history
```

---

## Project Structure

```
featureflaglite/
├── src/
│   ├── main/
│   │   ├── java/com/FeatureFlagLite/FeartureFlagSmasher/
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   ├── entity/
│   │   │   ├── dto/
│   │   │   ├── exception/
│   │   │   ├── config/
│   │   │   ├── mapper/
│   │   │   └── util/
│   │   └── resources/
│   │       ├── application.yml
│   │       └── static/           (dashboard files)
│   └── test/
│       ├── java/                 (unit + integration tests)
│       └── resources/
│           └── application.yml   (H2 test config)
├── frontend/
│   ├── index.html
│   ├── style.css
│   └── app.js
├── database/
│   └── schema.sql
├── pom.xml
├── README.md
└── .gitignore
```

---

## How to Run

### Prerequisites

- **Java 25** (or compatible JDK)
- **MySQL 8.x** (or compatible)
- **Maven** (wrapper included)

### 1. Clone the Repository

```bash
git clone https://github.com/SanjaiPS-tech/FeatureFlagSmasher.git
cd FeatureFlagSmasher
```

### 2. Set Up MySQL

```bash
mysql -u root -p
```

```sql
CREATE DATABASE IF NOT EXISTS featureflaglite;
```

Or run the full schema:

```bash
mysql -u root -p < database/schema.sql
```

### 3. Configure Environment Variables (Optional)

```bash
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=featureflaglite
export DB_USERNAME=root
export DB_PASSWORD=yourpassword
export SERVER_PORT=8080
```

Defaults work with a local MySQL using `root/root`.

### 4. Run the Application

```bash
./mvnw spring-boot:run
```

### 5. Open the Dashboard

Navigate to: **[http://localhost:8080/index.html](http://localhost:8080/index.html)**

---

## Environment Variables

| Variable       | Default        | Description             |
|----------------|----------------|-------------------------|
| `DB_HOST`      | `localhost`    | MySQL host              |
| `DB_PORT`      | `3306`         | MySQL port              |
| `DB_NAME`      | `featureflaglite` | Database name        |
| `DB_USERNAME`  | `root`         | Database username       |
| `DB_PASSWORD`  | `root`         | Database password       |
| `SERVER_PORT`  | `8080`         | Application port        |

---

## Test Instructions

### Run All Tests

```bash
./mvnw test
```

Tests use an **H2 in-memory database** — no MySQL required.

### Test Coverage

| Category               | Tests                                      |
|------------------------|--------------------------------------------|
| Flag CRUD              | Create, get, update, delete                |
| Validation             | Blank name, duplicate name, invalid rollout|
| Environment States     | Per-env config, isolation, invalid env     |
| Percentage Rollout     | 0%, 100%, 50% distribution, determinism   |
| Change History         | Log creation, ordering, not-found          |
| REST API               | All endpoints, status codes, error bodies  |

---

## Caching Strategy

- **Caffeine** in-memory cache (single-instance)
- Cached:
  - Environment flag states (`flags:dev`, `flags:test`, `flags:prod`)
  - Individual flag evaluations (`flagName:env:userId`)
- **Cache invalidation**: All caches evicted on any flag create/update/delete
- **TTL**: 10 minutes maximum
- **Max entries**: 500

---

## Percentage Rollout Logic

```
Input:  flagName + userId
Hash:   String.hashCode() of "flagName:userId"
Bucket: abs(hash) % 100  →  value 0-99
Result: bucket < rolloutPercentage  →  enabled
```

- **Deterministic**: Same user always gets the same result
- **O(1) evaluation**: No database lookup if cached
- **Uniform distribution**: Hash-based bucketing distributes evenly

---

## Error Handling

All errors return structured JSON:

```json
{
  "timestamp": "2026-09-28T10:30:15",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "rolloutPercentage must be between 0 and 100"
}
```

| Exception                      | HTTP Status | Error Code         |
|--------------------------------|-------------|---------------------|
| `FeatureFlagNotFoundException` | 404         | `NOT_FOUND`         |
| `EnvironmentNotFoundException` | 404         | `NOT_FOUND`         |
| `DuplicateFeatureFlagException`| 409         | `CONFLICT`          |
| `InvalidFlagStateException`    | 400         | `VALIDATION_ERROR`  |
| Validation errors              | 400         | `VALIDATION_ERROR`  |
| Unexpected errors              | 500         | `INTERNAL_SERVER_ERROR` |

---

## Future Improvements

- [ ] User authentication (JWT/OAuth2)
- [ ] Role-based access control
- [ ] Flag targeting rules (beyond percentage)
- [ ] SDK libraries for Java, JS, Python
- [ ] WebSocket for real-time flag updates
- [ ] Flag scheduling (auto-enable at a future date)
- [ ] A/B testing integration
- [ ] Flag dependencies / prerequisites
- [ ] Bulk flag operations
- [ ] API rate limiting
- [ ] Export/import flag configurations
- [ ] Flag archiving (soft delete)

---

## License

This project is for educational and demonstration purposes.
