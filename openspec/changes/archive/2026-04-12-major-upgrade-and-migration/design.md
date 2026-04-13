## Context

web-quick-start-light 是一个 Spring Boot 3.5.6 + Java 25 的多模块三层架构脚手架项目，使用 SQLite 数据库。参考项目 speccoding-fullstack 基于 Spring Boot 4.0.2 + DDD 四层架构，拥有更完整的能力（代码生成器、系统配置、增强版客户端、完整日志系统）。

本变更将 speccoding 的核心能力移植到 web-quick-start-light，保持三层架构不变，升级全部依赖到最新稳定版。

关键约束：
- 架构保持三层（Controller → Service → Repository），不引入 DDD 四层
- 数据库保持 SQLite，SQL 语法需适配
- 客户端保留 5 个独立 Maven 子模块结构
- 客户端代码从 speccoding 移植而非照搬 DDD 结构
- 代码生成器生成的 DO/Mapper 禁止手动修改

## Goals / Non-Goals

**Goals:**
- 所有依赖升级到 Maven Central 最新稳定版（Spring Boot 4.0.2）
- 日志系统完全替换为 speccoding 版本
- 5 个客户端模块用 speccoding 实现全面替换
- 新增 MyBatis-Plus 代码生成器 + schema.sql/init.sql
- 新增系统配置管理功能（移植自 speccoding，适配三层架构）
- 时间类型统一为 Instant，新增类使用 record
- 分 4 个 Change 递进执行，每步可独立验证

**Non-Goals:**
- 不迁移到 DDD 四层架构（保持 Controller→Service→Repository）
- 不更换数据库（保持 SQLite）
- 不引入领域事件系统
- 不引入 CQRS 模式（系统配置合并 Command/Query 到单一 Service）
- 不引入 Redis 客户端实现（speccoding 有 RedisCacheClientImpl 但本项目暂不需要）
- 不引入 RustFS 对象存储实现（speccoding 有但本项目暂不需要）
- 不改动 BaseResult/BaseRequest/BaseDO 的继承体系（保持 class）

## Decisions

### D1: 执行策略 — 分层递进 4 个 Change

**选择**: 按 Change 1(基础设施) → Change 2(DAL) → Change 3(中间件) → Change 4(业务功能) 递进执行。

**替代方案**:
- Big Bang（一次性全部变更）— 风险过高，出错难定位
- 功能切片（3 个 Change 按功能分组）— 存在依赖倒置问题

**理由**: 依赖关系明确（框架升级必须先于所有其他变更），每个 Change 可独立验证，项目始终可运行。

### D2: 架构保持三层，移植而非照搬

**选择**: 从 speccoding 移植逻辑到三层架构中，而非引入 DDD 四层。

**适配规则**:
- domain 层的实体/值对象 → app 的 entity 包（值对象改为 record）
- infrastructure 的 PO/Mapper → app 的 generated 包（代码生成器）
- infrastructure 的 Repository 实现 → app 的 repository 包
- app 的 CQRS Service → 合并为单一 Service
- adapter 的 Controller → app 的 controller 包
- infrastructure 的客户端实现 → clients 子模块内部
- start 的配置类 → app 的 config 包

**理由**: 用户明确选择保持三层架构。speccoding 的 DDD 层结构不适用于脚手架项目。

### D3: Spring Boot 4.0.2

**选择**: 升级到 4.0.2（用户明确要求）。

**风险**: Spring Framework 7 有破坏性变更；speccoding 的 LogAspect 注释说 AOP 4.0.0-M2 存在兼容性问题；MyBatis-Plus 3.5.16 未经与 4.0.2 的广泛验证。

**缓解**: Change 1 完成后先编译验证，必要时降级或等待兼容版。

### D4: SQL 适配 SQLite

**选择**: MySQL DDL 适配为 SQLite 语法。

**关键映射**:
| MySQL | SQLite |
|-------|--------|
| BIGINT AUTO_INCREMENT | INTEGER PRIMARY KEY AUTOINCREMENT |
| JSON | TEXT |
| TIMESTAMP | TEXT（ISO 8601） |
| ENGINE/CHARSET | 删除 |
| ON UPDATE CURRENT_TIMESTAMP | 应用层处理（MyMetaObjectHandler） |
| NOW() | datetime('now') |

