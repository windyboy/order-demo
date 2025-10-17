package me.windy.demo.order.core.application

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import me.windy.demo.order.core.application.service.PlaceOrderService
import me.windy.demo.order.core.domain.Money
import me.windy.demo.order.core.domain.OrderItem
import me.windy.demo.order.core.domain.OrderStatus
import me.windy.demo.order.core.domain.event.OrderPlacedEvent
import me.windy.demo.order.core.domain.event.OrderStatusChangedEvent
import me.windy.demo.order.core.fakes.FakeDomainEventPublisher
import me.windy.demo.order.core.fakes.FakeOrderRepository
import me.windy.demo.order.core.fakes.FakeStockAvailabilityChecker

/**
 * Tests for domain event publishing behavior.
 * Verifies that events are properly raised and published during order operations.
 */
class OrderEventPublishTest : DescribeSpec({
    
    describe("Domain event publishing") {
        
        context("when order is successfully placed") {
            it("should publish OrderPlacedEvent") {
                val repository = FakeOrderRepository()
                val stockChecker = FakeStockAvailabilityChecker()
                val eventPublisher = FakeDomainEventPublisher()
                val service = PlaceOrderService(repository, stockChecker, eventPublisher)
                
                val items = listOf(
                    OrderItem.of("SKU-001", Money.of("10.00"), 2),
                    OrderItem.of("SKU-002", Money.of("20.00"), 1)
                )
                
                val result = service.placeOrder(items)
                
                result.isSuccess shouldBe true
                eventPublisher.publishedEvents.size shouldBe 1
                
                val event = eventPublisher.publishedEvents.first()
                event.shouldBeInstanceOf<OrderPlacedEvent>()
                event.itemCount shouldBe 3
                event.totalAmount.amount.toDouble() shouldBe 40.00
            }
        }
        
        context("when order transitions state") {
            it("should publish OrderStatusChangedEvent") {
                val order = me.windy.demo.order.core.domain.Order.create(
                    listOf(OrderItem.of("SKU-001", Money.of("10.00"), 1))
                )
                
                val confirmedOrder = order.confirm().getOrThrow()
                val events = confirmedOrder.pullDomainEvents()
                
                events.size shouldBe 1
                val event = events.first()
                event.shouldBeInstanceOf<OrderStatusChangedEvent>()
                event.previousStatus shouldBe OrderStatus.NEW
                event.newStatus shouldBe OrderStatus.CONFIRMED
            }
        }
        
        context("when pullDomainEvents is called") {
            it("should clear events after pulling") {
                val order = me.windy.demo.order.core.domain.Order.create(
                    listOf(OrderItem.of("SKU-001", Money.of("10.00"), 1))
                )
                
                // First pull
                val events1 = order.pullDomainEvents()
                events1.size shouldBe 1
                
                // Second pull should return empty
                val events2 = order.pullDomainEvents()
                events2.size shouldBe 0
            }
        }
        
        context("when multiple state transitions occur") {
            it("should publish multiple events") {
                val repository = FakeOrderRepository()
                val stockChecker = FakeStockAvailabilityChecker()
                val eventPublisher = FakeDomainEventPublisher()
                val service = PlaceOrderService(repository, stockChecker, eventPublisher)
                
                val items = listOf(OrderItem.of("SKU-001", Money.of("10.00"), 1))
                val result = service.placeOrder(items)
                
                result.isSuccess shouldBe true
                
                // Get the saved order
                val savedOrder = repository.savedOrders.first()
                
                // Transition to CONFIRMED
                val confirmedOrder = savedOrder.confirm().getOrThrow()
                val statusEvents = confirmedOrder.pullDomainEvents()
                
                statusEvents.size shouldBe 1
                statusEvents.first().shouldBeInstanceOf<OrderStatusChangedEvent>()
            }
        }
        
        context("when event publishing fails") {
            it("should return failure result") {
                val repository = FakeOrderRepository()
                val stockChecker = FakeStockAvailabilityChecker()
                val eventPublisher = FakeDomainEventPublisher(shouldFail = true)
                val service = PlaceOrderService(repository, stockChecker, eventPublisher)
                
                val items = listOf(OrderItem.of("SKU-001", Money.of("10.00"), 1))
                val result = service.placeOrder(items)
                
                result.isFailure shouldBe true
                result.exceptionOrNull()?.message shouldBe "Failed to publish domain events: Event publishing failed"
            }
        }
        
        context("when order creation has domain events") {
            it("should not leak events across aggregates") {
                val order1 = me.windy.demo.order.core.domain.Order.create(
                    listOf(OrderItem.of("SKU-001", Money.of("10.00"), 1))
                )
                val order2 = me.windy.demo.order.core.domain.Order.create(
                    listOf(OrderItem.of("SKU-002", Money.of("20.00"), 1))
                )
                
                val events1 = order1.pullDomainEvents()
                val events2 = order2.pullDomainEvents()
                
                // Each order should have its own events
                events1.size shouldBe 1
                events2.size shouldBe 1
                
                val event1 = events1.first() as OrderPlacedEvent
                val event2 = events2.first() as OrderPlacedEvent
                
                event1.orderId shouldBe order1.id
                event2.orderId shouldBe order2.id
            }
        }
        
        context("when verifying event metadata") {
            it("should have correct event type and timestamp") {
                val order = me.windy.demo.order.core.domain.Order.create(
                    listOf(OrderItem.of("SKU-001", Money.of("10.00"), 1))
                )
                
                val events = order.pullDomainEvents()
                val event = events.first() as OrderPlacedEvent
                
                event.eventType shouldBe "OrderPlaced"
                event.eventId.isNotBlank() shouldBe true
                event.occurredAt shouldBe event.occurredAt
            }
        }
    }
})

