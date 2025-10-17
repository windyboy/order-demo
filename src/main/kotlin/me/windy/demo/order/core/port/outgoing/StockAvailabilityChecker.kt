package me.windy.demo.order.core.port.outgoing

/**
 * Port for checking product stock availability.
 * Business-oriented interface for inventory operations.
 */
interface StockAvailabilityChecker {
    /**
     * Checks if the specified product has sufficient stock.
     * @param sku Product identifier
     * @param quantity Required quantity
     * @return Result with available quantity, or error if check fails
     */
    fun checkAvailability(sku: String, quantity: Int): Result<Boolean>
    
    /**
     * Reserves stock for the specified product.
     * @param sku Product identifier
     * @param quantity Required quantity
     * @return Result indicating success or failure with reason
     */
    fun reserve(sku: String, quantity: Int): Result<Unit>
    
    /**
     * Checks and reserves in a single operation (convenience method).
     * Uses Result-based error handling to avoid exceptions.
     * @param sku Product identifier
     * @param quantity Required quantity
     * @return Result indicating success or failure with reason
     */
    fun checkAndReserve(sku: String, quantity: Int): Result<Unit> {
        return checkAvailability(sku, quantity).fold(
            onSuccess = { available ->
                if (available) {
                    reserve(sku, quantity)
                } else {
                    Result.failure(RuntimeException("Insufficient stock for $sku"))
                }
            },
            onFailure = { throwable ->
                Result.failure(throwable)
            }
        )
    }
}

