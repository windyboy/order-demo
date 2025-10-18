# Architecture Patterns Comparison

> **Purpose**: Understand WHY hexagonal architecture is superior by comparing it with traditional approaches.

---

## The Challenge: Build an Order Service

**Requirements**:
- Accept order requests (via HTTP REST API)
- Validate business rules (non-empty items, positive prices)
- Check inventory availability
- Save order to database
- Publish events for notifications
- Return order confirmation

Let's implement this with **3 different architectures**...

---

## ❌ Approach 1: Traditional Layered Architecture

### Structure
```
Presentation Layer (Controller)
        ↓
Business Layer (Service)
        ↓
Data Access Layer (DAO/Repository)
```

### Implementation

```kotlin
// Presentation Layer
@RestController
class OrderController @Autowired constructor(
    private val orderService: OrderService  // ❌ Depends on concrete service
) {
    @PostMapping("/orders")
    fun createOrder(@RequestBody orderDTO: OrderDTO): ResponseEntity<*> {
        // ❌ DTO leaks into service layer
        val orderId = orderService.createOrder(orderDTO)
        return ResponseEntity.ok(orderId)
    }
}

// Business Layer
@Service
class OrderService @Autowired constructor(
    private val orderDAO: OrderDAO,  // ❌ Depends on data layer
    private val inventoryClient: InventoryClient  // ❌ Depends on infrastructure
) {
    fun createOrder(dto: OrderDTO): String {
        // ❌ Validation scattered across layers
        if (dto.items.isEmpty()) {
            throw IllegalArgumentException("Order must have items")
        }
        
        // ❌ Business logic mixed with infrastructure concerns
        if (!inventoryClient.checkStock(dto.items)) {
            throw InsufficientStockException()
        }
        
        // ❌ Anemic domain model (just data carrier)
        val order = Order(
            id = UUID.randomUUID().toString(),
            items = dto.items.map { /* manual mapping */ },
            total = dto.items.sumOf { it.price * it.quantity }
        )
        
        orderDAO.save(order)
        return order.id
    }
}

// Data Access Layer
@Repository
class OrderDAO @Autowired constructor(
    private val jdbcTemplate: JdbcTemplate  // ❌ Tight coupling to JDBC
) {
    fun save(order: Order) {
        jdbcTemplate.update(
            "INSERT INTO orders (id, total) VALUES (?, ?)",
            order.id, order.total
        )
    }
}
```

### Problems ❌

1. **Tight Coupling**
   - Controller depends on concrete `OrderService`
   - Service depends on concrete `OrderDAO` and `InventoryClient`
   - Hard to swap implementations

2. **Anemic Domain Model**
   - `Order` is just a data container
   - Business logic lives in service layer
   - Violates object-oriented principles

3. **Testability Issues**
   - Must mock infrastructure (JDBC, HTTP clients)
   - Integration tests require database
   - Cannot test business logic in isolation

4. **Framework Dependency**
   - Core business logic depends on Spring (`@Service`, `@Autowired`)
   - Cannot reuse logic outside Spring context
   - Hard to migrate to different framework

5. **DTO Pollution**
   - DTOs leak into service layer
   - Presentation concerns mixed with business logic
   - Cannot reuse service with different UI (CLI, GraphQL)

---

## ❌ Approach 2: Transaction Script Pattern

### Structure
```
Simple procedural code - no layering
```

### Implementation

```kotlin
@RestController
class OrderController {
    @Autowired lateinit var database: Database
    @Autowired lateinit var inventoryApi: InventoryApi
    @Autowired lateinit var eventBus: EventBus
    
    @PostMapping("/orders")
    fun createOrder(@RequestBody request: CreateOrderRequest): String {
        // ❌ All logic in one place
        
        // Validation
        if (request.items.isEmpty()) {
            throw BadRequestException("Items required")
        }
        
        // Check inventory (direct HTTP call)
        val stockResponse = inventoryApi.checkStock(request.items)
        if (!stockResponse.allAvailable) {
            throw ConflictException("Out of stock")
        }
        
        // Calculate total (no domain model)
        val total = request.items.sumOf { it.price * it.quantity }
        
        // Save to database (direct SQL)
        val orderId = UUID.randomUUID().toString()
        database.execute(
            "INSERT INTO orders (id, total, status) VALUES (?, ?, ?)",
            orderId, total, "NEW"
        )
        
        // Publish event (direct message)
        eventBus.publish("""{"type": "OrderCreated", "id": "$orderId"}""")
        
        return orderId
    }
}
```

### Problems ❌

1. **No Abstraction**
   - Direct dependencies on infrastructure (Database, HTTP clients)
   - Impossible to test without real database/API
   - Cannot swap implementations

2. **Procedural, Not Object-Oriented**
   - No domain model at all
   - Business logic as procedures
   - Hard to maintain as complexity grows

3. **Duplication**
   - If you add CLI interface, you duplicate all logic
   - If you add batch processing, you duplicate validation
   - DRY principle violated

4. **No Encapsulation**
   - Business rules scattered everywhere
   - No single source of truth
   - Easy to introduce bugs

