## Context

web-quick-start-light 项目经过多轮迭代（单模块 → 多模块、PostgreSQL → SQLite、架构增强），留下了大量文档与代码脱节的问题。全量一致性审计发现 34 处不一致，分布在文档（16处）、配置（3处）、代码（6处）、POM（4处）、OpenSpec（7处）五个维度。

当前状态：
- 代码是真实、可运行的（134 测试全绿）
- 文档、配置、OpenSpec 中有大量残留过时信息
- 部分 AGENTS.md 规范被代码违反（@Data、@Value）

## Goals / Non-Goals

**Goals:**
- 以代码为真实状态，全面对齐文档、配置、OpenSpec
- 消除所有 34 处不一致
- 严格执行 AGENTS.md 规范（或修改规范）
- 最终状态：文档准确描述代码、配置与 Properties 类对应、OpenSpec 反映实际实现

**Non-Goals:**
- 不增加新功能、新 Client 方法、新接口
- 不修改任何 Client 接口签名（Cache/Oss/Email/Sms/Search）
- 不升级任何依赖版本
- 不修改生产数据库 schema（仅改 test 的 schema.sql）
- 不重构架构或调整模块结构
- 不修改异常体系（ErrorCode/CommonErrorCode 等）

## Decisions

### Decision 1: 以代码为准对齐文档

**选择：** 文档、配置、OpenSpec 全部以代码实际状态为准进行修正。

**替代方案：**
- 以文档为准改代码 → 风险太大，可能破坏功能
- 选择性忽略 → 问题会持续积累

**理由：** 代码是经过测试验证的真实状态，文档/配置中的错误是信息过时导致的。

### Decision 2: 严格执行 Lombok 规范

**选择：** 将 `@Data` 替换为 `@Getter @Setter`，严格执行 AGENTS.md 禁令。

**替代方案：** 放宽规范，允许 DTO/Properties 使用 `@Data`

**理由：** 用户在讨论中选择了严格执行。client 模块的 Properties 类已遵循此规范，app 模块也应保持一致。

### Decision 3: @Value → @ConfigurationProperties

**选择：** 创建 `AppInfoProperties` 类替代 WebStartLightApplication 中的 5 处 `@Value`。

**替代方案：** 放宽规范，允许 CommandLineRunner 中使用 `@Value`

**理由：** 用户在讨论中选择了严格执行规范，且 `@ConfigurationProperties` 更有利于类型安全和 IDE 提示。

### Decision 4: 补全 Template Method 模式

**选择：** 为 client-search 创建 `AbstractSearchClient`，与其他 4 个模块保持一致。

**理由：** 一致性是脚手架项目的核心价值，5 个 client 模块应遵循相同的模式。

### Decision 5: POM 版本统一管理

**选择：** 所有依赖版本集中到根 pom.xml 的 `properties` + `dependencyManagement`。

**理由：** Maven 多模块最佳实践，避免版本漂移。

### Decision 6: OpenSpec 回写而非归档或删除

**选择：** 以实际代码为准回写 OpenSpec 的 specs/tasks/proposal/design。

**替代方案：** 全部归档 / 清空重建

**理由：** OpenSpec 是变更记录，应反映实际做了什么。归档 enhance-architecture-from-backend（已完成），更新 modularize-clients（进行中）。

### Decision 7: 逻辑删除字段统一为时间戳式

**选择：** schema.sql 中 `deleted INTEGER` → `delete_time BIGINT DEFAULT 0`

**理由：** 与 speccoding 最佳实践一致，时间戳式优于布尔式（能记录删除时间）。

### Decision 8: Hutool 保留但标注

**选择：** 保留 Hutool 依赖声明（脚手架预置），在 AGENTS.md 标注"预置暂未使用"。

**替代方案：** 删除 Hutool 依赖

**理由：** 脚手架项目预置工具库是合理的，未来使用者可能需要。删除会破坏用户预期。

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|---------|
| @Data → @Getter @Setter 可能影响 Lombok 生成的 equals/hashCode/toString | BasePageRequest/BasePageResult 需保留 `@EqualsAndHashCode(callSuper = true)` |
| AppInfoProperties 需要新增 YAML 配置键 | 使用 `@ConfigurationProperties` 松绑定，可直接映射已有的 `server.port` 等属性 |
| POM 重构可能引入版本冲突 | 使用 `mvn dependency:tree` 验证，不改变任何实际版本号 |
| OpenSpec 回写工作量大 | 只更新关键不一致点，不重写全部内容 |
| AbstractSearchClient 重构可能破坏现有测试 | 保持 SimpleSearchClient 的公开方法签名不变，仅重构内部结构 |
