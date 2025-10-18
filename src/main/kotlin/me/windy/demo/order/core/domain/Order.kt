package me.windy.demo.order.core.domain

import me.windy.demo.order.core.domain.event.DomainEvent
import me.windy.demo.order.core.domain.event.OrderPlacedEvent
import me.windy.demo.order.core.domain.event.OrderStatusChangedEvent

/**
 * Aggregate root representing an Order.
 * Ensures domain invariants: at least one item, non-negative total.
 * Total is calculated from items, not passed in.
 * Manages order lifecycle through OrderStatus and publishes domain events.
 *
 * ## HEXAGONAL ARCHITECTURE DEMONSTRATION:
 * This is an **Aggregate Root** in the **DOMAIN layer** (innermost hexagon).
 *
 * **Key DDD Patterns Demonstrated:**
 * - ✅ **Aggregate Root**: Consistency boundary for order and its items
 * - ✅ **Private Constructor**: Enforces creation through factory methods
 * - ✅ **Factory Method**: `Order.create()` ensures valid object creation
 * - ✅ **Domain Events**: `OrderPlacedEvent`, `OrderStatusChangedEvent`
 * - ✅ **State Machine**: OrderStatus with validated transitions
 * - ✅ **Invariant Protection**: Business rules enforced at construction
 * - ✅ **Zero Framework Dependencies**: Pure Kotlin, no Micronaut imports
 *
 * **Why Private Constructor?**
 * - Prevents creation of invalid orders (empty items list)
 * - Forces use of factory methods that validate
 * - Controls all creation points in the codebase
 *
 * **Domain Events Pattern:**
 * - Events are raised when state changes occur
 * - Events are collected in aggregate, not published immediately
 * - Application layer pulls and publishes events after persistence
 *
 * @see OrderStatus for state machine rules
 * @see Money for value object pattern
 * @see OrderItem for value object pattern
 */
@ConsistentCopyVisibility
data class Order private constructor(
    val id: OrderId,
    val items: List<OrderItem>,
    val status: OrderStatus = OrderStatus.NEW,
) {
    /**
     * Uncommitted domain events raised by this aggregate.
     * These should be published after successful persistence.
     */
    private val _domainEvents = mutableListOf<DomainEvent>()
    val domainEvents: List<DomainEvent> get() = _domainEvents.toList()

    init {
        require(items.isNotEmpty()) { "Order must contain at least one item" }
    }

    /**
     * Calculates the total amount for all items in this order.
     */
    fun total(): Money = items.fold(Money.ZERO) { acc, item -> acc + item.subtotal() }

    /**
     * Transitions order to a new status.
     * Validates state transition rules and raises OrderStatusChangedEvent.
     * @return Result with updated Order or OrderError.InvalidState
     */
    fun transitionTo(newStatus: OrderStatus): Result<Order> {
        if (!status.canTransitionTo(newStatus)) {
            return Result.failure(
                OrderError.InvalidState(
                    message = "Cannot transition order ${id.value} from $status to $newStatus",
                    currentState = status.name,
                    targetState = newStatus.name,
                ),
            )
        }

        val updatedOrder = copy(status = newStatus)
        updatedOrder._domainEvents.add(
            OrderStatusChangedEvent(
                orderId = id,
                previousStatus = status,
                newStatus = newStatus,
            ),
        )
        return Result.success(updatedOrder)
    }

    /**
     * Confirms the order (moves from NEW to CONFIRMED).
     * @return Result with confirmed Order or OrderError.InvalidState
     */
    fun confirm(): Result<Order> = transitionTo(OrderStatus.CONFIRMED)

    /**
     * Cancels the order.
     * @return Result with cancelled Order or OrderError.InvalidState
     */
    fun cancel(): Result<Order> = transitionTo(OrderStatus.CANCELLED)

    /**
     * Pulls all uncommitted domain events and clears the internal list.
     * Should be called after successful persistence to publish events.
     * This prevents duplicate event publishing.
     * @return List of domain events that were pending
     */
    fun pullDomainEvents(): List<DomainEvent> {
        val events = _domainEvents.toList()
        _domainEvents.clear()
        return events
    }

    /**
     * Clears domain events after they have been published.
     * @deprecated Use pullDomainEvents() instead for better control
     */
    @Deprecated("Use pullDomainEvents() instead", ReplaceWith("pullDomainEvents()"))
    fun clearDomainEvents() {
        _domainEvents.clear()
    }

    /**
     * Checks if order can be modified (only NEW orders can be modified).
     */
    fun canBeModified(): Boolean = status == OrderStatus.NEW

    companion object {
        /**
         * Creates a new Order with a generated ID.
         * Raises OrderPlacedEvent.
         */
        fun create(items: List<OrderItem>): Order {
            val order = Order(OrderId.generate(), items, OrderStatus.NEW)
            order._domainEvents.add(
                OrderPlacedEvent(
                    orderId = order.id,
                    totalAmount = order.total(),
                    itemCount = items.sumOf { it.quantity },
                ),
            )
            return order
        }

        /**
         * Reconstitutes an Order from persistence with existing ID and status.
         * Does not raise domain events (already persisted).
         */
        fun reconstitute(
            id: OrderId,
            items: List<OrderItem>,
            status: OrderStatus = OrderStatus.NEW,
        ): Order {
            return Order(id, items, status)
        }
    }
}
