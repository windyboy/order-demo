package me.windy.demo.order.core.application.service

import io.micronaut.transaction.annotation.Transactional
import me.windy.demo.order.core.domain.Order
import me.windy.demo.order.core.domain.OrderError
import me.windy.demo.order.core.domain.OrderId
import me.windy.demo.order.core.domain.OrderItem
import me.windy.demo.order.core.port.outgoing.DomainEventPublisher
import me.windy.demo.order.core.port.outgoing.OrderRepository
import me.windy.demo.order.core.port.outgoing.StockAvailabilityChecker

/**
 * Application service for order placement business logic.
 * Orchestrates domain objects and external dependencies.
 * Uses OrderError for type-safe error handling.
 * Transaction boundary is defined at this level.
 *
 * ## Event Publishing Pattern
 *
 * This service follows a specific pattern for domain event publishing:
 * 1. Create order (events are added to aggregate)
 * 2. Persist order to repository
 * 3. Pull events from aggregate (using pullDomainEvents())
 * 4. Publish events to event bus
 *
 * ### Current Implementation (Pull-based)
 * Events are published AFTER persistence succeeds. If event publishing fails,
 * the order is already saved, which breaks strict transactional consistency.
 * This is acceptable for:
 * - Development/testing environments
 * - Non-critical events (logging, notifications)
 * - In-memory implementations
 *
 * ### Production Recommendation: Outbox Pattern
 * For production systems requiring guaranteed event delivery:
 * 1. Save order AND events in same database transaction (atomic)
 * 2. Separate process reads events from outbox table
 * 3. Publishes events to message broker
 * 4. Marks events as published
 *
 * This ensures exactly-once delivery and maintains consistency.
 *
 * @see <a href="https://microservices.io/patterns/data/transactional-outbox.html">Transactional Outbox Pattern</a>
 */
class PlaceOrderService(
    private val repository: OrderRepository,
    private val stockChecker: StockAvailabilityChecker,
    private val eventPublisher: DomainEventPublisher,
) {
    /**
     * Places an order after validating stock availability.
     * This operation is transactional - either everything succeeds or everything rolls back.
     * Uses Result-based error handling to avoid exception-based control flow.
     *
     * @param items List of items to order
     * @return Result containing OrderId on success, OrderError on failure
     */
    @Transactional
    fun placeOrder(items: List<OrderItem>): Result<OrderId> {
        // 1. Validate domain rules (empty items check happens in Order.create)
        if (items.isEmpty()) {
            return Result.failure(
                OrderError.InvalidOrder(
                    message = "Order must contain at least one item",
                ),
            )
        }

        // 2. Check and reserve stock for all items
        val stockReservationResults =
            items.map { item ->
                item to stockChecker.checkAndReserve(item.sku, item.quantity)
            }

        val failedReservations = stockReservationResults.filter { it.second.isFailure }
        if (failedReservations.isNotEmpty()) {
            val unavailableSkus = failedReservations.map { it.first.sku }
            return Result.failure(
                OrderError.InsufficientStock(
                    message = "Items out of stock: ${unavailableSkus.joinToString(", ")}",
                    unavailableItems = unavailableSkus,
                ),
            )
        }

        // 3. Create order (domain validation and event raising happen here)
        val order =
            runCatching { Order.create(items) }.fold(
                onSuccess = { it },
                onFailure = { throwable ->
                    return Result.failure(
                        OrderError.DomainViolation(
                            message = throwable.message ?: "Failed to create order",
                            cause = throwable,
                        ),
                    )
                },
            )

        // 4. Persist order
        val savedOrderId =
            repository.save(order).fold(
                onSuccess = { it },
                onFailure = { throwable ->
                    return Result.failure(
                        OrderError.OrderPlacementFailed(
                            message = "Failed to persist order: ${throwable.message}",
                            cause = throwable,
                        ),
                    )
                },
            )

        // 5. Pull and publish domain events (after successful persistence)
        val events = order.pullDomainEvents()
        eventPublisher.publishAll(events).fold(
            onSuccess = { /* Events published successfully */ },
            onFailure = { throwable ->
                return Result.failure(
                    OrderError.OrderPlacementFailed(
                        message = "Failed to publish domain events: ${throwable.message}",
                        cause = throwable,
                    ),
                )
            },
        )

        return Result.success(savedOrderId)
    }
}
