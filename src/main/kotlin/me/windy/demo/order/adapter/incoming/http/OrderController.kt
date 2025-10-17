package me.windy.demo.order.adapter.incoming.http

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import jakarta.validation.Valid
import me.windy.demo.order.adapter.incoming.http.dto.ApiResponse
import me.windy.demo.order.adapter.incoming.http.dto.PlaceOrderRequest
import me.windy.demo.order.adapter.incoming.http.dto.PlaceOrderResponse
import me.windy.demo.order.adapter.incoming.http.mapper.OrderMapper
import me.windy.demo.order.core.domain.OrderError
import me.windy.demo.order.core.port.incoming.PlaceOrderUseCase
import org.slf4j.LoggerFactory

/**
 * HTTP adapter for order operations.
 * Translates HTTP requests to use case commands and domain responses back to HTTP.
 * Only depends on Port layer (PlaceOrderUseCase) and Domain layer (OrderError).
 * Uses standardized ApiResponse wrapper for consistent API responses.
 */
@Controller("/orders")
class OrderController(
    private val placeOrderUseCase: PlaceOrderUseCase,
    private val mapper: OrderMapper
) {
    
    private val log = LoggerFactory.getLogger(OrderController::class.java)

    /**
     * Places a new order via HTTP POST.
     * Request body is validated using Bean Validation constraints.
     * Returns 201 Created on success with order ID in ApiResponse wrapper.
     * Returns appropriate error status with error details in ApiResponse wrapper:
     * - 400 Bad Request for validation errors
     * - 409 Conflict for insufficient stock
     * - 500 Internal Server Error for system failures
     */
    @Post
    fun place(@Body request: PlaceOrderRequest): HttpResponse<ApiResponse<PlaceOrderResponse>> {
        val requestId = request.requestId ?: "unknown"
        log.info("Received place order request: requestId={}, items={}", requestId, request.items.size)
        
        val command = mapper.toCommand(request)
        val result = placeOrderUseCase.execute(command)
        
        return result.fold(
            onSuccess = { orderId ->
                log.info("Order placed successfully: orderId={}, requestId={}", orderId.value, requestId)
                val response = mapper.toResponse(orderId)
                HttpResponse.status<ApiResponse<PlaceOrderResponse>>(HttpStatus.CREATED)
                    .body(ApiResponse.success(response))
            },
            onFailure = { throwable ->
                toErrorResponse(throwable, requestId)
            }
        )
    }
    
    /**
     * Maps domain errors to HTTP responses.
     * Centralizes error response mapping logic.
     */
    private fun toErrorResponse(
        throwable: Throwable,
        requestId: String
    ): HttpResponse<ApiResponse<PlaceOrderResponse>> {
        return when (val error = throwable as? OrderError) {
            is OrderError.InsufficientStock -> {
                log.warn("Failed to place order - insufficient stock: requestId={}, items={}", 
                    requestId, error.unavailableItems)
                HttpResponse.status<ApiResponse<PlaceOrderResponse>>(HttpStatus.CONFLICT)
                    .body(
                        ApiResponse.error(
                            code = error.code,
                            message = error.message,
                            details = error.unavailableItems
                        )
                    )
            }
            is OrderError.InvalidOrder -> {
                log.warn("Failed to place order - invalid request: requestId={}, error={}", 
                    requestId, error.message)
                HttpResponse.badRequest(
                    ApiResponse.error<PlaceOrderResponse>(
                        code = error.code,
                        message = error.message
                    )
                )
            }
            is OrderError.InvalidState -> {
                log.warn("Failed to place order - invalid state: requestId={}, currentState={}, targetState={}", 
                    requestId, error.currentState, error.targetState)
                HttpResponse.badRequest(
                    ApiResponse.error<PlaceOrderResponse>(
                        code = error.code,
                        message = error.message
                    )
                )
            }
            is OrderError.DomainViolation -> {
                log.warn("Failed to place order - domain violation: requestId={}, error={}", 
                    requestId, error.message)
                HttpResponse.badRequest(
                    ApiResponse.error<PlaceOrderResponse>(
                        code = error.code,
                        message = error.message
                    )
                )
            }
            is OrderError.OrderPlacementFailed -> {
                log.error("Failed to place order - system error: requestId={}, error={}", 
                    requestId, error.message, error.cause)
                HttpResponse.serverError(
                    ApiResponse.error<PlaceOrderResponse>(
                        code = error.code,
                        message = error.message
                    )
                )
            }
            else -> {
                log.error("Unexpected error during order placement: requestId={}", requestId, throwable)
                HttpResponse.serverError(
                    ApiResponse.error<PlaceOrderResponse>(
                        code = "INTERNAL_ERROR",
                        message = "An unexpected error occurred"
                    )
                )
            }
        }
    }

    /**
     * Health check endpoint.
     */
    @Get("/health")
    fun health(): HttpResponse<ApiResponse<Map<String, String>>> {
        return HttpResponse.ok(
            ApiResponse.success(mapOf("status" to "healthy", "service" to "order"))
        )
    }
}
