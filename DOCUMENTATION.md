# 📚 Project Documentation Index

This document provides quick navigation and overview of all project documentation.

## 🎯 Core Documentation

### 1. [README.md](./README.md) 🌟
**Quick Start Required Reading**

- Project overview and feature introduction
- Quick start guide
- Project structure description
- API usage examples
- Technology stack list

**Target Audience**: Everyone, especially developers new to this project

---

### 2. [ARCHITECTURE.md](./ARCHITECTURE.md) 🏛️
**Core Architecture Design Document (v2.0 Production-Ready)**

**Content**:
- 🎨 System architecture overview (Mermaid)
- 🔄 Order placement sequence diagram
- 📊 Port and adapter mapping table
- 🧪 Testing strategy and coverage
- 🚀 Extension guide (adding new states, switching databases, adding new use cases)
- 🏗️ Architecture decision records (ADR)
- 📚 Design pattern applications
- ✅ Best practices vs ❌ Anti-patterns
- 📈 Next evolution steps
- 🔄 Complete refactoring improvement history

**Target Audience**: 
- Developers needing in-depth understanding of architecture design
- Developers preparing to extend or modify the system
- Learners studying hexagonal architecture

**Key Topics**:
- Hexagonal architecture principles
- Dependency inversion practices
- DDD tactical patterns
- Result type error handling
- Domain event-driven design
- State machine design

---

### 3. [ErrorMapping.md](./ErrorMapping.md) 🚨
**Error Handling Mapping Guide**

**Content**:
- Domain error types → HTTP status code mapping
- Error response format specification
- Error handling best practices
- Examples of various error scenarios

**Target Audience**: 
- API developers
- Frontend integration developers
- Error handling mechanism maintainers

---

## 📖 Historical and Reference Documentation

### 4. [ARCHITECTURE_v1_legacy.md](./ARCHITECTURE_v1_legacy.md) 📜
**Initial Architecture Design Version**

This is the project's early architecture document, retained as historical reference.

**Reasons to Review**:
- Understand project evolution history
- Compare differences before and after architecture improvements
- Learn architecture iteration thinking

**Recommendation**: Prioritize reading [ARCHITECTURE.md](./ARCHITECTURE.md), this document is for reference only

---

### 5. Refactoring History 🔄

The detailed refactoring history has been integrated into the "Refactoring Improvement History" section of [ARCHITECTURE.md](./ARCHITECTURE.md), including complete 6-phase improvement descriptions.

---

## 🗺️ Reading Roadmap

### 🎯 Quick Start Route (30 minutes)
```
README.md → Run project → Test API
```

### 📚 Architecture Learning Route (2-3 hours)
```
README.md 
  ↓
ARCHITECTURE.md (Key reading)
  ↓
ErrorMapping.md
  ↓
Code practice: Read core/ directory source code
```

### 🔧 Developer Deep Dive Route (1 day)
```
README.md
  ↓
ARCHITECTURE.md
  ↓
Study test code (src/test/)
  ↓
Compare ARCHITECTURE_v1_legacy.md
  ↓
Practice: Extend new features
```

---

## 📂 Documentation vs Code Mapping

| Document Section | Corresponding Code Location | Description |
|---------|-------------|------|
| **Domain Layer Design** | `core/domain/` | Aggregate roots, value objects, domain events |
| **Port Layer Interfaces** | `core/port/incoming/`, `core/port/outgoing/` | Inbound/outbound port definitions |
| **Application Layer** | `core/application/` | Handlers and Services |
| **Adapter Layer** | `adapter/incoming/`, `adapter/outgoing/` | HTTP, persistence, messaging adapters |
| **Testing Strategy** | `src/test/kotlin/` | Unit, integration, E2E tests |
| **Error Handling** | `core/domain/OrderError.kt` | Sealed error types |
| **Configuration Management** | `src/main/resources/application-*.yml` | Environment configuration |

---

## 🎓 Categorized by Learning Objectives

### Learning Hexagonal Architecture
📖 Main Reading: `ARCHITECTURE.md` - Architecture layer description, dependency rules  
💻 Practice: Compare dependency directions of `core/` and `adapter/`

### Learning DDD Tactical Patterns
📖 Main Reading: `ARCHITECTURE.md` - Domain layer design, aggregate roots  
💻 Practice: Study `Order.kt`, `OrderItem.kt`, `Money.kt`

### Learning Result Type Error Handling
📖 Main Reading: `ErrorMapping.md` + `ARCHITECTURE.md` (Best practices section)  
💻 Practice: Trace error flow in `PlaceOrderService.kt`

### Learning Testing Strategy
📖 Main Reading: `ARCHITECTURE.md` - Test coverage  
💻 Practice: Run tests and read `src/test/` directory

### Learning State Machine Design
📖 Main Reading: `ARCHITECTURE.md` - OrderStatus state machine  
💻 Practice: Study `OrderStatus.kt` and state transition tests

---

## 📝 Contributing Documentation

If you've modified the architecture or added new features, please:

1. ✅ Update relevant sections in `ARCHITECTURE.md`
2. ✅ Update `ErrorMapping.md` if there are new error types
3. ✅ Update examples in `README.md` if necessary
4. ✅ Add corresponding tests and update test coverage documentation

---

## 🔗 External References

This project is based on the following classic theories and best practices:

- [Hexagonal Architecture (Alistair Cockburn)](https://alistair.cockburn.us/hexagonal-architecture/)
- [Clean Architecture (Robert C. Martin)](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Domain-Driven Design (Eric Evans)](https://www.domainlanguage.com/ddd/)
- [Micronaut Framework Official Docs](https://docs.micronaut.io/)
- [Kotlin Result Type](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-result/)

---

## 📞 Need Help?

1. **Architecture Questions**: Refer to `ARCHITECTURE.md` or file an Issue
2. **API Usage**: Refer to `README.md` quick start section
3. **Error Handling**: Refer to `ErrorMapping.md`
4. **Code Examples**: Refer to test cases in `src/test/` directory

---

**Last Updated**: 2025-10-17  
**Document Version**: v2.0

