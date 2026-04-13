## 0. 前置检查

- [x] 0.1 确认 test-driven-development、dispatching-parallel-agents 技能已加载（未加载则加载，已加载则跳过）

## 1. P0 Bug 修复：MDC traceId 注入

- [x] 1.1 RED：编写 ContextFillFilter MDC 注入的失败测试（覆盖场景：请求携带 traceId 头 → MDC 有值；请求无 traceId 头 → MDC 有 UUID；请求异常 → MDC 清理）
- [x] 1.2 GREEN：修改 ContextFillFilter.java，在 doFilterInternal 中添加 MDC.put("traceId", traceId)，在 finally 中 MDC.remove("traceId")，确保异常时也清理
- [x] 1.3 RED：编写 logback pattern 包含 traceId 的失败测试（覆盖场景：有 MDC traceId 时日志包含 traceId；无 MDC traceId 时日志包含空括号）
- [x] 1.4 GREEN：修改 logback-spring.xml 所有 Appender 的 pattern，添加 %X{traceId:-} 占位符
- [x] 1.5 RED：编写 ContextRunnable/ContextCallable MDC 同步的失败测试（覆盖场景：异步线程继承主线程 MDC traceId；异步线程执行后 MDC 清理）
- [x] 1.6 GREEN：修改 ContextRunnable.java 和 ContextCallable.java，在 run/call 前复制调用线程的 MDC，执行后清理 MDC
- [x] 1.7 REFACTOR：重构上述代码，确保 MDC 操作一致且无泄漏风险

## 2. P1 日志基础设施：LoggingProperties + LoggingConfiguration

- [x] 2.1 RED：编写 LoggingProperties 的失败测试（覆盖场景：默认值 slow-query.enabled=false, threshold-ms=1000, sampling.enabled=false, sampling.rate=0.1；自定义值绑定正确）
- [x] 2.2 GREEN：创建 config/properties/LoggingProperties.java（@ConfigurationProperties(prefix="logging")，包含 slow-query 和 sampling 两组属性）
- [x] 2.3 RED：编写 LoggingConfiguration 启动验证的失败测试（覆盖场景：日志目录不存在 → 自动创建；日志目录不可写 → 抛异常；磁盘空间不足 → WARN 日志；正常情况 → 无异常无警告）
- [x] 2.4 GREEN：创建 config/logging/LoggingConfiguration.java（实现 ApplicationListener<ApplicationReadyEvent>，验证日志目录存在/可写/磁盘空间）
- [x] 2.5 REFACTOR：确保 LoggingProperties 与现有 ThreadPoolProperties 等属性类风格一致

## 3. P1 日志基础设施：SlowQueryInterceptor

- [x] 3.1 RED：编写 SlowQueryInterceptor 的失败测试（覆盖场景：正常速度查询 → 无慢查询日志；超阈值查询 → WARN 日志含 SQL ID/参数/耗时；异常 SQL → finally 清理无泄漏）
- [x] 3.2 GREEN：创建 config/logging/SlowQueryInterceptor.java（MyBatis Interceptor，拦截 Executor.query/update，超阈值用 Logger SLOW_QUERY 记录 WARN）
- [x] 3.3 RED：编写 LoggingConfigure 条件注册的失败测试（覆盖场景：默认配置 → 仅 LoggingConfiguration 注册；启用 slow-query → SlowQueryInterceptor 注册；启用 sampling → SamplingTurboFilter 注册）
- [x] 3.4 GREEN：创建 config/logging/LoggingConfigure.java（@Configuration，使用 @ConditionalOnProperty 条件注册 LoggingConfiguration、SlowQueryInterceptor）

## 4. P2 高级日志：SamplingTurboFilter

- [x] 4.1 RED：编写 SamplingTurboFilter 的失败测试（覆盖场景：ERROR 级别始终放行；采样率 0.1 → 10 条 INFO 放行 1 条；采样率 1.0 → 全部放行）
- [x] 4.2 GREEN：创建 config/logging/SamplingTurboFilter.java（继承 TurboFilter，AtomicInteger 计数器，ERROR 始终记录，其余按 rate 采样）
- [x] 4.3 GREEN：修改 LoggingConfigure.java，添加 SamplingTurboFilter 条件注册（@ConditionalOnProperty("logging.sampling.enabled=true")）

## 5. P2 高级日志：AuditLogService + AuditEvent

- [x] 5.1 RED：编写 AuditEvent record 的失败测试（覆盖场景：userLogin 工厂方法 → auditType=USER_LOGIN, result=SUCCESS；dataDelete 工厂方法 → auditType=DATA_DELETE；自定义构造 → 所有字段正确）
- [x] 5.2 GREEN：创建 service/log/AuditEvent.java（record，包含 auditType/userId/operation/resource/result/timestamp/clientIp/device/context，提供 5 个工厂方法）
- [x] 5.3 RED：编写 AuditLogService 的失败测试（覆盖场景：log 方法 → 使用 AUDIT_LOGGER 输出 INFO；审计事件字段完整输出；同时写入主日志和审计文件）
- [x] 5.4 GREEN：创建 service/log/AuditLogService.java（使用独立 Logger AUDIT_LOGGER，INFO 级别记录审计事件）
- [x] 5.5 REFACTOR：确保 AuditEvent 与现有 record DTO 风格一致

## 6. P2 高级日志：SensitiveLogUtils + LogMarkers

