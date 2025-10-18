package me.windy.demo.order.core.application.usecase

import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton
import me.windy.demo.order.core.domain.Order
import me.windy.demo.order.core.domain.OrderError
import me.windy.demo.order.core.domain.OrderId
import me.windy.demo.order.core.domain.OrderItem
import me.windy.demo.order.core.port.incoming.PlaceOrderCommand
import me.windy.demo.order.core.port.incoming.PlaceOrderUseCase
import me.windy.demo.order.core.port.outgoing.DomainEventPublisher
import me.windy.demo.order.core.port.outgoing.OrderRepository
import me.windy.demo.order.core.port.outgoing.StockAvailabilityChecker

/**
 * Coordinating implementation of the PlaceOrderUseCase.
 *
 * The previous design had a thin handler delegating to an imperative service.
 * This refactoring folds the orchestration into a single pipeline-focused
 * component so the application layer reads like a workflow:
 *
 * 1. Validate incoming command
 * 2. Check & reserve stock
 * 3. Create the aggregate
 * 4. Persist the aggregate
 * 5. Publish raised domain events
 *
 * Each step returns a [Result] populated with the domain-specific [OrderError]
 * hierarchy.  This keeps the hexagon free of framework exceptions and makes the
 * flow explicit for demo purposes.
 */
@Singleton
@Requires(property = "features.order.enabled", notEquals = "false", defaultValue = "true")
class OrderPlacementHandler(
    private val repository: OrderRepository,
    private val stockChecker: StockAvailabilityChecker,
    private val eventPublisher: DomainEventPublisher,
) : PlaceOrderUseCase {

    override fun execute(command: PlaceOrderCommand): Result<OrderId> {
        return validate(command)
            .flatMap(::reserveStock)
            .flatMap(::createAggregate)
            .flatMap(::persistAndPublish)
            .map { it.orderId }
    }

    private fun validate(command: PlaceOrderCommand): Result<OrderPlacementContext> {
        if (command.items.isEmpty()) {
            return Result.failure(
                OrderError.InvalidOrder(
                    message = "Order must contain at least one item",
                ),
            )
        }
        return Result.success(OrderPlacementContext(command, command.items))
    }

    private fun reserveStock(context: OrderPlacementContext): Result<OrderPlacementContext> {
        val stockReservationResults =
            context.items.map { item ->
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

        return Result.success(context)
    }

    private fun createAggregate(context: OrderPlacementContext): Result<OrderPlacementContext> {
        return runCatching { Order.create(context.items) }.fold(
            onSuccess = { order -> Result.success(context.withOrder(order)) },
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

    private fun persistAndPublish(context: OrderPlacementContext): Result<OrderPlacementResult> {
        val order = context.order
            ?: return Result.failure(
                OrderError.DomainViolation(
                    message = "Order aggregate was not created before persistence",
                ),
            )

        return repository.save(order).fold(
            onSuccess = { orderId ->
                publishDomainEvents(order).fold(
                    onSuccess = { Result.success(OrderPlacementResult(order, orderId)) },
                    onFailure = { error -> Result.failure(error) },
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

    private fun publishDomainEvents(order: Order): Result<Unit> {
        val events = order.pullDomainEvents()
        if (events.isEmpty()) {
            return Result.success(Unit)
        }

        return eventPublisher.publishAll(events).fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { throwable ->
                Result.failure(
                    OrderError.OrderPlacementFailed(
                        message = "Failed to publish domain events: ${throwable.message}",
                        cause = throwable,
                    ),
                )
            },
        )
    }

    private fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> =
        fold(onSuccess = transform, onFailure = { Result.failure(it) })

    private data class OrderPlacementContext(
        val command: PlaceOrderCommand,
        val items: List<OrderItem>,
        val order: Order? = null,
    ) {
        fun withOrder(order: Order): OrderPlacementContext = copy(order = order)
    }

    private data class OrderPlacementResult(
        val order: Order,
        val orderId: OrderId,
    )
}
