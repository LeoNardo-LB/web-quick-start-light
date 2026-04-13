## Context

web-quick-start-light 是一个 Spring Boot 4.0.2 + Java 25 多模块三层架构脚手架，当前具备优秀的工程化基础（ArchUnit 架构守护、生产级日志 8 Appender、Template Method 客户端模块、1GB 内存极致优化、JaCoCo 95% 覆盖率目标），但在生产级基础设施能力上存在明显缺口。

当前架构状态：
- 三层架构：Controller → Service → Repository，Controller 中混入了业务判断逻辑（null check + 失败响应构造）
- 无认证/授权：ContextFillFilter 中 userId 硬编码为 "SYSTEM"
- 无 API 版本控制、无限流/幂等防护
- 无国际化：错误消息全部硬编码中文
- 无操作日志持久化：@BusinessLog 只写文件，无 DB 存储
- 无分页实战示例：BasePageResult/BasePageRequest 已定义但从未使用
- 已有 6 个 client 模块（cache/oss/email/sms/search/log），统一遵循 Template Method + 条件装配 + NoOp 模式

技术栈约束：Java 25、Spring Boot 4.0.2、MyBatis-Plus 3.5.16、Caffeine、SQLite、1GB JVM 内存。

## Goals / Non-Goals

**Goals:**

- 补齐认证/授权（Sa-Token 纯登录认证）、API 版本控制、限流、幂等防护、国际化、操作日志持久化等基础设施能力
- 引入 Facade 薄层，将架构从三层升级为四层（Controller → Facade → Service → Repository）
- 所有新增能力遵循现有 client 模块的 Template Method + 条件装配 + NoOp 模式
- 以操作日志和系统配置为业务载体，提供完整的分页查询示例
- 所有改造完成后，全面对齐文档与代码

**Non-Goals:**

- 不做微服务架构改造（保持单体）
- 不做 OAuth2 / SSO / RBAC 角色权限（仅登录认证）
- 不做前端界面（纯后端脚手架）
- 不做 Redis 集成（纯本地 Caffeine + Sa-Token 内存会话）
- 不做 JWT（单体应用纯内存会话已足够）
- 不做工作流 / 定时任务框架 / 消息队列
- 不做数据库迁移工具（Flyway/Liquibase，留待后续）
- 不做 HTTP 状态码语义化改造（保持 HTTP 200 + 业务码模式）

## Decisions

### D1: API 版本控制 — 使用 Spring Boot 4 原生能力 + Header 方式

**选择**：Spring Boot 4 内置 `spring.mvc.apiversion.use-header` + `@GetMapping(version = "1.x")`

**替代方案**：
- URL 前缀（`/v1/xxx`）：URL 膨胀，Spring Boot 4 原生支持但不如 Header 优雅
- 第三方 `spring-api-versioning`：不再需要，Spring Boot 4 已内置

**统一前缀**：通过 `server.servlet.context-path` 已有 `/quickstart-light`，在此基础上为业务 API 统一添加 `/api` 路径段（在 Controller `@RequestMapping` 层面配置），而非修改 context-path。最终 API 路径形如 `/quickstart-light/api/system/configs`。

### D2: 限流 — Bucket4j + Caffeine，`@RateLimit` 注解式

**选择**：`@RateLimit` 注解 + Bucket4j 令牌桶 + Caffeine 存储 Bucket 实例

**设计**：
```java
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    double capacity() default 10;           // 桶容量
    double refillTokens() default 10;       // 补充令牌数
    long refillDuration() default 1;        // 补充时间
    TimeUnit refillUnit() default TimeUnit.SECONDS;
    String key() default "";                // SpEL 表达式，提取限流 Key
    LimitFallback fallback() default LimitFallback.REJECT;  // 降级策略
}
```

**降级策略枚举**：
- `REJECT`：直接拒绝，抛 BizException（默认）
- `WAIT`：阻塞等待直到获取令牌（适合后台任务）
- `FALLBACK`：执行 fallbackMethod 指定的降级方法

**实现方式**：AOP 切面 + Bucket4j `tryConsume()`，Bucket 实例存储在 Caffeine Cache 中（key = 限流 Key 的 hash）。

**做成 `client-ratelimit` 模块**：遵循 Template Method + 条件装配模式，Bucket4j 为 optional 依赖。

### D3: 幂等防护 — `@Idempotent` 通用注解 + Caffeine

**选择**：`@Idempotent` 注解 + Caffeine 缓存 + SpEL 表达式提取 Key 字段

