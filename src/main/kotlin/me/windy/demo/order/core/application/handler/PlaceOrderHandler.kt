package me.windy.demo.order.core.application.handler

import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton
import me.windy.demo.order.core.application.service.PlaceOrderService
import me.windy.demo.order.core.domain.OrderId
import me.windy.demo.order.core.port.incoming.PlaceOrderCommand
import me.windy.demo.order.core.port.incoming.PlaceOrderUseCase

/**
 * Handler for the Place Order use case.
 * Acts as the entry point and delegates to the application service.
 * This is the implementation of the inbound port.
 */
@Singleton
@Requires(property = "features.order.enabled", notEquals = "false", defaultValue = "true")
class PlaceOrderHandler(
    private val service: PlaceOrderService
) : PlaceOrderUseCase {
    
    override fun execute(command: PlaceOrderCommand): Result<OrderId> {
        return service.placeOrder(command.items)
    }
}

