## Context

当前项目 `clients/` 目录下有 9 个模块，其中 6 个（cache、oss、email、sms、search、auth）遵循 Template Method 模式，是真正的中间件接入层。另外 3
个（ratelimit、idempotent、log）是 AOP 横切关注点，不属于中间件封装，却以 client 模块形式存在。

问题：

1. **职责错位**：client 模块定位是"中间件接入层"，但 ratelimit/idempotent/log 是应用层横切关注点
2. **重复造轮子**：client-idempotent 自建 Caffeine Cache，而 client-cache 已有完整的 CacheClient 体系
3. **不必要的独立模块**：这 3 个模块只被 app 依赖，没有复用场景，拆成独立模块增加了构建复杂度
4. **client-log 依赖越界**：它依赖了 mybatis-plus（SlowQueryInterceptor）和 fastjson2，超出了 client 模块应有的依赖范围

约束：

- app 模块四层架构（Controller→Facade→Service→Repository）不变
- ArchUnit 守护规则不变
- 三个模块的外部行为（注解属性、切面逻辑）不变，只是包路径迁移
- 文档已在探索阶段先行更新（module-structure.md、java-conventions.md、AGENTS.md、design-patterns.md）

## Goals / Non-Goals

**Goals:**

- 将 client-ratelimit、client-idempotent、client-log 的代码迁移到 app 模块的 `shared/` 包下
- 建立 `shared/` 包的组织规范（aspect/logging/util 三级子包）
- 将 `util/` 顶级包内容迁移到 `shared/util/`，消除顶级 util 包
- client-idempotent 的存储改为依赖 CacheClient
- 新增 `config/` 下的 @Configuration 类注册 Bean
- 保持所有外部行为不变

**Non-Goals:**

- 不改变 @RateLimit、@Idempotent、@BusinessLog 注解的属性和行为
- 不引入 RateLimitStore 接口抽象（保留当前 Bucket4j 本地实现，未来扩展时再加）
- 不实现 Redis 分布式限流或幂等
- 不重构 LogAspect 内部逻辑
- 不修改 client-cache、client-oss、client-email、client-sms、client-search、client-auth 模块
- 不修改已有文档（已在探索阶段更新完毕）

## Decisions

### D1: `shared/` 包采用"职责子包"而非"业务子包"

**选择**：`shared/aspect/`、`shared/logging/`、`shared/util/`
**放弃**：`shared/ratelimit/`、`shared/idempotent/`、`shared/operationlog/`（没有中间的 aspect 层）

**理由**：`shared/` 下的第一级按职责分（AOP 切面 / 日志基础设施 / 通用工具），第二级按业务分。这样 `shared/logging/` 下的东西（慢查询拦截器、采样过滤器）不会和
AOP 切面混在一起。如果只有两级（shared/ratelimit），那慢查询拦截器和 RateLimitAspect 就在同一层级，职责不清。

### D2: client-idempotent 存储改为依赖 CacheClient

**选择**：IdempotentAspect 注入 CacheClient，用 `hasKey()` / `put(key, value, duration)` / `delete(key)` 实现幂等检查
**放弃**：自建 Caffeine Cache 或定义独立 IdempotentStore 接口

**理由**：

- CacheClient 已有 TTL 能力（`put(key, value, duration)`），比当前手动管理时间戳更简洁
- 换成 Redis CacheClient 实现后，幂等天然支持分布式，无需额外抽象
- 不需要新增接口，减少代码量

**迁移变化**：

```java
// 之前
Long existing = cache.getIfPresent(key);
if (existing != null && (now - existing) < timeoutMillis) { 拒绝 }
cache.put(key, now);

// 之后
if (Boolean.TRUE.equals(cacheClient.hasKey(key))) { 拒绝 }
cacheClient.put(key, "1", Duration.ofMillis(timeoutMillis));
```

### D3: client-ratelimit 保留当前实现不变

**选择**：Bucket4j 本地 Bucket + ConcurrentHashMap 逻辑原封不动迁移
**放弃**：抽象 RateLimitStore 接口支持存储件扩展

**理由**：

- 当前没有分布式限流需求，过早抽象增加复杂度
- 如果未来需要 Redis，可以单独引入 `RateLimitStore` 接口 + `ProxyManager` 实现
- Bucket4j 的 `BucketProxy extends Bucket` 证明了扩展路径是可行的

### D4: config/ 下新增 3 个 @Configuration 类

**选择**：`IdempotentConfigure`、`RatelimitConfigure`、`LoggingConfigure`
**放弃**：合并为一个 `SharedConfigure`

**理由**：与现有 `WebConfigure`、`ThreadPoolConfigure`、`MybatisPlusConfigure` 风格一致——一个关注点一个 Configure 类。各自管理自己的 Properties
和 Bean 注册。

### D5: util/ 顶级包迁移到 shared/util/

**选择**：`util/` → `shared/util/`，包括 context/、dal/、IpUtils、KryoSerializer、SpringContextUtils
**放弃**：保留 util/ 和 shared/ 两个顶级包

**理由**：util 的内容和 shared 语义一致——都是跨层共享的基础设施。合并后项目顶级包更清晰：四层业务（controller/facade/service/repository/entity）+
共享（shared）+ 配置（config）+ 生成器（generated）。

### D6: 测试迁移策略

**选择**：测试类随源码一起迁移到 app 测试目录，包路径同步更新
**放弃**：保留测试在原 client 模块中

**理由**：源码迁移后原模块删除，测试必须跟随。

## Risks / Trade-offs

| 风险                                                            | 缓解措施                                                                 |
|---------------------------------------------------------------|----------------------------------------------------------------------|
| 包路径变更导致外部项目编译失败                                               | 项目是骨架模板，不会被外部项目引用；README 和文档已更新                                      |
| IdempotentAspect 改用 CacheClient 后 TTL 精度差异                    | CacheClient 的 `put(key, value, duration)` 底层 Caffeine 也用毫秒级 TTL，行为等价 |
| client-log 的 OperationLogWriter 接口原设计是给 app 实现的，迁移后接口和实现在同一模块 | 反而更合理——接口和实现不再跨模块                                                    |
| app/pom.xml 新增 bucket4j/micrometer/fastjson2 直接依赖             | 这些依赖原本通过 client 模块间接引入，现在是显式声明，更清晰                                   |
| ArchUnit 规则可能因 shared 包路径不匹配而报错                               | shared 包不在四层流转中，需确认 ArchUnit 规则的包路径匹配不受影响                            |
