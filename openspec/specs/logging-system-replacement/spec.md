## ADDED Requirements

### Requirement: 删除现有日志系统代码
以下文件 SHALL 被删除：LoggingConfigure、LoggingConfiguration、SamplingTurboFilter、SlowQueryInterceptor、LogMarkers、SensitiveLogUtils、LoggingProperties、BizLog、BizLogAspect、AuditLogService、AuditEvent、ContextFillFilter、logback-spring.xml（共 13 个文件）。

#### Scenario: 现有日志文件已删除
- **WHEN** 检查 app 模块的 config/logging/、util/log/、service/log/、controller/global/ 目录
- **THEN** 上述 13 个文件不存在

### Requirement: 移植 speccoding 日志系统
SHALL 从 speccoding-fullstack 移植以下日志组件到 app 模块：LoggingConfigure、LoggingConfiguration、SamplingTurboFilter、SlowQueryInterceptor、LogMarkers、SensitiveLogUtils、LoggingProperties、LogConfigure、BusinessLog（替代 BizLog）、LogAspect（替代 BizLogAspect）、MethodExecutionLog、ContextFillFilter、logback-spring.xml、MemoryLogAppender（测试用）。

#### Scenario: 日志组件已移植
- **WHEN** 检查 app 模块
- **THEN** 上述 14 个文件存在且包路径适配到 `org.smm.archetype.*`

### Requirement: BusinessLog 注解替代 BizLog
项目 SHALL 使用 `@BusinessLog` 注解替代原来的 `@BizLog`。所有使用 `@BizLog` 的代码 SHALL 更新为 `@BusinessLog`。

#### Scenario: 注解引用已更新
- **WHEN** 搜索项目代码中的 `@BizLog` 引用
- **THEN** 不存在任何 `@BizLog` 引用，所有处已改为 `@BusinessLog`

### Requirement: MethodExecutionLog 适配为独立类
MethodExecutionLog SHALL 作为独立类（使用 `@Getter @Builder`），不继承 Entity 或 BaseDO。

#### Scenario: MethodExecutionLog 无领域基类依赖
- **WHEN** 查看 MethodExecutionLog 的类定义
- **THEN** 不继承 domain.shared.base.Entity 或 BaseDO，使用 Lombok @Getter @Builder

### Requirement: logback-spring.xml 包含 8 个 Appender
logback-spring.xml SHALL 配置以下 Appender：CONSOLE、ASYNC_FILE、ASYNC_JSON_FILE（仅 prod）、ASYNC_CURRENT、ERROR_FILE、WARN_FILE、SLOW_QUERY_FILE、AUDIT_FILE（180 天保留）。

#### Scenario: 生产环境日志完整
- **WHEN** 应用以 prod profile 启动
- **THEN** 日志输出到 ASYNC_FILE、ASYNC_JSON_FILE、ERROR_FILE、WARN_FILE、SLOW_QUERY_FILE、AUDIT_FILE

### Requirement: 配置 spring.application.name
application.yaml SHALL 配置 `spring.application.name`，供 logback-spring.xml 引用 `${spring.application.name}`。

#### Scenario: 日志中包含服务名
- **WHEN** 应用启动并输出日志
- **THEN** 日志格式中包含服务名称

### Requirement: 条件装配日志组件
SamplingTurboFilter 和 SlowQueryInterceptor SHALL 通过 `@ConditionalOnProperty` 条件装配，默认不启用。

#### Scenario: 默认不启用采样过滤器
- **WHEN** 未配置 `logging.sampling.enabled=true`
- **THEN** SamplingTurboFilter Bean 不注册

#### Scenario: 配置后启用慢查询拦截
- **WHEN** 配置 `logging.slow-query.enabled=true` 和 `logging.slow-query.threshold-ms=1000`
- **THEN** SlowQueryInterceptor Bean 注册，超过阈值的 SQL 记录到 SLOW_QUERY logger
