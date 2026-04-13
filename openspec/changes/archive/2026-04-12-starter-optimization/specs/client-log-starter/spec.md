## ADDED Requirements

### Requirement: client-log 模块自动装配
client-log 模块 SHALL 提供 `LogAutoConfiguration`，通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册，实现引入 Maven 依赖即可自动装配 BusinessLog 注解的 AOP 支持。

#### Scenario: 引入 client-log 依赖后自动启用 BusinessLog 注解
- **WHEN** 项目在 pom.xml 中添加 `client-log` 依赖，并在方法上标注 `@BusinessLog`
- **THEN** 应用启动后 LogAspect Bean 自动注册，被 `@BusinessLog` 标注的方法执行时自动记录方法日志（类名、方法名、参数、返回值、耗时）和 Micrometer 指标

#### Scenario: 未引入 client-log 依赖时 BusinessLog 注解不生效
- **WHEN** 项目未添加 `client-log` 依赖
- **THEN** `@BusinessLog` 注解类不在 classpath 中，编译不可用，不会产生任何 AOP 代理

### Requirement: LogAutoConfiguration 注册 LogAspect Bean
LogAutoConfiguration SHALL 注册 `LogAspect` Bean（依赖 `MeterRegistry`），并启用 `@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)`。

#### Scenario: LogAspect Bean 正常注册
- **WHEN** classpath 中存在 `io.micrometer.core.instrument.MeterRegistry`
- **THEN** `LogAutoConfiguration` 创建 LogAspect Bean 并注册到 Spring 容器

#### Scenario: MeterRegistry 不存在时不注册 LogAspect
- **WHEN** classpath 中不存在 `MeterRegistry`
- **THEN** `LogAutoConfiguration` 不加载（通过 `@ConditionalOnClass(MeterRegistry.class)` 控制），不报错

### Requirement: 慢 SQL 监控条件装配
client-log 模块 SHALL 通过 `@ConditionalOnClass(Interceptor.class)` 和 `@ConditionalOnProperty(name = "logging.slow-query.enabled", havingValue = "true")` 条件装配 `SlowQueryInterceptor`。MyBatis 依赖 SHALL 声明为 optional。

#### Scenario: 有 MyBatis 且启用慢 SQL 监控
- **WHEN** classpath 存在 MyBatis Interceptor 且 `logging.slow-query.enabled=true`
- **THEN** `SlowQueryInterceptor` Bean 自动注册，超过阈值的 SQL 记录到 SLOW_QUERY logger

#### Scenario: 无 MyBatis 依赖
- **WHEN** classpath 不存在 `org.apache.ibatis.plugin.Interceptor`
- **THEN** SlowQueryInterceptor 相关配置类不加载，应用正常启动不报错

### Requirement: 日志采样条件装配
client-log 模块 SHALL 通过 `@ConditionalOnProperty(name = "logging.sampling.enabled", havingValue = "true")` 条件装配 `SamplingTurboFilter`。

#### Scenario: 启用日志采样
- **WHEN** `logging.sampling.enabled=true` 且 `logging.sampling.rate=0.1`
- **THEN** SamplingTurboFilter 注册到 Logback，ERROR 级别始终通过，其他级别按 10% 采样率通过

### Requirement: 日志目录自动验证
client-log 模块 SHALL 在 `ApplicationReadyEvent` 时验证日志目录是否存在可写，磁盘空间是否充足（>500MB）。

#### Scenario: 日志目录不存在时自动创建
- **WHEN** 应用启动且 `logging.file.path` 指向的目录不存在
- **THEN** 自动创建目录并记录 INFO 日志

### Requirement: 公开工具类
client-log 模块 SHALL 公开 `LogMarkers`（日志标记常量）和 `SensitiveLogUtils`（脱敏工具），供业务代码直接使用。

#### Scenario: 业务代码使用 LogMarkers
- **WHEN** 业务代码 `import org.smm.archetype.client.log.logging.LogMarkers`
- **THEN** 可使用 `LogMarkers.AUDIT`、`LogMarkers.SECURITY` 等标记

#### Scenario: 业务代码使用 SensitiveLogUtils 脱敏
- **WHEN** 调用 `SensitiveLogUtils.mask("13812345678")`
- **THEN** 返回脱敏后的字符串（如 `138****5678`）