5. **Framework Lock-In**
   - Completely dependent on Spring
   - Cannot use outside web context
   - Migration is a rewrite

---

## ✅ Approach 3: Hexagonal Architecture (This Project!)

### Structure
```
       Adapters (Incoming)
     (HTTP, CLI, GraphQL)
              ↓
         Ports (Interfaces)
      (PlaceOrderUseCase)
              ↓
      Application Layer
    (OrderPlacementHandler)
              ↓
        Domain Layer
   (Order, Money, OrderItem)
              ↓
         Ports (Interfaces)
   (OrderRepository, StockChecker)
              ↓
       Adapters (Outgoing)
    (InMemory, JPA, REST API)
```

### Implementation

```kotlin
// ============================================
// DOMAIN LAYER (Core - No Framework Dependencies)
// ============================================

/**
 * Rich domain model with business logic
 */
data class Order private constructor(
    val id: OrderId,
    val items: List<OrderItem>,
    val status: OrderStatus = OrderStatus.NEW
) {
    init {
        require(items.isNotEmpty()) { "Order must contain at least one item" }
    }
    
    fun total(): Money = items.fold(Money.ZERO) { acc, item -> acc + item.subtotal() }
    
    companion object {
        fun create(items: List<OrderItem>): Order {
            val order = Order(OrderId.generate(), items, OrderStatus.NEW)
            order._domainEvents.add(OrderPlacedEvent(...))
            return order
        }
    }
}

// ============================================
// PORT LAYER (Interfaces)
// ============================================

// Inbound Port (what external world can do)
interface PlaceOrderUseCase {
    fun execute(command: PlaceOrderCommand): Result<OrderId>
}

// Outbound Ports (what domain needs from external world)
interface OrderRepository {
    fun save(order: Order): Result<OrderId>
}

interface StockAvailabilityChecker {
    fun checkAndReserve(sku: String, quantity: Int): Result<Unit>
}

// ============================================
// APPLICATION LAYER (Orchestration)
// ============================================

@Singleton
class OrderPlacementHandler(
    private val repository: OrderRepository,
    private val stockChecker: StockAvailabilityChecker,
    private val eventPublisher: DomainEventPublisher,
) : PlaceOrderUseCase {  // ✅ Implements inbound port

    override fun execute(command: PlaceOrderCommand): Result<OrderId> {
        return validate(command)
            .flatMap(::reserveStock)
            .flatMap(::createAggregate)
            .flatMap(::persistAndPublish)
    }

    private fun validate(command: PlaceOrderCommand): Result<PlaceOrderCommand> {
        return if (command.items.isEmpty()) {
            Result.failure(OrderError.InvalidOrder("Order must contain at least one item"))
        } else {
            Result.success(command)
        }
    }

    private fun reserveStock(command: PlaceOrderCommand): Result<PlaceOrderCommand> {
        val unavailable = command.items.filter { item ->
            stockChecker.checkAndReserve(item.sku, item.quantity).isFailure
        }
        return if (unavailable.isEmpty()) {
            Result.success(command)
        } else {
            Result.failure(
                OrderError.InsufficientStock(
                    message = "Items out of stock: ${unavailable.joinToString { it.sku }}",
                    unavailableItems = unavailable.map { it.sku },
                ),
            )
        }
    }

    private fun createAggregate(command: PlaceOrderCommand): Result<Order> {
        return runCatching { Order.create(command.items) }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { throwable ->
                Result.failure(
                    OrderError.DomainViolation(
                        message = throwable.message ?: "Failed to create order",
                        cause = throwable,
                    ),
                )
            },
        )
    }

    private fun persistAndPublish(order: Order): Result<OrderId> {
        return repository.save(order).fold(
            onSuccess = { orderId ->
                eventPublisher.publishAll(order.pullDomainEvents()).fold(
                    onSuccess = { Result.success(orderId) },
                    onFailure = { throwable ->
                        Result.failure(
                            OrderError.OrderPlacementFailed(
                                message = "Failed to publish domain events: ${throwable.message}",
                                cause = throwable,
                            ),
                        )
                    },
                )
            },
            onFailure = { throwable ->
                Result.failure(
                    OrderError.OrderPlacementFailed(
                        message = "Failed to persist order: ${throwable.message}",
                        cause = throwable,
                    ),
                )
            },
        )
    }
}

// ============================================
// ADAPTER LAYER (Infrastructure)
// ============================================

// HTTP Adapter (Inbound)// ============================================
// ADAPTER LAYER (Infrastructure)
// ============================================

// HTTP Adapter (Inbound)
@Controller("/orders")
class OrderController(
    private val placeOrderUseCase: PlaceOrderUseCase  // ✅ Depends on PORT!
) {
    @Post
    fun place(@Body request: PlaceOrderRequest): HttpResponse<*> {
        val command = mapper.toCommand(request)
        val result = placeOrderUseCase.execute(command)
        return toHttpResponse(result)
    }
}

// CLI Adapter (Inbound) - SAME USE CASE!
@Singleton
class OrderCLI(
    private val placeOrderUseCase: PlaceOrderUseCase  // ✅ Same port!
) {
    fun runDemo() {
        val command = PlaceOrderCommand(items)
        placeOrderUseCase.execute(command)  // ✅ Same logic!
    }
}

// InMemory Adapter (Outbound)
@Singleton
class InMemoryOrderRepository : OrderRepository {  // ✅ Implements PORT
    override fun save(order: Order): Result<OrderId> {
        store[order.id.value] = order
        return Result.success(order.id)
    }
}

// JPA Adapter (Outbound) - SAME PORT!
@Singleton
@Requires(env = ["prod"])
class JpaOrderRepository : OrderRepository {  // ✅ Same interface!
    override fun save(order: Order): Result<OrderId> {
        // Different implementation, same contract!
    }
}
```

