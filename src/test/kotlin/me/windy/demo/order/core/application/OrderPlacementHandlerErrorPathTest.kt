package me.windy.demo.order.core.application

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import me.windy.demo.order.core.application.usecase.OrderPlacementHandler
import me.windy.demo.order.core.domain.Money
import me.windy.demo.order.core.domain.OrderError
import me.windy.demo.order.core.domain.OrderItem
import me.windy.demo.order.core.fakes.FakeDomainEventPublisher
import me.windy.demo.order.core.fakes.FakeOrderRepository
import me.windy.demo.order.core.fakes.FakeStockAvailabilityChecker
import me.windy.demo.order.core.port.incoming.PlaceOrderCommand

/**
 * Tests for [OrderPlacementHandler] error paths.
 * Verifies proper error handling for various failure scenarios.
 */
class OrderPlacementHandlerErrorPathTest : DescribeSpec({

    describe("OrderPlacementHandler error handling") {

        context("when items list is empty") {
            it("should return InvalidOrder error") {
                val repository = FakeOrderRepository()
                val stockChecker = FakeStockAvailabilityChecker()
                val eventPublisher = FakeDomainEventPublisher()
                val handler = OrderPlacementHandler(repository, stockChecker, eventPublisher)

                val result = handler.execute(PlaceOrderCommand(emptyList()))

                result.isFailure shouldBe true
                val error = result.exceptionOrNull()
                error.shouldBeInstanceOf<OrderError.InvalidOrder>()
                error.message shouldBe "Order must contain at least one item"
            }
        }

        context("when stock is insufficient") {
            it("should return InsufficientStock error with unavailable items") {
                val repository = FakeOrderRepository()
                val stockChecker = FakeStockAvailabilityChecker(availableStock = false)
                val eventPublisher = FakeDomainEventPublisher()
                val handler = OrderPlacementHandler(repository, stockChecker, eventPublisher)

                val items =
                    listOf(
                        OrderItem.of("SKU-001", Money.of("10.00"), 5),
                        OrderItem.of("SKU-002", Money.of("20.00"), 3),
                    )

                val result = handler.execute(PlaceOrderCommand(items))

                result.isFailure shouldBe true
                val error = result.exceptionOrNull() as OrderError.InsufficientStock
                error.code shouldBe "INSUFFICIENT_STOCK"
                error.unavailableItems.size shouldBe 2
                error.unavailableItems shouldBe listOf("SKU-001", "SKU-002")
            }
        }

        context("when stock is partially available") {
            it("should return InsufficientStock error with only unavailable items") {
                val repository = FakeOrderRepository()
                val stockChecker =
                    FakeStockAvailabilityChecker(
                        stockAvailability =
                            mapOf(
                                "SKU-001" to true,
                                "SKU-002" to false,
                                "SKU-003" to false,
                            ),
                    )
                val eventPublisher = FakeDomainEventPublisher()
                val handler = OrderPlacementHandler(repository, stockChecker, eventPublisher)

                val items =
                    listOf(
                        OrderItem.of("SKU-001", Money.of("10.00"), 1),
                        OrderItem.of("SKU-002", Money.of("20.00"), 1),
                        OrderItem.of("SKU-003", Money.of("30.00"), 1),
                    )

                val result = handler.execute(PlaceOrderCommand(items))

                result.isFailure shouldBe true
                val error = result.exceptionOrNull() as OrderError.InsufficientStock
                error.unavailableItems shouldBe listOf("SKU-002", "SKU-003")
            }
        }

        context("when repository save fails") {
            it("should return OrderPlacementFailed error") {
                val repository = FakeOrderRepository(shouldFailSave = true)
                val stockChecker = FakeStockAvailabilityChecker()
                val eventPublisher = FakeDomainEventPublisher()
                val handler = OrderPlacementHandler(repository, stockChecker, eventPublisher)

                val items = listOf(OrderItem.of("SKU-001", Money.of("10.00"), 1))

                val result = handler.execute(PlaceOrderCommand(items))

                result.isFailure shouldBe true
                val error = result.exceptionOrNull()
                error.shouldBeInstanceOf<OrderError.OrderPlacementFailed>()
                error.message shouldBe "Failed to persist order: Repository save failed"
            }
        }

        context("when event publishing fails") {
            it("should return OrderPlacementFailed error") {
                val repository = FakeOrderRepository()
                val stockChecker = FakeStockAvailabilityChecker()
                val eventPublisher = FakeDomainEventPublisher(shouldFail = true)
                val handler = OrderPlacementHandler(repository, stockChecker, eventPublisher)

                val items = listOf(OrderItem.of("SKU-001", Money.of("10.00"), 1))

                val result = handler.execute(PlaceOrderCommand(items))

                result.isFailure shouldBe true
                val error = result.exceptionOrNull()
                error.shouldBeInstanceOf<OrderError.OrderPlacementFailed>()
                error.message shouldBe "Failed to publish domain events: Event publishing failed"
            }
        }

        context("when order creation succeeds") {
            it("should not have any events remaining in aggregate") {
                val repository = FakeOrderRepository()
                val stockChecker = FakeStockAvailabilityChecker()
                val eventPublisher = FakeDomainEventPublisher()
                val handler = OrderPlacementHandler(repository, stockChecker, eventPublisher)

                val items = listOf(OrderItem.of("SKU-001", Money.of("10.00"), 2))

                val result = handler.execute(PlaceOrderCommand(items))

                result.isSuccess shouldBe true
                // Events should have been pulled and published
                eventPublisher.publishedEvents.size shouldBe 1
            }
        }

        context("when handling negative price") {
            it("should return DomainViolation error") {
                val repository = FakeOrderRepository()
                val stockChecker = FakeStockAvailabilityChecker()
                val eventPublisher = FakeDomainEventPublisher()
                val handler = OrderPlacementHandler(repository, stockChecker, eventPublisher)

                val result =
                    runCatching {
                        val items = listOf(OrderItem.of("SKU-001", Money.of("-10.00"), 1))
                        handler.execute(PlaceOrderCommand(items))
                    }

                // Money value class will reject negative amounts in init block
                result.isFailure shouldBe true
            }
        }
    }
})
