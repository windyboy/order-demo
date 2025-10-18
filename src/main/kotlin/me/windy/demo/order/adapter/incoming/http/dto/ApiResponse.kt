package me.windy.demo.order.adapter.incoming.http.dto

import io.micronaut.serde.annotation.Serdeable

/**
 * Generic API response wrapper.
 * Provides consistent response structure for both success and error cases.
 *
 * @param T The type of data in successful responses
 * @param data The actual response data (null on error)
 * @param error Error details (null on success)
 * @param meta Optional metadata (pagination, timestamps, etc.)
 */
@Serdeable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorDetails? = null,
    val meta: Map<String, Any>? = null,
) {
    /**
     * Error details for failed requests.
     */
    @Serdeable
    data class ErrorDetails(
        val code: String,
        val message: String,
        val details: List<String>? = null,
    )

    companion object {
        /**
         * Creates a successful response with data.
         */
        fun <T> success(
            data: T,
            meta: Map<String, Any>? = null,
        ): ApiResponse<T> {
            return ApiResponse(
                success = true,
                data = data,
                error = null,
                meta = meta,
            )
        }

        /**
         * Creates an error response.
         */
        fun <T> error(
            code: String,
            message: String,
            details: List<String>? = null,
        ): ApiResponse<T> {
            return ApiResponse(
                success = false,
                data = null,
                error = ErrorDetails(code, message, details),
                meta = null,
            )
        }
    }
}
