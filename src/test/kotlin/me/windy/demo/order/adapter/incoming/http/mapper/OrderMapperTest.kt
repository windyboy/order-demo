package me.windy.demo.order.adapter.incoming.http.mapper

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import me.windy.demo.order.adapter.incoming.http.dto.PlaceOrderRequest
import me.windy.demo.order.core.domain.Money
import me.windy.demo.order.core.domain.OrderId
import java.math.BigDecimal

/**
 * Tests for OrderMapper.
 * Verifies correct mapping between HTTP DTOs and Domain objects.
 */
class OrderMapperTest : DescribeSpec({

    val mapper = OrderMapper()

    describe("toDomain") {
        it("should map PlaceOrderRequest.Item to OrderItem correctly") {
            val dto =
                PlaceOrderRequest.Item(
                    sku = "SKU-001",
                    unitPrice = BigDecimal("10.50"),
                    quantity = 3,
                )

            val orderItem = mapper.toDomain(dto)

            orderItem.sku shouldBe "SKU-001"
            orderItem.unitPrice shouldBe Money.of("10.50")
            orderItem.quantity shouldBe 3
        }

        it("should handle decimal precision correctly") {
            val dto =
                PlaceOrderRequest.Item(
                    sku = "SKU-002",
                    unitPrice = BigDecimal("99.99"),
                    quantity = 1,
                )

            val orderItem = mapper.toDomain(dto)

            orderItem.unitPrice shouldBe Money.of("99.99")
            orderItem.subtotal() shouldBe Money.of("99.99")
        }
    }

    describe("toCommand") {
        it("should map PlaceOrderRequest to PlaceOrderCommand") {
            val request =
                PlaceOrderRequest(
                    items =
                        listOf(
                            PlaceOrderRequest.Item("SKU-001", BigDecimal("10.00"), 2),
                            PlaceOrderRequest.Item("SKU-002", BigDecimal("5.00"), 3),
                        ),
                )

            val command = mapper.toCommand(request)

            command.items shouldHaveSize 2
            command.items[0].sku shouldBe "SKU-001"
            command.items[0].quantity shouldBe 2
            command.items[1].sku shouldBe "SKU-002"
            command.items[1].quantity shouldBe 3
        }

        it("should handle single item requests") {
            val request =
                PlaceOrderRequest(
                    items =
                        listOf(
                            PlaceOrderRequest.Item("SINGLE-SKU", BigDecimal("25.00"), 1),
                        ),
                )

            val command = mapper.toCommand(request)

            command.items shouldHaveSize 1
            command.items[0].sku shouldBe "SINGLE-SKU"
        }
    }

    describe("toResponse") {
        it("should map OrderId to PlaceOrderResponse") {
            val orderId = OrderId.generate()

            val response = mapper.toResponse(orderId)

            response.orderId shouldBe orderId.value
        }

        it("should preserve order ID value exactly") {
            val customOrderId = OrderId.of("custom-order-123")

            val response = mapper.toResponse(customOrderId)

            response.orderId shouldBe "custom-order-123"
        }
    }

    describe("round-trip mapping") {
        it("should preserve data through request -> command -> domain flow") {
            val originalRequest =
                PlaceOrderRequest(
                    items =
                        listOf(
                            PlaceOrderRequest.Item("TEST-SKU", BigDecimal("15.75"), 5),
                        ),
                )

            val command = mapper.toCommand(originalRequest)
            val orderItem = command.items.first()

            // Verify data integrity
            orderItem.sku shouldBe originalRequest.items.first().sku
            orderItem.unitPrice.amount shouldBe originalRequest.items.first().unitPrice
            orderItem.quantity shouldBe originalRequest.items.first().quantity
            orderItem.subtotal() shouldBe Money.of("78.75") // 15.75 * 5
        }
    }
})
