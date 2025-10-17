# 🏛️ 架构设计文档

## 📐 六边形架构图示

```
┌─────────────────────────────────────────────────────────────────┐
│                        外部世界 (External)                        │
│                                                                   │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐      │
│  │   HTTP Client │    │  Inventory   │    │   Message    │      │
│  │   (Browser)   │    │   Service    │    │    Queue     │      │
│  └───────┬──────┘    └──────┬───────┘    └──────┬───────┘      │
└──────────┼──────────────────┼────────────────────┼──────────────┘
           │                   │                    │
           │                   │                    │
┌──────────▼───────────────────▼────────────────────▼──────────────┐
│                      适配器层 (Adapters)                          │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  入站适配器 (Inbound)    │   出站适配器 (Outbound)           │ │
│  │  ┌────────────────┐      │   ┌──────────────────────────┐  │ │
│  │  │ OrderController│◄─────┼───│ StockAvailabilityChecker │  │ │
│  │  │                │      │   │   (Dummy)                │  │ │
│  │  │   + place()    │      │   └──────────────────────────┘  │ │
│  │  │   + health()   │      │   ┌──────────────────────────┐  │ │
│  │  └────────┬───────┘      │   │   OrderRepository        │  │ │
│  │           │              │   │   (InMemory)             │  │ │
│  │           │              │   └──────────────────────────┘  │ │
│  │           │              │   ┌──────────────────────────┐  │ │
│  │           │              │   │ OrderPlacedNotifier      │  │ │
│  │           │              │   │   (Logging)              │  │ │
│  │  ┌────────▼───────┐      │   └──────────────────────────┘  │ │
│  │  │   OrderMapper  │      │                                  │ │
│  │  │  (DTO ↔ Domain)│      │                                  │ │
│  │  └────────────────┘      │                                  │ │
│  └─────────────────────────────────────────────────────────────┘ │
└───────────────────────────┬───────────────────────────────────────┘
                            │
                            │ 端口 (Ports)
                            │
┌───────────────────────────▼───────────────────────────────────────┐
│                       核心层 (Core)                                │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                   Port 层 (接口定义)                          │ │
│  │  ┌──────────────────┐          ┌────────────────────────┐  │ │
│  │  │  PlaceOrderUseCase│◄─────────│StockAvailabilityChecker│  │ │
│  │  │   (In Port)       │          │    (Out Port)          │  │ │
│  │  └─────────┬─────────┘          └────────────────────────┘  │ │
│  │            │                    ┌────────────────────────┐  │ │
│  │            │                    │   OrderRepository      │  │ │
│  │            │                    │    (Out Port)          │  │ │
│  │            │                    └────────────────────────┘  │ │
│  │            │                    ┌────────────────────────┐  │ │
│  │            │                    │ OrderPlacedNotifier    │  │ │
│  │            │                    │    (Out Port)          │  │ │
│  │            │                    └────────────────────────┘  │ │
│  └────────────┼─────────────────────────────────────────────────┘ │
│               │                                                   │
│  ┌────────────▼─────────────────────────────────────────────────┐ │
│  │                 Application 层 (用例实现)                      │ │
│  │  ┌──────────────────┐         ┌────────────────────────┐    │ │
│  │  │PlaceOrderHandler │────────►│ PlaceOrderService      │    │ │
│  │  │  implements      │         │  (Business Logic)      │    │ │
│  │  │PlaceOrderUseCase │         └────────────────────────┘    │ │
│  │  └──────────────────┘                                        │ │
│  └───────────────────────────────────────────────────────────────┘ │
│               │                                                   │
│  ┌────────────▼─────────────────────────────────────────────────┐ │
│  │                    Domain 层 (领域模型)                       │ │
│  │  ┌──────────┐  ┌───────────┐  ┌───────┐  ┌───────────┐     │ │
│  │  │  Order   │  │ OrderItem │  │ Money │  │  OrderId  │     │ │
│  │  │(Aggregate)│  │  (Value)  │  │(Value)│  │  (Value)  │     │ │
│  │  └──────────┘  └───────────┘  └───────┘  └───────────┘     │ │
│  │                                                               │ │
│  │  特性:                                                        │ │
│  │  ✓ 私有构造函数 + 工厂方法                                    │ │
│  │  ✓ 强不变式验证                                              │ │
│  │  ✓ 无框架依赖                                                │ │
│  │  ✓ 纯业务逻辑                                                │ │
│  └───────────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────────────┘
```

