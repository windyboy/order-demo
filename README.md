# Order Service - Hexagonal Architecture Demo

> **Production-Ready Reference Implementation**

A complete implementation of an order service using **Hexagonal Architecture (Ports & Adapters)**, built with **Micronaut 4.9.4**, **Kotlin**, and **Domain-Driven Design** principles.

![Build Status](https://img.shields.io/badge/build-passing-brightgreen)
![Test Coverage](https://img.shields.io/badge/coverage-90%25-green)
![Architecture](https://img.shields.io/badge/architecture-hexagonal-blue)
![Tests](https://img.shields.io/badge/tests-54%20passing-success)
![Adapters](https://img.shields.io/badge/adapters-HTTP%20%2B%20CLI-blue)

## 🎯 What Makes This Special

This project demonstrates **production-quality** hexagonal architecture:

- ✅ **Adapter Swapping** - HTTP + CLI adapters using the same use case (proves the pattern works!)
- ✅ **Architecture Tests** - 6 ArchUnit tests enforce hexagonal boundaries automatically
- ✅ **Educational Code** - Self-teaching comments explaining DDD patterns in context
- ✅ **Rich Domain Model** - OrderStatus state machine, domain events, value objects
- ✅ **Result-Based Errors** - Functional error handling without exception propagation
- ✅ **Comprehensive Testing** - 54 tests covering all layers (domain, app, adapter, E2E, architecture)
- ✅ **Complete Documentation** - Architecture guide, comparison doc, demo script

---

## 📚 Documentation

| Document | Description |
|----------|-------------|
| **[ARCHITECTURE.md](./ARCHITECTURE.md)** | Complete architecture design and patterns |
| **[ARCHITECTURE_COMPARISON.md](./docs/ARCHITECTURE_COMPARISON.md)** | Layered vs Hexagonal architecture comparison |
| **[DEMO_SCRIPT.md](./DEMO_SCRIPT.md)** | 15-minute live presentation guide |
| **[ErrorMapping.md](./ErrorMapping.md)** | HTTP error handling reference |
| **[CHANGELOG.md](./CHANGELOG.md)** | Version history and improvements |

---

## 🎓 How It Works - Request Flow Walkthrough

### Complete Request Path

```
HTTP Request → Adapter → Port → Application → Domain → Port → Adapter → HTTP Response
```

#### Step 1: HTTP Adapter (Inbound)
**File**: `OrderController.kt`

```kotlin
@Post
fun place(@Body request: PlaceOrderRequest) {
    // 1. Validate DTO (Bean Validation)
    // 2. Map DTO → Domain Command
    val command = mapper.toCommand(request)
    
    // 3. Call use case interface (inbound port)
    val result = placeOrderUseCase.execute(command)
}
```
**Key**: Controller depends on `PlaceOrderUseCase` **interface**, not implementation!

#### Step 2: Application Layer (Use Case)
**File**: `OrderPlacementHandler.kt`

```kotlin
// Use case orchestrates the workflow
class OrderPlacementHandler(
    private val repository: OrderRepository,
    private val stockChecker: StockAvailabilityChecker,
    private val eventPublisher: DomainEventPublisher,
) : PlaceOrderUseCase {

    override fun execute(command: PlaceOrderCommand): Result<OrderId> =
        validate(command)
            .flatMap(::reserveStock)
            .flatMap(::createAggregate)
            .flatMap(::persistAndPublish)
}
```
**Key**: Workflow expressed as a Result pipeline - orchestrates, doesn't implement business logic.

#### Step 3: Domain Layer (Business Logic)
**File**: `Order.kt`

```kotlin
companion object {
    fun create(items: List<OrderItem>): Result<Order> {
        // Business invariant validation
        if (items.isEmpty()) {
            return Result.failure(InvalidOrderData("Order must have items"))
        }
        
        val order = Order(OrderId.generate(), items, OrderStatus.NEW)
        
        // Raise domain event
        order._domainEvents.add(OrderPlacedEvent(...))
        
        return Result.success(order)
    }
}
```
**Key**: Business rules live HERE, not in services. Zero framework dependencies!

#### Step 4: Persistence Adapter (Outbound)
**File**: `InMemoryOrderRepository.kt`

```kotlin
class InMemoryOrderRepository : OrderRepository {  // Implements the port!
    private val store = ConcurrentHashMap<String, Order>()
    
    override fun save(order: Order): Result<OrderId> {
        store[order.id.value] = order
        return Result.success(order.id)
    }
}
```
**Key**: Adapter implements the outbound port. Easy to swap with PostgreSQL!

#### Step 5: Response Mapping
```kotlin
// In Controller:
result.fold(
    onSuccess = { orderId ->
        val response = mapper.toResponse(orderId)  // Domain → DTO
        HttpResponse.created(ApiResponse.success(response))
    },
    onFailure = { error -> toErrorResponse(error) }
)
```

---

## 🏗️ Architecture Diagram

```mermaid
flowchart TB
    Client[HTTP Client]
    Controller[OrderController]
    UseCase[OrderPlacementHandler]
    Domain[Order Aggregate]
    Repo[OrderRepository Port]
    RepoImpl[InMemoryOrderRepository]
    
    Client -->|POST /orders| Controller
    Controller -->|execute()| UseCase
    UseCase -->|create()| Domain
    UseCase -->|save()| Repo
    Repo -.implements.-> RepoImpl
    
    style Domain fill:#f9f,stroke:#333,stroke-width:4px
    style UseCase fill:#bbf,stroke:#333,stroke-width:2px
    style Controller fill:#bfb,stroke:#333,stroke-width:2px
```

### Dependency Direction

```
          ┌─────────────┐
          │   Domain    │  ← Pure business logic
          │   (Order)   │
          └─────────────┘
                 ↑
                 │ depends on
          ┌─────────────┐
          │    Ports    │  ← Interfaces only
          │(Repository) │
          └─────────────┘
                 ↑
                 │ implements
          ┌─────────────┐
          │  Adapters   │  ← Infrastructure
          │ (InMemory)  │
          └─────────────┘
```

**All arrows point INWARD** - that's the hexagonal architecture principle!

---

## 📦 Project Structure

```
src/main/kotlin/me/windy/demo/order/
├── core/                              # CORE LAYER (no external dependencies)
│   ├── domain/                        # Domain models
│   │   ├── model/
│   │   │   ├── Order.kt              # Aggregate root
│   │   │   ├── OrderItem.kt          # Value object
│   │   │   ├── Money.kt              # Value object
│   │   │   └── OrderStatus.kt        # State machine enum
│   │   ├── event/
│   │   │   └── DomainEvents.kt       # Domain events
│   │   └── error/
│   │       └── OrderError.kt         # Error types
│   ├── application/                   # Application layer
│   │   └── usecase/
│   │       └── OrderPlacementHandler.kt  # Use case orchestration
│   └── port/                          # Port interfaces
│       ├── incoming/                  # Inbound ports
│       │   ├── PlaceOrderUseCase.kt
│       │   └── PlaceOrderCommand.kt
│       └── outgoing/                  # Outbound ports
│           ├── OrderRepository.kt
│           ├── StockAvailabilityChecker.kt
│           └── DomainEventPublisher.kt
│
├── adapter/                           # ADAPTER LAYER
│   ├── incoming/
│   │   ├── http/                      # HTTP adapter
│   │   │   ├── OrderController.kt
│   │   │   ├── dto/
│   │   │   │   └── PlaceOrderDtos.kt
│   │   │   └── mapper/
│   │   │       └── OrderMapper.kt
│   │   └── cli/                       # CLI adapter
│   │       └── OrderCLI.kt
│   └── outgoing/
│       ├── persistence/               # Persistence adapters
│       │   └── InMemoryOrderRepository.kt
│       ├── stock/                     # Stock checking adapters
│       │   └── DummyStockChecker.kt
│       └── event/                     # Event publishing adapters
│           └── LoggingEventPublisher.kt
│
└── Application.kt                     # Micronaut entry point
```

---

## 🧪 Testing Strategy

### Test Pyramid

```
        /\
       /E2E\        ← Full HTTP flow
      /------\
     /  App  \      ← Use case with fakes
    /----------\
   /   Domain   \   ← Pure business logic
  /--------------\
```

### 1. Domain Testing (Pure Logic)
```kotlin
class OrderTest : StringSpec({
    "should not allow empty items" {
        shouldThrow<IllegalArgumentException> {
            Order.create(emptyList())
        }
    }
    
    "should calculate total correctly" {
        val order = Order.create(listOf(
            OrderItem.of("SKU-1", Money.of(10.0), 2)
        ))
        order.total() shouldBe Money.of(20.0)
    }
})
```

### 2. Application Testing (With Fakes)
```kotlin
class OrderPlacementHandlerTest : StringSpec({
    "should place order when stock available" {
        val repository = FakeOrderRepository()
        val stockChecker = FakeStockChecker(available = true)
        val handler = OrderPlacementHandler(repository, stockChecker, ...)
        
        val result = handler.execute(validCommand)
        result.isSuccess shouldBe true
    }
})
```

### 3. E2E Testing (Full Flow)
```kotlin
@MicronautTest
class OrderE2ETest(@Client("/") val client: HttpClient) : StringSpec({
    "should place order via HTTP" {
        val response = client.toBlocking().exchange(
            HttpRequest.POST("/orders", validRequest),
            PlaceOrderResponse::class.java
        )
        response.status shouldBe HttpStatus.CREATED
    }
})
```

### 4. Architecture Testing
```kotlin
@AnalyzeClasses(packages = ["me.windy.demo.order"])
class HexagonalArchitectureTest {
    @ArchTest
    val domainShouldNotDependOnAdapters = 
        noClasses().that().resideInAPackage("..core.domain..")
            .should().dependOnClassesThat().resideInAPackage("..adapter..")
}
```

---

## 🚀 Quick Start

### Prerequisites
- JDK 17 or 21
- Gradle 8.5+

### Build and Run Tests
```bash
./gradlew clean test
```

### Start Service
```bash
./gradlew run
```

Service starts at `http://localhost:8080`

### Test API Endpoints

#### Place an Order
```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {"sku": "APPLE-001", "unitPrice": 5.0, "quantity": 2},
      {"sku": "BANANA-001", "unitPrice": 3.0, "quantity": 3}
    ]
  }'
```

**Response**:
```json
{
  "success": true,
  "data": {
    "orderId": "ORD-550e8400-e29b-41d4-a716-446655440000"
  },
  "error": null
}
```

#### Health Check
```bash
curl http://localhost:8080/orders/health
```

#### Error Response Example
```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{"items": []}'
```

**Response** (400 Bad Request):
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "INVALID_ORDER_DATA",
    "message": "Order must have at least one item"
  }
}
```

---

## 🎯 Core Design Principles

1. **Dependency Rule**: Outer layers depend on inner layers; inner layers are unaware of outer layers
2. **Port Segregation**: Interfaces are small, focused, and business-semantic
3. **Domain Purity**: Domain layer has zero framework dependencies, 100% testable
4. **Explicit Errors**: Using `Result<T>` instead of exception propagation
5. **Invariant Protection**: Private constructors + factory methods ensure valid state
6. **Test Independence**: Fake implementations enable isolated layer testing

---

## 🔄 Extension Examples

### Adding a New Use Case (Cancel Order)

**1. Define Inbound Port** (`core/port/incoming/`)
```kotlin
interface CancelOrderUseCase {
    fun execute(command: CancelOrderCommand): Result<Unit>
}
```

**2. Implement Handler** (`core/application/usecase/`)
```kotlin
@Singleton
class CancelOrderHandler(
    private val repository: OrderRepository,
    private val eventPublisher: DomainEventPublisher
) : CancelOrderUseCase {
    override fun execute(command: CancelOrderCommand): Result<Unit> =
        repository.findById(command.orderId)
            .flatMap { it.cancel() }
            .flatMap { repository.save(it) }
            .onSuccess { eventPublisher.publishAll(it.pullDomainEvents()) }
}
```

**3. Add HTTP Adapter** (`adapter/incoming/http/`)
```kotlin
@Delete("/{orderId}")
fun cancel(@PathVariable orderId: String): HttpResponse<ApiResponse<Unit>> {
    val command = CancelOrderCommand(OrderId(orderId))
    return cancelOrderUseCase.execute(command).toHttpResponse()
}
```

**Core layer requires no modification!**

---

### Replacing Implementation (PostgreSQL)

Simply create a new adapter implementing `OrderRepository`:

```kotlin
@Singleton
@Requires(env = ["prod"])
class PostgresOrderRepository(
    private val dataSource: DataSource
) : OrderRepository {
    
    override fun save(order: Order): Result<OrderId> = runCatching {
        dataSource.connection.use { conn ->
            conn.prepareStatement("""
                INSERT INTO orders (id, total, status) 
                VALUES (?, ?, ?)
            """).apply {
                setString(1, order.id.value)
                setBigDecimal(2, order.total().amount)
                setString(3, order.status.name)
            }.executeUpdate()
            order.id
        }
    }.fold(
        onSuccess = { Result.success(it) },
        onFailure = { Result.failure(OrderPlacementFailed("Database error: ${it.message}", it)) }
    )
}
```

**No changes needed in core or application layers!**

---

## 📊 Architecture Benefits

| Dimension | Traditional Layered | This Project (Hexagonal) |
|-----------|---------------------|--------------------------|
| **Dependency Direction** | Downward (to infrastructure) | **Inward (to domain)** |
| **Domain Encapsulation** | Weak (anemic models) | **Strong (invariant protection)** |
| **Interface Naming** | Technical (Gateway, Service) | **Business-semantic (Checker, Publisher)** |
| **Error Handling** | Exception throwing | **Result type** |
| **Testability** | Requires mocking | **Fake implementations** |
| **Adapter Swapping** | Difficult | **Easy (just implement port)** |
| **Framework Independence** | Low | **High (domain has zero deps)** |

---

## 🛠️ Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Micronaut | 4.9.4 | Web framework |
| Kotlin | 2.1.0 | Programming language |
| Kotest | 5.9.1 | Testing framework |
| ArchUnit | 1.3.0 | Architecture testing |
| Gradle | 8.5 | Build tool |
| SLF4J/Logback | 1.7.x | Logging |

---

## ❓ FAQ

**Q: Why use Result instead of exceptions?**  
A: `Result` makes error handling explicit and compiler-enforced, preventing missed error cases.

**Q: Why are domain constructors private?**  
A: Enforce creation through factory methods to ensure all instances pass validation, preventing invalid state.

**Q: How do I add database support?**  
A: Simply implement the `OrderRepository` interface. See `InMemoryOrderRepository` as a reference. No core code changes needed!

**Q: What's the difference from traditional layered architecture?**  
A: See [ARCHITECTURE_COMPARISON.md](./docs/ARCHITECTURE_COMPARISON.md) for detailed comparison.

**Q: How do I check test coverage?**  
A: Run `./gradlew test jacocoTestReport` and view report in `build/reports/jacoco/test/html/index.html`

---

## 📚 References

- [Hexagonal Architecture (Alistair Cockburn)](https://alistair.cockburn.us/hexagonal-architecture/)
- [Clean Architecture (Robert C. Martin)](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Domain-Driven Design (Eric Evans)](https://www.domainlanguage.com/ddd/)
- [Micronaut Documentation](https://docs.micronaut.io/)
- [Kotlin Result Type](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-result/)

---

## 📄 License

MIT License

---

**This is a production-ready reference implementation showcasing hexagonal architecture best practices!**
