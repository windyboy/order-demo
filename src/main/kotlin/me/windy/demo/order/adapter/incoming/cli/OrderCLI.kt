package me.windy.demo.order.adapter.incoming.cli

import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton
import me.windy.demo.order.core.domain.Money
import me.windy.demo.order.core.domain.OrderItem
import me.windy.demo.order.core.port.incoming.PlaceOrderCommand
import me.windy.demo.order.core.port.incoming.PlaceOrderUseCase
import org.slf4j.LoggerFactory

/**
 * Command-Line Interface adapter for the order service.
 *
 * ## HEXAGONAL ARCHITECTURE DEMONSTRATION:
 * This is an **alternative INBOUND ADAPTER** that uses the SAME use case as the HTTP adapter!
 *
 * **Key Adapter Swapping Concept:**
 * - ✅ **Same Core Logic**: Uses PlaceOrderUseCase (same as OrderController)
 * - ✅ **Different UI**: CLI instead of HTTP REST API
 * - ✅ **Zero Core Changes**: Domain/Application layers unchanged
 * - ✅ **Proves Ports & Adapters**: This is the essence of hexagonal architecture!
 *
 * **How to Run:**
 * ```bash
 * ./gradlew run --args="cli demo"
 * ```
 *
 * **Architecture Benefit:**
 * - Want HTTP? Use OrderController
 * - Want CLI? Use OrderCLI
 * - Want GraphQL? Add GraphQLAdapter
 * - Want gRPC? Add GrpcAdapter
 *
 * **Core business logic NEVER changes** - that's the power of ports!
 *
 * @see me.windy.demo.order.adapter.incoming.http.OrderController for HTTP adapter
 */
@Singleton
@Requires(property = "adapter.cli.enabled", value = "true", defaultValue = "false")
class OrderCLI(
    private val placeOrderUseCase: PlaceOrderUseCase,
) {
    private val log = LoggerFactory.getLogger(OrderCLI::class.java)

    companion object {
        // Demo sample data constants
        private const val DEMO_APPLE_PRICE = 5.99
        private const val DEMO_APPLE_QUANTITY = 3
        private const val DEMO_BANANA_PRICE = 2.49
        private const val DEMO_BANANA_QUANTITY = 5
        private const val DEMO_ORANGE_PRICE = 4.50
        private const val DEMO_ORANGE_QUANTITY = 2
    }

    /**
     * Runs an interactive CLI demo of the order service.
     */
    fun runDemo() {
        println()
        println("╔════════════════════════════════════════════════════════════════╗")
        println("║          Order Service - CLI Adapter Demo                     ║")
        println("║          Hexagonal Architecture in Action                     ║")
        println("╚════════════════════════════════════════════════════════════════╝")
        println()
        println("📦 Creating sample order...")
        println()

        // Create order items (same domain objects as HTTP adapter!)
        val items =
            listOf(
                OrderItem.of("APPLE-001", Money.of(DEMO_APPLE_PRICE), DEMO_APPLE_QUANTITY),
                OrderItem.of("BANANA-002", Money.of(DEMO_BANANA_PRICE), DEMO_BANANA_QUANTITY),
                OrderItem.of("ORANGE-003", Money.of(DEMO_ORANGE_PRICE), DEMO_ORANGE_QUANTITY),
            )

        // Display order details
        println("🛒 Order Items:")
        items.forEach { item ->
            val subtotal = item.subtotal()
            println("   • ${item.sku}: $${item.unitPrice.amount} × ${item.quantity} = $${subtotal.amount}")
        }

        val total = items.fold(Money.ZERO) { acc, item -> acc + item.subtotal() }
        println("   ─────────────────────────────────")
        println("   💰 Total: $${total.amount}")
        println()

        // Place order via USE CASE (same as HTTP adapter!)
        println("🚀 Placing order via PlaceOrderUseCase...")
        val command = PlaceOrderCommand(items = items, requestId = "CLI-DEMO-${System.currentTimeMillis()}")

        val result = placeOrderUseCase.execute(command)

        println()
        result.fold(
            onSuccess = { orderId ->
                println("✅ SUCCESS!")
                println("   Order ID: ${orderId.value}")
                println()
                println("🎯 Architecture Insight:")
                println("   This CLI adapter used the SAME use case as the HTTP REST API!")
                println("   - Same domain logic")
                println("   - Same validation")
                println("   - Same business rules")
                println("   - Different UI (CLI vs HTTP)")
                println()
                println("   That's hexagonal architecture - swap adapters without touching core!")
                log.info("CLI demo order placed successfully: orderId={}", orderId.value)
            },
            onFailure = { error ->
                println("❌ FAILED!")
                println("   Error: ${error.message}")
                println()
                log.error("CLI demo order failed: {}", error.message, error)
            },
        )

        println()
        println("╔════════════════════════════════════════════════════════════════╗")
        println("║  Try the HTTP adapter too: POST http://localhost:8080/orders  ║")
        println("║  Same logic, different adapter - that's the power of ports!   ║")
        println("╚════════════════════════════════════════════════════════════════╝")
        println()
    }

    /**
     * Places a single order with given SKU and quantity.
     * Useful for scripting and automation.
     */
    fun placeSimpleOrder(
        sku: String,
        quantity: Int,
        unitPrice: Double,
    ): String {
        val items = listOf(OrderItem.of(sku, Money.of(unitPrice), quantity))
        val command = PlaceOrderCommand(items = items)

        return placeOrderUseCase.execute(command).fold(
            onSuccess = { orderId ->
                "✓ Order placed: ${orderId.value}"
            },
            onFailure = { error ->
                "✗ Error: ${error.message}"
            },
        )
    }
}
