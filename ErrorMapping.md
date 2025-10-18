# Error Mapping Guide

This document describes the mapping between error types and HTTP response status codes in the order service.

## Error Type Hierarchy

```
OrderError (sealed class)
├── InvalidOrder           - Invalid order data
├── InvalidState           - Illegal state transition
├── DomainViolation        - Domain rule violation
├── InsufficientStock      - Insufficient stock
└── OrderPlacementFailed   - Order placement failed (system error)
```

## HTTP Status Code Mapping

| Error Type | Error Code | HTTP Status | Scenario |
|---------|--------|-----------|------|
| `InvalidOrder` | `INVALID_ORDER` | **400 Bad Request** | Order data validation failed (empty list, negative price, etc.) |
| `InvalidState` | `INVALID_STATE` | **400 Bad Request** | Illegal state transition (e.g., CANCELLED→CONFIRMED) |
| `DomainViolation` | `DOMAIN_VIOLATION` | **400 Bad Request** | Domain invariant violation (e.g., order must have at least one item) |
| `InsufficientStock` | `INSUFFICIENT_STOCK` | **409 Conflict** | One or more items out of stock |
| `OrderPlacementFailed` | `ORDER_PLACEMENT_FAILED` | **500 Internal Server Error** | System failure (database, message queue, etc.) |
| DTO Validation Error | `VALIDATION_ERROR` | **400 Bad Request** | Bean Validation failure (handled automatically by framework) |

## Error Response Format

