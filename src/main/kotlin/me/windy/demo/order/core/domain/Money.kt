package me.windy.demo.order.core.domain

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Value object representing monetary amount.
 * Immutable and provides safe arithmetic operations.
 * All monetary values are normalized to 2 decimal places using HALF_UP rounding.
 *
 * ## HEXAGONAL ARCHITECTURE DEMONSTRATION:
 * This is a **Value Object** in the **DOMAIN layer**.
 *
 * **Key Value Object Patterns Demonstrated:**
 * - ✅ **Immutability**: Cannot be changed after creation
 * - ✅ **Self-Validation**: Enforces non-negative amounts in init block
 * - ✅ **Value Semantics**: Compared by value, not identity
 * - ✅ **Encapsulation**: Business rules (precision, rounding) are hidden
 * - ✅ **Type Safety**: Cannot accidentally use Double/BigDecimal as money
 * - ✅ **Inline Class**: Zero runtime overhead (Kotlin optimization)
 *
 * **Why Value Object?**
 * - Prevents primitive obsession (using Double for money is dangerous)
 * - Centralizes money calculations and rounding rules
 * - Makes code more expressive: `Money.of(10.0)` vs `10.0`
 * - Prevents bugs like adding incompatible currencies (future enhancement)
 *
 * **Example**:
 * ```kotlin
 * val price = Money.of(19.99)
 * val total = price * 3  // Result: Money(59.97)
 * // NOT: 19.99 * 3 = 59.97000000000001 (float precision issues)
 * ```
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
