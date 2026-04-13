## Context

web-quick-start-light 项目在第一轮架构增强（enhance-architecture-from-backend）中已完成核心基础设施搭建：异常体系、ScopedThreadContext、MyBatis-Plus、技术客户端接口、测试体系等。但日志系统和项目文档仍存在显著缺陷：

**日志系统现状**：
- BizLog 注解 + BizLogAspect（SLF4J + Micrometer Timer/Counter）
- logback-spring.xml 仅有 4 个 Appender（FILE, ASYNC_FILE, CURRENT, ERROR_FILE）
- ContextFillFilter 设置 ScopedThreadContext.traceId，但**未写入 MDC**，logback pattern 也**无 %X{traceId}**
- 缺少启动验证、慢查询拦截、采样过滤、审计追踪、脱敏工具等生产级能力

**文档体系现状**：
- 无 AGENTS.md（AI 助手无入口指引）
- 无 docs/ 目录（无编码规范、架构文档、工作流指南）
- README.md 过时（仍提到 H2、缺少 SQLite）
- 前一个变更的 tasks.md 未反映 Phase 2 精简工作

参考项目 speccoding-fullstack 拥有成熟的日志基础设施（7 组件）和文档体系（3 个 AGENTS.md + 25 个 docs 文件），其设计理念可以在保持三层架构的前提下适配引入。

核心约束：**保持三层架构不变**（Controller → Service → Repository），不引入 DDD/六边形架构模式。

## Goals / Non-Goals

**Goals:**
- 修复 traceId 在日志中不可见的 Bug（P0）
- 引入生产级日志基础设施：启动验证、慢查询拦截、JSON 结构化日志（P1）
- 引入高级日志能力：采样过滤、审计追踪、脱敏工具、日志标记（P2）
- 建立 AI 编码规范体系：AGENTS.md + docs/ 目录
- 全面更新项目文档，反映当前真实状态

**Non-Goals:**
- **不引入 APM 分布式追踪**（如 Zipkin/Jaeger），仅做日志级别 traceId
- **不引入日志收集平台**（如 ELK/Loki），日志仅写本地文件
- **不引入 Micrometer Tracing 框架**（Spring Boot 3.x 的分布式追踪），保持轻量
- **不引入数据库审计表**，审计日志仅写文件（符合前一轮去除数据库日志的决策）
- **不改变三层架构**
- **不引入前端相关文档**（本项目为纯后端骨架）
- **不复制 speccoding 的 DDD 文档**，所有架构文档重写为三层版本

## Decisions

### Decision 1: MDC 注入方式

**选择**: 在 ContextFillFilter 中直接调用 `MDC.put("traceId", traceId)` + `MDC.remove("traceId")`

**理由**:
- MDC 是 SLF4J 的标准上下文传递方式，与 logback 无缝集成
- 放在 Filter 中确保每个请求的生命周期内 traceId 始终可用
- 在 finally 块中清理 MDC，避免线程池复用导致的 traceId 泄漏

**备选方案**:
- Micrometer Tracing：引入重量级依赖，超出轻量骨架范围
- 在 BizLogAspect 中注入：仅覆盖 @BizLog 标注的方法，非 @BizLog 方法无 traceId

### Decision 2: 日志组件在三层架构中的位置

**选择**: 所有日志组件放在 `config/logging/` 包下

```
config/
├── logging/
│   ├── LoggingConfigure.java        # @Configuration，条件注册各组件
│   ├── LoggingConfiguration.java    # 启动验证 ApplicationListener
│   ├── SamplingTurboFilter.java     # 日志采样过滤器
│   ├── SlowQueryInterceptor.java    # MyBatis 慢查询拦截器
│   ├── LogMarkers.java              # 通用日志标记常量
│   └── SensitiveLogUtils.java       # 日志脱敏工具
├── properties/
│   └── LoggingProperties.java       # 日志配置属性
├── WebConfigure.java
├── ThreadPoolConfigure.java
└── TechClientConfigure.java
```

**理由**:
- 三层架构没有 infrastructure 层，config/ 是最自然的配置组件存放位置
- 参考项目 speccoding 将日志组件放在 `start/config/logging/`，理念一致
- 集中管理便于维护和理解

### Decision 3: AuditLogService 在三层架构中的位置

**选择**: `service/log/AuditLogService.java` + `service/log/AuditEvent.java`

**理由**:
- AuditLogService 在参考项目中是 domain 层服务（DDD），但在三层架构中最合适的位置是 Service 层
- AuditEvent 作为 record 放在同一包下，保持内聚
- 审计日志不是业务逻辑，但也不是配置——放在 service 包下以 `log/` 子包隔离

### Decision 4: LogMarkers 通用化设计

**选择**: 提供通用分类标记，不绑定具体业务域

