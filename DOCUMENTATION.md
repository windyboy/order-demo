# 📚 项目文档索引

本文档提供项目所有文档的快速导航和概览。

## 🎯 核心文档

### 1. [README.md](./README.md) 🌟
**快速开始必读**

- 项目概览和特性介绍
- 快速启动指南
- 项目结构说明
- API 使用示例
- 技术栈清单

**适合人群**: 所有人，特别是首次接触本项目的开发者

---

### 2. [ARCHITECTURE.md](./ARCHITECTURE.md) 🏛️
**架构设计核心文档 (v2.0 生产就绪版)**

**包含内容**:
- 🎨 系统架构全景图 (Mermaid)
- 🔄 下单流程序列图
- 📊 端口与适配器映射表
- 🧪 测试策略和覆盖情况
- 🚀 扩展指南（添加新状态、切换数据库、添加新用例）
- 🏗️ 架构决策记录 (ADR)
- 📚 设计模式应用
- ✅ 最佳实践 vs ❌ 反模式
- 📈 下一步演进方向
- 🔄 完整重构改进历史

**适合人群**: 
- 需要深入了解架构设计的开发者
- 准备扩展或修改系统的开发者
- 学习六边形架构的学习者

**关键主题**:
- 六边形架构原则
- 依赖倒置实践
- DDD 战术模式
- Result 类型错误处理
- 领域事件驱动
- 状态机设计

---

### 3. [ErrorMapping.md](./ErrorMapping.md) 🚨
**错误处理映射指南**

**包含内容**:
- 领域错误类型 → HTTP 状态码映射
- 错误响应格式规范
- 错误处理最佳实践
- 各类错误场景示例

**适合人群**: 
- API 开发者
- 前端集成开发者
- 错误处理机制维护者

---

## 📖 历史和参考文档

### 4. [ARCHITECTURE_v1_legacy.md](./ARCHITECTURE_v1_legacy.md) 📜
**架构设计初始版本**

这是项目早期的架构文档，保留作为历史参考。

**查看原因**:
- 了解项目演进历程
- 对比架构改进前后的差异
- 学习架构迭代思路

**建议**: 优先阅读 [ARCHITECTURE.md](./ARCHITECTURE.md)，此文档仅作参考

---

### 5. 重构历史 🔄

重构的详细历史已经整合到 [ARCHITECTURE.md](./ARCHITECTURE.md) 的 "重构改进历史" 章节中，包含完整的 6 个 Phase 改进说明。

---

## 🗺️ 阅读路线图

### 🎯 快速上手路线 (30分钟)
```
README.md → 运行项目 → 测试 API
```

### 📚 架构学习路线 (2-3小时)
```
README.md 
  ↓
ARCHITECTURE.md (重点阅读)
  ↓
ErrorMapping.md
  ↓
代码实践：阅读 core/ 目录源码
```

### 🔧 开发者深入路线 (1天)
```
README.md
  ↓
ARCHITECTURE.md
  ↓
研究测试代码 (src/test/)
  ↓
对比 ARCHITECTURE_v1_legacy.md
  ↓
实践：扩展新功能
```

---

## 📂 文档 vs 代码映射

| 文档章节 | 对应代码位置 | 说明 |
|---------|-------------|------|
| **Domain 层设计** | `core/domain/` | 聚合根、值对象、领域事件 |
| **Port 层接口** | `core/port/incoming/`, `core/port/outgoing/` | 入站/出站端口定义 |
| **Application 层** | `core/application/` | Handler 和 Service |
| **Adapter 层** | `adapter/incoming/`, `adapter/outgoing/` | HTTP、持久化、消息适配器 |
| **测试策略** | `src/test/kotlin/` | 单元、集成、E2E 测试 |
| **错误处理** | `core/domain/OrderError.kt` | Sealed 错误类型 |
| **配置管理** | `src/main/resources/application-*.yml` | 环境配置 |

---

## 🎓 按学习目标分类

### 学习六边形架构
📖 主读: `ARCHITECTURE.md` - 架构层次说明、依赖规则  
💻 实践: 对比 `core/` 和 `adapter/` 的依赖方向

### 学习 DDD 战术模式
📖 主读: `ARCHITECTURE.md` - Domain 层设计、聚合根  
💻 实践: 研究 `Order.kt`, `OrderItem.kt`, `Money.kt`

### 学习 Result 类型错误处理
📖 主读: `ErrorMapping.md` + `ARCHITECTURE.md` (最佳实践章节)  
💻 实践: 追踪 `PlaceOrderService.kt` 的错误流转

### 学习测试策略
📖 主读: `ARCHITECTURE.md` - 测试覆盖情况  
💻 实践: 运行测试并阅读 `src/test/` 目录

### 学习状态机设计
📖 主读: `ARCHITECTURE.md` - OrderStatus 状态机  
💻 实践: 研究 `OrderStatus.kt` 和状态转换测试

---

## 📝 贡献文档

如果你修改了架构或添加了新功能，请：

1. ✅ 更新 `ARCHITECTURE.md` 相关章节
2. ✅ 如有新错误类型，更新 `ErrorMapping.md`
3. ✅ 在 `README.md` 中更新示例（如有必要）
4. ✅ 添加相应的测试并更新测试覆盖率说明

---

## 🔗 外部参考资料

本项目基于以下经典理论和最佳实践：

- [Hexagonal Architecture (Alistair Cockburn)](https://alistair.cockburn.us/hexagonal-architecture/)
- [Clean Architecture (Robert C. Martin)](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Domain-Driven Design (Eric Evans)](https://www.domainlanguage.com/ddd/)
- [Micronaut Framework Official Docs](https://docs.micronaut.io/)
- [Kotlin Result Type](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-result/)

---

## 📞 需要帮助？

1. **架构疑问**: 查阅 `ARCHITECTURE.md` 或提 Issue
2. **API 使用**: 查阅 `README.md` 快速开始章节
3. **错误处理**: 查阅 `ErrorMapping.md`
4. **代码示例**: 参考 `src/test/` 目录的测试用例

---

**最后更新**: 2025-10-17  
**文档版本**: v2.0

