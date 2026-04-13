## ADDED Requirements

### Requirement: ContextFillFilter 注入 MDC traceId
ContextFillFilter SHALL 在每个 HTTP 请求开始时将 traceId 写入 MDC（`MDC.put("traceId", traceId)`），并在请求结束时清理（`MDC.remove("traceId")`）。traceId 来源优先从请求头 `X-Trace-Id` 获取，为空时生成 UUID。

#### Scenario: 请求携带 X-Trace-Id 头
- **WHEN** HTTP 请求携带 `X-Trace-Id: abc123` 头
- **THEN** MDC 中 `traceId` 值为 `abc123`，在请求处理期间所有日志输出包含该 traceId，请求结束后 MDC 清理

#### Scenario: 请求无 X-Trace-Id 头
- **WHEN** HTTP 请求不携带 `X-Trace-Id` 头
- **THEN** 系统生成一个 32 位无连字符 UUID 写入 MDC `traceId`

#### Scenario: 请求处理异常时 MDC 清理
- **WHEN** 请求处理过程中抛出异常
- **THEN** MDC 中的 `traceId` 仍然被清理，不会泄漏到后续请求

### Requirement: logback pattern 包含 traceId
logback-spring.xml 的所有 Appender 的日志格式 SHALL 包含 `%X{traceId:-}` 占位符，使每行日志都包含当前请求的 traceId。

#### Scenario: 有 traceId 时的日志格式
- **WHEN** MDC 中存在 `traceId=abc123`
- **THEN** 日志输出格式为 `2026-04-11 12:00:00.000 [abc123] [http-nio-8080-1] INFO  c.e.Service - message`

#### Scenario: 无 traceId 时的日志格式
- **WHEN** MDC 中不存在 traceId（非 HTTP 请求上下文，如定时任务）
- **THEN** 日志输出格式中 traceId 位置为空 `2026-04-11 12:00:00.000 [] [scheduler-1] INFO  c.e.Service - message`

### Requirement: LoggingProperties 配置属性
系统 SHALL 提供 `LoggingProperties` 类（`@ConfigurationProperties(prefix = "logging")`），支持以下配置项：
- `logging.slow-query.enabled`（boolean，默认 false）
- `logging.slow-query.threshold-ms`（long，默认 1000）
- `logging.sampling.enabled`（boolean，默认 false）
- `logging.sampling.rate`（double，默认 0.1，范围 0.01~1.0）

#### Scenario: 默认配置
- **WHEN** 未配置任何 logging 属性
- **THEN** 慢查询拦截和采样过滤均不启用

#### Scenario: 启用慢查询检测
- **WHEN** 配置 `logging.slow-query.enabled=true` 且 `logging.slow-query.threshold-ms=500`
- **THEN** 执行时间超过 500ms 的 SQL 被记录到慢查询日志

### Requirement: LoggingConfiguration 启动验证
系统 SHALL 在应用启动完成后（`ApplicationListener<ApplicationReadyEvent>`）验证日志环境：
1. 检查日志目录是否存在，不存在则自动创建
2. 检查日志目录是否可写（尝试写入临时文件）
3. 检查磁盘剩余空间，低于 500MB 时输出 WARN 日志

#### Scenario: 日志目录不存在
- **WHEN** 应用启动时日志目录不存在
- **THEN** 自动创建日志目录，应用正常启动

#### Scenario: 磁盘空间不足
- **WHEN** 磁盘剩余空间低于 500MB
- **THEN** 输出 WARN 级别日志警告，但不阻止启动

### Requirement: SlowQueryInterceptor 慢查询拦截
系统 SHALL 提供 MyBatis `Interceptor` 拦截 `Executor.query` 和 `Executor.update`，当 SQL 执行时间超过配置阈值时，使用独立 Logger `SLOW_QUERY`（WARN 级别）记录 traceId、SQL ID、参数和耗时。

#### Scenario: 正常速度查询
- **WHEN** SQL 执行耗时 200ms，阈值为 1000ms
- **THEN** 不记录慢查询日志

#### Scenario: 超过阈值的查询
- **WHEN** SQL 执行耗时 1500ms，阈值为 1000ms
- **THEN** 使用 Logger `SLOW_QUERY` 输出 WARN 级别日志，包含 traceId、MappedStatement ID、SQL 参数、耗时

#### Scenario: 组件默认不启用
- **WHEN** `logging.slow-query.enabled` 未配置或为 false
- **THEN** SlowQueryInterceptor 不注册为 Bean

### Requirement: SamplingTurboFilter 日志采样
系统 SHALL 提供 Logback `TurboFilter`，ERROR 级别始终记录，其余级别按采样率（`1/rate`）间隔放行。使用 AtomicInteger 计数器实现无锁采样。

#### Scenario: ERROR 级别不受采样影响
- **WHEN** 采样率为 0.1（10%），且产生一条 ERROR 日志
- **THEN** 该 ERROR 日志始终被记录

#### Scenario: 采样放行
- **WHEN** 采样率为 0.1，计数器到达第 10 的倍数
- **THEN** 该条 INFO/WARN/DEBUG 日志被放行记录