```java
public final class LogMarkers {
    public static final Marker API = MarkerFactory.getMarker("API");
    public static final Marker BUSINESS = MarkerFactory.getMarker("BUSINESS");
    public static final Marker SECURITY = MarkerFactory.getMarker("SECURITY");
    public static final Marker AUDIT = MarkerFactory.getMarker("AUDIT");
    public static final Marker SLOW_QUERY = MarkerFactory.getMarker("SLOW_QUERY");
}
```

**理由**:
- 参考项目的 LogMarkers 定义了 ORDER/PAYMENT 等具体业务标记，不适合骨架项目
- 骨架项目的用户会根据自己的业务域扩展具体标记
- 通用标记（API/SECURITY/AUDIT/SLOW_QUERY）与日志基础设施配合（如 SLOW_QUERY 可路由到慢查询文件）

### Decision 5: AGENTS.md 采用混合模式

**选择**: AGENTS.md < 200 行，核心规则自包含 + 引用 docs/ 详细文档

**理由**:
- Anthropic 官方推荐 CLAUDE.md < 200 行，过长会导致 AI 忽略指令
- 业界最佳实践：AGENTS.md 作为入口 + docs/ 存放详细规范
- 参考项目 speccoding 的 AGENTS.md 为 342 行（含完整文档索引），偏长
- CLAUDE.md 创建为 AGENTS.md 的符号链接，兼容 Claude Code

**文件组织**:
```
项目根目录/
├── AGENTS.md                   # 入口文件（<200行）
├── CLAUDE.md → AGENTS.md       # 符号链接
├── docs/
│   ├── README.md               # 文档导航
│   ├── architecture/           # 架构文档（三层版）
│   ├── backend/                # 后端编码规范
│   ├── workflow/               # 工作流指南
│   └── guides/                 # 入门指南
```

### Decision 6: docs/ 内容来源策略

**选择**: 从 speccoding 适配迁移 + 新编写

| 文档 | 策略 | 理由 |
|------|------|------|
| architecture/overview.md | **新写** | 三层架构 vs DDD 四层，完全不同 |
| architecture/dependency-rules.md | **新写** | 三层依赖规则 vs DDD 六边形依赖 |
| backend/coding-standards.md | **适配迁移** | 依赖库清单通用，去掉 DDD 特有规则 |
| backend/testing-guide.md | **适配迁移** | UTest/ITest 通用，测试模板相同 |
| backend/error-handling.md | **适配迁移** | 三级异常体系已对齐 |
| backend/performance-rules.md | **适配迁移** | 通用性能规则 |
| workflow/tdd-guide.md | **直接复用** | 技术栈通用 |
| workflow/openspec-guide.md | **直接复用** | 工作流工具相同 |
| guides/getting-started.md | **新写** | 项目特定的启动流程 |

### Decision 7: logback-spring.xml 多 Appender 架构

**选择**: 7 个 Appender，按功能分类输出

```
CONSOLE          → 控制台（dev/test 环境）
FILE + ASYNC     → 主日志文件（所有环境，INFO+，保留30天）
JSON_FILE + ASYNC → JSON格式日志（仅生产环境）
ERROR_FILE       → 错误日志（ERROR级，保留60天）
AUDIT_FILE       → 审计日志（独立Logger，保留180天）
SLOW_QUERY_FILE  → 慢查询日志（独立Logger）
CURRENT + ASYNC  → 当前会话日志（启动覆盖）
```

**日志格式**:
- 普通格式：`%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{traceId:-}] [%thread] %-5level %logger{36} - %msg%n`
- JSON 格式：生产环境使用 LogstashEncoder 或自定义 JSON layout

### Decision 8: SamplingTurboFilter 采样策略

**选择**: ERROR 级别始终记录，其余级别按采样率间隔放行

**理由**:
- ERROR 日志是故障排查的关键，不能被采样丢弃
- 使用 AtomicInteger 计数器，每 N 条日志放行 1 条（默认 10%）
- 通过 `logging.sampling.enabled` 和 `logging.sampling.rate` 控制
- 仅在高流量场景开启，默认关闭

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|---------|
| 日志组件过多导致骨架膨胀 | 所有 P2 组件通过 `@ConditionalOnProperty` 默认关闭，按需开启 |
| logback-spring.xml 从 4 个扩展到 7 个 Appender 增加复杂度 | 使用 springProfile 条件控制环境差异，dev 环境保持简洁 |
| 审计日志写文件而非数据库，查询和聚合不便 | 符合"去除数据库日志"的设计决策；可配合 Filebeat/Fluentd 采集到日志平台 |
| AGENTS.md + docs/ 体系需要持续维护 | docs/ 内容与代码一同演进，通过 OpenSpec 变更流程管理 |
| MDC 在异步线程中不自动传递 | ContextRunnable/ContextCallable 已处理 ScopedThreadContext 传递，但 MDC 需要额外处理（在 ContextRunnable 中同步 MDC） |
| SlowQueryInterceptor 依赖 MyBatis-Plus | 项目已确定使用 MyBatis-Plus，无兼容性问题 |
