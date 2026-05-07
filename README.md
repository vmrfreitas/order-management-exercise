# Order Management Service

A production-grade backend service for an e-commerce platform that handles order creation, warehouse selection, inventory management, and payment processing.



## Getting Started

### Prerequisites

- **Docker** and **Docker Compose**
- **Java 17+** (only needed for running tests locally)

### Run with Docker Compose (recommended)

```bash
# Start everything: PostgreSQL, Kafka, Zookeeper, and the app
docker compose up --build -d

# Wait for the app to be ready (~30s)
curl -s http://localhost:8080/orders/{any-uuid} 2>/dev/null
# A 404 JSON response means it's up
```

### Test It

**1. Create an order:**

```bash
curl -s -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customer": "Jane Doe",
    "shippingAddress": "742 Evergreen Terrace, Springfield, IL 62704",
    "creditCardNumber": "4111111111111111",
    "items": [
      { "productId": "550e8400-e29b-41d4-a716-446655440001", "quantity": 1 },
      { "productId": "550e8400-e29b-41d4-a716-446655440002", "quantity": 2 }
    ]
  }'
```

Response (202 Accepted):
```json
{
  "orderId": "<uuid>",
  "status": "PENDING",
  "message": "Order created. Check /orders/<uuid> for updates."
}
```

**2. Check order status** (after ~2 seconds for async fulfillment):

```bash
curl -s http://localhost:8080/orders/<uuid> | json_pp
```

The status should be `FULFILLED` once the warehouse selection, inventory deduction, and payment all succeed.

**3. Run the automated e2e script:**

```bash
./verify_e2e.sh
```

This script starts the containers, creates an order, waits for async processing, and verifies the final status.

### Available Seed Data

| Product | UUID | Price |
|---|---|---|
| Laptop | `550e8400-...-440001` | $999.99 |
| Mouse | `550e8400-...-440002` | $29.99 |
| Keyboard | `550e8400-...-440003` | $79.99 |
| Monitor | `550e8400-...-440004` | $399.99 |
| USB Cable | `550e8400-...-440005` | $9.99 |

Three warehouses are pre-seeded (New York, Los Angeles, Chicago) with varying inventory.

### Run Tests

```bash
./gradlew test
```

Tests use an embedded Kafka broker and H2 in-memory database — no Docker needed.

### Useful Endpoints

| Endpoint | Description |
|---|---|
| `POST /orders` | Create a new order |
| `GET /orders/{id}` | Check order status |
| `http://localhost:8081` | Kafka UI (when running Docker Compose) |

### Shut Down

```bash
docker compose down -v   # -v removes the PostgreSQL data volume
```

## Architecture

```
┌──────────────┐    POST /orders     ┌──────────────────┐
│   Client     │ ──────────────────► │ OrdersController  │
└──────────────┘                     └────────┬─────────┘
                                              │
                              ┌───────────────┼───────────────┐
                              ▼               ▼               ▼
                        OrderAdapter   OrderRepository   OrderEventPublisher
                        (DTO→Entity)   (save PENDING)         │
                                                              │ Kafka
                                                              ▼
                                                     ┌─────────────────┐
                                                     │OrderEventConsumer│
                                                     └────────┬────────┘
                                                              │
                                                              ▼
                                                     ┌─────────────────┐
                                                     │  OrderService    │
                                                     │  (fulfillOrder)  │
                                                     └────────┬────────┘
                                                              │
                              ┌───────────────┬───────────────┼───────────────┐
                              ▼               ▼               ▼               ▼
                    Find warehouses   Select closest   Deduct inventory   Process payment
                    with all items    (LocationClient)  (atomic UPDATE)   (PaymentClient)
                              │               │               │               │
                              └───────────────┴───────────────┴───────────────┘
                                                              │
                                                    Order → FULFILLED / FAILED
```

### Key Design Decisions

| Decision | Rationale |
|---|---|
| **Async fulfillment via Kafka** | `POST /orders` returns immediately (202 Accepted). Fulfillment runs asynchronously via a Kafka consumer, keeping the API responsive. |
| **Single-warehouse constraint** | A JPQL relational division query finds all warehouses that stock every item in the order with sufficient quantity. |
| **Atomic inventory deduction** | `UPDATE ... WHERE quantity >= :requested` prevents overselling without pessimistic locks. The entire fulfillment runs in a single `@Transactional` — if any item or payment fails, all deductions roll back. |
| **BigDecimal for money** | Avoids IEEE 754 floating-point rounding errors in financial calculations. |
| **PostgreSQL** | Production-grade relational database, containerized via Docker Compose. Tests use H2 in-memory for speed. |

### Tech Stack

- **Java 17** / **Spring Boot 4.x**
- **Spring Data JPA** + **PostgreSQL 16**
- **Apache Kafka** for async event processing
- **Docker Compose** for local infrastructure
- **Lombok** for boilerplate reduction
- **Google Java Format** via Spotless

### Project Structure

```
src/main/java/com/canals/homework/
├── controller/          # REST endpoints, DTOs, external API clients
│   ├── dto/             # CreateOrderRequest, OrderItemRequest, CreateOrderResponse
│   ├── OrdersController # POST /orders, GET /orders/{id}
│   ├── LocationClient   # Mock geocoding/distance API
│   ├── PaymentClient    # Mock external payment API
│   └── GlobalExceptionHandler
├── adapter/             # OrderAdapter — DTO-to-entity mapping
├── service/             # OrderService — fulfillment orchestration
├── model/               # JPA entities: Order, OrderItem, Product, Warehouse, InventoryItem
├── repository/          # Spring Data repositories with custom JPQL queries
├── event/               # Kafka publisher, consumer, and event DTOs
└── config/              # KafkaConfig
```
