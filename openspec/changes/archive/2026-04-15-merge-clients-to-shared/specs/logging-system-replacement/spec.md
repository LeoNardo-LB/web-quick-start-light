## MODIFIED Requirements

### Requirement: 日志配置注册方式

日志基础设施（SlowQueryInterceptor、SamplingTurboFilter、LogAspect）SHALL 由 app 模块的 `config/LoggingConfigure` 类注册，不再使用
`@AutoConfiguration` + `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。

#### Scenario: AutoConfiguration imports 文件

- **WHEN** 检查 META-INF/spring 目录
- **THEN** 该文件中 SHALL NOT 包含 `org.smm.archetype.client.log.LogAutoConfiguration` 条目

#### Scenario: 日志 Bean 由 app config 注册

- **WHEN** 应用启动并加载日志配置
- **THEN** LogAspect、SlowQueryInterceptor、SamplingTurboFilter SHALL 通过 `@Configuration` + `@Bean` 在 app 模块内注册
