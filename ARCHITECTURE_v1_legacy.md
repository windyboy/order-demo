# 🏛️ Architecture Design Document

## 📐 Hexagonal Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                      Adapter Layer (Adapters)                   │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  Inbound Adapters    │   Outbound Adapters                  │ │
│  │  ┌────────────────┐      │   ┌──────────────────────────┐  │ │
│  │  │ OrderController│◄─────┼───│ StockAvailabilityChecker │  │ │
│  │  │                │      │   │   (Dummy)                │  │ │
│  │  │   + place()    │      │   └──────────────────────────┘  │ │
│  │  │   + health()   │      │   ┌──────────────────────────┐  │ │
│  │  └────────┬───────┘      │   │   OrderRepository        │  │ │
│  │           │              │   │   (InMemory)             │  │ │
│  │           │              │   └──────────────────────────┘  │ │
│  │           │              │   ┌──────────────────────────┐  │ │
│  │           │              │   │ OrderPlacedNotifier      │  │ │
│  │           │              │   │   (Logging)              │  │ │
│  │  ┌────────▼───────┐      │   └──────────────────────────┘  │ │
│  │  │   OrderMapper  │      │                                  │ │
│  │  │  (DTO ↔ Domain)│      │                                  │ │
│  │  └────────────────┘      │                                  │ │
│  └─────────────────────────────────────────────────────────────┘ │
└───────────────────────────┬───────────────────────────────────────┘
                            │
                            │ Ports
                            │
┌───────────────────────────▼───────────────────────────────────────┐
│                       Core Layer                                 │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                   Port Layer (Interface Definitions)        │ │
│  │  ┌──────────────────┐          ┌────────────────────────┐  │ │
│  │  │  PlaceOrderUseCase│◄─────────│StockAvailabilityChecker│  │ │
│  │  │   (In Port)       │          │    (Out Port)          │  │ │
│  │  └─────────┬─────────┘          └────────────────────────┘  │ │
│  │            │                    ┌────────────────────────┐  │ │
│  │            │                    │   OrderRepository      │  │ │
│  │            │                    │    (Out Port)          │  │ │
│  │            │                    └────────────────────────┘  │ │
│  │            │                    ┌────────────────────────┐  │ │
│  │            │                    │ OrderPlacedNotifier    │  │ │
│  │            │                    │    (Out Port)          │  │ │
│  │            │                    └────────────────────────┘  │ │
│  └────────────┼─────────────────────────────────────────────────┘ │
│               │                                                   │
│  ┌────────────▼─────────────────────────────────────────────────┐ │
│  │                 Application Layer (Use Case Implementation) │ │
│  │  ┌──────────────────┐         ┌────────────────────────┐    │ │
│  │  │PlaceOrderHandler │────────►│ PlaceOrderService      │    │ │
│  │  │  implements      │         │  (Business Logic)      │    │ │
│  │  │PlaceOrderUseCase │         └────────────────────────┘    │ │
│  │  └──────────────────┘                                        │ │
│  └───────────────────────────────────────────────────────────────┘ │
│               │                                                   │
│  ┌────────────▼─────────────────────────────────────────────────┐ │
│  │                    Domain Layer (Domain Models)             │ │
│  │  ┌──────────┐  ┌───────────┐  ┌───────┐  ┌───────────┐     │ │
│  │  │  Order   │  │ OrderItem │  │ Money │  │  OrderId  │     │ │
│  │  │(Aggregate)│  │  (Value)  │  │(Value)│  │  (Value)  │     │ │
│  │  └──────────┘  └───────────┘  └───────┘  └───────────┘     │ │
│  │                                                               │ │
│  │  Features:                                                    │ │
│  │  ✓ Private constructor + factory method                      │ │
│  │  ✓ Strong invariant validation                               │ │
│  │  ✓ No framework dependencies                                 │ │
│  │  ✓ Pure business logic                                       │ │
│  └───────────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────────────┘
```

## 🔄 Data Flow Example (Order Placement Process)

```
1. HTTP Request ────────────────────────────────────────────┐
   POST /orders                                              │
   {                                                         │
     "items": [{"sku": "A", "unitPrice": 10, "quantity": 2}]│
   }                                                         │
                                                             │
2. OrderController (Adapter) ◄──────────────────────────────┘
   │
   ├─► OrderMapper.toCommand(request)
   │   └─► PlaceOrderCommand(items=[OrderItem(...)])
   │
3. PlaceOrderHandler (Application) ◄────────────────────────┐
   │  execute(command)                                       │
   │                                                         │
4. PlaceOrderService (Application) ◄────────────────────────┘
   │
   ├─► StockAvailabilityChecker.checkAndReserve(sku, qty)
   │   └─► return true  (Sufficient stock)
   │
   ├─► Order.create(items)  (Domain validation)
   │   └─► return Order(id, items)
   │
   ├─► OrderRepository.save(order)
   │   └─► Save to memory
   │
   ├─► OrderPlacedNotifier.notify(order)
   │   └─► Log record
   │
   └─► return Result.success(orderId)
   
5. OrderController ◄────────────────────────────────────────┐
   │  result.fold(...)                                       │
   │                                                         │
6. HTTP Response ───────────────────────────────────────────┘
   201 Created
   {
     "orderId": "550e8400-e29b-41d4-a716-446655440000"
   }
```

## 🎯 Dependency Rules

```
┌─────────────────────────────────────────┐
│  Outer layers depend on inner layers, inner layers are unaware of outer layers │
└─────────────────────────────────────────┘

Adapter ──────► Port ──────► Application ──────► Domain
  (Adapter)     (Interface)   (Use Case)         (Domain)

