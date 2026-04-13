## MODIFIED Requirements

### Requirement: 日志核心代码位于独立 starter 模块
日志系统的 Java 代码 SHALL 位于 `client-log` starter 模块（`org.smm.archetype.client.log` 包），而非 app 模块。app 模块 SHALL 仅通过 yaml 配置控制日志行为。

#### Scenario: app 模块不包含日志 Java 代码
- **WHEN** 查看 app 模块的 `src/main/java` 目录
- **THEN** 不存在 `org.smm.archetype.log` 包（BusinessLog、LogAspect、MethodExecutionLog）
- **THEN** 不存在 `org.smm.archetype.config.logging` 包（LoggingConfigure、LoggingConfiguration、SamplingTurboFilter、SlowQueryInterceptor、LogMarkers、SensitiveLogUtils）
- **THEN** 不存在 `org.smm.archetype.config.LogConfigure` 类
- **THEN** 不存在 `org.smm.archetype.config.properties.LoggingProperties` 类

#### Scenario: client-log 模块包含全部日志代码
- **WHEN** 查看 client-log 模块的 `src/main/java` 目录
- **THEN** 存在 `org.smm.archetype.client.log` 包及子包，包含 BusinessLog、LogAspect、MethodExecutionLog、LogAutoConfiguration、logging/ 子包、properties/ 子包

#### Scenario: app 代码引用更新为 client-log 包路径
- **WHEN** app 模块中的代码（如 TestController）使用 `@BusinessLog` 注解
- **THEN** import 路径为 `org.smm.archetype.client.log.BusinessLog`（而非旧的 `org.smm.archetype.log.BusinessLog`）

## ADDED Requirements

### Requirement: LogAutoConfiguration 取代 LogConfigure
client-log 模块 SHALL 提供 `LogAutoConfiguration`（`@AutoConfiguration`），取代原 app 模块的 `LogConfigure`（`@Configuration`），并通过 `.imports` 文件注册。

#### Scenario: LogAutoConfiguration 自动装配
- **WHEN** classpath 中存在 `io.micrometer.core.instrument.MeterRegistry`（即有 Micrometer 依赖）
- **THEN** LogAutoConfiguration 加载，注册 LogAspect Bean，启用 `@EnableAspectJAutoProxy`
