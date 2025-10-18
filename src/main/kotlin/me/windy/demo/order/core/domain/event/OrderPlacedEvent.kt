package me.windy.demo.order.core.domain.event

import me.windy.demo.order.core.domain.Money
import me.windy.demo.order.core.domain.OrderId
import java.time.Instant

/**
 * Domain event published when an order is successfully placed.
 * This event can trigger various reactions like:
 * - Sending confirmation email
 * - Reserving inventory
 * - Creating shipment request
 * - Analytics tracking
 */
data class OrderPlacedEvent(
    val orderId: OrderId,
    val totalAmount: Money,
    val itemCount: Int,
    override val eventId: String = java.util.UUID.randomUUID().toString(),
    override val occurredAt: Instant = Instant.now(),
) : BaseDomainEvent(eventId, occurredAt) {
    override val eventType: String = "OrderPlaced"

    companion object {
        const val EVENT_TYPE = "OrderPlaced"
    }
}
