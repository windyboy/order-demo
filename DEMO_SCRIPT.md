# Live Demo Script - Hexagonal Architecture

> **Duration**: 15-20 minutes  
> **Audience**: Developers learning hexagonal architecture  
> **Goal**: Show hexagonal architecture principles in action

---

## Prerequisites (Before Demo)

```bash
# 1. Start the application
cd order-demo
./gradlew run

# 2. Open in separate terminal tabs:
# - Tab 1: Application logs
# - Tab 2: curl commands
# - Tab 3: Code editor (open key files)

# 3. Have these files ready to show:
# - Order.kt (domain)
# - OrderRepository.kt (port)
# - InMemoryOrderRepository.kt (adapter)
# - OrderController.kt (HTTP adapter)
# - OrderCLI.kt (CLI adapter)
# - HexagonalArchitectureTest.kt (architecture tests)
```

---

## Demo Flow

### Part 1: Introduction (2 minutes)

**Say**:
> "Today I'll show you a complete hexagonal architecture implementation. Not theory - actual working code. By the end, you'll see:
> - How hexagonal architecture prevents tight coupling
> - How to swap implementations without touching business logic
> - How to prove architecture rules with automated tests"

**Show**: Open `README.md` and scroll to the architecture diagram

---

### Part 2: The Domain Layer - Pure Business Logic (3 minutes)

**Say**:
> "Let's start at the heart - the domain layer. This is where business logic lives."

**Open**: `src/main/kotlin/order/core/domain/Order.kt`

**Point Out**:
1. **Line 7**: See the educational comment explaining this is the domain layer
2. **Line 40**: Private constructor - enforces factory method
3. **Line 42-44**: Business invariant: "Order must contain at least one item"
4. **Line 130**: Factory method `Order.create()` - controlled creation
5. **Top of file**: Check imports - **NO Micronaut, NO Spring, NO frameworks**

**Say**:
> "This is pure Kotlin. Zero framework dependencies. We can test this in isolation, use it in batch jobs, CLI tools, anywhere. That's the power of hexagonal architecture."

**Demo**: Run domain tests only
```bash
./gradlew test --tests "*OrderDomainTest*"
```

**Show**: Tests pass without any infrastructure!

---

### Part 3: The Port Layer - Defining Contracts (2 minutes)

**Say**:
> "The domain needs external services - databases, APIs, etc. But we don't want tight coupling. So we define interfaces - called 'ports'."

**Open**: `src/main/kotlin/order/core/port/outgoing/OrderRepository.kt`

**Point Out**:
1. **Line 10**: Educational comment explaining this is a PORT
2. **Line 14**: "Dependency Inversion - Domain defines what it needs"
3. **Line 40**: The interface - note NO JPA, NO JDBC, just domain types
4. **Line 32**: Returns `Result<OrderId>` - functional error handling

**Say**:
> "This interface lives WITH the domain, not with the database code. The domain says 'I need persistence', and adapters provide it. Dependencies point INWARD."

---

### Part 4: Adapter Swapping - The Magic (5 minutes)

#### 4A: Show Repository Adapters

**Say**:
> "Now watch this - we have TWO implementations of OrderRepository, and we can swap them without changing business logic."

**Open Side-by-Side**:
- `src/main/kotlin/order/adapter/outgoing/persistence/repo/InMemoryOrderRepository.kt`
- Show that it implements `OrderRepository` interface
- Point out `@Singleton` - Micronaut DI will use this

**Say**:
> "In dev/test, we use InMemory for speed. In production, we'd use JPA or PostgreSQL. **Same interface, different implementation.**"

**Show Test**: Open `src/test/kotlin/order/core/fakes/FakeOrderRepository.kt`
```kotlin
class FakeOrderRepository : OrderRepository {  // ✅ Same interface!
    val savedOrders = mutableListOf<Order>()
    // Controllable behavior for testing
}
```

**Say**:
> "For tests, we use Fakes - not mocks! Fakes are simple implementations that give us full control."

#### 4B: Show UI Adapters - The WOW Moment

**Say**:
> "But here's where it gets really cool. The SAME business logic can be used from different UIs."

**Split Screen**:
- Left: `OrderController.kt` (HTTP adapter)
- Right: `OrderCLI.kt` (CLI adapter)

**Point Out**:
Both depend on `PlaceOrderUseCase` - the SAME interface!

**Live Demo**:

**Terminal 1** - HTTP Request:
```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {"sku": "APPLE-001", "unitPrice": 5.99, "quantity": 3}
    ]
  }'
```

**Show**: Response with orderId

**Terminal 2** - CLI Request:
```bash
# (If CLI is enabled in config)
./gradlew run --args="cli demo"
```

**Show**: Same order placement, different UI

**Say**:
> "Did you see that? **Same business logic, same validation, same persistence - different adapters!**  
> Want to add GraphQL? Add an adapter.  
> Want gRPC? Add an adapter.  
> **Business logic never changes.**"

---

### Part 5: Proving Architecture with Tests (3 minutes)

