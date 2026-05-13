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
├── components/                               (parent POM, packaging=pom)
│   ├── component-cache/                      (Caffeine 本地缓存)
│   ├── component-oss/                        (本地对象存储)
│   ├── component-email/                      (Jakarta Mail 邮件)
│   ├── component-sms/                        (短信)
│   ├── component-search/                     (内存搜索)
│   └── component-auth/                       (认证组件)
└── app/                                   (主应用, 依赖 common + 组件 component-*)
```

> app 模块内含业务模块（auth/operationlog/systemconfig）和 shared/ 两个顶层概念。shared/ 含 15 个自包含功能域（ratelimit/idempotent/logging/context/dal/…），详见 [模块结构](docs/architecture/module-structure.md)。

## 快速开始

```bash
mvn clean package -DskipTests          # 构建
mvn spring-boot:run -pl app            # 启动（开发环境）
mvn test -pl app                       # 全量测试
mvn test -pl app -Dtest="*UTest"       # 仅单元测试
mvn test -pl app -Dtest="*ITest"       # 仅集成测试
mvn clean verify                       # 测试 + 覆盖率报告
```

## ⛔ ArchUnit 守护规则

> 项目通过 ArchUnit + SourceScanner + Spring Modulith 自动守护架构约束，测试文件位于 `app/.../support/basic/` 下。所有规则均为 MUST，违反即 CI 红灯。

### 高频触犯红线
- **禁止 `@Data`** → 用 `@Builder` + `@RequiredArgsConstructor`
- **禁止 `@Autowired` 字段注入** → 构造器注入 + `@RequiredArgsConstructor`
- **禁止 `LocalDateTime` / `java.util.Date`** → 统一 `Instant`
- **Controller 不得直接调用 Repository/Mapper** → 必须经 Facade → Service
- **Facade 不得暴露 Entity** → 转为 VO/DTO（record）
- **测试类禁止 `Thread.sleep`** → Awaitility 或 CountDownLatch
- **模块间零循环依赖** → 通过 Facade 或 Domain Event 通信

### 完整规则清单
详见 [模块结构 - ArchUnit 守护规则](docs/architecture/module-structure.md#archunit-守护规则)、[Java 编码规范](docs/conventions/java-conventions.md)、[测试规范](docs/conventions/testing-conventions.md)

---

## 核心编码规则

> 详细规则见 [Java 编码规范](docs/conventions/java-conventions.md)、[错误处理](docs/conventions/error-handling.md)、[配置规范](docs/conventions/configuration.md)。

### 必知红线
- **四层架构方向**：Controller → Facade → Service → Repository，严格单向
- **Facade 不得暴露 Entity**，必须转为 VO/DTO
- **BizContext** 传 userId（基于 Java 25 ScopedValue），traceId 由 OTel Span 管理
- **异步传播**：`ThreadPoolConfigure.ContextPropagatingTaskDecorator`（三合一：BizContext + OTel + MDC）
- **异常体系**：`BizException`/`ClientException`/`SysException` + `ErrorCode`（禁止泛型异常）
- **对象转换**：MapStruct（`@Mapper(config = CentralMapperConfig.class)`），禁止 `BeanUtils.copyProperties`

### 限流与幂等
- `@RateLimit` 注解标记限流接口，支持 SpEL Key，策略：REJECT/WAIT/FALLBACK
- `@Idempotent` 注解标记幂等接口，基于 CacheComponent + TTL 过期

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
| ⚠️ SHOULD | 缓存组件 | [component-cache.md](docs/modules/component-cache.md) | Caffeine + 10 方法 |
| ⚠️ SHOULD | 对象存储组件 | [component-oss.md](docs/modules/component-oss.md) | 本地存储 + NIO |
| 💡 MAY | 邮件组件 | [component-email.md](docs/modules/component-email.md) | Jakarta Mail + NoOp |
| 💡 MAY | 短信组件 | [component-sms.md](docs/modules/component-sms.md) | 3 方法 + NoOp |
| 💡 MAY | 搜索组件 | [component-search.md](docs/modules/component-search.md) | 内存搜索 + 15 方法 |
| ⚠️ SHOULD | 认证组件 | [component-auth.md](docs/modules/component-auth.md) | AuthComponent 接口 + Sa-Token |

### 文档系统说明

| 强度 | 文档 | 路径 | 一句话描述 |
|------|------|------|-----------|
| ⛔ MUST | 文档系统说明 | [README.md](docs/README.md) | 三轨体系/模板/导航 |

## OpenSpec Intent 索引

> 暂未创建。Intent 轨文档计划存放在独立的 `openspec/specs/` 目录，记录各功能的设计初衷和决策理由。

## graphify

> 暂未生成。`graphify-out/` 目录不存在时，忽略以下规则，使用 grep/glob 探索代码库。
