package me.windy.demo.order.core.domain

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import me.windy.demo.order.core.domain.event.OrderPlacedEvent
import me.windy.demo.order.core.domain.event.OrderStatusChangedEvent

/**
 * Tests for Order state transitions and domain events.
 */
class OrderStateTransitionTest : DescribeSpec({

    describe("Order creation") {
        it("should create order in NEW status") {
            val items =
                listOf(
                    OrderItem.of("SKU-001", Money.of("10.00"), 2),
                )

            val order = Order.create(items)

            order.status shouldBe OrderStatus.NEW
        }

        it("should raise OrderPlacedEvent on creation") {
            val items =
                listOf(
                    OrderItem.of("SKU-001", Money.of("10.00"), 2),
                    OrderItem.of("SKU-002", Money.of("5.00"), 3),
                )

            val order = Order.create(items)

            order.domainEvents shouldHaveSize 1
            val event = order.domainEvents.first()
            event.shouldBeInstanceOf<OrderPlacedEvent>()
            event.orderId shouldBe order.id
            event.totalAmount shouldBe Money.of("35.00") // 10*2 + 5*3
            event.itemCount shouldBe 5 // 2 + 3
        }
    }

    describe("Order status transitions") {
        it("should transition from NEW to CONFIRMED") {
            val order = createSampleOrder()

            val confirmedOrder = order.confirm().getOrThrow()

            confirmedOrder.status shouldBe OrderStatus.CONFIRMED
            confirmedOrder.domainEvents shouldHaveSize 1
            val event = confirmedOrder.domainEvents.first()
            event.shouldBeInstanceOf<OrderStatusChangedEvent>()
            event.previousStatus shouldBe OrderStatus.NEW
            event.newStatus shouldBe OrderStatus.CONFIRMED
        }

        it("should transition from NEW to CANCELLED") {
            val order = createSampleOrder()

            val cancelledOrder = order.cancel().getOrThrow()

            cancelledOrder.status shouldBe OrderStatus.CANCELLED
        }

        it("should transition from CONFIRMED to PROCESSING") {
            val order = createSampleOrder().confirm().getOrThrow()

            val processingOrder = order.transitionTo(OrderStatus.PROCESSING).getOrThrow()

            processingOrder.status shouldBe OrderStatus.PROCESSING
        }

        it("should transition from PROCESSING to SHIPPED") {
            val order =
                createSampleOrder()
                    .confirm().getOrThrow()
                    .transitionTo(OrderStatus.PROCESSING).getOrThrow()

            val shippedOrder = order.transitionTo(OrderStatus.SHIPPED).getOrThrow()

            shippedOrder.status shouldBe OrderStatus.SHIPPED
        }

        it("should transition from SHIPPED to DELIVERED") {
            val order =
                createSampleOrder()
                    .confirm().getOrThrow()
                    .transitionTo(OrderStatus.PROCESSING).getOrThrow()
                    .transitionTo(OrderStatus.SHIPPED).getOrThrow()

            val deliveredOrder = order.transitionTo(OrderStatus.DELIVERED).getOrThrow()

            deliveredOrder.status shouldBe OrderStatus.DELIVERED
        }
    }

    describe("Invalid state transitions") {
        it("should reject transition from NEW to PROCESSING") {
            val order = createSampleOrder()

            val result = order.transitionTo(OrderStatus.PROCESSING)

            result.isFailure shouldBe true
            result.exceptionOrNull().shouldBeInstanceOf<OrderError.InvalidState>()
        }

        it("should reject transition from DELIVERED to CANCELLED") {
            val order =
                createSampleOrder()
                    .confirm().getOrThrow()
                    .transitionTo(OrderStatus.PROCESSING).getOrThrow()
                    .transitionTo(OrderStatus.SHIPPED).getOrThrow()
                    .transitionTo(OrderStatus.DELIVERED).getOrThrow()

            val result = order.cancel()

            result.isFailure shouldBe true
            result.exceptionOrNull().shouldBeInstanceOf<OrderError.InvalidState>()
        }

        it("should reject transition from CANCELLED to CONFIRMED") {
            val order = createSampleOrder().cancel().getOrThrow()

            val result = order.confirm()

            result.isFailure shouldBe true
            result.exceptionOrNull().shouldBeInstanceOf<OrderError.InvalidState>()
        }
    }

    describe("Order modification rules") {
        it("should allow modification when status is NEW") {
            val order = createSampleOrder()

            order.canBeModified() shouldBe true
        }

        it("should not allow modification when status is CONFIRMED") {
            val order = createSampleOrder().confirm().getOrThrow()

            order.canBeModified() shouldBe false
        }

        it("should not allow modification when status is CANCELLED") {
            val order = createSampleOrder().cancel().getOrThrow()

            order.canBeModified() shouldBe false
        }
    }

    describe("Domain events") {
        it("should accumulate multiple domain events") {
            val order = createSampleOrder()
            val confirmedOrder = order.confirm().getOrThrow()

            // Initial order has OrderPlacedEvent
            order.domainEvents shouldHaveSize 1

            // Confirmed order has OrderStatusChangedEvent
            confirmedOrder.domainEvents shouldHaveSize 1
        }

        it("should clear domain events") {
            val order = createSampleOrder()
            order.domainEvents shouldHaveSize 1

            @Suppress("DEPRECATION")
            order.clearDomainEvents()

            order.domainEvents shouldHaveSize 0
        }
    }

    describe("Order reconstitution") {
        it("should reconstitute order without raising domain events") {
            val orderId = OrderId.generate()
            val items =
                listOf(
                    OrderItem.of("SKU-001", Money.of("10.00"), 2),
                )

            val order = Order.reconstitute(orderId, items, OrderStatus.CONFIRMED)

            order.id shouldBe orderId
            order.status shouldBe OrderStatus.CONFIRMED
            order.domainEvents shouldHaveSize 0
        }
    }
})

private fun createSampleOrder(): Order {
    val items =
        listOf(
            OrderItem.of("SKU-001", Money.of("10.00"), 2),
        )
    return Order.create(items)
}
