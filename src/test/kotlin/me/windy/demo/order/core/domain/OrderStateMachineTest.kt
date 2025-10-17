package me.windy.demo.order.core.domain

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Order state machine transitions.
 * Verifies that invalid state transitions are properly rejected.
 */
class OrderStateMachineTest : DescribeSpec({
    
    describe("Order state transitions") {
        
        context("from NEW status") {
            it("should allow transition to CONFIRMED") {
                val order = Order.create(listOf(OrderItem.of("SKU-001", Money.of("10.00"), 1)))
                
                val result = order.confirm()
                
                result.isSuccess shouldBe true
                result.getOrNull()?.status shouldBe OrderStatus.CONFIRMED
            }
            
            it("should allow transition to CANCELLED") {
                val order = Order.create(listOf(OrderItem.of("SKU-001", Money.of("10.00"), 1)))
                
                val result = order.cancel()
                
                result.isSuccess shouldBe true
                result.getOrNull()?.status shouldBe OrderStatus.CANCELLED
            }
            
            it("should reject transition to PROCESSING") {
                val order = Order.create(listOf(OrderItem.of("SKU-001", Money.of("10.00"), 1)))
                
                val result = order.transitionTo(OrderStatus.PROCESSING)
                
                result.isFailure shouldBe true
                result.exceptionOrNull().shouldBeInstanceOf<OrderError.InvalidState>()
            }
            
            it("should reject transition to SHIPPED") {
                val order = Order.create(listOf(OrderItem.of("SKU-001", Money.of("10.00"), 1)))
                
                val result = order.transitionTo(OrderStatus.SHIPPED)
                
                result.isFailure shouldBe true
                result.exceptionOrNull().shouldBeInstanceOf<OrderError.InvalidState>()
            }
        }
        
        context("from CONFIRMED status") {
            it("should allow transition to PROCESSING") {
                val order = Order.create(listOf(OrderItem.of("SKU-001", Money.of("10.00"), 1)))
                    .confirm().getOrThrow()
                
                val result = order.transitionTo(OrderStatus.PROCESSING)
                
                result.isSuccess shouldBe true
                result.getOrNull()?.status shouldBe OrderStatus.PROCESSING
            }
            
            it("should allow transition to CANCELLED") {
                val order = Order.create(listOf(OrderItem.of("SKU-001", Money.of("10.00"), 1)))
                    .confirm().getOrThrow()
                
                val result = order.cancel()
                
                result.isSuccess shouldBe true
                result.getOrNull()?.status shouldBe OrderStatus.CANCELLED
            }
            
            it("should reject transition to SHIPPED") {
                val order = Order.create(listOf(OrderItem.of("SKU-001", Money.of("10.00"), 1)))
                    .confirm().getOrThrow()
                
                val result = order.transitionTo(OrderStatus.SHIPPED)
                
                result.isFailure shouldBe true
                result.exceptionOrNull().shouldBeInstanceOf<OrderError.InvalidState>()
            }
        }
        
        context("from PROCESSING status") {
            it("should allow transition to SHIPPED") {
                val order = Order.create(listOf(OrderItem.of("SKU-001", Money.of("10.00"), 1)))
                    .confirm().getOrThrow()
                    .transitionTo(OrderStatus.PROCESSING).getOrThrow()
                
                val result = order.transitionTo(OrderStatus.SHIPPED)
                
                result.isSuccess shouldBe true
                result.getOrNull()?.status shouldBe OrderStatus.SHIPPED
            }
            
            it("should allow transition to CANCELLED") {
                val order = Order.create(listOf(OrderItem.of("SKU-001", Money.of("10.00"), 1)))
                    .confirm().getOrThrow()
                    .transitionTo(OrderStatus.PROCESSING).getOrThrow()
                
                val result = order.cancel()
                
                result.isSuccess shouldBe true
                result.getOrNull()?.status shouldBe OrderStatus.CANCELLED
            }
            
            it("should reject transition to CONFIRMED") {
                val order = Order.create(listOf(OrderItem.of("SKU-001", Money.of("10.00"), 1)))
                    .confirm().getOrThrow()
                    .transitionTo(OrderStatus.PROCESSING).getOrThrow()
                
                val result = order.confirm()
                
                result.isFailure shouldBe true
                result.exceptionOrNull().shouldBeInstanceOf<OrderError.InvalidState>()
            }
        }
        
        context("from SHIPPED status") {
            it("should allow transition to DELIVERED") {
                val order = Order.create(listOf(OrderItem.of("SKU-001", Money.of("10.00"), 1)))
                    .confirm().getOrThrow()
                    .transitionTo(OrderStatus.PROCESSING).getOrThrow()
                    .transitionTo(OrderStatus.SHIPPED).getOrThrow()
                
                val result = order.transitionTo(OrderStatus.DELIVERED)
                
                result.isSuccess shouldBe true
                result.getOrNull()?.status shouldBe OrderStatus.DELIVERED
            }
            
            it("should reject transition to CANCELLED") {
                val order = Order.create(listOf(OrderItem.of("SKU-001", Money.of("10.00"), 1)))
                    .confirm().getOrThrow()
                    .transitionTo(OrderStatus.PROCESSING).getOrThrow()
                    .transitionTo(OrderStatus.SHIPPED).getOrThrow()
                
                val result = order.cancel()
                
                result.isFailure shouldBe true
                result.exceptionOrNull().shouldBeInstanceOf<OrderError.InvalidState>()
            }
        }
        
        context("from DELIVERED status (terminal)") {
            it("should reject all transitions") {
                val order = Order.create(listOf(OrderItem.of("SKU-001", Money.of("10.00"), 1)))
                    .confirm().getOrThrow()
                    .transitionTo(OrderStatus.PROCESSING).getOrThrow()
                    .transitionTo(OrderStatus.SHIPPED).getOrThrow()
                    .transitionTo(OrderStatus.DELIVERED).getOrThrow()
                
                order.cancel().isFailure shouldBe true
                order.confirm().isFailure shouldBe true
                order.transitionTo(OrderStatus.PROCESSING).isFailure shouldBe true
            }
        }
        
        context("from CANCELLED status (terminal)") {
            it("should reject all transitions") {
                val order = Order.create(listOf(OrderItem.of("SKU-001", Money.of("10.00"), 1)))
                    .cancel().getOrThrow()
                
                order.confirm().isFailure shouldBe true
                order.transitionTo(OrderStatus.PROCESSING).isFailure shouldBe true
                order.transitionTo(OrderStatus.SHIPPED).isFailure shouldBe true
            }
            
            it("should include state information in error") {
                val order = Order.create(listOf(OrderItem.of("SKU-001", Money.of("10.00"), 1)))
                    .cancel().getOrThrow()
                
                val result = order.confirm()
                val error = result.exceptionOrNull() as OrderError.InvalidState
                
                error.currentState shouldBe "CANCELLED"
                error.targetState shouldBe "CONFIRMED"
                error.code shouldBe "INVALID_STATE"
            }
        }
    }
})

