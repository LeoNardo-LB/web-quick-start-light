## ADDED Requirements

### Requirement: AuditEvent 时间字段使用 Instant
`AuditEvent` record 的 `timestamp` 字段 SHALL 从 `LocalDateTime` 改为 `Instant`。所有使用 `timestamp` 的代码 SHALL 适配此变更。

#### Scenario: AuditEvent 使用 Instant
- **WHEN** 创建 AuditEvent 实例
- **THEN** timestamp 字段类型为 `java.time.Instant`

### Requirement: BaseResult 时间字段使用 Instant
`BaseResult` 的 `time` 字段 SHALL 从 `Long`（毫秒时间戳）改为 `Instant`。`success()` 和 `fail()` 工厂方法 SHALL 使用 `Instant.now()` 赋值。

#### Scenario: BaseResult 使用 Instant
- **WHEN** 调用 `BaseResult.success(data)` 或 `BaseResult.fail(message)`
- **THEN** 返回的 result 对象的 time 字段类型为 `java.time.Instant`

### Requirement: 新增类时间字段统一使用 Instant
所有新增的实体、DTO、VO 中涉及时间存储或传输的字段 SHALL 使用 `java.time.Instant`。展示用途的时间格式化（如日志时间戳格式化、目录名生成）不在此约束范围内。

#### Scenario: 新增实体使用 Instant
- **WHEN** 创建新的实体类（如 SystemConfig）包含时间字段
- **THEN** 时间字段类型为 `java.time.Instant`

#### Scenario: 展示用途保持不变
- **WHEN** LocalOssClient 使用 `LocalDate.now()` 生成分层目录名
- **THEN** 保持 `LocalDate.now()` 不变（属于展示用途）

### Requirement: 新增 DTO/VO/Result 使用 Java record
所有新增的纯数据传输类（DTO、VO、Request、Command）SHALL 使用 Java record 而非 class。有继承链的基类（BaseResult、BaseRequest、BaseDO）保持 class 不变。

#### Scenario: 新增 VO 使用 record
- **WHEN** 创建新的 VO 类（如 SystemConfigVO、ConfigGroupVO）
- **THEN** 使用 Java record 关键字定义

#### Scenario: 有继承链的类保持不变
- **WHEN** 查看 BaseResult、BaseRequest、BaseDO 类定义
- **THEN** 仍使用 class 关键字（非 record），保持继承体系
