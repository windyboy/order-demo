package me.windy.demo.order.core.port.outgoing

import me.windy.demo.order.core.domain.Order
import me.windy.demo.order.core.domain.OrderId
import me.windy.demo.order.core.domain.OrderStatus

/**
 * Query repository for read-only Order operations.
 * Part of CQRS pattern - separates read operations from write operations.
 * This allows for different optimization strategies for queries vs commands.
 */
interface OrderQueryRepository {
    
    /**
     * Finds an order by its unique identifier.
     * @param id The order ID
     * @return Result with Order if found, failure otherwise
     */
    fun findById(id: OrderId): Result<Order>
    
    /**
     * Finds all orders with a specific status.
     * @param status The order status to filter by
     * @return Result with list of orders, empty list if none found
     */
    fun findByStatus(status: OrderStatus): Result<List<Order>>
    
    /**
     * Finds all orders (potentially paginated in real implementation).
     * @param limit Maximum number of orders to return
     * @param offset Starting position for pagination
     * @return Result with list of orders
     */
    fun findAll(limit: Int = 100, offset: Int = 0): Result<List<Order>>
    
    /**
     * Checks if an order exists.
     * @param id The order ID
     * @return true if order exists, false otherwise
     */
    fun exists(id: OrderId): Boolean
    
    /**
     * Counts total number of orders.
     * @return Total order count
     */
    fun count(): Int
    
    /**
     * Counts orders by status.
     * @param status The order status
     * @return Number of orders with given status
     */
    fun countByStatus(status: OrderStatus): Int
}

