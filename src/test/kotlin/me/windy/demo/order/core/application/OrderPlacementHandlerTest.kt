package me.windy.demo.order.core.application

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import me.windy.demo.order.core.application.usecase.OrderPlacementHandler
import me.windy.demo.order.core.domain.Money
import me.windy.demo.order.core.domain.OrderError
import me.windy.demo.order.core.domain.OrderItem
import me.windy.demo.order.core.domain.event.OrderPlacedEvent
import me.windy.demo.order.core.fakes.FakeDomainEventPublisher
import me.windy.demo.order.core.fakes.FakeOrderRepository
import me.windy.demo.order.core.fakes.FakeStockAvailabilityChecker
import me.windy.demo.order.core.port.incoming.PlaceOrderCommand

/**
 * Application use-case tests using fake implementations.
 * Validates the orchestration pipeline exposed by [OrderPlacementHandler].
 */
class OrderPlacementHandlerTest : StringSpec({

    "should place order successfully when stock is available" {
        val repository = FakeOrderRepository()
        val stockChecker = FakeStockAvailabilityChecker(availableStock = true)
        val eventPublisher = FakeDomainEventPublisher()
        val useCase = OrderPlacementHandler(repository, stockChecker, eventPublisher)

        val items =
            listOf(
                OrderItem.of("apple", Money.of(5.0), 2),
            )

        val result = useCase.execute(PlaceOrderCommand(items))

        result.isSuccess shouldBe true
        repository.savedOrders shouldHaveSize 1
        eventPublisher.publishedEvents shouldHaveSize 1
        eventPublisher.publishedEvents.first().shouldBeInstanceOf<OrderPlacedEvent>()
    }

    "should fail when stock is insufficient" {
        val repository = FakeOrderRepository()
        val stockChecker = FakeStockAvailabilityChecker(availableStock = false)
        val eventPublisher = FakeDomainEventPublisher()
        val useCase = OrderPlacementHandler(repository, stockChecker, eventPublisher)

        val items =
            listOf(
                OrderItem.of("apple", Money.of(5.0), 2),
            )

        val result = useCase.execute(PlaceOrderCommand(items))

        result.isFailure shouldBe true
        result.exceptionOrNull().shouldBeInstanceOf<OrderError.InsufficientStock>()
        val error = result.exceptionOrNull() as OrderError.InsufficientStock
        error.code shouldBe "INSUFFICIENT_STOCK"
        error.unavailableItems shouldBe listOf("apple")
        repository.savedOrders shouldHaveSize 0
        eventPublisher.publishedEvents shouldHaveSize 0
    }

    "should fail when specific SKUs are out of stock" {
        val repository = FakeOrderRepository()
        val stockChecker =
            FakeStockAvailabilityChecker(
                availableStock = true,
                unavailableSkus = setOf("banana"),
            )
        val eventPublisher = FakeDomainEventPublisher()
        val useCase = OrderPlacementHandler(repository, stockChecker, eventPublisher)

        val items =
            listOf(
                OrderItem.of("apple", Money.of(5.0), 2),
                OrderItem.of("banana", Money.of(3.0), 1),
            )

        val result = useCase.execute(PlaceOrderCommand(items))

        result.isFailure shouldBe true
        result.exceptionOrNull().shouldBeInstanceOf<OrderError.InsufficientStock>()
        val error = result.exceptionOrNull() as OrderError.InsufficientStock
        error.unavailableItems shouldBe listOf("banana")
    }

    "should validate all items before creating order" {
        val repository = FakeOrderRepository()
        val stockChecker = FakeStockAvailabilityChecker(availableStock = true)
        val eventPublisher = FakeDomainEventPublisher()
        val useCase = OrderPlacementHandler(repository, stockChecker, eventPublisher)

        val items =
            listOf(
                OrderItem.of("apple", Money.of(5.0), 2),
                OrderItem.of("banana", Money.of(3.0), 1),
            )

        val result = useCase.execute(PlaceOrderCommand(items))

        result.isSuccess shouldBe true
        stockChecker.checkedItems shouldHaveSize 2
        stockChecker.reservedItems shouldHaveSize 2
    }

    "should handle empty items list gracefully" {
        val repository = FakeOrderRepository()
        val stockChecker = FakeStockAvailabilityChecker(availableStock = true)
        val eventPublisher = FakeDomainEventPublisher()
        val useCase = OrderPlacementHandler(repository, stockChecker, eventPublisher)

        val result = useCase.execute(PlaceOrderCommand(emptyList()))

        result.isFailure shouldBe true
        result.exceptionOrNull().shouldBeInstanceOf<OrderError.InvalidOrder>()
    }

    "should wrap repository failure in OrderPlacementFailed" {
        val repository = FakeOrderRepository(shouldFailSave = true)
        val stockChecker = FakeStockAvailabilityChecker(availableStock = true)
        val eventPublisher = FakeDomainEventPublisher()
        val useCase = OrderPlacementHandler(repository, stockChecker, eventPublisher)

        val items =
            listOf(
                OrderItem.of("apple", Money.of(5.0), 2),
            )

        val result = useCase.execute(PlaceOrderCommand(items))

        result.isFailure shouldBe true
        result.exceptionOrNull().shouldBeInstanceOf<OrderError.OrderPlacementFailed>()
        // Stock was checked but order not saved
        stockChecker.checkedItems shouldHaveSize 1
        repository.savedOrders shouldHaveSize 0
        eventPublisher.publishedEvents shouldHaveSize 0
    }

    "should surface event publisher failure" {
        val repository = FakeOrderRepository()
        val stockChecker = FakeStockAvailabilityChecker(availableStock = true)
        val eventPublisher = FakeDomainEventPublisher(shouldFail = true)
        val useCase = OrderPlacementHandler(repository, stockChecker, eventPublisher)

        val items =
            listOf(
                OrderItem.of("apple", Money.of(5.0), 2),
            )

        val result = useCase.execute(PlaceOrderCommand(items))

        result.isFailure shouldBe true
        result.exceptionOrNull().shouldBeInstanceOf<OrderError.OrderPlacementFailed>()
    }
})
