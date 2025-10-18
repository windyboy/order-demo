package me.windy.demo.order.core.port.outgoing

import me.windy.demo.order.core.domain.Order
import me.windy.demo.order.core.domain.OrderId

/**
 * Port for Order persistence operations.
 * Returns Result types for better error handling.
 *
 * ## HEXAGONAL ARCHITECTURE DEMONSTRATION:
 * This is an **Outbound Port** (interface) in the **PORT layer**.
 *
 * **Key Port Pattern Demonstrated:**
 * - ✅ **Dependency Inversion**: Domain defines what it needs, adapters provide it
 * - ✅ **Interface in Core**: Port lives with the domain, not with adapters
 * - ✅ **Business Semantics**: Named from domain perspective ("OrderRepository")
 * - ✅ **Framework Agnostic**: No JPA, JDBC, or database-specific types
 * - ✅ **Result Types**: Explicit error handling, no checked exceptions
 *
 * **How It Works:**
 * 1. This interface is defined in the **core** (domain side)
 * 2. Application services depend on THIS interface
 * 3. Adapters IMPLEMENT this interface (InMemoryOrderRepository, JpaOrderRepository)
 * 4. Dependency points INWARD: Adapter → Port ← Application
 *
 * **Why This Design?**
 * - Domain doesn't know about databases, HTTP, or any infrastructure
 * - Easy to swap implementations (In-Memory → PostgreSQL → MongoDB)
 * - Easy to test with Fake implementations
 * - Business logic is portable across technologies
 *
 * **Implementations:**
 * - `InMemoryOrderRepository`: For dev/testing (fast, no setup)
 * - `JpaOrderRepository`: For production (persistent, transactional)
 * - `FakeOrderRepository`: For unit testing (controllable behavior)
 *
 * @see me.windy.demo.order.adapter.outgoing.persistence.repo.InMemoryOrderRepository
 */
interface OrderRepository {
    /**
     * Saves an order to persistent storage.
     * @param order The order to save
     * @return Result containing the OrderId on success, or error on failure
     */
    fun save(order: Order): Result<OrderId>

    /**
     * Finds an order by its ID.
     * @param id The order identifier
     * @return Result containing the Order if found, or error if not found or failed
     */
    fun findById(id: OrderId): Result<Order>

    /**
     * Checks if an order exists.
     * @param id The order identifier
     * @return true if order exists, false otherwise
     */
    fun exists(id: OrderId): Boolean

    /**
     * Returns the total count of orders.
     * Useful for testing and monitoring.
     */
    fun count(): Int
}
