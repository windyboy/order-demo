package me.windy.demo.order.adapter.incoming.http.dto

import io.micronaut.serde.annotation.Serdeable
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import java.math.BigDecimal

/**
 * HTTP request DTO for placing an order.
 * Includes validation constraints to ensure data integrity.
 */
@Serdeable
data class PlaceOrderRequest(
    val items: List<Item>,
    val requestId: String? = null
) {
    @Serdeable
    data class Item(
        val sku: String,
        val unitPrice: BigDecimal,
        val quantity: Int
    )
}

/**
 * HTTP response DTO for order placement result.
 */
@Serdeable
data class PlaceOrderResponse(
    val orderId: String
)

/**
 * HTTP error response DTO.
 */
data class ErrorResponse(
    val message: String,
    val code: String
)
