package me.windy.demo.order.core.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Domain model tests - pure logic without framework dependencies.
 */
class OrderDomainTest : StringSpec({

    "Order should not allow empty items" {
        shouldThrow<IllegalArgumentException> {
            Order.create(emptyList())
        }
    }

    "Order should calculate total from items" {
        val items =
            listOf(
                OrderItem.of("apple", Money.of(5.0), 2),
                OrderItem.of("banana", Money.of(3.0), 3),
            )
        val order = Order.create(items)

        order.total() shouldBe Money.of("19.00")
    }

    "Order should generate unique ID" {
        val items = listOf(OrderItem.of("sku-1", Money.of(10.0), 1))
        val order1 = Order.create(items)
        val order2 = Order.create(items)

        order1.id shouldNotBe order2.id
    }

    "OrderItem should validate positive quantity" {
        shouldThrow<IllegalArgumentException> {
            OrderItem.of("sku-1", Money.of(10.0), 0)
        }

        shouldThrow<IllegalArgumentException> {
            OrderItem.of("sku-1", Money.of(10.0), -1)
        }
    }

    "OrderItem should validate non-blank SKU" {
        shouldThrow<IllegalArgumentException> {
            OrderItem.of("", Money.of(10.0), 1)
        }

        shouldThrow<IllegalArgumentException> {
            OrderItem.of("   ", Money.of(10.0), 1)
        }
    }

    "OrderItem should calculate subtotal correctly" {
        val item = OrderItem.of("sku-1", Money.of(5.50), 3)
        item.subtotal() shouldBe Money.of("16.50")
    }

    "Money should not allow negative amounts" {
        shouldThrow<IllegalArgumentException> {
            Money.of(-10.0)
        }
    }

    "Money should support addition" {
        val m1 = Money.of(10.0)
        val m2 = Money.of(5.0)
        val result = m1 + m2

        result shouldBe Money.of("15.00")
    }

    "Money multiplication should validate non-negative quantity" {
        val money = Money.of(10.0)

        shouldThrow<IllegalArgumentException> {
            money * -1
        }
    }

    "OrderId should not allow blank values" {
        shouldThrow<IllegalArgumentException> {
            OrderId.of("")
        }

        shouldThrow<IllegalArgumentException> {
            OrderId.of("   ")
        }
    }
})