### Benefits ✅

1. **Dependency Inversion**
   - Core defines interfaces (ports)
   - Adapters implement interfaces
   - Dependencies point INWARD → core is independent

2. **Rich Domain Model**
   - Business logic in domain objects
   - Self-validating aggregates
   - Object-oriented, not anemic

3. **Perfect Testability**
   - Test domain in isolation (pure functions)
   - Test application with Fakes (no mocks needed)
   - Test adapters with real infrastructure

4. **Framework Independence**
   - Domain has ZERO framework dependencies
   - Can use outside Micronaut (CLI, batch jobs)
   - Easy to migrate frameworks

5. **Adapter Swapping**
   - HTTP or CLI? Same core logic!
   - InMemory or JPA? Same port!
   - REST or gRPC? Just add adapter!

6. **Explicit Error Handling**
   - Result types, not exceptions
   - Compiler-enforced error handling
   - Type-safe, functional approach

---

## Side-by-Side Comparison

| Aspect | Layered | Transaction Script | Hexagonal (This Project) |
|--------|---------|-------------------|-------------------------|
| **Domain Model** | Anemic | None | Rich ✅ |
| **Testability** | Requires mocks | Requires infrastructure | Fakes, pure ✅ |
| **Framework Coupling** | High | Very High | Zero ✅ |
| **Adapter Swapping** | Hard | Impossible | Easy ✅ |
| **Business Logic Location** | Service Layer | Scattered | Domain Layer ✅ |
| **Dependency Direction** | Downward | Chaotic | Inward ✅ |
| **Error Handling** | Exceptions | Exceptions | Result Types ✅ |
| **Code Reusability** | Low | Very Low | High ✅ |
| **Maintainability** | Medium | Low | High ✅ |
| **Learning Curve** | Easy | Easy | Medium |
| **Production Ready** | Yes | No | Yes ✅ |

---

## Real-World Example: Adding a New UI

### Requirement: Add CLI interface for batch processing

#### Layered Architecture ❌
```kotlin
// Must duplicate all logic or extract to shared utility
class CLIProcessor {
    fun processBatch() {
        // Copy-paste validation logic
        // Copy-paste business logic
        // Copy-paste database access
        // High maintenance cost!
    }
}
```

#### Hexagonal Architecture ✅
```kotlin
// Just add new adapter! Core logic unchanged!
@Singleton
class OrderCLI(
    private val placeOrderUseCase: PlaceOrderUseCase  // Reuse existing!
) {
    fun processBatch() {
        // Same use case, different UI!
        placeOrderUseCase.execute(command)
    }
}
```

**Lines of Code**: ~20 lines vs ~200 lines (10x less code!)

---

## Conclusion

### When to Use Each Pattern

**Layered Architecture**: 
- ⚠️ Simple CRUD applications
- ⚠️ When team is unfamiliar with DDD
- ⚠️ Prototype/MVP with short lifespan

**Transaction Script**:
- ⚠️ Very simple applications
- ⚠️ Scripts and utilities
- ❌ NOT for complex business logic

**Hexagonal Architecture**:
- ✅ Complex business logic
- ✅ Multiple interfaces (HTTP, CLI, gRPC)
- ✅ Long-lived applications
- ✅ Need for high testability
- ✅ Want framework independence
- ✅ Expect requirements to change

### This Project Demonstrates

1. ✅ **Perfect hexagonal architecture** with ArchUnit tests
2. ✅ **Adapter swapping** with HTTP + CLI adapters
3. ✅ **Rich domain model** with DDD patterns
4. ✅ **Production patterns** (Result types, events, state machine)
5. ✅ **Comprehensive testing** (domain, application, integration, E2E)

---

## Try It Yourself!

### See HTTP Adapter in Action
```bash
./gradlew run
curl -X POST http://localhost:8080/orders -d '{"items":[...]}' 
```

### See CLI Adapter in Action
```bash
./gradlew run --args="cli demo"
```

**Same use case, different adapter** - that's hexagonal architecture! 🎯

---

## References

- [Original Hexagonal Architecture Article](https://alistair.cockburn.us/hexagonal-architecture/) - Alistair Cockburn
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html) - Uncle Bob
- [Domain-Driven Design](https://www.domainlanguage.com/ddd/) - Eric Evans
- This Project: Complete working example of all patterns!

