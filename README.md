# Transaction Service - Daily Banking Demo

Spring Boot 3.2 microservice implementing core transaction capabilities from the architecture summary.

## Implemented Scope

- Deposit, withdrawal, and transfer operations
- Transaction history and lookup APIs
- Idempotency key support
- Centralized exception handling
- Kafka transaction event publishing
- Actuator health and metrics endpoints
- Unit and integration tests

## Tech Stack

- Java 21
- Spring Boot 3.2
- Spring Data JPA
- Spring Kafka
- Resilience4j
- H2 (default), PostgreSQL (dev/prod)
- Maven

## Run Locally

```bash
mvn spring-boot:run
```

Swagger UI:

- `http://localhost:8080/swagger-ui/index.html`

## API Endpoints

- `POST /api/v1/transactions/deposit`
- `POST /api/v1/transactions/withdraw`
- `POST /api/v1/transactions/transfer`
- `GET /api/v1/transactions/{transactionId}`
- `GET /api/v1/transactions?accountId=<id>`

## Quick Example

```bash
curl -X POST http://localhost:8080/api/v1/transactions/deposit \
  -H "Content-Type: application/json" \
  -d '{
    "accountId":"acc-100",
    "amount":120.50,
    "currency":"EUR",
    "description":"salary",
    "idempotencyKey":"dep-100-1"
  }'
```

## Docker Compose

```bash
docker-compose up -d --build
```

## Test

```bash
mvn test
```
