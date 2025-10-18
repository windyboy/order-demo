package me.windy.demo.order.core.domain

import java.util.UUID

/**
 * Value object representing a unique order identifier.
 */
@JvmInline
value class OrderId(val value: String) {
    init {
        require(value.isNotBlank()) { "OrderId cannot be blank" }
    }

    companion object {
        fun generate(): OrderId = OrderId(UUID.randomUUID().toString())

        fun of(value: String): OrderId = OrderId(value)
    }
}