✓ Adapter depends on Port and Domain (but not Application)
✓ Application depends on Port and Domain
✓ Port depends on Domain
✓ Domain does not depend on any layer
✗ Domain cannot depend on Application/Port/Adapter
✗ Application cannot depend on Adapter
✗ Adapter cannot directly depend on Application (only through Port interface)
```

## 🧩 Detailed Layer Responsibilities

### Domain Layer (Core Domain)
**Responsibility**: Encapsulate business rules and invariants

**Features**:
- No framework dependencies
- Private constructor + factory method
- Strong invariant validation
- Pure Kotlin code

**示例**:
```kotlin
// ❌ 不好的做法
data class Order(val id: String, val items: List<OrderItem>, val total: BigDecimal)

// ✅ Good practice
class Order private constructor(val id: OrderId, val items: List<OrderItem>) {
    init {
        require(items.isNotEmpty()) { "Order must contain at least one item" }
    }
    fun total(): Money = items.fold(Money.ZERO) { acc, item -> acc + item.subtotal() }
    companion object {
        fun create(items: List<OrderItem>): Order = Order(OrderId.generate(), items)
    }
}
```

### Port Layer (Port Interfaces)
**Responsibility**: Define interaction contracts between core domain and external systems

**Inbound Port (In Port)**:
- Define application use case interfaces
- Business semantic naming
- Return Result type

**Outbound Port (Out Port)**:
- Define external dependency interfaces
- Business semantic naming (non-technical)
- Single responsibility principle

**Naming Comparison**:
```kotlin
// ❌ Technical naming
interface InventoryGateway
interface OrderEventPublisher

// ✅ Business semantic naming
interface StockAvailabilityChecker
interface OrderPlacedNotifier
```

### Application Layer (Application Services)
**Responsibility**: Orchestrate business processes, coordinate domain objects and external dependencies

**Handler**:
- Implement inbound ports
- Use case entry point
- Thin layer, forwarding only

**Service**:
- Business logic orchestration
- Call domain objects
- Call outbound ports
- Return Result type

**Error Handling**:
```kotlin
// ❌ Bad practice (throwing exceptions)
fun placeOrder(items: List<OrderItem>): Order {
    if (!stockChecker.check()) throw InsufficientStockException()
    return order
}

// ✅ Good practice (returning Result with OrderError)
fun placeOrder(items: List<OrderItem>): Result<OrderId> {
    if (!stockChecker.check()) return Result.failure(OrderError.InsufficientStock())
    return Result.success(order.id)
}
```

**Domain Error Types**:
```kotlin
// Domain layer defines error types
sealed interface OrderError {
    val message: String
    val code: String
    
    data class InsufficientStock(...) : OrderError
    data class InvalidOrder(...) : OrderError
}
```

### Adapter Layer (Adapters)
**Responsibility**: Connect external technologies with the core domain

**Inbound Adapters**:
- HTTP Controller
- Message queue listener
- Scheduled tasks

**Outbound Adapters**:
- Database implementation
- External service calls
- Message publishing

**Mapper**:
- DTO ↔ Domain conversion
- Maintain layer decoupling

**Important**:
- Adapter **only depends on** Port (interfaces) and Domain (error types, etc.)
- Adapter **should not** directly depend on Application layer implementations
- This ensures the architecture's dependency inversion principle

## 📊 Testing Strategy

### 1. Domain Testing (Unit Testing)
```kotlin
class OrderDomainTest : StringSpec({
    "should not allow empty items" {
        shouldThrow<IllegalArgumentException> {
            Order.create(emptyList())
        }
    }
})
```

### 2. Application Testing (Integration Testing)
```kotlin
class PlaceOrderServiceTest : StringSpec({
    val repository = FakeOrderRepository()
    val stockChecker = FakeStockAvailabilityChecker(available = true)
    val notifier = FakeOrderPlacedNotifier()
    val service = PlaceOrderService(repository, stockChecker, notifier)
    
    "should place order successfully" {
        val result = service.placeOrder(items)
        result.isSuccess shouldBe true
    }
})
```

### 3. E2E Testing (End-to-End)
```kotlin
@MicronautTest
class OrderE2ETest(@Client("/") private val client: HttpClient) : StringSpec({
    "should place order via HTTP" {
        val response = client.toBlocking().exchange(...)
        response.status shouldBe HttpStatus.CREATED
    }
})
```

## 🔌 Extension Points

### Adding New Use Cases
1. Define Port (`core/port/in/`)
2. Implement Handler (`core/application/handler/`)
3. Implement Service (`core/application/service/`)
4. Add Adapter (`adapter/in/http/`)

### Replacing Implementation
Simply create a new Adapter implementation without modifying core code:

```kotlin
@Singleton
@Replaces(InMemoryOrderRepository::class)
class PostgresOrderRepository : OrderRepository {
    // Implementation using JPA/R2DBC
}
```

## 🎓 Design Principles

1. **Dependency Inversion Principle (DIP)**: High-level modules do not depend on low-level modules
2. **Interface Segregation Principle (ISP)**: Interfaces are small and focused
3. **Single Responsibility Principle (SRP)**: Each class has only one reason to change
4. **Open/Closed Principle (OCP)**: Open for extension, closed for modification
5. **Liskov Substitution Principle (LSP)**: Subclasses can replace parent classes

## 📚 References

- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [DDD (Domain-Driven Design)](https://www.domainlanguage.com/ddd/)
- [Ports and Adapters Pattern](https://jmgarridopaz.github.io/content/hexagonalarchitecture.html)

---

**This architecture ensures high cohesion, low coupling, testability, and maintainability.** 🚀