## 🔄 数据流示例（下单流程）

```
1. HTTP Request ────────────────────────────────────────────┐
   POST /orders                                              │
   {                                                         │
     "items": [{"sku": "A", "unitPrice": 10, "quantity": 2}]│
   }                                                         │
                                                             │
2. OrderController (Adapter) ◄──────────────────────────────┘
   │
   ├─► OrderMapper.toCommand(request)
   │   └─► PlaceOrderCommand(items=[OrderItem(...)])
   │
3. PlaceOrderHandler (Application) ◄────────────────────────┐
   │  execute(command)                                       │
   │                                                         │
4. PlaceOrderService (Application) ◄────────────────────────┘
   │
   ├─► StockAvailabilityChecker.checkAndReserve(sku, qty)
   │   └─► return true  (库存充足)
   │
   ├─► Order.create(items)  (Domain 验证)
   │   └─► return Order(id, items)
   │
   ├─► OrderRepository.save(order)
   │   └─► 保存到内存
   │
   ├─► OrderPlacedNotifier.notify(order)
   │   └─► 记录日志
   │
   └─► return Result.success(orderId)
   
5. OrderController ◄────────────────────────────────────────┐
   │  result.fold(...)                                       │
   │                                                         │
6. HTTP Response ───────────────────────────────────────────┘
   201 Created
   {
     "orderId": "550e8400-e29b-41d4-a716-446655440000"
   }
```

## 🎯 依赖规则

```
┌─────────────────────────────────────────┐
│  外层依赖内层，内层不知道外层存在         │
└─────────────────────────────────────────┘

Adapter ──────► Port ──────► Application ──────► Domain
  (适配器)      (接口)       (用例)             (领域)

✓ Adapter 依赖 Port 和 Domain（但不依赖Application）
✓ Application 依赖 Port 和 Domain
✓ Port 依赖 Domain
✓ Domain 不依赖任何层
✗ Domain 不能依赖 Application/Port/Adapter
✗ Application 不能依赖 Adapter
✗ Adapter 不能直接依赖 Application（只能通过Port接口）
```

## 🧩 各层职责详解

### Domain 层 (核心域)
**责任**: 封装业务规则和不变式

**特性**:
- 无框架依赖
- 私有构造函数 + 工厂方法
- 强不变式验证
- 纯 Kotlin 代码

**示例**:
```kotlin
// ❌ 不好的做法
data class Order(val id: String, val items: List<OrderItem>, val total: BigDecimal)

// ✅ 好的做法
class Order private constructor(val id: OrderId, val items: List<OrderItem>) {
    init {
        require(items.isNotEmpty()) { "Order must contain at least one item" }
    }
    fun total(): Money = items.fold(Money.ZERO) { acc, item -> acc + item.subtotal() }
    companion object {
        fun create(items: List<OrderItem>): Order = Order(OrderId.generate(), items)
    }
}
```

### Port 层 (端口接口)
**责任**: 定义核心域与外部的交互契约

**入站端口 (In Port)**:
- 定义应用用例接口
- 面向业务语义命名
- 返回 Result 类型

**出站端口 (Out Port)**:
- 定义外部依赖接口
- 业务语义化命名 (非技术化)
- 单一职责原则

**命名对比**:
```kotlin
// ❌ 技术化命名
interface InventoryGateway
interface OrderEventPublisher

// ✅ 业务语义化命名
interface StockAvailabilityChecker
interface OrderPlacedNotifier
```

