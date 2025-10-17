package me.windy.demo.order.core.port.incoming

import me.windy.demo.order.core.domain.OrderError
import me.windy.demo.order.core.domain.OrderId

/**
 * Use case interface for placing orders.
 * Entry point to the application core.
 * Returns Result with OrderError for type-safe error handling.
 */
interface PlaceOrderUseCase {
    /**
     * Places a new order.
     * @param command The order placement command
     * @return Result containing OrderId on success, or OrderError on failure
     */
    fun execute(command: PlaceOrderCommand): Result<OrderId>
}

