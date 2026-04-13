## Why

全项目一致性审计发现 34 处不一致：代码与文档、文档与文档、OpenSpec 与代码、配置文件与 Properties 类之间存在大量脱节。新人按文档操作会 404、日志提示指向不存在的文件、配置模板取消注释后无法绑定、规范被广泛违反。需要以代码为真实状态，全面对齐文档、配置、OpenSpec。

## What Changes

### 文档修正（16 处）
- getting-started.md：修正端点端口（8080→9201）、补充 context-path、修正 Swagger 路径
- getting-started.md：prod 数据库 PostgreSQL → SQLite
- performance-rules.md：生产慢查询阈值 500ms → 1000ms（以实际配置为准）
- error-handling.md：CommonErrorCode message 文本 5 处不匹配
- error-handling.md：WebExceptionAdvise 代码示例过时（缺少处理器、返回类型不匹配）
- AGENTS.md：删除重复的 SQLite 行
- AGENTS.md：补充 client-sms 描述（遗漏了 AbstractSmsClient）
- overview.md：标注 ResultCode 已废弃
- AGENTS.md：Hutool 标注"预置暂未使用"
- testing-guide.md：补充无 `-pl app` 的测试命令
- README.md：补充常用命令（*UTest / *ITest / start.sh / stop.sh）

### 配置修正（3 处）
- application-optional.yaml：修正 middleware. 前缀、字段名对应 Properties、删除 Redis 幽灵配置
- schema.sql：逻辑删除字段 `deleted INTEGER` → `delete_time BIGINT`（时间戳式）
- application-test.yaml：补全 `map-underscore-to-camel-case` 和 `id-type`

### 代码修正（6 处）
- start.sh：日志提示 `spring.log` → 实际日志文件名
- logback-spring.xml：删除重复的 CONSOLE appender include
- 6 个 DTO/Properties 文件：`@Data` → `@Getter @Setter`
- WebStartLightApplication：`@Value` → 创建 `AppInfoProperties`
- client-search：创建 `AbstractSearchClient`（补全 Template Method 模式）
- stop.sh：PID 验证改进

### POM 重构（4 处）
- 删除 `caffeine.version` 死属性
- fastjson2 版本统一管理到根 POM
- app/pom.xml 11 个依赖版本提取到根 POM dependencyManagement
- 删除 clients/pom.xml 冗余的 dependencyManagement

### OpenSpec 回写（7 处）
- 更新 enhance-architecture-from-backend 和 modularize-clients 的 specs/tasks/proposal/design
- 将 enhance-architecture-from-backend 归档
- 补充 common 模块描述、修正包路径、修正错误码格式、修正接口方法签名
- 清理 H2 残留引用和 BaseDO 路径错误

## Capabilities

### New Capabilities
- `consistency-fix`: 覆盖文档、配置、代码、POM、OpenSpec 五个维度的一致性修复，以代码为真实状态全面对齐

### Modified Capabilities

## Impact

- **文档**：AGENTS.md、README.md、docs/ 下 10 个文档文件内容修正
- **配置**：application-optional.yaml、application-test.yaml、schema.sql
- **代码**：app 模块 6+ 个 Java 文件、client-search 模块新增 AbstractSearchClient、start.sh/stop.sh
- **POM**：根 pom.xml、app/pom.xml、clients/pom.xml、client-search/pom.xml
- **OpenSpec**：openspec/changes/ 下 2 个 change 目录的所有 artifacts
- **无 API 变更**、无数据库 schema 变更（仅 test schema.sql）、无依赖版本升级
