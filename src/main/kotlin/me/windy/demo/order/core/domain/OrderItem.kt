package me.windy.demo.order.core.domain

/**
 * Value object representing an item in an order.
 * Ensures invariants: non-empty SKU, positive price and quantity.
 */
@ConsistentCopyVisibility
data class OrderItem private constructor(
    val sku: String,
    val unitPrice: Money,
    val quantity: Int,
) {
    init {
        require(sku.isNotBlank()) { "SKU cannot be blank" }
        require(quantity > 0) { "Quantity must be positive: $quantity" }
    }

    /**
     * Calculates the subtotal for this line item.
     */
    fun subtotal(): Money = unitPrice * quantity

    companion object {
        fun of(
            sku: String,
            unitPrice: Money,
            quantity: Int,
        ): OrderItem {
            return OrderItem(sku, unitPrice, quantity)
        }
    }
}