### Application 层 (应用服务)
**责任**: 编排业务流程，协调领域对象和外部依赖

**Handler**:
- 实现入站端口
- 用例入口点
- 薄层，仅转发

**Service**:
- 业务逻辑编排
- 调用领域对象
- 调用出站端口
- 返回 Result 类型

**错误处理**:
```kotlin
// ❌ 不好的做法 (抛异常)
fun placeOrder(items: List<OrderItem>): Order {
    if (!stockChecker.check()) throw InsufficientStockException()
    return order
}

// ✅ 好的做法 (返回 Result with OrderError)
fun placeOrder(items: List<OrderItem>): Result<OrderId> {
    if (!stockChecker.check()) return Result.failure(OrderError.InsufficientStock())
    return Result.success(order.id)
}
```

**领域错误类型**:
```kotlin
// Domain层定义错误类型
sealed interface OrderError {
    val message: String
    val code: String
    
    data class InsufficientStock(...) : OrderError
    data class InvalidOrder(...) : OrderError
}
```

### Adapter 层 (适配器)
**责任**: 连接外部技术与核心域

**入站适配器**:
- HTTP Controller
- 消息队列监听器
- 定时任务

**出站适配器**:
- 数据库实现
- 外部服务调用
- 消息发布

**Mapper**:
- DTO ↔ Domain 转换
- 保持层次解耦

**重要**:
- Adapter **只依赖** Port（接口）和 Domain（错误类型等）
- Adapter **不应该** 直接依赖 Application 层的具体实现
- 这保证了架构的依赖倒置原则

## 📊 测试策略

### 1. Domain 测试 (单元测试)
```kotlin
class OrderDomainTest : StringSpec({
    "should not allow empty items" {
        shouldThrow<IllegalArgumentException> {
            Order.create(emptyList())
        }
    }
})
```

### 2. Application 测试 (集成测试)
```kotlin
class PlaceOrderServiceTest : StringSpec({
    val repository = FakeOrderRepository()
    val stockChecker = FakeStockAvailabilityChecker(available = true)
    val notifier = FakeOrderPlacedNotifier()
    val service = PlaceOrderService(repository, stockChecker, notifier)
    
    "should place order successfully" {
        val result = service.placeOrder(items)
        result.isSuccess shouldBe true
    }
})
```

### 3. E2E 测试 (端到端)
```kotlin
@MicronautTest
class OrderE2ETest(@Client("/") private val client: HttpClient) : StringSpec({
    "should place order via HTTP" {
        val response = client.toBlocking().exchange(...)
        response.status shouldBe HttpStatus.CREATED
    }
})
```

## 🔌 扩展点

### 添加新用例
1. 定义 Port (`core/port/in/`)
2. 实现 Handler (`core/application/handler/`)
3. 实现 Service (`core/application/service/`)
4. 添加 Adapter (`adapter/in/http/`)

### 替换实现
只需创建新的 Adapter 实现即可，无需修改核心代码：

```kotlin
@Singleton
@Replaces(InMemoryOrderRepository::class)
class PostgresOrderRepository : OrderRepository {
    // 使用 JPA/R2DBC 实现
}
```

## 🎓 设计原则

1. **依赖倒置原则 (DIP)**: 高层模块不依赖低层模块
2. **接口隔离原则 (ISP)**: 接口小而专注
3. **单一职责原则 (SRP)**: 每个类只有一个变化的理由
4. **开闭原则 (OCP)**: 对扩展开放，对修改封闭
5. **里氏替换原则 (LSP)**: 子类可以替换父类

## 📚 参考资料

- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [DDD (Domain-Driven Design)](https://www.domainlanguage.com/ddd/)
- [Ports and Adapters Pattern](https://jmgarridopaz.github.io/content/hexagonalarchitecture.html)

---

**此架构确保了高内聚、低耦合、可测试性和可维护性。** 🚀