#### Scenario: 组件默认不启用
- **WHEN** `logging.sampling.enabled` 未配置或为 false
- **THEN** SamplingTurboFilter 不注册

### Requirement: AuditLogService 审计日志
系统 SHALL 提供 `AuditLogService`，使用独立 Logger `AUDIT_LOGGER`（INFO 级别）记录审计事件。审计事件包含 auditType、userId、operation、resource、result、timestamp、clientIp、context 字段。提供通用工厂方法（`userLogin`、`userLogout`、`permissionChange`、`dataDelete`、`configChange`）。

#### Scenario: 记录审计事件
- **WHEN** 调用 `auditLogService.log(AuditEvent.userLogin(userId, ip, true))`
- **THEN** 使用 Logger `AUDIT_LOGGER` 输出 INFO 级别日志，包含完整审计事件字段

#### Scenario: 审计日志写入独立文件
- **WHEN** 产生审计日志
- **THEN** 日志同时写入审计文件（AUDIT_FILE appender）和主日志文件

### Requirement: AuditEvent 审计事件值对象
系统 SHALL 提供 `AuditEvent` record（不可变值对象），包含字段：auditType（String）、userId（String）、operation（String）、resource（String）、result（String）、timestamp（LocalDateTime）、clientIp（String）、device（String）、context（Map<String, Object>）。

#### Scenario: 创建审计事件
- **WHEN** 调用 `AuditEvent.userLogin("user1", "192.168.1.1", true)`
- **THEN** 返回的 AuditEvent 的 auditType 为 "USER_LOGIN"，userId 为 "user1"，clientIp 为 "192.168.1.1"，result 为 "SUCCESS"

### Requirement: SensitiveLogUtils 日志脱敏
系统 SHALL 提供脱敏工具，默认将字符串中间 75% 替换为 `*`，前后各保留 12.5%。支持自定义脱敏比例。

#### Scenario: 标准脱敏
- **WHEN** 调用 `SensitiveLogUtils.desensitize("13812345678")`
- **THEN** 返回脱敏后的字符串，如 `138***5678`

#### Scenario: 短字符串脱敏
- **WHEN** 调用 `SensitiveLogUtils.desensitize("ab")`
- **THEN** 返回 `a*`（保留首位，其余替换）

### Requirement: LogMarkers 通用日志标记
系统 SHALL 提供通用日志标记常量：API、BUSINESS、SECURITY、AUDIT、SLOW_QUERY。不绑定具体业务域（如 ORDER、PAYMENT），允许用户按需扩展。

#### Scenario: 使用日志标记
- **WHEN** 调用 `log.info(LogMarkers.API, "API call: {}", path)`
- **THEN** 日志携带 Marker `API`，可被 logback 按 Marker 路由到特定 Appender

### Requirement: logback-spring.xml 多 Appender 架构
logback-spring.xml SHALL 包含 7 个 Appender：CONSOLE、FILE（+ASYNC）、JSON_FILE（+ASYNC）、ERROR_FILE、AUDIT_FILE、SLOW_QUERY_FILE、CURRENT（+ASYNC）。通过 Spring Profile 控制环境差异。

#### Scenario: dev 环境日志输出
- **WHEN** 激活 `dev` profile
- **THEN** 输出到 CONSOLE + FILE + CURRENT + ERROR_FILE，不输出 JSON_FILE

#### Scenario: prod 环境日志输出
- **WHEN** 激活 `prod` profile
- **THEN** 不输出 CONSOLE，输出到 FILE + JSON_FILE + ERROR_FILE + AUDIT_FILE

#### Scenario: 审计日志保留策略
- **WHEN** 审计日志文件滚动
- **THEN** 审计日志保留 180 天，错误日志保留 60 天，主日志保留 30 天

### Requirement: LoggingConfigure 条件注册
系统 SHALL 提供 `@Configuration` 类，使用 `@ConditionalOnProperty` 条件注册各日志组件：
- LoggingConfiguration：无条件注册
- SlowQueryInterceptor：`logging.slow-query.enabled=true`
- SamplingTurboFilter：`logging.sampling.enabled=true`

#### Scenario: 默认配置下仅启动验证注册
- **WHEN** 未配置任何 logging 开关
- **THEN** 仅 LoggingConfiguration 注册为 Bean，SlowQueryInterceptor 和 SamplingTurboFilter 不注册

#### Scenario: 全部启用
- **WHEN** 配置 `logging.slow-query.enabled=true` 和 `logging.sampling.enabled=true`
- **THEN** LoggingConfiguration、SlowQueryInterceptor、SamplingTurboFilter 均注册为 Bean

### Requirement: ContextRunnable/ContextCallable 同步 MDC
ContextRunnable 和 ContextCallable SHALL 在跨线程传递时同步 MDC 上下文，确保异步线程中的日志也包含 traceId。

#### Scenario: 异步线程中的 traceId
- **WHEN** 主线程 MDC 中有 `traceId=abc123`，通过 ContextRunnable 提交到线程池
- **THEN** 线程池中执行时 MDC 仍有 `traceId=abc123`，执行完毕后清理
