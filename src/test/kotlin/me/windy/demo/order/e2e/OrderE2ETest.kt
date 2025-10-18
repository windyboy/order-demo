package me.windy.demo.order.e2e

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import me.windy.demo.order.adapter.incoming.http.dto.PlaceOrderRequest
import java.math.BigDecimal

/**
 * End-to-end tests with full application context.
 * Tests the entire flow from HTTP to domain and back.
 * Uses the new ApiResponse wrapper format.
 */
@MicronautTest
class OrderE2ETest(
    @Client("/") private val client: HttpClient,
) : StringSpec({

        "should place order via HTTP endpoint" {
            val request =
                PlaceOrderRequest(
                    items =
                        listOf(
                            PlaceOrderRequest.Item(
                                sku = "APPLE-001",
                                unitPrice = BigDecimal("5.00"),
                                quantity = 2,
                            ),
                            PlaceOrderRequest.Item(
                                sku = "BANANA-001",
                                unitPrice = BigDecimal("3.00"),
                                quantity = 3,
                            ),
                        ),
                )

            // Use String to avoid deserialization issues, then parse
            val response =
                client.toBlocking().exchange(
                    HttpRequest.POST("/orders", request),
                    String::class.java,
                )

            response.status shouldBe HttpStatus.CREATED
            val body = response.body()
            body shouldNotBe null
            body!!.contains("\"success\":true") shouldBe true
            body.contains("\"orderId\":") shouldBe true
        }

        "should return error for empty order" {
            val request = PlaceOrderRequest(items = emptyList())

            try {
                client.toBlocking().exchange(
                    HttpRequest.POST("/orders", request),
                    String::class.java,
                )
                // Should not reach here
                throw AssertionError("Expected exception but request succeeded")
            } catch (e: io.micronaut.http.client.exceptions.HttpClientResponseException) {
                // Should throw exception with 4xx or 5xx status code
                (
                    e.status == HttpStatus.BAD_REQUEST ||
                        e.status == HttpStatus.CONFLICT ||
                        e.status == HttpStatus.INTERNAL_SERVER_ERROR
                ) shouldBe true
            }
        }

        "should return health status" {
            val response =
                client.toBlocking().exchange(
                    HttpRequest.GET<Any>("/orders/health"),
                    String::class.java,
                )

            response.status shouldBe HttpStatus.OK
            val body = response.body()
            body shouldNotBe null
            body!!.contains("\"success\":true") shouldBe true
            body.contains("\"status\":\"healthy\"") shouldBe true
            body.contains("\"service\":\"order\"") shouldBe true
        }
    })