All error responses use a unified `ApiResponse<T>` wrapper:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "INSUFFICIENT_STOCK",
    "message": "Items out of stock: SKU-003",
    "details": ["SKU-003"]
  },
  "timestamp": "2025-10-17T10:30:00Z"
}
```

## Detailed Scenario Descriptions

### 1. InvalidOrder (400)

**Trigger Conditions**:
- Order item list is empty
- Item quantity is negative or zero (though intercepted by DTO validation)
- Other business rule validation failures

**示例请求**：
```json
{
  "items": [],
  "requestId": "req-001"
}
```

**Example Response**:
```json
{
  "success": false,
  "error": {
    "code": "INVALID_ORDER",
    "message": "Order must contain at least one item"
  }
}
```

### 2. InvalidState (400)

**Trigger Conditions**:
- Attempting to transition from terminal state (CANCELLED, DELIVERED) to another state
- Skipping necessary intermediate states (e.g., NEW directly to SHIPPED)

**Example**:
```kotlin
val order = Order.create(items).cancel().getOrThrow()
order.confirm() // Failure: CANCELLED is a terminal state
```

**Example Response**:
```json
{
  "success": false,
  "error": {
    "code": "INVALID_STATE",
    "message": "Cannot transition order 12345 from CANCELLED to CONFIRMED",
    "details": {
      "currentState": "CANCELLED",
      "targetState": "CONFIRMED"
    }
  }
}
```

### 3. DomainViolation (400)

**Trigger Conditions**:
- Money amount is negative (checked during Money initialization)
- Order total calculation exception
- Other domain invariant violations

**Example Response**:
```json
{
  "success": false,
  "error": {
    "code": "DOMAIN_VIOLATION",
    "message": "Money amount cannot be negative: -10.00"
  }
}
```

### 4. InsufficientStock (409)

**Trigger Conditions**:
- One or more SKUs out of stock
- Inventory service unavailable returning error

**Example Request**:
```json
{
  "items": [
    { "sku": "SKU-003", "unitPrice": 10.00, "quantity": 5 }
  ],
  "requestId": "req-002"
}
```

**Example Response**:
```json
{
  "success": false,
  "error": {
    "code": "INSUFFICIENT_STOCK",
    "message": "Items out of stock: SKU-003",
    "details": ["SKU-003"]
  }
}
```

### 5. OrderPlacementFailed (500)

**Trigger Conditions**:
- Database save failure
- Event publishing failure
- Other system-level errors

**Example Response**:
```json
{
  "success": false,
  "error": {
    "code": "ORDER_PLACEMENT_FAILED",
    "message": "Failed to publish domain events: Connection refused"
  }
}
```

### 6. DTO Validation Errors (400)

**Trigger Conditions** (Automatically handled by Bean Validation):
- SKU is empty or blank
- Unit price is negative
- Quantity ≤ 0
- requestId is empty (if marked as required)

**Example Request**:
```json
{
  "items": [
    { "sku": "", "unitPrice": -10.00, "quantity": 0 }
  ]
}
```

**Error Response Automatically Returned by Micronaut**:
```json
{
  "_embedded": {
    "errors": [
      {
        "message": "SKU is required"
      },
      {
        "message": "Unit price must be zero or positive"
      },
      {
        "message": "Quantity must be positive"
      }
    ]
  },
  "message": "Bad Request"
}
```

## Result Type and Exception Collaboration

### Result-Based Error Handling

All business logic uses the `Result<T>` type:

```kotlin
fun placeOrder(items: List<OrderItem>): Result<OrderId> {
    if (items.isEmpty()) {
        return Result.failure(OrderError.InvalidOrder("..."))
    }
    
    // Business logic
    return Result.success(orderId)
}
```

### Controller Layer Error Mapping

Controller handles errors uniformly through `toErrorResponse()`:

```kotlin
result.fold(
    onSuccess = { orderId -> HttpResponse.created(...) },
    onFailure = { throwable -> toErrorResponse(throwable, requestId) }
)
```

## Logging Standards

Each error type logs at the appropriate level:

| Error Type | Log Level | Included Information |
|---------|---------|---------|
| `InvalidOrder` | **WARN** | requestId, error details |
| `InvalidState` | **WARN** | requestId, currentState, targetState |
| `DomainViolation` | **WARN** | requestId, violated rules |
| `InsufficientStock` | **WARN** | requestId, unavailable SKU list |
| `OrderPlacementFailed` | **ERROR** | requestId, exception stack trace |

**Log Examples**:
```
2025-10-17 10:30:15 [WARN ] OrderController - Failed to place order - insufficient stock: requestId=req-002, items=[SKU-003]
2025-10-17 10:31:20 [ERROR] OrderController - Failed to place order - system error: requestId=req-003, error=Failed to publish domain events: Connection refused
```

## Client Handling Recommendations

### Retriable Errors

The following errors can be retried:
- **500 Internal Server Error** (`ORDER_PLACEMENT_FAILED`) - Temporary system failure
- **409 Conflict** (`INSUFFICIENT_STOCK`) - May retry after inventory recovery

### Non-Retriable Errors

The following errors should be corrected before retrying:
- **400 Bad Request** - All validation errors and domain rule violations

### Example Client Code

```typescript
async function placeOrder(request: PlaceOrderRequest): Promise<OrderId> {
  try {
    const response = await fetch('/orders', {
      method: 'POST',
      body: JSON.stringify(request)
    });
    
    if (!response.ok) {
      const error = await response.json();
      
      switch (error.error.code) {
        case 'INSUFFICIENT_STOCK':
          // Show out-of-stock notification
          showAlert('部分商品库存不足', error.error.details);
          break;
        case 'INVALID_ORDER':
        case 'INVALID_STATE':
        case 'DOMAIN_VIOLATION':
          // Show validation error
          showAlert('请求参数错误', error.error.message);
          break;
        case 'ORDER_PLACEMENT_FAILED':
          // System error, retriable
          if (confirm('系统繁忙，是否重试？')) {
            return placeOrder(request); // Retry
          }
          break;
      }
    }
    
    return response.json();
  } catch (e) {
    // Network error handling
  }
}
```

## Extensibility

Adding New Error Types:

1. Add new sealed subclass in `OrderError.kt`
2. Add mapping in `OrderController.toErrorResponse()`
3. Update this document
4. Notify frontend team to update error handling logic

**示例**：
```kotlin
// 1. Define new error type
class PaymentFailed(
    message: String,
    val paymentId: String
) : OrderError(message, "PAYMENT_FAILED")

// 2. Map in Controller
is OrderError.PaymentFailed -> {
    log.error("Payment failed: orderId={}, paymentId={}", 
        orderId, error.paymentId)
    HttpResponse.status<ApiResponse<PlaceOrderResponse>>(HttpStatus.PAYMENT_REQUIRED)
        .body(ApiResponse.error(
            code = error.code,
            message = error.message
        ))
}
```

## References

- [RFC 7807 - Problem Details for HTTP APIs](https://tools.ietf.org/html/rfc7807)
- [Micronaut Error Handling Guide](https://docs.micronaut.io/latest/guide/#errorHandling)
- [HTTP Status Codes Best Practices](https://restfulapi.net/http-status-codes/)

