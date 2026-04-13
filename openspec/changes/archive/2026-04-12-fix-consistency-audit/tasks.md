## 1. 预备

- [x] 1.1 确认 test-driven-development、dispatching-parallel-agents 技能已加载（未加载则加载，已加载则跳过）

## 2. Chunk A: 文档批量修正（11 个文件，低风险）

> 纯文档改动，不涉及代码，可并行修改。

- [x] 2.1 修正 `docs/guides/getting-started.md` 验证端点：端口 8080→9201、补充 context-path /quickstart-light、Swagger 路径 /swagger-ui.html → /openapi-doc.html、API Docs 路径补充 context-path
- [x] 2.2 修正 `docs/guides/getting-started.md` 多环境配置表：prod 数据库 PostgreSQL → SQLite；补充 test profile 行（SQLite 内存库 + DEBUG）
- [x] 2.3 修正 `docs/backend/performance-rules.md`：生产慢查询阈值 500ms → 1000ms（与 application-prod.yaml 一致）
- [x] 2.4 修正 `docs/backend/error-handling.md`：CommonErrorCode 的 5 个 message 文本改为代码实际值（success/操作失败/外部服务调用失败/系统异常/未知异常）
- [x] 2.5 修正 `docs/backend/error-handling.md`：WebExceptionAdvise 代码示例，补充 ConstraintViolationException 和 BindException 处理器、返回类型改为 ResponseEntity、BizException handler 补充 e.getMessage() 参数、补充 SysException 处理器
- [x] 2.6 修正 `AGENTS.md` 技术栈表格：删除重复的 SQLite 行（"数据库（开发）"和"数据库"合并为一行"数据库"）
- [x] 2.7 修正 `AGENTS.md` client-sms 描述：从"仅 SmsClient 接口 + SmsProperties，无真实实现"改为"包含 SmsClient 接口 + SmsProperties + AbstractSmsClient + SmsAutoConfiguration，无真实实现"
- [x] 2.8 修正 `docs/architecture/overview.md`：在 ResultCode 的结构树条目后标注"（已废弃，使用 CommonErrorCode 代替）"
- [x] 2.9 修正 `AGENTS.md` Hutool 行：在说明列标注"预置工具库（暂未使用）"
- [x] 2.10 修正 `docs/backend/testing-guide.md`：补充 `mvn test`（从根目录运行所有模块测试）命令并说明与 `mvn test -pl app` 的区别
- [x] 2.11 修正 `README.md`：补充 `mvn test -Dtest="*UTest"`、`mvn test -Dtest="*ITest"`、`scripts/start.sh prod`、`scripts/stop.sh` 到快速开始部分

## 3. Chunk B: 配置文件修正（3 个文件，低风险）

- [x] 3.1 重写 `app/src/main/resources/application-optional.yaml`：前缀改为 middleware.*（middleware.cache/middleware.object-storage/middleware.email/middleware.sms）、字段名对应 Properties 类、删除 Redis 幽灵配置（spring.data.redis）
- [x] 3.2 修正 `app/src/test/resources/schema.sql`：`deleted INTEGER DEFAULT 0` → `delete_time BIGINT DEFAULT 0`
- [x] 3.3 修正 `app/src/test/resources/application-test.yaml`：补充 `map-underscore-to-camel-case: true` 和 `id-type: assign_id` 到 mybatis-plus.configuration 和 mybatis-plus.global-config.db-config 下

## 4. Chunk C: 代码规范修正 — @Data 替换（6 个文件）

- [x] 4.1 RED：编写 ArchUnit 或自定义测试，断言项目中所有 Java 文件不存在 @Data 注解（搜索 @lombok.Data 或 @Data 的使用）
- [x] 4.2 GREEN：将 `app/src/main/java/org/smm/archetype/entity/api/BaseRequest.java` 的 @Data 替换为 @Getter @Setter
- [x] 4.3 GREEN：将 `app/src/main/java/org/smm/archetype/entity/api/BasePageRequest.java` 的 @Data 替换为 @Getter @Setter @EqualsAndHashCode(callSuper = true)
- [x] 4.4 GREEN：将 `app/src/main/java/org/smm/archetype/entity/api/BaseResult.java` 的 @Data 替换为 @Getter @Setter
- [x] 4.5 GREEN：将 `app/src/main/java/org/smm/archetype/entity/api/BasePageResult.java` 的 @Data 替换为 @Getter @Setter @EqualsAndHashCode(callSuper = true)
- [x] 4.6 GREEN：将 `app/src/main/java/org/smm/archetype/config/properties/ThreadPoolProperties.java` 的 @Data 替换为 @Getter @Setter
- [x] 4.7 GREEN：将 `app/src/main/java/org/smm/archetype/config/properties/LoggingProperties.java` 及其内部类的 @Data 替换为 @Getter @Setter
- [x] 4.8 REFACTOR：运行 `mvn test -pl app` 确认所有测试通过（覆盖正常场景：DTO 序列化/反序列化、Properties 绑定）