- [x] 6.1 RED：编写 SensitiveLogUtils 的失败测试（覆盖场景：标准 11 位手机号脱敏 → 中间替换为 *；2 位短字符串 → 首位保留其余 *；null/空字符串 → 原样返回；自定义比例脱敏）
- [x] 6.2 GREEN：创建 config/logging/SensitiveLogUtils.java（静态工具方法，默认中间 75% 替换，支持自定义比例）
- [x] 6.3 RED：编写 LogMarkers 的失败测试（覆盖场景：API/BUSINESS/SECURITY/AUDIT/SLOW_QUERY 常量存在且名称正确）
- [x] 6.4 GREEN：创建 config/logging/LogMarkers.java（5 个通用 Marker 常量：API、BUSINESS、SECURITY、AUDIT、SLOW_QUERY）

## 7. logback-spring.xml 多 Appender 扩展

- [x] 7.1 RED：编写集成测试验证 logback 多 Appender 配置（覆盖场景：dev profile → CONSOLE + FILE + ERROR_FILE 激活；prod profile → 无 CONSOLE，JSON_FILE 激活；AUDIT_FILE 仅接收 AUDIT_LOGGER；SLOW_QUERY_FILE 仅接收 SLOW_QUERY Logger）
- [x] 7.2 GREEN：重写 logback-spring.xml，扩展为 7 个 Appender（CONSOLE / FILE+ASYNC / JSON_FILE+ASYNC / ERROR_FILE / AUDIT_FILE / SLOW_QUERY_FILE / CURRENT+ASYNC），添加 springProfile 条件控制，日志格式包含 %X{traceId:-}
- [x] 7.3 GREEN：修改 application-dev.yaml 和 application-prod.yaml，添加 logging.slow-query 和 logging.sampling 配置项（dev 默认关闭，prod 可按需开启）
- [x] 7.4 REFACTOR：验证日志滚动策略（审计 180 天、错误 60 天、主日志 30 天）

## 8. AGENTS.md 入口文件

- [x] 8.1 创建 AGENTS.md（< 200 行，包含：项目概述 + 技术栈 + 快速开始命令 + 核心编码规则 + 项目结构索引 + docs/ 引用）
- [x] 8.2 创建 CLAUDE.md 符号链接指向 AGENTS.md（ln -s AGENTS.md CLAUDE.md）

## 9. docs/ 目录结构创建

- [x] 9.1 创建 docs/ 目录及所有子目录（architecture/、backend/、workflow/、guides/）
- [x] 9.2 创建 docs/README.md（文档导航索引，按分类列出所有文档及简介）
- [x] 9.3 创建 docs/architecture/overview.md（三层架构总览：Controller/Service/Repository 各层职责、允许注解、包结构约定）
- [x] 9.4 创建 docs/architecture/dependency-rules.md（依赖规则：层间调用方向、ArchUnit 守护规则说明）
- [x] 9.5 创建 docs/backend/coding-standards.md（编码规范：允许依赖库清单、Lombok 规范禁止 @Data、命名规范、配置类位置）
- [x] 9.6 创建 docs/backend/testing-guide.md（测试规范：UTest/ITest 命名、UnitTestBase/IntegrationTestBase 使用、Mock 规范、ArchUnit 说明）
- [x] 9.7 创建 docs/backend/error-handling.md（错误处理规范：三级异常体系、ErrorCode 接口、CommonErrorCode 枚举、全局异常处理器行为）
- [x] 9.8 创建 docs/backend/performance-rules.md（性能规则：N+1 预防、MyBatis-Plus 分页、批量操作、索引使用建议）
- [x] 9.9 创建 docs/workflow/tdd-guide.md（TDD 指南：Red-Green-Refactor 流程、质量门控标准）
- [x] 9.10 创建 docs/workflow/openspec-guide.md（OpenSpec 工作流：explore → new → continue → apply → archive 流程说明）
- [x] 9.11 创建 docs/guides/getting-started.md（快速开始：环境要求 Java 25 + Maven、项目初始化、启动命令、测试运行）

## 10. README.md 全面更新

- [x] 10.1 更新 README.md 技术栈表格：将数据库从 H2 改为 SQLite（统一）；添加日志基础设施行（SLF4J + Micrometer + Logback）
- [x] 10.2 更新 README.md 多环境配置命令：dev 环境描述从 "使用 H2 内存数据库" 改为 "使用 SQLite 文件数据库"
- [x] 10.3 更新 README.md 项目特性列表：添加日志系统能力（MDC traceId、慢查询拦截、日志采样、审计追踪、日志脱敏、结构化 JSON 日志）
- [x] 10.4 更新 README.md 项目特性列表：添加 AI 规范体系（AGENTS.md + docs/ 文档体系）

## 11. 前一变更 tasks.md 追加记录

- [x] 11.1 追加 enhance-architecture-from-backend/tasks.md Phase 2 精简任务记录（约 10 个任务，包括删除 BizLogDto/PersistenceHandler/StringifyHandler、重写 BizLog/BizLogAspect/TestController、移除 @MapperScan、更新测试等），全部标记 [x]

## 12. 集成测试与验证

- [x] 12.1 编写/更新 ApplicationStartupITest：验证 LoggingConfiguration Bean 注册、LoggingProperties 默认值
- [x] 12.2 编写 LoggingEnhancementITest（集成测试）：启动应用 → 发送 HTTP 请求 → 验证日志文件中包含 traceId → 验证 JSON Appender 配置（prod profile）
- [x] 12.3 编写 AuditLogITest（集成测试）：调用 AuditLogService → 验证审计日志文件输出内容正确
- [x] 12.4 运行全量测试（mvn test），确认所有测试通过（原有 20 个 + 新增测试）
- [x] 12.5 进行 artifact 文档、讨论结果的一致性检查
