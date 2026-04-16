## ADDED Requirements

### Requirement: LogAspect 移除手动 Micrometer 指标

`LogAspect` SHALL 移除所有 Micrometer `Timer`/`Counter` 指标采集逻辑，由 OTel 自动 instrumentation 覆盖 HTTP 请求级别的指标需求。

#### Scenario: LogAspect 不再依赖 MeterRegistry

- **WHEN** 查看 LogAspect 构造函数
- **THEN** SHALL NOT 接受 `MeterRegistry` 参数
- **AND** SHALL NOT 包含 `Timer`、`Counter` 类型的字段
- **AND** SHALL NOT 调用 `Timer.start()`、`Timer.Sample.stop()`、`Counter.increment()`

#### Scenario: @BusinessLog 注解保持功能

- **WHEN** 业务方法标注 `@BusinessLog` 注解
- **THEN** LogAspect SHALL 仍然执行 `@Around` 切面逻辑
- **AND** SHALL 记录 SLF4J 业务日志（方法名、参数、结果、耗时）
- **AND** SHALL 创建 `OperationLogRecord`（traceId 从 OTel Span 获取）
- **AND** SHALL 调用 `OperationLogWriter.write()`（如果 writer 存在）
- **AND** SHALL 执行采样率过滤（`samplingRate` 属性）

#### Scenario: OperationLogRecord traceId 来源

- **WHEN** LogAspect 创建 OperationLogRecord
- **THEN** traceId 字段 SHALL 从 `Span.current().getSpanContext().getTraceId()` 获取
- **AND** SHALL NOT 传空字符串 `""` 等待 writer 填充

### Requirement: LoggingConfigure 移除 MeterRegistry 依赖

`LoggingConfigure` 中 `logAspect()` Bean 方法 SHALL 移除 `MeterRegistry` 参数。

#### Scenario: logAspect Bean 方法签名

- **WHEN** 查看 LoggingConfigure.logAspect() 方法
- **THEN** 方法签名 SHALL NOT 包含 `MeterRegistry` 参数
- **AND** SHALL 仅接受可选的 `OperationLogWriter` 参数

## REMOVED Requirements

### Requirement: LogAspect Micrometer 指标注册

**Reason**: OTel 自动 instrumentation 覆盖 HTTP 请求级别的计时和错误计数，无需手动 Micrometer 指标
**Migration**: 以下 3 个 Micrometer 指标将不再注册：
- `log_aspect_timer_seconds`（Timer）→ 由 OTel HTTP Span 计时替代
- `log_aspect_counter_total`（Counter）→ 由 OTel Span 计数替代
- `log_aspect_errors_total`（Counter）→ 由 OTel 异常 Span 属性替代
