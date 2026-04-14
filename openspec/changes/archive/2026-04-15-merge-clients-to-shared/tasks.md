## 1. 准备工作

- [x] 1.1 确认 test-driven-development、dispatching-parallel-agents 技能已加载（未加载则加载，已加载则跳过）
- [x] 1.2 在 app/pom.xml 中新增 bucket4j、micrometer、fastjson2 直接依赖（从原 client 模块继承的依赖提升为显式声明）
- [x] 1.3 在 app/pom.xml 中移除 client-log、client-ratelimit、client-idempotent 三个依赖
- [x] 1.4 创建 shared/
  包目录结构：shared/aspect/ratelimit/、shared/aspect/idempotent/、shared/aspect/operationlog/、shared/logging/、shared/util/context/、shared/util/dal/

## 2. 迁移限流模块（client-ratelimit → shared/aspect/ratelimit）

- [x] 2.1 RED：迁移限流测试到 app 测试目录（RateLimitAspectUTest、SpelKeyResolverUTest、BucketFactoryUTest、RateLimitUTest），更新包路径为
  `org.smm.archetype.shared.aspect.ratelimit`，确认测试编译通过
- [x] 2.2 GREEN：迁移限流源码到
  shared/aspect/ratelimit/（RateLimit.java、RateLimitAspect.java、SpelKeyResolver.java、BucketFactory.java、LimitFallback.java），更新包声明，确认测试全绿
- [x] 2.3 REFACTOR：新增 RatelimitConfigure @Configuration 类注册 RateLimitAspect Bean，新增 RateLimitProperties 到 config/properties/，删除
  IdempotentProperties

## 3. 迁移幂等模块（client-idempotent → shared/aspect/idempotent）

- [x] 3.1 RED：迁移幂等测试到 app
  测试目录（IdempotentAspectUTest、IdempotentKeyResolverUTest、IdempotentAnnotationUTest、IdempotentAutoConfigurationUTest），更新包路径为
  `org.smm.archetype.shared.aspect.idempotent`，将测试中对 Caffeine Cache 的 mock 替换为对 CacheClient 的 mock，确认测试编译通过
- [x] 3.2 GREEN：迁移幂等源码到 shared/aspect/idempotent/（Idempotent.java、IdempotentAspect.java、IdempotentKeyResolver.java），更新包声明；改造
  IdempotentAspect 使用 CacheClient（hasKey/put/delete 替代 getIfPresent/put/invalidate），确认测试全绿
- [x] 3.3 REFACTOR：新增 IdempotentConfigure @Configuration 类注册 IdempotentAspect Bean（注入 CacheClient），删除
  IdempotentProperties（参数已在注解上定义）

## 4. 迁移操作日志模块（client-log → shared/aspect/operationlog + shared/logging）

- [x] 4.1 RED：迁移操作日志测试到 app 测试目录（LogAspectUTest、LogAspectDBUTest），更新包路径为 `org.smm.archetype.shared.aspect.operationlog`
  ，确认测试编译通过
- [x] 4.2 GREEN：迁移操作日志 AOP 源码到
  shared/aspect/operationlog/（BusinessLog.java、LogAspect.java、OperationLogWriter.java、OperationLogRecord.java、OperationType.java），更新包声明和
  @Pointcut 引用路径，确认测试全绿
- [x] 4.3 REFACTOR：迁移日志基础设施到
  shared/logging/（LoggingConfiguration.java、SlowQueryInterceptor.java、SamplingTurboFilter.java、SensitiveLogUtils.java、LogMarkers.java），更新
  LogMarkers 内容（删除 ORDER/PAYMENT 占位符，新增 SYSTEM、SECURITY、AUDIT）；迁移 LoggingProperties 到 config/properties/；新增 LoggingConfigure
  @Configuration 类整合所有日志 Bean 注册

## 5. 迁移工具类（util/ → shared/util/）

- [x] 5.1 将 util/ 顶级包下所有内容迁移到 shared/util/：context/（ScopedThreadContext、ContextRunnable、ContextCallable）→
  shared/util/context/；dal/（MyMetaObjectHandler）→ shared/util/dal/；IpUtils、KryoSerializer、SpringContextUtils → shared/util/
- [x] 5.2 更新所有引用 util 包路径的 import 语句（controller/global/ContextFillFilter、facade/operationlog/OperationLogFacadeImpl、util/context/
  内部引用等）

## 6. 清理旧模块和配置

- [x] 6.1 删除 clients/client-ratelimit/ 整个目录
- [x] 6.2 删除 clients/client-idempotent/ 整个目录
- [x] 6.3 删除 clients/client-log/ 整个目录
- [x] 6.4 更新 clients/pom.xml：移除三个 submodule 声明
- [x] 6.5 清理 META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports：移除三个 client 的 AutoConfiguration 条目
- [x] 6.6 清理 client-ratelimit 和 client-idempotent 的 spring.factories（如果存在）

## 7. 更新所有 import 引用

- [x] 7.1 全局搜索并替换 `org.smm.archetype.client.ratelimit` → `org.smm.archetype.shared.aspect.ratelimit`
- [x] 7.2 全局搜索并替换 `org.smm.archetype.client.idempotent` → `org.smm.archetype.shared.aspect.idempotent`
- [x] 7.3 全局搜索并替换 `org.smm.archetype.client.log` → `org.smm.archetype.shared.aspect.operationlog` 或
  `org.smm.archetype.shared.logging`（根据具体类）
- [x] 7.4 全局搜索并替换 `org.smm.archetype.util` → `org.smm.archetype.shared.util`（排除 shared.util 自身）

## 8. 集成测试验证

- [x] 8.1 运行 `mvn test -pl app` 确认所有测试通过（包括迁移后的单元测试和现有集成测试）
- [x] 8.2 运行 `mvn test` 确认全量构建通过（包括 ArchUnit 架构约束检查）
- [x] 8.3 启动应用 `mvn spring-boot:run -pl app`，验证 @RateLimit、@Idempotent、@BusinessLog 注解功能正常

## 9. 一致性检查

- [x] 9.1 进行 artifact 文档与讨论结果的一致性检查