**理由**: 保持 SQLite 作为轻量脚手架的数据库。

### D5: 日志系统完全替换而非渐进增强

**选择**: 删除现有日志代码，用 speccoding 版本完全替换。

**替代方案**: 渐进增强（保留现有，补充缺失组件）— 会导致两套日志体系共存，代码不一致。

**理由**: 用户明确要求"全面替换"。speccoding 的日志系统更完整（多了 LogMarkers、SensitiveLogUtils、审计日志、MemoryLogAppender），直接替换更干净。

### D6: 客户端全面移植 speccoding 实现

**选择**: 5 个客户端模块全部用 speccoding 实现替换，包括 Aliyun SDK（Email/Sms）和 Elasticsearch（Search）。

**替代方案**: 仅移植 Cache 和 OSS 的增强版，保留 Email/Sms/Search 的简单实现。

**理由**: 用户明确选择"全面移植"。条件装配（`@ConditionalOnProperty`）确保未配置的服务不注册 Bean。

### D7: MethodExecutionLog 适配

**选择**: MethodExecutionLog 改为独立类，不继承 Entity 或 BaseDO。使用 Lombok `@Getter @Builder`。

**理由**: MethodExecutionLog 是日志 DTO，不是持久化对象，不需要继承领域基类。

### D8: 代码生成器目录与规范

**选择**: 代码生成器工具和生成的代码都放在 `app/.../generated/` 包下。AGENTS.md 新增"禁止手动修改生成代码"的规范。

**理由**: 用户明确要求。生成代码与手写代码通过包路径隔离。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| Spring Boot 4.0.2 与 MyBatis-Plus 3.5.16 不兼容 | Change 1 先编译验证，必要时降级 |
| speccoding LogAspect AOP 在 4.0.2 不可用 | 保持配置结构，切面暂不可用（speccoding 也标注了此问题） |
| Elasticsearch 客户端无 ES 服务 | 条件装配 `@ConditionalOnBean`，无 ES 时不注册 |
| 阿里云 SDK 需 AccessKey | 条件装配，未配置不启用 |
| CacheClient 接口 4→10 方法 | Change 3 全量替换时一并更新使用方 |
| SQLite 与代码生成器兼容性 | 可能需手动调整生成结果 |
| 变更范围大（~60 文件） | 分 4 个 Change 递进，每步可验证 |

## Migration Plan

### 执行顺序

```
Change 1 (基础设施层)
  ├── 依赖升级（Spring Boot 4.0.2 + 23 个依赖）
  ├── AuditEvent.timestamp: LocalDateTime → Instant
  ├── BaseResult.time: Long → Instant
  └── 验证: mvn clean compile + mvn test

Change 2 (DAL 层)
  ├── MybatisPlusGenerator 移植（适配 SQLite）
  ├── schema.sql + init.sql（适配 SQLite 语法）
  ├── AGENTS.md 新增代码生成器规范
  └── 验证: 代码生成器运行 + 建表

Change 3 (中间件层)
  ├── 日志系统完全替换（13 文件删 + 14 文件移植）
  ├── 客户端全面替换（5 个模块代码替换）
  ├── 配置类移植
  └── 验证: mvn test + 日志输出验证

Change 4 (业务功能层)
  ├── SystemConfig 移植（18 个新文件）
  ├── 代码生成器生成 DO + Mapper
  └── 验证: 启动 + API 端点测试
```

### 回滚策略

每个 Change 在独立分支上完成，如遇不可解决的问题可回退到上一个 Change 的稳定状态。

## Open Questions

1. Elasticsearch Java Client 的具体版本号 — 在 Change 3 实施时查最新
2. Spring Boot 4.0.2 是否已正式发布 GA 版本 — 已确认在 Maven Central 存在
3. 阿里云 SDK 是否支持 Spring Boot 4.0.2 的 Jakarta EE 11 — 需 Change 3 时验证
