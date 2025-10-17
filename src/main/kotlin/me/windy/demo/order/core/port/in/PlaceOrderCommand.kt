package me.windy.demo.order.core.port.`in`

import me.windy.demo.order.core.domain.OrderItem
import java.util.UUID

/**
 * Command object for placing an order.
 * Encapsulates all data required for the use case.
 * Includes requestId for idempotency support.
 */
data class PlaceOrderCommand(
    val items: List<OrderItem>,
    val requestId: String = UUID.randomUUID().toString()
)

