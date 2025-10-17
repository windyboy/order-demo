package me.windy.demo.order.core.fakes

import me.windy.demo.order.core.domain.Order
import me.windy.demo.order.core.domain.OrderId
import me.windy.demo.order.core.port.outgoing.OrderRepository

/**
 * Fake implementation for testing.
 * Tracks saved orders in memory for verification.
 * Can be configured to simulate save failures.
 */
class FakeOrderRepository(
    private val shouldFailSave: Boolean = false
) : OrderRepository {
    
    val savedOrders = mutableListOf<Order>()
    
    override fun save(order: Order): Result<OrderId> {
        return if (shouldFailSave) {
            Result.failure(RuntimeException("Repository save failed"))
        } else {
            savedOrders.add(order)
            Result.success(order.id)
        }
    }
    
    override fun findById(id: OrderId): Result<Order> {
        val order = savedOrders.find { it.id == id }
        return if (order != null) {
            Result.success(order)
        } else {
            Result.failure(NoSuchElementException("Order not found: ${id.value}"))
        }
    }
    
    override fun exists(id: OrderId): Boolean {
        return savedOrders.any { it.id == id }
    }
    
    override fun count(): Int {
        return savedOrders.size
    }
}

