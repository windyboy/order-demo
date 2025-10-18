package me.windy.demo.order.core.fakes

import me.windy.demo.order.core.port.outgoing.StockAvailabilityChecker

/**
 * Fake implementation for testing.
 * Configurable to simulate various stock availability scenarios.
 */
class FakeStockAvailabilityChecker(
    private val availableStock: Boolean = true,
    private val unavailableSkus: Set<String> = emptySet(),
    private val stockAvailability: Map<String, Boolean> = emptyMap(),
) : StockAvailabilityChecker {
    data class CheckedItem(val sku: String, val quantity: Int)

    val checkedItems = mutableListOf<CheckedItem>()
    val reservedItems = mutableListOf<CheckedItem>()

    override fun checkAvailability(
        sku: String,
        quantity: Int,
    ): Result<Boolean> {
        return runCatching {
            checkedItems.add(CheckedItem(sku, quantity))

            // Priority: explicit map > unavailable set > default
            when {
                stockAvailability.containsKey(sku) -> stockAvailability[sku]!!
                unavailableSkus.contains(sku) -> false
                else -> availableStock
            }
        }
    }

    override fun reserve(
        sku: String,
        quantity: Int,
    ): Result<Unit> {
        return runCatching {
            val available =
                when {
                    stockAvailability.containsKey(sku) -> stockAvailability[sku]!!
                    unavailableSkus.contains(sku) -> false
                    else -> availableStock
                }

            if (!available) {
                error("Insufficient stock for $sku")
            }
            reservedItems.add(CheckedItem(sku, quantity))
        }
    }
}
