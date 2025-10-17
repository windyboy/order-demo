package me.windy.demo.order.core.port.out

import me.windy.demo.order.core.domain.Order
import me.windy.demo.order.core.domain.OrderId

/**
 * Port for Order persistence operations.
 * Returns Result types for better error handling.
 */
interface OrderRepository {
    /**
     * Saves an order to persistent storage.
     * @param order The order to save
     * @return Result containing the OrderId on success, or error on failure
     */
    fun save(order: Order): Result<OrderId>
    
    /**
     * Finds an order by its ID.
     * @param id The order identifier
     * @return Result containing the Order if found, or error if not found or failed
     */
    fun findById(id: OrderId): Result<Order>
    
    /**
     * Checks if an order exists.
     * @param id The order identifier
     * @return true if order exists, false otherwise
     */
    fun exists(id: OrderId): Boolean
    
    /**
     * Returns the total count of orders.
     * Useful for testing and monitoring.
     */
    fun count(): Int
}
