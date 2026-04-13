## ADDED Requirements

### Requirement: BusinessLog 注解扩展
系统 SHALL 扩展 `@BusinessLog` 注解，新增 `module`（模块名）、`operation`（操作类型枚举）、`samplingRate`（采集率 0.0-1.0）属性。`value` 属性保持向后兼容。

#### Scenario: 使用扩展属性标注业务方法
- **WHEN** 方法标注了 `@BusinessLog(value="更新配置", module="SYSTEM", operation=OperationType.UPDATE, samplingRate=0.5)`
- **THEN** 系统 SHALL 记录模块为 "SYSTEM"、操作类型为 "UPDATE"、采集率为 50%

#### Scenario: samplingRate 为默认值 1.0 时全量采集
- **WHEN** 方法标注了 `@BusinessLog(value="查询配置")`（未指定 samplingRate）
- **THEN** 系统 SHALL 对该方法的每次调用都进行日志持久化（全量采集）

#### Scenario: 旧版 @BusinessLog 保持向后兼容
- **WHEN** 方法仅使用 `@BusinessLog("描述")` 格式（不使用新属性）
- **THEN** 系统 SHALL 正常工作，module 默认为空，operation 默认为空，samplingRate 默认为 1.0

### Requirement: 操作日志数据库持久化
系统 SHALL 将操作日志异步写入 `operation_log` 数据库表。日志字段包括 id、trace_id、user_id、module、operation_type、description、method、params（JSON，最大 2048）、result（JSON，最大 2048）、execution_time（ms）、ip、status、error_message、create_time。

#### Scenario: 方法执行成功后异步写入日志
- **WHEN** 标注了 `@BusinessLog` 的方法成功执行
- **THEN** 系统 SHALL 异步写入一条 status=SUCCESS 的日志记录到 operation_log 表

#### Scenario: 方法执行失败后异步写入错误日志
- **WHEN** 标注了 `@BusinessLog` 的方法抛出异常
- **THEN** 系统 SHALL 异步写入一条 status=ERROR、error_message 为异常消息的日志记录

#### Scenario: 采集率控制日志持久化
- **WHEN** 方法标注了 `@BusinessLog(samplingRate = 0.5)` 
- **THEN** 系统 SHALL 以约 50% 的概率将日志写入数据库，但 Micrometer 指标和文件日志 SHALL 全量记录

### Requirement: 操作日志分页查询 API
系统 SHALL 提供 `GET /api/system/operation-logs` 分页查询端点，支持按 module、operationType、startTime、endTime 筛选。

#### Scenario: 分页查询操作日志
- **WHEN** 客户端发送 `GET /api/system/operation-logs?pageNo=1&pageSize=20`
- **THEN** 系统 SHALL 返回 BasePageResult<OperationLogVO>，包含 total、pageNo、pageSize 和日志列表

#### Scenario: 按模块和操作类型筛选日志
- **WHEN** 客户端发送 `GET /api/system/operation-logs?module=SYSTEM&operationType=UPDATE`
- **THEN** 系统 SHALL 仅返回匹配模块和操作类型的日志

#### Scenario: 按时间范围筛选日志
- **WHEN** 客户端发送 `GET /api/system/operation-logs?startTime=2026-01-01T00:00:00Z&endTime=2026-04-13T23:59:59Z`
- **THEN** 系统 SHALL 仅返回指定时间范围内的日志
