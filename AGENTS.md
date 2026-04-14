# AGENTS.md — AI 编码规范入口

> Spring Boot 4.x + Java 25 多模块四层架构脚手架项目，基包 `org.smm.archetype`。

---

## ⛔⛔⛔ 文档引用规则（分层披露原则）⛔⛔⛔

> **本规则是最高优先级，没有任何例外。违反此规则 = 一次性加载整个文档文件 = 浪费上下文窗口 = 降低输出质量。**

### 规则

加载 docs/ 下的任何文档时，**⛔ 绝对禁止⛔ 直接读取全文或猜测行号**。必须遵循以下两步：

**第 1 步：加载结构** — 运行 `scripts/md-sections <文件路径>` 获取 JSON 章节树（含 start/end/children）

```bash
# ✅ 正确：先看结构（输出 JSON）
scripts/md-sections docs/modules/auth.md
```

**第 2 步：按需加载** — 运行 `scripts/md-sections <文件路径> "标题1" ["标题2" ...]` 精准提取章节

```bash
# ✅ 正确：只加载需要的章节
scripts/md-sections docs/modules/auth.md "API 参考"
# ✅ 正确：精确定位子章节
scripts/md-sections docs/modules/auth.md "技术设计" "登录时序图"
scripts/md-sections docs/conventions/java-conventions.md "规则"
```

### 禁止事项

```
❌ 禁止：Read docs/modules/auth.md（全文加载）
❌ 禁止：Read docs/modules/auth.md offset=163 limit=50（猜行号）
❌ 禁止：Read docs/conventions/java-conventions.md（全文加载）
```

### 工具位置

```
scripts/md-sections <file>                              # JSON 章节树（含 start/end/children）
scripts/md-sections <file> "标题1" ["标题2" ...]         # 按层级路径提取章节内容
scripts/md-sections <file> --line <行号>                  # 按行号定位章节
```

错误时输出 JSON（退出码 1）：

| error | 含义 |
|---|---|
| `missing_file` | 未指定文件路径 |
| `file_not_found` | 文件不存在 |
| `not_found` | 标题未匹配到（`missing` 字段为未命中的标题，`children` 为当前范围子树，据此重试） |
| `line_not_found` | 行号不在任何章节内 |

---

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 25 |
| 框架 | Spring Boot | 4.x |
| ORM | MyBatis-Plus | 3.5.x |
| 对象转换 | MapStruct | 1.6.x |
| 认证 | Sa-Token | 1.45.x |
| 限流 | Bucket4j | 8.17.x |
| 工具库 | Hutool | 5.8.x |
| 数据库 | SQLite | 3.x |
| 测试 | JUnit 5 + Mockito + ArchUnit | - |
| 覆盖率 | JaCoCo | 0.8.x |
| 构建 | Maven（多模块 POM 聚合） | - |

> 精确版本号见 `pom.xml` 或 [docs/architecture/system-overview.md](docs/architecture/system-overview.md)。

## 多模块结构

```
web-quick-start-light/                     (根 POM, packaging=pom)
├── common/                                (异常体系)
├── clients/                               (parent POM, packaging=pom)
│   ├── client-cache/                      (Caffeine 本地缓存)
│   ├── client-oss/                        (本地对象存储)
│   ├── client-email/                      (Jakarta Mail 邮件)
│   ├── client-sms/                        (短信)
│   ├── client-search/                     (内存搜索)
│   └── client-auth/                       (认证客户端)
└── app/                                   (主应用, 依赖 common + 中间件 client-*)
```