**设计**：
```java
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    long timeout() default 3000;            // 幂等窗口（毫秒）
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;
    String field() default "";              // SpEL 表达式，从入参提取幂等 Key
    String message() default "请勿重复操作";
}
```

**通用性设计**：
- 不仅限于 HTTP 接口，任何方法都可标注
- `field` 支持 SpEL 表达式：如 `#request.key`、`#id`、`#p0`（第一个参数）
- 默认行为：当 `field` 为空时，使用方法签名 + 所有参数的 hash 作为 Key
- Key 组成：`className.methodName(fieldValue)` → 存入 Caffeine（expireAfterWrite = timeout）
- 第二次调用在窗口内 → 抛 BizException（重复提交）

**做成 `client-idempotent` 模块**：遵循 Template Method + 条件装配模式，依赖 client-cache。

### D4: 操作日志 — 扩展 @BusinessLog + DB 持久化 + 采集率

**选择**：扩展现有 `@BusinessLog` 注解 + 新增 `operation_log` 表 + 异步写入 + 采集率

**注解扩展**：
```java
@BusinessLog(
    value = "更新配置",           // 业务描述（保持向后兼容）
    module = "SYSTEM",            // 模块名
    operation = OperationType.UPDATE,  // 操作类型
    samplingRate = 1.0            // 采集率（0.0-1.0，1.0=全量采集）
)
```

**数据库表**（`operation_log`）：
| 字段 | 类型 | 说明 |
|------|------|------|
| id | TEXT | 主键（ASSIGN_ID） |
| trace_id | TEXT | 链路追踪 ID |
| user_id | TEXT | 操作用户 ID |
| module | TEXT | 模块名 |
| operation_type | TEXT | 操作类型（CREATE/UPDATE/DELETE/QUERY/EXPORT/IMPORT） |
| description | TEXT | 业务描述 |
| method | TEXT | 方法签名 |
| params | TEXT | 参数 JSON（最大 2048） |
| result | TEXT | 结果 JSON（最大 2048） |
| execution_time | INTEGER | 执行耗时（ms） |
| ip | TEXT | 客户端 IP |
| status | TEXT | SUCCESS/ERROR |
| error_message | TEXT | 错误信息（仅失败时） |
| create_time | TEXT | 创建时间（ISO 8601） |

**异步写入**：复用 `ThreadPoolConfigure` 的 `ioThreadPool`，通过 `LogAspect` 在方法执行后异步写入。

**采集率**：`samplingRate` 控制是否写入 DB（0.5 = 50% 概率写入），但不影响文件日志和 Micrometer 指标（全量采集）。

### D5: 四层架构 — Facade 薄层

**选择**：Controller → Facade(接口+DTO) → Service → Repository

**包结构**：
```
app/.../facade/
├── system/
│   ├── SystemConfigFacade.java          ← 接口
│   ├── SystemConfigFacadeImpl.java      ← 实现
│   ├── SystemConfigVO.java             ← DTO（从 service 包移入）
│   ├── ConfigGroupVO.java              ← DTO
│   └── UpdateConfigCommand.java        ← Command DTO
├── auth/
│   ├── LoginFacade.java                ← 接口
│   └── LoginFacadeImpl.java            ← 实现
└── log/
    ├── OperationLogFacade.java         ← 接口
    ├── OperationLogFacadeImpl.java     ← 实现
    └── OperationLogVO.java             ← DTO
```

**Facade 职责**：
1. DTO 转换：Request → Command，Entity → VO
2. 多 Service 编排：一个 Facade 可调用多个 Service
3. VO 组装：复杂展示逻辑从 Service 上移到 Facade
4. 参数校验前置：Controller 只做 Bean Validation，Facade 做业务校验

**Service 层变化**：返回 Entity（而非 VO），不再关心展示层需求。

### D6: 认证 — Sa-Token client-auth 模块

**选择**：`sa-token-spring-boot4-starter:1.45.0`，做成 `client-auth` 模块

**模块结构**：
```
clients/client-auth/
├── pom.xml  (sa-token optional)
└── src/main/java/.../client/auth/
    ├── AuthClient.java               ← 接口
    ├── AbstractAuthClient.java       ← Template Method
    ├── NoOpAuthClient.java           ← 默认降级
    ├── SaTokenAuthClient.java        ← Sa-Token 实现
    ├── AuthProperties.java           ← middleware.auth.* 配置
    ├── AuthInterceptorConfigurer.java ← 路由拦截配置
    └── AuthAutoConfiguration.java    ← 条件装配
```

