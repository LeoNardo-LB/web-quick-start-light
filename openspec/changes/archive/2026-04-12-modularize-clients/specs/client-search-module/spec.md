## ADDED Requirements

### Requirement: SearchClient 接口定义
SearchClient 接口 SHALL 提供 3 个搜索操作方法：`search(SearchQuery)`、`index(String indexName, String id, Object document)`、`deleteIndex(String indexName, String id)`。所有方法声明在 `org.smm.archetype.client.search.SearchClient` 接口中。

#### Scenario: SearchClient 接口方法完整性
- **WHEN** 编译 clients 模块
- **THEN** SearchClient 接口包含 3 个方法签名

### Requirement: AbstractSearchClient 模板方法
AbstractSearchClient SHALL 实现 SearchClient 接口，所有接口方法标记为 `final`，内部调用 `doSearch`/`doIndex`/`doDeleteIndex` 抽象扩展点方法。SHALL 在模板方法中统一执行：参数校验、入/出日志记录、异常包装为 ClientException。

### Requirement: SimpleSearchClient 内存实现
SimpleSearchClient SHALL 继承 AbstractSearchClient，基于 `ConcurrentHashMap<String, Map<String, Object>>` 实现。每个 indexName 对应一个内部 Map。`search()` 方法 SHALL 遍历指定 index 下所有 document 的 value，做关键词字符串匹配过滤。SHALL 不依赖任何外部服务。

#### Scenario: 索引后可搜索
- **WHEN** 调用 `index("products", "1", Map.of("name", "iPhone"))` 后调用 `search(SearchQuery("iPhone", "products", 1, 10))`
- **THEN** SearchResult 的 total=1，records 包含 name="iPhone" 的 document

#### Scenario: 删除后搜索不到
- **WHEN** 索引 document 后调用 `deleteIndex("products", "1")` 再搜索
- **THEN** SearchResult 的 total=0

#### Scenario: 空索引搜索返回空结果
- **WHEN** 搜索一个不存在的 indexName
- **THEN** SearchResult 的 total=0，records 为空列表

### Requirement: SearchAutoConfiguration 条件装配
SearchAutoConfiguration SHALL 使用 `@AutoConfiguration` + `@ConditionalOnProperty(prefix="middleware.search", name="enabled", havingValue="true")` + `@EnableConfigurationProperties(SearchProperties.class)`。SHALL 在 `@ConditionalOnMissingBean` 条件下注册 SimpleSearchClient Bean。

#### Scenario: enabled=true 时注册 SimpleSearchClient
- **WHEN** `middleware.search.enabled=true`
- **THEN** Spring 容器中存在 SimpleSearchClient 类型的 Bean

#### Scenario: enabled=false 时不注册
- **WHEN** `middleware.search.enabled=false` 或未配置
- **THEN** Spring 容器中不存在 SimpleSearchClient Bean

### Requirement: SearchProperties 配置属性
SearchProperties SHALL 使用 `@ConfigurationProperties("middleware.search")`，包含字段：enabled(boolean, 默认false)、type(String, 默认"simple")。SHALL 不使用 @Data 注解。

#### Scenario: 默认配置值
- **WHEN** 不设置任何 middleware.search 配置
- **THEN** SearchProperties 的 enabled=false, type="simple"
