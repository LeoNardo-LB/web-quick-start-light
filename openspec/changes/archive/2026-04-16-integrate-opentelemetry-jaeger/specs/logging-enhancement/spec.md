## MODIFIED Requirements

### Requirement: LoggingConfigure Bean 注册

LoggingConfigure 中 LogAspect Bean 的注册 SHALL 移除 MeterRegistry 依赖。

#### Scenario: LogAspect Bean 注册（变更后）

- **WHEN** 应用启动并执行 LoggingConfigure.logAspect()
- **THEN** LogAspect SHALL 在无 MeterRegistry 参数的情况下创建
- **AND** OperationLogWriter 仍 SHALL 通过 `@Autowired(required = false)` 可选注入

### Requirement: 日志 traceId 关联

logback-spring.xml 中的 `%X{traceId:--}` 格式 SHALL 通过 OTel 自动 MDC 注入获得有效值。

#### Scenario: OTel 自动填充 MDC traceId

- **WHEN** 应用处理 HTTP 请求并输出日志
- **THEN** 日志中的 `%X{traceId}` SHALL 显示当前请求的 OTel traceId（非 `--`）
- **AND** OTel traceId SHALL 由 Spring Boot Micrometer Tracing bridge 自动写入 MDC

#### Scenario: JSON 日志包含 traceId 和 spanId

- **WHEN** JSON_FILE appender 输出日志（生产环境）
- **THEN** JSON 结构中 SHALL 包含 `traceId` 字段（来自 MDC）
- **AND** JSON 结构中 SHALL 包含 `spanId` 字段（来自 MDC，OTel 自动注入）
