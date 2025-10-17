package me.windy.demo.order.core.domain.event

import me.windy.demo.order.core.domain.OrderId
import me.windy.demo.order.core.domain.OrderStatus
import java.time.Instant

/**
 * Domain event published when an order status changes.
 */
data class OrderStatusChangedEvent(
    val orderId: OrderId,
    val previousStatus: OrderStatus,
    val newStatus: OrderStatus,
    override val eventId: String = java.util.UUID.randomUUID().toString(),
    override val occurredAt: Instant = Instant.now()
) : BaseDomainEvent(eventId, occurredAt) {
    
    override val eventType: String = "OrderStatusChanged"
    
    companion object {
        const val EVENT_TYPE = "OrderStatusChanged"
    }
}

