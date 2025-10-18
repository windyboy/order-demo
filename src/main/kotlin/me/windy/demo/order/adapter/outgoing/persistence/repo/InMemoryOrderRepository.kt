package me.windy.demo.order.adapter.outgoing.persistence.repo

import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton
import me.windy.demo.order.core.domain.Order
import me.windy.demo.order.core.domain.OrderId
import me.windy.demo.order.core.port.outgoing.OrderRepository
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory implementation of OrderRepository.
 * Thread-safe for testing and demo purposes.
 * Active in all environments (dev/test/prod) unless overridden.
 */
@Singleton
@Requires(missingBeans = [OrderRepository::class])
class InMemoryOrderRepository : OrderRepository {
    private val log = LoggerFactory.getLogger(InMemoryOrderRepository::class.java)
    private val store = ConcurrentHashMap<String, Order>()

    override fun save(order: Order): Result<OrderId> {
        return runCatching {
            store[order.id.value] = order
            log.debug(
                "Order saved: id={}, items={}, status={}",
                order.id.value,
                order.items.size,
                order.status,
            )
            order.id
        }
    }

    override fun findById(id: OrderId): Result<Order> {
        return runCatching {
            store[id.value] ?: throw NoSuchElementException("Order not found: ${id.value}")
        }
    }

    override fun exists(id: OrderId): Boolean {
        return store.containsKey(id.value)
    }

    override fun count(): Int {
        return store.size
    }

    /**
     * Clears all orders. Useful for testing.
     */
    fun clear() {
        store.clear()
        log.debug("All orders cleared")
    }
}