**Say**:
> "Architecture diagrams are nice, but how do you PROVE you're following hexagonal architecture? With tests!"

**Open**: `src/test/kotlin/order/architecture/HexagonalArchitectureTest.kt`

**Point Out**:
1. Uses ArchUnit library
2. Tests like "domain layer should not depend on adapter layer"
3. **These tests FAIL if someone violates the architecture**

**Live Demo - Break the Architecture**:
```kotlin
// In Order.kt, add this import:
import me.windy.demo.order.adapter.incoming.http.OrderController  // ❌ WRONG!
```

**Run**:
```bash
./gradlew test --tests "*HexagonalArchitectureTest*"
```

**Show**: Test fails! Architecture violation detected!

**Remove the bad import and run again**:
```bash
./gradlew test --tests "*HexagonalArchitectureTest*"
```

**Show**: Tests pass! ✅

**Say**:
> "This isn't just documentation - it's **enforced by the compiler**. You can't accidentally break the architecture."

---

### Part 6: Complete Request Flow (3 minutes)

**Say**:
> "Let's trace one request through all layers."

**Open**: `README.md` → Scroll to "How It Works" section

**Walk Through**:
```
HTTP Request 
   → OrderController (adapter)
   → PlaceOrderUseCase (port)
   → OrderPlacementHandler (application)
   → OrderPlacementHandler (orchestration)
   → Order.create() (domain - business logic!)
   → OrderRepository (port)
   → InMemoryOrderRepository (adapter)
```

**Say**:
> "Notice:
> - Adapters on the edges
> - Ports as interfaces
> - Domain in the center with NO dependencies
> - All arrows point INWARD"

**Show Diagram** in README

---

### Part 7: Testing Pyramid (2 minutes)

**Say**:
> "Hexagonal architecture makes testing incredibly easy."

**Run All Tests**:
```bash
./gradlew test
```

**Show Output**: ~52 tests

**Explain**:
```
Domain Tests (14) → Pure logic, no infrastructure
Application Tests (18) → With Fakes, controlled behavior
Adapter Tests (8) → With real/test infrastructure
E2E Tests (6) → Full stack integration
Architecture Tests (6) → Enforce rules ← NEW!
```

**Say**:
> "We can test each layer independently. No need to spin up databases or HTTP servers for domain tests."

---

## Closing (1 minute)

**Say**:
> "So that's hexagonal architecture in action:
> 1. ✅ Pure domain with business logic
> 2. ✅ Ports define contracts
> 3. ✅ Adapters implement contracts
> 4. ✅ Easy to swap implementations
> 5. ✅ Easy to test
> 6. ✅ Architecture enforced by tests
> 
> All the code is available at [GitHub link]. Clone it, try it, learn from it!"

---

## Q&A Preparation

### Common Questions

**Q: Isn't this over-engineering for simple CRUD?**  
A: For simple CRUD, maybe. But when business logic grows, you'll be glad you have clear boundaries. Also, this demo IS simple CRUD - it's not that much more code, but infinitely more maintainable.

**Q: What about performance overhead?**  
A: Minimal. Interfaces are zero-cost in JVM. The indirection is compile-time, not runtime. We've measured - no significant overhead.

**Q: How do you handle transactions across layers?**  
A: Transaction boundaries are at the application service layer (`@Transactional`). Domain doesn't know about transactions - that's infrastructure.

**Q: What about DTOs vs Domain objects?**  
A: DTOs live in adapters. Domain objects live in core. Mappers convert between them. This prevents presentation concerns from leaking into business logic.

**Q: Can I use this with Spring instead of Micronaut?**  
A: Absolutely! The core (domain, ports, application) is framework-agnostic. Only adapters change. That's the point!

---

## Backup Demos (If Time Allows)

### Show Comparison Document
**Open**: `docs/ARCHITECTURE_COMPARISON.md`

**Show**: Side-by-side code comparison of Layered vs Transaction Script vs Hexagonal

### Show State Machine
**Open**: `OrderStatus.kt`

**Explain**: State transition rules, validated at runtime

### Show Domain Events
**Open**: `Order.kt` - `pullDomainEvents()` method

**Explain**: Events raised by domain, published by infrastructure

---

## Technical Setup Checklist

Before demo:
- [ ] Application builds (`./gradlew clean build`)
- [ ] All tests pass (`./gradlew test`)
- [ ] Application runs (`./gradlew run`)
- [ ] Health endpoint responds (`curl http://localhost:8080/orders/health`)
- [ ] Code formatted (`./gradlew ktlintFormat`)
- [ ] Have example curl commands ready
- [ ] Have backup if live demo fails (screenshots/recording)

---

## After Demo

**Share**:
- GitHub repository link
- README.md for quick start
- ARCHITECTURE.md for deep dive
- ARCHITECTURE_COMPARISON.md for why hexagonal is better

**Encourage**:
- Clone and run locally
- Read the educational comments in code
- Run the architecture tests
- Try adding a new adapter

---

**Demo Complete! Questions?** 🎯

