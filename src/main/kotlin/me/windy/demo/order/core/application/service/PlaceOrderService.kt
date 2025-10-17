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
 */
class PlaceOrderService(
    private val repository: OrderRepository,
    private val stockChecker: StockAvailabilityChecker,
    private val eventPublisher: DomainEventPublisher
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
                    message = "Order must contain at least one item"
                )
            )
        }
        
        // 2. Check and reserve stock for all items
        val stockReservationResults = items.map { item ->
            item to stockChecker.checkAndReserve(item.sku, item.quantity)
        }
        
        val failedReservations = stockReservationResults.filter { it.second.isFailure }
        if (failedReservations.isNotEmpty()) {
            val unavailableSkus = failedReservations.map { it.first.sku }
            return Result.failure(
                OrderError.InsufficientStock(
                    message = "Items out of stock: ${unavailableSkus.joinToString(", ")}",
                    unavailableItems = unavailableSkus
                )
            )
        }
        
        // 3. Create order (domain validation and event raising happen here)
        val order = runCatching { Order.create(items) }.fold(
            onSuccess = { it },
            onFailure = { throwable ->
                return Result.failure(
                    OrderError.DomainViolation(
                        message = throwable.message ?: "Failed to create order",
                        cause = throwable
                    )
                )
            }
        )
        
        // 4. Persist order
        val savedOrderId = repository.save(order).fold(
            onSuccess = { it },
            onFailure = { throwable ->
                return Result.failure(
                    OrderError.OrderPlacementFailed(
                        message = "Failed to persist order: ${throwable.message}",
                        cause = throwable
                    )
                )
            }
        )
        
        // 5. Pull and publish domain events (after successful persistence)
        val events = order.pullDomainEvents()
        eventPublisher.publishAll(events).fold(
            onSuccess = { /* Events published successfully */ },
            onFailure = { throwable ->
                return Result.failure(
                    OrderError.OrderPlacementFailed(
                        message = "Failed to publish domain events: ${throwable.message}",
                        cause = throwable
                    )
                )
            }
        )
        
        return Result.success(savedOrderId)
    }
}


