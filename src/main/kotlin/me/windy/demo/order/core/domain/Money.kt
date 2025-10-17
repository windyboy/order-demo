package me.windy.demo.order.core.domain

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Value object representing monetary amount.
 * Immutable and provides safe arithmetic operations.
 * All monetary values are normalized to 2 decimal places using HALF_UP rounding.
 */
@JvmInline
value class Money(val amount: BigDecimal) {
    
    init {
        require(amount >= BigDecimal.ZERO) { "Money amount cannot be negative: $amount" }
    }
    
    operator fun plus(other: Money) = Money(normalize(this.amount + other.amount))
    
    operator fun times(qty: Int): Money {
        require(qty >= 0) { "Quantity cannot be negative: $qty" }
        return Money(normalize(this.amount * qty.toBigDecimal()))
    }

    companion object {
        private const val SCALE = 2
        private val ROUNDING_MODE = RoundingMode.HALF_UP
        
        /**
         * Normalizes the amount to 2 decimal places with HALF_UP rounding.
         */
        private fun normalize(value: BigDecimal): BigDecimal {
            return value.setScale(SCALE, ROUNDING_MODE)
        }
        
        fun of(value: String) = Money(normalize(BigDecimal(value)))
        fun of(value: Double) = Money(normalize(BigDecimal.valueOf(value)))
        fun of(value: BigDecimal) = Money(normalize(value))
        val ZERO = Money(BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE))
    }
}
