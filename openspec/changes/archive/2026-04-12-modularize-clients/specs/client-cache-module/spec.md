## ADDED Requirements

### Requirement: CacheClient 接口定义
CacheClient 接口 SHALL 提供 10 个缓存操作方法：`get`、`getList`、`put`（带过期时间）、`put`（默认过期）、`append`、`delete`、`hasKey`、`expire`、`getExpire`、`keys`。所有方法声明在 `org.smm.archetype.client.cache.CacheClient` 接口中。

#### Scenario: CacheClient 接口方法完整性
- **WHEN** 编译 clients 模块
- **THEN** CacheClient 接口包含 10 个方法签名，泛型方法正确声明类型参数

### Requirement: AbstractCacheClient 模板方法
AbstractCacheClient SHALL 实现 CacheClient 接口，所有接口方法标记为 `final`，内部调用 `doGet`/`doPut`/`doDelete`/`doAppend`/`doHasKey`/`doExpire`/`doGetExpire`/`doKeys` 等抽象扩展点方法。SHALL 在模板方法中统一执行：key 空值校验（抛 ClientException）、入/出日志记录、异常包装为 ClientException(ClientErrorCode.CACHE_OPERATION_FAILED)。

#### Scenario: key 为空时抛出异常
- **WHEN** 调用 AbstractCacheClient 的任意方法并传入 null 或空字符串 key
- **THEN** 抛出 ClientException，错误码为 CACHE_OPERATION_FAILED

#### Scenario: 模板方法调用扩展点
- **WHEN** 调用 `put("user:1", value, 60)` 且子类实现了 `doPut`
- **THEN** 先执行 key 校验和入参日志，再调用子类的 `doPut`，最后记录出参日志

### Requirement: CaffeineCacheClient 真实实现
CaffeineCacheClient SHALL 继承 AbstractCacheClient，基于 Caffeine `Cache<String, CacheValueWrapper>` 实现。SHALL 包含内部类 CacheValueWrapper（volatile Object value + volatile long expiryTimestamp）和 CaffeineExpiry（实现 Expiry 接口，基于 CacheValueWrapper.expiryTimestamp 计算剩余 TTL）。`keys(pattern)` 方法 SHALL 通过 cache.asMap().keySet() 配合 glob 模式匹配。

#### Scenario: put 后 get 返回正确值
- **WHEN** 调用 `put("user:1", "Alice", 60)` 后调用 `get("user:1", String.class)`
- **THEN** 返回 "Alice"

#### Scenario: 过期后 get 返回 null
- **WHEN** 调用 `put("user:1", "Alice", 1)` 并等待超过 1 秒后调用 `get("user:1", String.class)`
- **THEN** 返回 null

#### Scenario: keys 模式匹配
- **WHEN** 已有 key "user:1" 和 "user:2" 和 "order:1"，调用 `keys("user:*")`
- **THEN** 返回包含 "user:1" 和 "user:2" 的 Set

### Requirement: CacheAutoConfiguration 条件装配
CacheAutoConfiguration SHALL 使用 `@AutoConfiguration` + `@ConditionalOnClass(Caffeine.class)` + `@ConditionalOnProperty(prefix="middleware.cache", name="type", havingValue="caffeine", matchIfMissing=true)`。SHALL 通过 `@EnableConfigurationProperties(CacheProperties.class)` 启用配置属性。SHALL 在 `@ConditionalOnMissingBean` 条件下注册 CaffeineCacheClient Bean。

#### Scenario: Caffeine 在 classpath 且配置匹配时注册 Bean
- **WHEN** Caffeine 类在 classpath 上且 `middleware.cache.type=caffeine`（或未配置）
- **THEN** Spring 容器中存在 CaffeineCacheClient 类型的 Bean

#### Scenario: Caffeine 不在 classpath 时不注册
- **WHEN** Caffeine 类不在 classpath 上
- **THEN** Spring 容器中不存在 CacheAutoConfiguration 注册的 Bean

### Requirement: CacheProperties 配置属性
CacheProperties SHALL 使用 `@ConfigurationProperties("middleware.cache")`，包含字段：initialCapacity(int, 默认100)、maximumSize(long, 默认1000)、expireAfterWrite(Duration, 默认30m)、expireAfterAccess(Duration, 默认0)。SHALL 不使用 @Data 注解（遵循项目规范）。

#### Scenario: 默认配置值
- **WHEN** 不设置任何 middleware.cache 配置
- **THEN** CacheProperties 的 initialCapacity=100, maximumSize=1000, expireAfterWrite=PT30M

<!-- CacheClient Logging fallback 需求已移除：LoggingCacheClient 和 TechClientConfigure 已删除，项目不再使用 fallback 机制。CacheClient 仅通过 CacheAutoConfiguration 在 Caffeine classpath 可用时注册。 -->