> **注意**：限流（ratelimit）、幂等（idempotent）、操作日志（operationlog）和日志基础设施（logging）属于应用层横切关注点，直接集成在 app 模块的
`shared/` 包下，不作为独立 client 模块。详见 [模块结构 - app 内部包组织](docs/architecture/module-structure.md#app-内部包组织)。

## 快速开始

```bash
# 构建（跳过测试）
mvn clean package -DskipTests

# 运行全部测试
mvn test

# 仅单元测试（*UTest）
mvn test -Dtest="*UTest"

# 仅集成测试（*ITest）
mvn test -Dtest="*ITest"

# 启动应用（开发环境）—— 必须指定 -pl app
mvn spring-boot:run -pl app

# 启动应用（生产环境）
scripts/start.sh prod

# 停止应用
scripts/stop.sh

# 测试 + 覆盖率报告
mvn clean verify
# 报告：app/target/site/jacoco-aggregate/index.html
```

## 核心编码规则

### 1. 四层架构约束（仅 app 模块）
- **Controller** → **Facade** → **Service** → **Repository**（Mapper），依赖方向严格单向
- Controller **禁止**直接注入/调用 Repository（Mapper），ArchUnit 守护此规则
- Facade 层负责 Entity→VO 转换，**禁止直接暴露 Entity 到 Controller**
- Service 层处理业务逻辑，返回 Entity 给 Facade
- Entity 层禁止依赖 Spring Framework

### 2. Lombok 规范
- **禁止 `@Data`**，使用 `@Builder` + `@RequiredArgsConstructor` + 手动 getter/setter
- 原因：`@Data` 含 `@EqualsAndHashCode` / `@ToString` 可能导致意外行为

### 3. 测试命名
- 单元测试：`*UTest`（继承 `UnitTestBase`，Mockito 环境）
- 集成测试：`*ITest`（继承 `IntegrationTestBase`，Spring 上下文）

### 4. 异常体系（common 模块，`org.smm.archetype.exception`）
- `ErrorCode` — 接口（`code()` + `message()` + `messageKey()`）
- `CommonErrorCode` — 枚举（通用错误码，含 i18n messageKey）
- `BaseException` — 异常基类
- `BizException` — 业务异常（可预期，前端展示）
- `ClientException` — 客户端参数异常（400 类）
- `SysException` — 系统内部异常（500 类）

### 5. 日志规范

- 使用 `@BusinessLog` 注解记录业务方法日志（SLF4J + Micrometer 指标）
- `@BusinessLog` 扩展属性：`module`（业务模块）、`operation`（操作类型）、`samplingRate`（采样率，默认 1.0）
- 操作日志持久化：`OperationLogWriter` 接口 + `OperationLogRecord`，`OperationType` 枚举（CREATE/UPDATE/DELETE/QUERY/EXPORT/IMPORT）
- 使用 `@Slf4j` + 参数化日志，**禁止 `System.out.println`**

### 6. 依赖约束
- ORM 仅允许 MyBatis-Plus（禁止 JPA / Hibernate）
- 工具库优先：Hutool / Apache Commons / Guava
- 序列化：Jackson（JSON）、Kryo（二进制）
- 对象转换：MapStruct（编译期安全）

### 7. 线程上下文
- 使用 `ScopedThreadContext`（基于 Java 25 ScopedValue）传递 userId / traceId
- 异步场景使用 `ContextRunnable` / `ContextCallable` 包装

### 8. 配置管理
- 多环境：`application-dev.yaml` / `application-prod.yaml` / `application-optional.yaml`
- 配置类使用 `@ConfigurationProperties`，禁止 `@Value`

### 9. 技术客户端规范（clients 模块）
- 每个客户端模块独立 artifactId，依赖 `common`
- **Template Method 模式**：`AbstractXxxClient` 的公开方法为 `final`（参数校验+日志），子类实现 `do*` 扩展点
- **条件装配**：`@AutoConfiguration` + `@ConditionalOnClass` + `@ConditionalOnProperty`
- **Properties 前缀统一**：`middleware.*`（middleware.cache / middleware.object-storage / middleware.email / middleware.sms / middleware.search / middleware.ratelimit / middleware.auth），日志前缀为 `logging`（对接 Spring Boot 日志配置惯例）。幂等模块不再有独立 Properties（参数在 @Idempotent 注解上定义）

### 10. 时间类型规范
- 所有时间存储与传输统一使用 `java.time.Instant`
- 禁止使用 `LocalDateTime`、`Long`（时间戳毫秒）作为时间字段类型

### 11. Record 规范
- 新增的 DTO/VO/Result 使用 Java record
- 有继承链的基类（BaseResult、BaseRequest、BaseDO）保持 class
- 值对象简化为 record（如 ConfigKey、ConfigValue、DisplayName），含静态工厂 of() 和校验

### 12. 代码生成器规范
- 代码生成器位置：`app/.../generated/` 包
- 生成的代码禁止手动修改

### 13. API 路径规范
- 统一 `/api` 前缀：所有控制器使用 `/api` 路径前缀
- API 版本通过 HTTP Header `API-Version` 控制

### 14. 限流规范
- 使用 `@RateLimit` 注解标记需要限流的接口
- 支持 SpEL 表达式自定义限流 Key
- `LimitFallback` 策略：REJECT / WAIT / FALLBACK

### 15. 幂等规范
- 使用 `@Idempotent` 注解标记需要幂等保护的接口
- 基于 CacheClient 实现幂等 Key 存储（通过 TTL 过期机制）
- 支持自定义幂等 Key 解析

### 16. 国际化规范（i18n）
- 错误消息国际化：使用 `ErrorCode.messageKey()` + `MessageSource` 解析
- 资源文件：`messages.properties` + `messages_en.properties`
- `WebExceptionAdvise` 根据 `Accept-Language` Header 自动切换语言

## 文档维护职责

> 维护文档前，必须了解文档编写原则和治理规则。详见 [docs/README.md](docs/README.md)：
>
> - [文档编写原则](docs/README.md#文档编写原则) — 7 维度（职责清晰/简要精准/层次分明/逻辑自洽/科学有据/双范式写作/索引具体规则泛化）
> - [维护职责分工](docs/README.md#维护职责分工) — 🤖 确定性 / 🤖👤 半确定性 / 👤 创造性 三级分类
> - [文档与代码对齐机制](docs/README.md#文档与代码对齐机制) — Contract/Constraint/Intent 三轨各自的对齐流程
> - [维护策略](docs/README.md#维护策略) — 常见维护场景的操作指南和维护者分工
> - [反模式警示](docs/README.md#反模式警示) — 4 类反模式（文档结构/AI 维护/维护遗漏/内容膨胀）

## 文档索引

> 引用强度：⛔ MUST（强制，无例外）/ ⚠️ SHOULD（推荐，允许有理由的例外）/ 💡 MAY（可选）

### 架构文档（docs/architecture/）

| 强度        | 文档    | 路径                                                             | 一句话描述                       |
|-----------|-------|----------------------------------------------------------------|-----------------------------|
| ⛔ MUST    | 系统全景  | [system-overview.md](docs/architecture/system-overview.md)     | C4 图 + 技术栈 + JVM 配置         |
| ⛔ MUST    | 模块结构  | [module-structure.md](docs/architecture/module-structure.md)   | Maven 多模块 + 四层架构 + ArchUnit |
| ⚠️ SHOULD | 请求流转  | [request-lifecycle.md](docs/architecture/request-lifecycle.md) | HTTP 请求完整处理链路               |
| ⚠️ SHOULD | 设计模式  | [design-patterns.md](docs/architecture/design-patterns.md)     | Template Method + 条件装配      |
| ⚠️ SHOULD | 线程上下文 | [thread-context.md](docs/architecture/thread-context.md)       | ScopedValue 传递链             |
| ⚠️ SHOULD | 骨架使用  | [archetype-usage.md](docs/architecture/archetype-usage.md)     | Maven 骨架安装/使用/故障排查          |

### 编码规范（docs/conventions/）

**编码规范必须强制遵守！！！**

| 强度     | 文档        | 路径                                                                | 一句话描述               |
|--------|-----------|-------------------------------------------------------------------|---------------------|
| ⛔ MUST | Java 编码规范 | [java-conventions.md](docs/conventions/java-conventions.md)       | Lombok/时间/Record/依赖 |
| ⛔ MUST | 测试规范      | [testing-conventions.md](docs/conventions/testing-conventions.md) | UTest/ITest/覆盖率标准   |
| ⛔ MUST | 错误处理规范    | [error-handling.md](docs/conventions/error-handling.md)           | 异常体系/错误码/i18n       |
| ⛔ MUST | 配置规范      | [configuration.md](docs/conventions/configuration.md)             | Properties/前缀/多环境   |

### 模块文档（docs/modules/）

| 强度 | 文档 | 路径 | 一句话描述 |
|------|------|------|-----------|
| ⛔ MUST | 认证模块 | [auth.md](docs/modules/auth.md) | Sa-Token 登录/注销/拦截 |
| ⛔ MUST | 系统配置模块 | [system-config.md](docs/modules/system-config.md) | CRUD + 分页 + 值对象 |
| ⚠️ SHOULD | 操作日志模块 | [operation-log.md](docs/modules/operation-log.md) | @BusinessLog + 分页查询 |
| ⚠️ SHOULD | 缓存客户端 | [client-cache.md](docs/modules/client-cache.md) | Caffeine + 10 方法 |
| ⚠️ SHOULD | 对象存储客户端 | [client-oss.md](docs/modules/client-oss.md) | 本地存储 + NIO |
| 💡 MAY | 邮件客户端 | [client-email.md](docs/modules/client-email.md) | Jakarta Mail + NoOp |
| 💡 MAY | 短信客户端 | [client-sms.md](docs/modules/client-sms.md) | 3 方法 + NoOp |
| 💡 MAY | 搜索客户端 | [client-search.md](docs/modules/client-search.md) | 内存搜索 + 15 方法 |
| ⚠️ SHOULD | 认证客户端 | [client-auth.md](docs/modules/client-auth.md) | AuthClient 接口 + Sa-Token |

> **注意**：限流、幂等、操作日志横切关注点已移至 app 模块 `shared/` 包下，对应的旧文档（client-ratelimit.md、client-idempotent.md、client-log.md）已归档至 `docs/archived/`。

### 文档系统说明

| 强度 | 文档 | 路径 | 一句话描述 |
|------|------|------|-----------|
| ⛔ MUST | 文档系统说明 | [README.md](docs/README.md) | 三轨体系/模板/导航 |

## OpenSpec Intent 索引

见：openspec/specs 与 openspec/changes 目录