**ContextFillFilter 改造**：
```java
String userId = resolveUserId();  // 从 StpUtil.getLoginIdDefaultNull() 获取
ScopedThreadContext.runWithContext(..., userId != null ? userId : "ANONYMOUS", traceId);
```

**用户表**：简单内置 `user` 表（id/username/password_hash/nickname/status/create_time/update_time），密码使用 BCrypt 哈希。

**登录端点**：`POST /api/auth/login`（用户名+密码），`POST /api/auth/logout`

### D7: 国际化 — MessageSource + ErrorCode 扩展

**选择**：Spring MessageSource + Accept-Language Header + 最小侵入改造

**ErrorCode 改造**：
```java
public interface ErrorCode {
    int code();
    String message();
    default String messageKey() { return "error." + code(); }  // 新增默认方法
}
```

**WebExceptionAdvise 改造**：注入 `MessageSource`，根据 `Accept-Language` Header 解析 Locale，使用 `errorCode.messageKey()` 查询国际化消息。

**资源文件**：
- `messages.properties`（默认中文）
- `messages_en.properties`（英文）
- `ValidationMessages.properties`（Bean Validation 中文）
- `ValidationMessages_en.properties`（Bean Validation 英文）

**不改变 ErrorCode 接口的 `message()` 方法签名**（向后兼容），新增 `messageKey()` 默认方法。

### D8: 分页查询 — 以操作日志和系统配置为例

**操作日志分页**：`GET /api/system/operation-logs?pageNo=1&pageSize=20&module=SYSTEM&operationType=UPDATE&startTime=...&endTime=...`

**系统配置分页**：`GET /api/system/configs/page?pageNo=1&pageSize=20&groupCode=SITE`

**链路**：Controller（PageQuery record）→ Facade → Service（返回 IPage<Entity>）→ Repository（MyBatis-Plus Page）

利用已有 `BasePageResult.fromPage(IPage)` 静态工厂方法。

### D9: 实施分期

| Phase | 内容 | 依赖 |
|-------|------|------|
| **P0** | API 版本控制 + 统一前缀 + client-ratelimit + client-idempotent | 无 |
| **P1** | Facade 四层架构改造 + client-auth（Sa-Token）+ ContextFillFilter 改造 | 无 |
| **P2** | 操作日志（@BusinessLog 扩展 + DB 表 + 异步写入 + 采集率）+ 国际化 i18n | P1（Facade） |
| **P3** | 分页查询示例（操作日志 + 系统配置）+ 文档全面对齐 | P1 + P2 |

## Risks / Trade-offs

| 风险 | 影响 | 缓解 |
|------|------|------|
| 四层架构 BREAKING 改动 | Controller 从 Service 切换到 Facade，包路径变更 | 先建 Facade + 测试，再切换 Controller |
| `/api` 前缀可能破坏现有集成测试 | ITest 中的 URL 路径全部需要更新 | 分期改造，Phase 0 先加前缀 |
| Sa-Token v1.45.0 对 Spring Boot 4 的兼容性 | 刚发布不久，可能有 edge case | 新增 client-auth 模块，NoOp 默认降级 |
| Bucket4j 作为新依赖 | 增加依赖管理复杂度 | 标记为 optional，通过条件装配控制 |
| 操作日志异步写入可能与请求上下文丢失 | ScopedValue 不可跨线程传播 | 使用 ContextRunnable 包装异步任务 |
| 1GB 内存下新增模块的内存压力 | 更多 Bean + 缓存 + 线程 | Caffeine maximumSize 限制 + Serial GC 监控 |
| 国际化改造的向后兼容性 | ErrorCode.message() 返回值语义变化 | 新增 messageKey() 默认方法，message() 保持不变 |

## Migration Plan

1. **Phase 0 部署**：添加 API 版本控制 + 前缀 + 限流 + 幂等模块。现有 API 路径变化（加 `/api`），需同步更新测试
2. **Phase 1 部署**：Facade 层改造 + 认证模块。Facade 层是无缝切换（Controller 依赖从 Service 换到 Facade），认证默认 NoOp（不引入 Sa-Token 依赖时行为不变）
3. **Phase 2 部署**：操作日志 DB 表需先执行 DDL（`schema.sql`），国际化是纯后端改造
4. **Phase 3 部署**：分页查询是新增端点，无破坏性。文档对齐最后进行

**回滚策略**：每个 Phase 独立可回滚。Facade 层可随时切换回 Service。认证模块 NoOp 降级。限流/幂等可配置关闭。
