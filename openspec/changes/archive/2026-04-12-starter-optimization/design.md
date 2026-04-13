## Context

当前项目有 5 个 client 子模块（cache/oss/email/sms/search），每个已具备完整的 Spring Boot Starter 机制（`@AutoConfiguration` + `.imports` + 条件装配）。但存在两个问题：

1. **冗余配置**：app 模块中有 4 个 Configure 类（CacheConfigure、OssConfigure、NotificationConfigure、SearchConfigure）与 client 自身的 AutoConfiguration 完全重复。Bean 注册由 `@ConditionalOnMissingBean` 保证只有一个生效，但代码冗余造成维护困惑。
2. **日志系统与 app 耦合**：日志相关代码（BusinessLog 注解、LogAspect 切面、LoggingConfigure、SamplingTurboFilter、SlowQueryInterceptor 等）散布在 app 模块中，无法被其他项目复用。

所有 client 模块已经添加了 `spring-boot-autoconfigure-processor` 和 `spring-boot-configuration-processor` 依赖，Properties 类大多已提供字段级默认值。

## Goals / Non-Goals

**Goals:**
- 删除 app 模块中与 client AutoConfiguration 完全重复的 4 个 Configure 类
- 将日志系统抽取为独立的 `client-log` starter 模块，引入即用
- 确保所有 client 模块"引入 Maven 依赖 + 配置 yaml 即可用"，无需在 app 中写任何 Java 配置代码

**Non-Goals:**
- 不重命名 client 模块的 artifactId（不改成 `*-spring-boot-starter` 命名规范）
- 不改变 client 模块的接口和公共 API
- 不修改 logback-spring.xml 配置文件
- 不改变 yaml 配置属性前缀（`middleware.*` / `logging.*`）
- 不将 ContextFillFilter、WebExceptionAdvise 等 web 层组件纳入 client-log（它们属于 app 的 web 层）

## Decisions

### Decision 1: 删除 app 冗余 Configure 类

**选择**：直接删除 CacheConfigure、OssConfigure、NotificationConfigure、SearchConfigure。

**理由**：client 模块的 `*AutoConfiguration` 已通过 `.imports` 注册到 Spring Boot 自动装配，且用 `@ConditionalOnMissingBean` 允许用户覆盖。app 的 Configure 类完全重复此逻辑。

**替代方案**：保留 app Configure、删除 client AutoConfiguration → 但这破坏了 client 模块的独立性，使"引入即用"失效。

### Decision 2: client-log 模块包结构

**选择**：`org.smm.archetype.client.log` 作为基包。

**包内结构**：
```
client-log/src/main/java/org/smm/archetype/client/log/
├── BusinessLog.java                          ← 注解（从 app/log/ 迁移）
├── LogAspect.java                            ← 切面（从 app/log/ 迁移）
├── MethodExecutionLog.java                   ← 数据对象（从 app/log/ 迁移）
├── LogAutoConfiguration.java                 ← 自动配置（取代 app/LogConfigure）
├── logging/                                  ← 日志基础设施（从 app/config/logging/ 迁移）
│   ├── LoggingConfiguration.java
│   ├── LoggingConfigure.java                 ← 内部 Bean 注册
│   ├── SamplingTurboFilter.java
│   ├── SlowQueryInterceptor.java
│   ├── LogMarkers.java
│   └── SensitiveLogUtils.java
└── properties/
    └── LoggingProperties.java                ← 从 app/config/properties/ 迁移
```

**理由**：
- 统一 `org.smm.archetype.client.log` 基包，与其他 client 模块一致
- `logging/` 子包放基础设施，`properties/` 子包放配置属性
- `LogAutoConfiguration` 是入口，注册 LogAspect Bean、开启 AOP 代理、注册 LoggingConfiguration

### Decision 3: SlowQueryInterceptor 的 MyBatis 依赖

**选择**：client-log 将 `mybatis-spring` 声明为 `optional` 依赖。SlowQueryInterceptor 通过 `@ConditionalOnClass(Interceptor.class)` 条件装配。

**理由**：SlowQueryInterceptor 依赖 MyBatis 的 `Interceptor` 接口。但 client-log 应该能在非 MyBatis 项目中使用（只启用 BusinessLog 注解功能）。将其设为 optional + 条件装配，实现"有 MyBatis 就启用慢 SQL 监控，没有也正常工作"。

### Decision 4: client-log 的 Micrometer 依赖

**选择**：client-log 依赖 `io.micrometer:micrometer-core`（compile scope），因为 LogAspect 核心功能依赖 MeterRegistry。

**理由**：LogAspect 的计时器和计数器是核心功能而非可选功能。如果去掉 Micrometer，LogAspect 将失去指标能力。

### Decision 5: app 模块日志相关代码的清理

**选择**：迁移后删除 app 中以下文件：
- `app/.../log/BusinessLog.java`
- `app/.../log/LogAspect.java`
- `app/.../log/MethodExecutionLog.java`
- `app/.../config/LogConfigure.java`
- `app/.../config/logging/` 整个目录（6 个文件）
- `app/.../config/properties/LoggingProperties.java`

app 的 `logback-spring.xml` 和 `logback-test.xml` 不动（属于资源文件，不是 Java 代码）。

### Decision 6: clients/pom.xml 新增 client-log 模块

**选择**：在 `clients/pom.xml` 的 `<modules>` 中新增 `<module>client-log</module>`。在 `app/pom.xml` 中将 `client-log` 替换 `app/.../log` 和 `app/.../config/logging` 的功能。

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|----------|
| 删除 app Configure 后 Bean 注入顺序变化 | client AutoConfiguration 用 `@AutoConfiguration` 注解在用户配置之后加载，`@ConditionalOnMissingBean` 保证兼容性 |
| SlowQueryInterceptor 依赖 MyBatis | `@ConditionalOnClass` + optional 依赖确保非 MyBatis 项目不报错 |
| 日志迁移后 app 测试可能编译错误 | 更新 import 路径：`org.smm.archetype.log.*` → `org.smm.archetype.client.log.*` |
| client-log 引入 fastjson2（LogAspect 用到 JSON.toJSONString） | LogAspect 依赖 fastjson2，client-log 需要 compile 引入 |
