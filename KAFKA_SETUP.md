# Order Fulfillment System with Kafka

This project demonstrates a production-grade order fulfillment system using Spring Boot with Apache Kafka for asynchronous processing.

## Architecture Overview

```
┌─────────────────────────────────────────────────┐
│                   REST API Controller           │
│         (POST /orders, GET /orders/{id})        │
└────────────────┬────────────────────────────────┘
                 │
                 ├─→ OrderAdapter (DTO → Entity)
                 │
                 └─→ OrderRepository (Save)
                    │
                    └─→ OrderEventPublisher (Publish to Kafka)
                       │
                       └─→ Kafka Topic: order-created
                          │
                          └─→ OrderEventConsumer
                             │
                             └─→ OrderService.fulfillOrder()
                                │
                                ├─→ Find warehouse with all items
                                ├─→ Deduct inventory (atomic)
                                └─→ Update order status (FULFILLED/FAILED)
```

## Prerequisites

- Docker & Docker Compose
- Java 17+
- Gradle (included via gradlew)

## Quick Start

### 1. Start Local Kafka Cluster

```bash
docker-compose up -d
```

This starts:
- **Kafka broker** on `localhost:9092`
- **Zookeeper** on `localhost:2181`
- **Kafka UI** (monitoring) on `localhost:8080`

Verify Kafka is running:
```bash
docker-compose ps
```

### 2. Run the Application

```bash
./gradlew bootRun
```

The app starts on `http://localhost:8080` (or check console output for port)

### 3. Test the API

#### Create an Order
```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customer": "John Doe",
    "shippingAddress": "123 Main St",
    "items": [
      {"productId": "550e8400-e29b-41d4-a716-446655440000", "quantity": 5},
      {"productId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8", "quantity": 3}
    ]
  }'
```

Response:
```json
Order created with ID: <uuid>. Status: PENDING. Check /orders/<uuid> for updates.
```

#### Check Order Status
```bash
curl http://localhost:8080/orders/<uuid>
```

Response (while processing):
```json
{
  "id": "<uuid>",
  "customer": "John Doe",
  "shippingAddress": "123 Main St",
  "status": "PENDING",
  "items": [...]
}
```

Response (after fulfillment):
```json
{
  "id": "<uuid>",
  "customer": "John Doe",
  "shippingAddress": "123 Main St",
  "status": "FULFILLED",
  "items": [...]
}
```

## How It Works

### 1. Order Creation (Synchronous)
- Client sends `POST /orders` request
- Controller creates Order entity and saves to database
- **Returns 202 ACCEPTED** immediately (non-blocking)
- Order status set to **PENDING**

### 2. Event Publishing
- After saving order, controller publishes `OrderCreatedEvent` to Kafka
- Event contains: `orderId`, `customer`, `shippingAddress`

### 3. Async Order Fulfillment (Kafka Consumer)
- `OrderEventConsumer` listens to `order-created` topic
- Receives event and calls `OrderService.fulfillOrder()`
- Service performs:
  1. **Find Warehouse** - Queries for warehouse with all items in stock
  2. **Deduct Inventory** - Atomically updates inventory (SQL constraint prevents overselling)
  3. **Update Status** - Sets order to `FULFILLED` or `FAILED`
- If any step fails, order status = `FAILED`

### 4. Status Polling
- Client polls `GET /orders/{id}` to check status
- When status changes from `PENDING` → `FULFILLED/FAILED`, order is complete

## Kafka Topics

### order-created
- **Partitions**: 1
- **Replication**: 1
- **Message format**: JSON (OrderCreatedEvent)
- **Consumer group**: `order-fulfillment-group`

## Key Components

### Models
- **Order** - Main order entity with status tracking
- **OrderItem** - Line items in an order (links to Product)
- **Product** - Product catalog
- **InventoryItem** - Tracks product quantities in warehouses
- **Warehouse** - Physical warehouse locations

### Services
- **OrderService** - Core fulfillment logic
- **OrderEventPublisher** - Publishes events to Kafka
- **OrderEventConsumer** - Listens for events and triggers fulfillment

### Repositories
- **OrderRepository** - CRUD operations for orders
- **WarehouseRepository** - Find warehouses with all items (complex query)
- **InventoryItemRepository** - Manage inventory deductions
- **ProductRepository** - Product lookup

## Configuration

All Kafka settings in `application.properties`:
```properties
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.group-id=order-fulfillment-group
```

## Monitoring

### Kafka UI
Open `http://localhost:8080` (Kafka UI, not the app)
- See topics, partitions, consumer lag
- View message contents in real-time

### Application Logs
Check logs for:
- Order creation events
- Fulfillment processing
- Inventory deductions
- Failures and retries

Example log output:
```
INFO | Order creation event published for orderId: abc123
INFO | Received order creation event for orderId: abc123
INFO | Order abc123 fulfilled from warehouse xyz
```

## Database

Uses H2 in-memory database (for development):
- Auto-creates tables on startup
- Access via `http://localhost:8080/h2-console`

## Production Considerations

This is **not production-ready** as-is. To move to production:

✅ Already implemented:
- Async event-driven architecture
- Atomic inventory deductions
- Proper error handling
- Kafka integration
- Status tracking

❌ Still needed:
- **Message durability**: Switch to persistent Kafka cluster (not Docker)
- **Retry logic**: Add DLQ (Dead Letter Queue) for failed messages
- **Monitoring**: Add Prometheus metrics
- **Security**: Add authentication/authorization
- **Testing**: Add unit/integration tests with embedded Kafka
- **Database**: Switch from H2 to PostgreSQL/MySQL
- **Transactions**: Add proper transaction management across services
- **Scaling**: Configure consumer groups for parallel processing

## Stopping Services

```bash
# Stop all containers
docker-compose down

# Stop and remove volumes (WARNING: deletes all data)
docker-compose down -v
```

## Troubleshooting

### Kafka Connection Refused
```bash
# Check if containers are running
docker-compose ps

# View logs
docker-compose logs kafka

# Restart
docker-compose restart kafka
```

### Order Not Processing
1. Check application logs for errors
2. Check Kafka UI to see if message is in `order-created` topic
3. Verify warehouse and inventory data exist in database

### Build Issues
```bash
# Clean and rebuild
./gradlew clean build -x test

# Enable daemon for faster builds
./gradlew --daemonize
```

