package me.windy.demo.order.core.domain

/**
 * Base class for all order-related domain errors.
 * Extends Throwable to be compatible with Kotlin's Result type,
 * but follows functional error handling patterns with explicit error codes.
 */
sealed class OrderError(
    override val message: String,
    val code: String,
    override val cause: Throwable? = null
) : Throwable(message, cause) {
    
    /**
     * Error when stock is insufficient for one or more items.
     */
    class InsufficientStock(
        message: String = "One or more items are out of stock",
        val unavailableItems: List<String> = emptyList()
    ) : OrderError(message, "INSUFFICIENT_STOCK")
    
    /**
     * Error when order data is invalid (e.g., empty items, negative prices).
     */
    class InvalidOrder(
        message: String,
        cause: Throwable? = null
    ) : OrderError(message, "INVALID_ORDER", cause)
    
    /**
     * Error when attempting an invalid state transition.
     */
    class InvalidState(
        message: String,
        val currentState: String,
        val targetState: String
    ) : OrderError(message, "INVALID_STATE")
    
    /**
     * Error when domain invariant is violated.
     */
    class DomainViolation(
        message: String,
        cause: Throwable? = null
    ) : OrderError(message, "DOMAIN_VIOLATION", cause)
    
    /**
     * Error when order placement fails due to system issues.
     */
    class OrderPlacementFailed(
        message: String = "Failed to place order",
        cause: Throwable? = null
    ) : OrderError(message, "ORDER_PLACEMENT_FAILED", cause)
}

