# Error Mapping Guide

本文档说明了订单服务中错误类型与HTTP响应状态码的映射关系。

## 错误类型层次结构

```
OrderError (sealed class)
├── InvalidOrder           - 订单数据无效
├── InvalidState           - 非法状态转换
├── DomainViolation        - 领域规则违反
├── InsufficientStock      - 库存不足
└── OrderPlacementFailed   - 订单创建失败（系统错误）
```

## HTTP 状态码映射

| 错误类型 | 错误码 | HTTP 状态 | 场景 |
|---------|--------|-----------|------|
| `InvalidOrder` | `INVALID_ORDER` | **400 Bad Request** | 订单数据验证失败（空列表、负价格等） |
| `InvalidState` | `INVALID_STATE` | **400 Bad Request** | 状态转换不合法（如CANCELLED→CONFIRMED） |
| `DomainViolation` | `DOMAIN_VIOLATION` | **400 Bad Request** | 违反领域不变式（如订单必须至少有一个商品） |
| `InsufficientStock` | `INSUFFICIENT_STOCK` | **409 Conflict** | 一个或多个商品库存不足 |
| `OrderPlacementFailed` | `ORDER_PLACEMENT_FAILED` | **500 Internal Server Error** | 系统故障（数据库、消息队列等） |
| DTO Validation Error | `VALIDATION_ERROR` | **400 Bad Request** | Bean Validation失败（由框架自动处理） |

## 错误响应格式

所有错误响应使用统一的 `ApiResponse<T>` 包装器：

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

## 详细场景说明

### 1. InvalidOrder (400)

**触发条件**：
- 订单商品列表为空
- 商品数量为负数或零（虽然会被DTO验证拦截）
- 其他业务规则验证失败

**示例请求**：
```json
{
  "items": [],
  "requestId": "req-001"
}
```

**示例响应**：
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

**触发条件**：
- 尝试从终态（CANCELLED, DELIVERED）转换到其他状态
- 跳过必要的中间状态（如NEW直接到SHIPPED）

**示例**：
```kotlin
val order = Order.create(items).cancel().getOrThrow()
order.confirm() // 失败：CANCELLED是终态
```

**示例响应**：
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

**触发条件**：
- Money金额为负数（在Money初始化时检查）
- 订单总金额计算异常
- 其他领域不变式违反

**示例响应**：
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

**触发条件**：
- 一个或多个SKU库存不足
- 库存服务不可用返回错误

**示例请求**：
```json
{
  "items": [
    { "sku": "SKU-003", "unitPrice": 10.00, "quantity": 5 }
  ],
  "requestId": "req-002"
}
```

**示例响应**：
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

**触发条件**：
- 数据库保存失败
- 事件发布失败
- 其他系统级错误

**示例响应**：
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

**触发条件** (由 Bean Validation 自动处理)：
- SKU为空或空白
- 单价为负数
- 数量≤0
- requestId为空（如果标记为必填）

**示例请求**：
```json
{
  "items": [
    { "sku": "", "unitPrice": -10.00, "quantity": 0 }
  ]
}
```

**Micronaut自动返回的错误响应**：
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

## Result类型与异常的协作

### Result-Based Error Handling

所有业务逻辑使用 `Result<T>` 类型：

```kotlin
fun placeOrder(items: List<OrderItem>): Result<OrderId> {
    if (items.isEmpty()) {
        return Result.failure(OrderError.InvalidOrder("..."))
    }
    
    // 业务逻辑
    return Result.success(orderId)
}
```

### Controller层错误映射

Controller通过 `toErrorResponse()` 统一处理：

```kotlin
result.fold(
    onSuccess = { orderId -> HttpResponse.created(...) },
    onFailure = { throwable -> toErrorResponse(throwable, requestId) }
)
```

## 日志记录规范

每个错误类型都会记录相应级别的日志：

| 错误类型 | 日志级别 | 包含信息 |
|---------|---------|---------|
| `InvalidOrder` | **WARN** | requestId, 错误详情 |
| `InvalidState` | **WARN** | requestId, currentState, targetState |
| `DomainViolation` | **WARN** | requestId, 违反的规则 |
| `InsufficientStock` | **WARN** | requestId, 不可用SKU列表 |
| `OrderPlacementFailed` | **ERROR** | requestId, 异常堆栈 |

**日志示例**：
```
2025-10-17 10:30:15 [WARN ] OrderController - Failed to place order - insufficient stock: requestId=req-002, items=[SKU-003]
2025-10-17 10:31:20 [ERROR] OrderController - Failed to place order - system error: requestId=req-003, error=Failed to publish domain events: Connection refused
```

## 客户端处理建议

### 可重试错误

以下错误可以重试：
- **500 Internal Server Error** (`ORDER_PLACEMENT_FAILED`) - 临时系统故障
- **409 Conflict** (`INSUFFICIENT_STOCK`) - 可能库存恢复后重试

### 不可重试错误

以下错误应该修正请求后再尝试：
- **400 Bad Request** - 所有验证错误和领域规则违反

### 示例客户端代码

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
          // 显示缺货提示
          showAlert('部分商品库存不足', error.error.details);
          break;
        case 'INVALID_ORDER':
        case 'INVALID_STATE':
        case 'DOMAIN_VIOLATION':
          // 显示验证错误
          showAlert('请求参数错误', error.error.message);
          break;
        case 'ORDER_PLACEMENT_FAILED':
          // 系统错误，可重试
          if (confirm('系统繁忙，是否重试？')) {
            return placeOrder(request); // 重试
          }
          break;
      }
    }
    
    return response.json();
  } catch (e) {
    // 网络错误处理
  }
}
```

## 扩展性

添加新的错误类型：

1. 在 `OrderError.kt` 中添加新的sealed子类
2. 在 `OrderController.toErrorResponse()` 中添加映射
3. 更新本文档
4. 通知前端团队更新错误处理逻辑

**示例**：
```kotlin
// 1. 定义新错误类型
class PaymentFailed(
    message: String,
    val paymentId: String
) : OrderError(message, "PAYMENT_FAILED")

// 2. 在Controller中映射
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

## 参考资料

- [RFC 7807 - Problem Details for HTTP APIs](https://tools.ietf.org/html/rfc7807)
- [Micronaut Error Handling Guide](https://docs.micronaut.io/latest/guide/#errorHandling)
- [HTTP Status Codes Best Practices](https://restfulapi.net/http-status-codes/)

