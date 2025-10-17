package me.windy.demo.order.core.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for OrderItem value object.
 * Verifies invariants and business logic.
 */
class OrderItemTest : DescribeSpec({
    
    describe("OrderItem creation") {
        it("should create valid OrderItem") {
            val item = OrderItem.of(
                sku = "SKU-001",
                unitPrice = Money.of("10.00"),
                quantity = 2
            )
            
            item.sku shouldBe "SKU-001"
            item.unitPrice shouldBe Money.of("10.00")
            item.quantity shouldBe 2
        }
        
        it("should reject blank SKU") {
            shouldThrow<IllegalArgumentException> {
                OrderItem.of(
                    sku = "",
                    unitPrice = Money.of("10.00"),
                    quantity = 1
                )
            }
        }
        
        it("should reject whitespace-only SKU") {
            shouldThrow<IllegalArgumentException> {
                OrderItem.of(
                    sku = "   ",
                    unitPrice = Money.of("10.00"),
                    quantity = 1
                )
            }
        }
        
        it("should reject zero quantity") {
            shouldThrow<IllegalArgumentException> {
                OrderItem.of(
                    sku = "SKU-001",
                    unitPrice = Money.of("10.00"),
                    quantity = 0
                )
            }
        }
        
        it("should reject negative quantity") {
            shouldThrow<IllegalArgumentException> {
                OrderItem.of(
                    sku = "SKU-001",
                    unitPrice = Money.of("10.00"),
                    quantity = -1
                )
            }
        }
    }
    
    describe("OrderItem subtotal calculation") {
        it("should calculate subtotal correctly") {
            val item = OrderItem.of(
                sku = "SKU-001",
                unitPrice = Money.of("10.50"),
                quantity = 3
            )
            
            item.subtotal() shouldBe Money.of("31.50")
        }
        
        it("should handle single quantity") {
            val item = OrderItem.of(
                sku = "SKU-001",
                unitPrice = Money.of("25.00"),
                quantity = 1
            )
            
            item.subtotal() shouldBe Money.of("25.00")
        }
        
        it("should handle large quantities") {
            val item = OrderItem.of(
                sku = "SKU-001",
                unitPrice = Money.of("5.00"),
                quantity = 100
            )
            
            item.subtotal() shouldBe Money.of("500.00")
        }
        
        it("should handle decimal prices correctly") {
            val item = OrderItem.of(
                sku = "SKU-001",
                unitPrice = Money.of("9.99"),
                quantity = 5
            )
            
            item.subtotal() shouldBe Money.of("49.95")
        }
    }
})

