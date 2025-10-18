package me.windy.demo.order.core.domain

/**
 * Represents the lifecycle status of an Order.
 * Follows state machine pattern to prevent invalid state transitions.
 */
enum class OrderStatus {
    /**
     * Order has been created but not yet confirmed/paid.
     */
    NEW,

    /**
     * Order has been confirmed and payment received.
     */
    CONFIRMED,

    /**
     * Order is being prepared/processed.
     */
    PROCESSING,

    /**
     * Order has been shipped/dispatched.
     */
    SHIPPED,

    /**
     * Order has been delivered to customer.
     */
    DELIVERED,

    /**
     * Order has been cancelled.
     */
    CANCELLED,

    ;

    /**
     * Validates if transition to target status is allowed.
     */
    fun canTransitionTo(target: OrderStatus): Boolean {
        return when (this) {
            NEW -> target in setOf(CONFIRMED, CANCELLED)
            CONFIRMED -> target in setOf(PROCESSING, CANCELLED)
            PROCESSING -> target in setOf(SHIPPED, CANCELLED)
            SHIPPED -> target in setOf(DELIVERED)
            DELIVERED -> false // Terminal state
            CANCELLED -> false // Terminal state
        }
    }
}
