## 1. 准备工作

- [x] 1.1 确认 test-driven-development、dispatching-parallel-agents 技能已加载（未加载则加载，已加载则跳过）

## 2. 删除 app 冗余 Configure 类

- [x] 2.1 RED: 编写集成测试验证当前 client Bean 由 AutoConfiguration 注册（CacheClient、OssClient、SearchClient Bean 存在性断言）
- [x] 2.2 GREEN: 确认现有测试通过（client AutoConfiguration 已生效）
- [x] 2.3 REFACTOR: 删除 app 模块的 CacheConfigure.java、OssConfigure.java、NotificationConfigure.java、SearchConfigure.java
- [x] 2.4 验证: 运行 `mvn test -pl app` 确认全部测试通过，所有 client Bean 正常注入

## 3. 创建 client-log 模块结构

- [x] 3.1 创建 `clients/client-log/` 目录结构及 pom.xml（依赖 common、aspectjweaver、micrometer-core、fastjson2、mybatis-plus-spring-boot3-starter[optional]、logback-classic[optional]、hutool-core、spring-boot-autoconfigure-processor、spring-boot-configuration-processor、lombok）
- [x] 3.2 在 `clients/pom.xml` 的 `<modules>` 中新增 `<module>client-log</module>`
- [x] 3.3 在 `app/pom.xml` 中新增 `client-log` 依赖

## 4. 迁移日志代码至 client-log

- [x] 4.1 迁移 `BusinessLog.java` 至 `org.smm.archetype.client.log.BusinessLog`（仅改包名）
- [x] 4.2 迁移 `MethodExecutionLog.java` 至 `org.smm.archetype.client.log.MethodExecutionLog`（仅改包名）
- [x] 4.3 迁移 `LogAspect.java` 至 `org.smm.archetype.client.log.LogAspect`（改包名 + 更新 @Pointcut 中 BusinessLog 的全限定名为新包路径）
- [x] 4.4 迁移 `LoggingProperties.java` 至 `org.smm.archetype.client.log.properties.LoggingProperties`（改包名）
- [x] 4.5 迁移 `LogMarkers.java` 至 `org.smm.archetype.client.log.logging.LogMarkers`（改包名）
- [x] 4.6 迁移 `SensitiveLogUtils.java` 至 `org.smm.archetype.client.log.logging.SensitiveLogUtils`（改包名）
- [x] 4.7 迁移 `SamplingTurboFilter.java` 至 `org.smm.archetype.client.log.logging.SamplingTurboFilter`（改包名）
- [x] 4.8 迁移 `LoggingConfiguration.java` 至 `org.smm.archetype.client.log.logging.LoggingConfiguration`（改包名 + 更新 LoggingProperties import）
- [x] 4.9 迁移 `SlowQueryInterceptor.java` 至 `org.smm.archetype.client.log.logging.SlowQueryInterceptor`（改包名 + 更新 LoggingProperties import）
- [x] 4.10 迁移 `LoggingConfigure.java` 至 `org.smm.archetype.client.log.logging.LoggingConfigure`（改包名 + 更新 LoggingProperties/SamplingTurboFilter/LoggingConfiguration/SlowQueryInterceptor import）

## 5. 创建 LogAutoConfiguration

- [x] 5.1 RED: 编写 LogAutoConfigurationUTest — 验证 LogAspect Bean 注册条件（有 MeterRegistry 时注册，无时不注册）
- [x] 5.2 GREEN: 创建 `LogAutoConfiguration.java`（@AutoConfiguration + @ConditionalOnClass(MeterRegistry) + @EnableAspectJAutoProxy + @EnableConfigurationProperties(LoggingProperties) + 注册 LogAspect Bean + @Import(LoggingConfigure)）
- [x] 5.3 创建 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件注册 LogAutoConfiguration

## 6. 清理 app 模块日志代码

- [x] 6.1 删除 `app/.../log/BusinessLog.java`
- [x] 6.2 删除 `app/.../log/LogAspect.java`
- [x] 6.3 删除 `app/.../log/MethodExecutionLog.java`
- [x] 6.4 删除 `app/.../config/LogConfigure.java`
- [x] 6.5 删除 `app/.../config/logging/LoggingConfigure.java`
- [x] 6.6 删除 `app/.../config/logging/LoggingConfiguration.java`
- [x] 6.7 删除 `app/.../config/logging/SamplingTurboFilter.java`
- [x] 6.8 删除 `app/.../config/logging/SlowQueryInterceptor.java`
- [x] 6.9 删除 `app/.../config/logging/LogMarkers.java`
- [x] 6.10 删除 `app/.../config/logging/SensitiveLogUtils.java`
- [x] 6.11 删除 `app/.../config/properties/LoggingProperties.java`

## 7. 更新 app 模块引用

- [x] 7.1 更新 `TestController.java` 的 import: `org.smm.archetype.log.BusinessLog` → `org.smm.archetype.client.log.BusinessLog`
- [x] 7.2 更新 `SystemConfigService.java` 的 LogMarkers import（无使用，无需更新）
- [x] 7.3 更新 `ContextFillFilter.java`（无日志相关引用，无需更新）
- [x] 7.4 更新 `WebExceptionAdvise.java`（无 LogMarkers/SensitiveLogUtils 引用，无需更新）
- [x] 7.5 更新 app 测试代码中所有旧包路径的 import（测试文件已迁移/删除）
- [x] 7.6 更新 `MybatisPlusGenerator.java`（无日志相关引用，无需更新）

## 8. 迁移日志测试

- [x] 8.1 将 app 中日志相关测试迁移至 client-log 模块（BusinessLogUTest、LogMarkersUTest、SensitiveLogUtilsUTest）
- [x] 8.2 运行 `mvn test -pl clients/client-log` 确认 client-log 测试通过（4 tests, 0 failures）

## 9. 构建验证

- [x] 9.1 运行 `mvn clean compile -DskipTests` 确认全项目编译通过（BUILD SUCCESS）
- [x] 9.2 运行 `mvn test` 确认全项目测试通过（102 tests, 0 failures, 0 errors）
- [x] 9.3 启动应用 `mvn spring-boot:run -pl app` 确认正常启动（1.9s）且 LogAutoConfiguration 日志输出正常
- [x] 9.4 调用 API 验证 @BusinessLog 注解功能正常（GET /test/hello + /test/bizlog + /system/configs 全部 200 OK）

## 10. 文档更新

- [x] 10.1 更新 AGENTS.md：clients 模块列表新增 client-log、删除冗余 Configure 描述、更新日志包路径
- [x] 10.2 更新 docs/architecture/overview.md：新增 client-log 模块条目、更新 app 模块目录树

## 11. 一致性检查

- [x] 11.1 进行 artifact 文档、讨论结果的一致性检查
