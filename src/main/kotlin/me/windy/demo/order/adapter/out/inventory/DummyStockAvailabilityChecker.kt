package me.windy.demo.order.adapter.out.inventory

import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton
import me.windy.demo.order.core.port.out.StockAvailabilityChecker
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory implementation of StockAvailabilityChecker.
 * Simulates stock levels for demo and testing purposes.
 * In production, this would call an external inventory service (REST API, gRPC, etc.).
 * Active by default unless a real inventory service URL is configured.
 */
@Singleton
@Requires(missingProperty = "inventory.service.url")
class DummyStockAvailabilityChecker : StockAvailabilityChecker {
    
    private val log = LoggerFactory.getLogger(DummyStockAvailabilityChecker::class.java)
    
    // Simulated stock levels: SKU -> available quantity
    private val stockLevels = ConcurrentHashMap<String, Int>().apply {
        // Pre-populate with some stock for testing
        put("SKU-001", 100)
        put("SKU-002", 50)
        put("SKU-003", 0) // Out of stock
    }
    
    override fun checkAvailability(sku: String, quantity: Int): Result<Boolean> {
        return runCatching {
            val availableStock = stockLevels.getOrDefault(sku, 100) // Default to 100 if not found
            val isAvailable = availableStock >= quantity
            log.debug("Stock check: sku={}, requested={}, available={}, sufficient={}", 
                sku, quantity, availableStock, isAvailable)
            isAvailable
        }
    }
    
    override fun reserve(sku: String, quantity: Int): Result<Unit> {
        return runCatching {
            val currentStock = stockLevels.getOrDefault(sku, 100)
            if (currentStock >= quantity) {
                stockLevels[sku] = currentStock - quantity
                log.debug("Stock reserved: sku={}, quantity={}, remaining={}", 
                    sku, quantity, stockLevels[sku])
            } else {
                throw IllegalStateException("Insufficient stock for $sku: available=$currentStock, requested=$quantity")
            }
        }
    }
    
    /**
     * Adds stock for a SKU. Useful for testing.
     */
    fun addStock(sku: String, quantity: Int) {
        stockLevels.merge(sku, quantity) { current, add -> current + add }
        log.debug("Stock added: sku={}, added={}, total={}", sku, quantity, stockLevels[sku])
    }
    
    /**
     * Clears all stock. Useful for testing.
     */
    fun clearStock() {
        stockLevels.clear()
        log.debug("All stock cleared")
    }
}
