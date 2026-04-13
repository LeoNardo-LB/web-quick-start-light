## 1. 确认技能与准备

- [x] 1.1 确认 test-driven-development、dispatching-parallel-agents 技能已加载（未加载则加载，已加载则跳过）（注：本次为纯文档变更，TDD 不适用，跳过测试步骤）
- [x] 1.2 创建 docs/ 目录结构：docs/architecture/、docs/conventions/、docs/modules/

## 2. 文档系统说明（docs/README.md）

- [x] 2.1 编写 docs/README.md：三轨文档体系说明（Intent=OpenSpec, Contract=docs/, Constraint=AGENTS.md+conventions/）
- [x] 2.2 编写 docs/README.md：模板使用说明（固定/半固定的定义、模板用途、如何使用、注意事项）
- [x] 2.3 编写 docs/README.md：文档导航（每个文档的一句话描述 + 路径）
- [x] 2.4 编写 docs/README.md：引用强度说明（MUST/SHOULD/MAY 三级体系）

## 3. 架构文档（docs/architecture/）

- [x] 3.1 编写 docs/architecture/system-overview.md：C4 Context + Container 图 + 技术栈概要（仅大版本号）
- [x] 3.2 编写 docs/architecture/module-structure.md：Maven 多模块依赖图 + 四层架构说明 + ArchUnit 6 条规则
- [x] 3.3 编写 docs/architecture/request-lifecycle.md：HTTP 请求完整流转时序图（含认证/异常处理分支）
- [x] 3.4 编写 docs/architecture/design-patterns.md：Template Method 模式详解 + 条件装配机制 + 所有客户端的扩展点列表
- [x] 3.5 编写 docs/architecture/thread-context.md：ScopedValue 传递链 + ContextRunnable/ContextCallable + 异步场景

## 4. 编码规范文档（docs/conventions/）

- [x] 4.1 编写 docs/conventions/java-conventions.md：Lombok 规范 + 时间类型 + Record 规范 + 命名规范 + 依赖约束
- [x] 4.2 编写 docs/conventions/testing-conventions.md：UTest/ITest 命名 + Mock 规范 + 覆盖率标准 + 条件装配测试模式
- [x] 4.3 编写 docs/conventions/error-handling.md：三级异常体系 + ErrorCode/CommonErrorCode + i18n + WebExceptionAdvise
- [x] 4.4 编写 docs/conventions/configuration.md：@ConfigurationProperties 规范 + middleware.* 前缀 + 禁止 @Value

## 5. 业务模块文档（docs/modules/）— 业务模块

- [x] 5.1 编写 docs/modules/auth.md：认证模块（Sa-Token 登录/注销 + 路由拦截 + ContextFillFilter + user 表 + BCrypt）
- [x] 5.2 编写 docs/modules/system-config.md：系统配置模块（CRUD + 分页 + ConfigKey/ConfigValue/DisplayName record + ConfigGroup/ValueType/InputType 枚举）
- [x] 5.3 编写 docs/modules/operation-log.md：操作日志模块（分页查询 + @BusinessLog 注解 + OperationType 枚举）

## 6. 业务模块文档（docs/modules/）— 技术客户端

- [x] 6.1 编写 docs/modules/client-cache.md：CaffeineCacheClient + AbstractCacheClient + CacheClient 接口 + 10 方法 + Template Method
- [x] 6.2 编写 docs/modules/client-oss.md：LocalOssClient + AbstractOssClient + OssClient 接口 + 7 方法 + NIO + 日期分层
- [x] 6.3 编写 docs/modules/client-email.md：NoOpEmailClient + AbstractEmailClient + EmailClient 接口 + 3 方法 + Jakarta Mail
- [x] 6.4 编写 docs/modules/client-sms.md：NoOpSmsClient + AbstractSmsClient + SmsClient 接口 + 3 方法
- [x] 6.5 编写 docs/modules/client-search.md：SimpleSearchClient + AbstractSearchClient + SearchClient 接口 + 15 方法 + ConcurrentHashMap
- [x] 6.6 编写 docs/modules/client-log.md：BusinessLog 注解（module/operation/samplingRate）+ LogAspect + OperationLogWriter + 8 个 Appender
- [x] 6.7 编写 docs/modules/client-ratelimit.md：@RateLimit 注解 + RateLimitAspect + Bucket4j + Caffeine + SpelKeyResolver + LimitFallback
- [x] 6.8 编写 docs/modules/client-idempotent.md：@Idempotent 注解 + IdempotentAspect + Caffeine + IdempotentKeyResolver
- [x] 6.9 编写 docs/modules/client-auth.md：AuthClient 接口 + SaTokenAuthClient/NoOpAuthClient + SaInterceptor + AuthProperties

## 7. AGENTS.md 优化

- [x] 7.1 删除 AGENTS.md 中的"项目结构索引"表格（从代码可推断，ETH Zurich 实验证明无用）
- [x] 7.2 将精确版本号改为大版本号（Spring Boot 4.0.2 → 4.x，MyBatis-Plus 3.5.16 → 3.x 等）
- [x] 7.3 删除"JVM 内存配置"详细表（移到 docs/architecture/system-overview.md 或保留在 scripts/start.sh）
- [x] 7.4 删除"详细文档"路径表（替换为精简的 docs/ 索引）
- [x] 7.5 添加带引用强度的文档索引（⛔ MUST / ⚠️ SHOULD / 💡 MAY 三级）
- [x] 7.6 添加 OpenSpec specs 索引（17 个 capability 路径列表）

## 8. 一致性检查

- [x] 8.1 进行 artifact 文档与讨论结果的一致性检查（proposal→specs→tasks→design 三维度交叉比对）

## 额外完成的工作（用户追加需求）

- [x] 创建 docs/architecture/README.md：架构文档模板和编写指南
- [x] 创建 docs/conventions/README.md：编码规范模板和编写指南
- [x] 创建 docs/modules/README.md：模块文档模板和编写指南
- [x] 重写 docs/README.md：补充完整的文档系统设计大章节（设计初衷/理念/整体架构/分类标准/模板哲学/引用强度/对齐机制/AI协同/反模式/权衡/业界实践/可演进性/维护策略/参考资料）
- [x] 重写 README.md：修复"三层架构"→"四层架构"，精简版本号，补充 OpenSpec 三轨说明
- [x] 重写 ARCHETYPE_README.md：精简版本号，补充文档引用
- [x] 在 docs/README.md 文档系统设计中补充 OpenSpec Intent 轨子章节
- [x] 在所有文件中补充 OpenSpec 引用说明（README.md + 3 个子文件夹 README + docs/README.md 相关文档）
