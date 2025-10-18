package me.windy.demo.order.adapter.incoming.cli

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.windy.demo.order.core.application.usecase.OrderPlacementHandler
import me.windy.demo.order.core.fakes.FakeDomainEventPublisher
import me.windy.demo.order.core.fakes.FakeOrderRepository
import me.windy.demo.order.core.fakes.FakeStockAvailabilityChecker
import me.windy.demo.order.core.port.incoming.PlaceOrderUseCase

/**
 * Tests for CLI adapter.
 * Demonstrates that CLI uses the SAME use case as HTTP adapter.
 */
class OrderCLITest : StringSpec({

    "CLI adapter should use PlaceOrderUseCase to place orders" {
        // Setup: Use the SAME components as HTTP adapter
        val repository = FakeOrderRepository()
        val stockChecker = FakeStockAvailabilityChecker(availableStock = true)
        val eventPublisher = FakeDomainEventPublisher()

        val useCase: PlaceOrderUseCase = OrderPlacementHandler(repository, stockChecker, eventPublisher)

        // CLI adapter uses the SAME use case!
        val cli = OrderCLI(useCase)

        // Execute via CLI
        val result = cli.placeSimpleOrder("TEST-SKU", 2, 10.0)

        // Verify it worked
        result shouldContain "Order placed"
        repository.savedOrders.size shouldBe 1

        // Key Point: Same domain logic executed, different UI (CLI vs HTTP)
    }
})