## 5. Chunk D: 代码规范修正 — @Value → AppInfoProperties（2 个文件）

- [x] 5.1 RED：编写测试，断言 WebStartLightApplication 不使用 @Value 注解
- [x] 5.2 GREEN：创建 `app/src/main/java/org/smm/archetype/config/properties/AppInfoProperties.java`（@ConfigurationProperties(prefix = "app")，包含 port/contextPath/appName/openapiUrl/apiDocUrl 字段）
- [x] 5.3 GREEN：修改 `WebStartLightApplication.java`：注入 AppInfoProperties 替代 5 处 @Value，CommandLineRunner.run() 中使用 properties.getXxx()
- [x] 5.4 GREEN：在 `application.yaml` 中添加 app.prefix 下的配置（引用已有的 server.port、server.servlet.context-path、spring.application.name、springdoc.swagger-ui.path、springdoc.api-docs.path）
- [x] 5.5 REFACTOR：运行 `mvn test -pl app` 确认所有测试通过

## 6. Chunk E: client-search 补全 Template Method 模式

- [x] 6.1 RED：编写单元测试，断言 client-search 模块存在 AbstractSearchClient 抽象基类，且其公开方法为 final
- [x] 6.2 GREEN：创建 `clients/client-search/src/main/java/org/smm/archetype/client/search/AbstractSearchClient.java`（implements SearchClient，final 模板方法 + protected do* 扩展点，与 AbstractCacheClient 模式一致）
- [x] 6.3 GREEN：修改 `clients/client-search/src/main/java/org/smm/archetype/client/search/SimpleSearchClient.java`：从 implements SearchClient 改为 extends AbstractSearchClient
- [x] 6.4 REFACTOR：运行 `mvn test` 确认所有模块测试通过

## 7. Chunk F: 脚本修正（2 个文件）

- [x] 7.1 修正 `scripts/start.sh`：将日志提示 `tail -f ${LOG_DIR}/spring.log` 改为按 profile 输出正确的日志文件名（prod → app.log，dev → current.log）
- [x] 7.2 修正 `scripts/stop.sh`：在 `kill -0 ${PID}` 后增加 `/proc/${PID}/cmdline` 检查验证进程名包含 "java"
- [x] 7.3 验证：运行 `mvn test -pl app` 确认脚本修改不影响构建

## 8. Chunk G: logback-spring.xml 修正

- [x] 8.1 修正 `app/src/main/resources/logback-spring.xml`：删除第 4 行 `<include resource="org/springframework/boot/logging/logback/console-appender.xml"/>`
- [x] 8.2 验证：运行 `mvn test -pl app` 确认日志配置正常

## 9. Chunk H: POM 版本统一管理（4 个文件）

- [x] 9.1 修正根 `pom.xml`：将 app/pom.xml 中的 11 个硬编码版本提取到 properties（springdoc.version/guava.version/commons-lang3.version 等），添加到 dependencyManagement
- [x] 9.2 修正根 `pom.xml`：添加 fastjson2 到 properties + dependencyManagement
- [x] 9.3 修正根 `pom.xml`：删除 caffeine.version 死属性（caffeine 由 Spring Boot BOM 管理，无需显式声明）
- [x] 9.4 修正 `app/pom.xml`：移除硬编码版本号，改用 ${} 属性引用
- [x] 9.5 修正 `clients/client-search/pom.xml`：fastjson2 版本改用 ${fastjson2.version}
- [x] 9.6 修正 `clients/pom.xml`：删除冗余的 dependencyManagement 段
- [x] 9.7 验证：运行 `mvn clean package -DskipTests` 确认构建成功；运行 `mvn dependency:tree -pl app` 确认版本正确
## 10. Chunk I: OpenSpec 回写

- [x] 10.1 将 `openspec/changes/enhance-architecture-from-backend/` 移动到 `openspec/changes/archive/2026-04-12-enhance-architecture-from-backend/`
- [x] 10.2 更新 `openspec/changes/modularize-clients/tasks.md`：将已实现的功能标记为 [x]（对照实际代码逐项核对）
- [x] 10.3 更新 `openspec/changes/modularize-clients/specs/` 中的 specs：修正 Client 接口方法签名、Properties 字段、AutoConfiguration 条件、错误码格式（字符串→int），以实际代码为准
- [x] 10.4 更新 `openspec/changes/modularize-clients/proposal.md` 和 `design.md`：补充 common 模块的描述
- [x] 10.5 清理 `openspec/changes/modularize-clients/` 和 `openspec/changes/enhance-architecture-from-backend/` 中的 H2 残留引用和 BaseDO 旧路径引用
- [x] 10.6 验证：grep 确认 OpenSpec 文件中无 "H2"、"entity/api/exception"、"entity/dataobj" 等过时引用

## 11. 全量验证

- [x] 11.1 运行 `mvn clean test` 确认全部 136 测试通过（代码修改后）
- [x] 11.2 运行 `mvn clean package -DskipTests` 确认构建成功
- [x] 11.3 进行 artifact 文档、讨论结果的一致性检查
