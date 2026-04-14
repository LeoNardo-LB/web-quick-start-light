## MODIFIED Requirements

### Requirement: 操作日志 AOP 包路径

@BusinessLog 注解及 LogAspect SHALL 位于 `org.smm.archetype.shared.aspect.operationlog` 包下。

#### Scenario: 包路径变更

- **WHEN** 开发者使用 @BusinessLog 注解
- **THEN** 导入路径 SHALL 为 `org.smm.archetype.shared.aspect.operationlog.BusinessLog`

#### Scenario: OperationLogWriter 接口路径

- **WHEN** app 模块实现操作日志写入
- **THEN** 接口路径 SHALL 为 `org.smm.archetype.shared.aspect.operationlog.OperationLogWriter`

### Requirement: 日志基础设施工具包路径

SlowQueryInterceptor、SamplingTurboFilter、SensitiveLogUtils、LogMarkers、LoggingConfiguration SHALL 位于 `org.smm.archetype.shared.logging` 包下。

#### Scenario: LoggingConfigure Bean 注册

- **WHEN** 应用启动
- **THEN** LogAspect、SlowQueryInterceptor、SamplingTurboFilter SHALL 由 `org.smm.archetype.config.LoggingConfigure` 通过 `@Bean` 方法注册

### Requirement: LoggingProperties 配置路径

LoggingProperties SHALL 位于 `org.smm.archetype.config.properties` 包下，配置前缀保持 `logging` 不变。

#### Scenario: 配置属性绑定

- **WHEN** 应用加载 logging.slow-query.enabled=true
- **THEN** LoggingProperties SHALL 正确绑定到 `config/properties/LoggingProperties.java`

## REMOVED Requirements

### Requirement: client-log 独立模块

**Reason**: 日志是应用层横切关注点，且依赖了 mybatis-plus（SlowQueryInterceptor），超出 client 模块应有的依赖范围
**Migration**: AOP 部分迁移到 `shared/aspect/operationlog/`，日志基础设施迁移到 `shared/logging/`
