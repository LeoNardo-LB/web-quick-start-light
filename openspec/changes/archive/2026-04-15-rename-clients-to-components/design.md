## Context

项目当前使用 `clients` 作为中间件接入层的模块名，子模块以 `client-xxx` 命名，Java 包名为 `org.smm.archetype.client.*`，配置前缀为 `middleware.*`。这些模块实际承载的是可插拔的技术组件能力（缓存、对象存储、邮件、短信、搜索、认证），"客户端"命名与实际职责存在语义偏差。需要统一重命名为 `component` 语义。

当前状态：
- 目录：`clients/` 下 6 个子模块 `client-cache`、`client-oss`、`client-email`、`client-sms`、`client-search`、`client-auth`
- Java 包：`org.smm.archetype.client.{cache,oss,email,sms,search,auth,dto}`
- 类名：`*Client` 后缀（`CacheClient`、`AbstractCacheClient`、`CaffeineCacheClient` 等）
- 配置前缀：`middleware.cache`、`middleware.object-storage`、`middleware.email`、`middleware.sms`、`middleware.search`、`middleware.auth`、`middleware.ratelimit`
- YAML 文件：`application-optional.yaml` 存放可选中间件配置示例

## Goals / Non-Goals

**Goals:**

- 将 `clients` 模块/目录/ArtifactId 重命名为 `components`/`component-xxx`
- 将 Java 包名从 `org.smm.archetype.client` 重命名为 `org.smm.archetype.component`
- 将所有 `*Client` 接口/类重命名为 `*Component`（如 `CacheClient` → `CacheComponent`）
- 将配置前缀从 `middleware.*` 统一改为 `component.*`
- 将 `middleware.object-storage` 简化为 `component.oss`
- 将 `application-optional.yaml` 重命名为 `application-component.yaml`
- 更新所有活跃文档（AGENTS.md、README.md、docs/ 下的活跃 MD）
- 文档中中文"客户端"统一改为"组件"（如"缓存客户端" → "缓存组件"）

**Non-Goals:**

- **不修改** OpenSpec 归档文件（`openspec/changes/archive/`、`openspec/specs/`）
- **不修改** `docs/archived/` 下的归档文档
- **不修改** `LoggingProperties`（前缀 `logging`）等非 middleware 前缀的配置
- **不修改** `ClientException` 异常类名（属于异常体系，非模块名）
- **不修改** `clientIp`/`client-ip` 等业务字段名
- **不改变**任何业务逻辑、方法签名（除类名/包名变更导致的必要调整外）
- **不改变**配置属性的子键名（如 `component.cache.enabled` 保持 `enabled` 不变）

## Decisions

### Decision 1：模块命名方案

**选择**：`clients/client-xxx` → `components/component-xxx`

**理由**：`component` 语义准确描述了这些模块的角色——可插拔的技术组件。比 `clients`（暗示远程调用）更贴切，比 `modules`（过于泛化）更具体。

**备选**：`infrastructure/infra-xxx`（过于底层语义）、`plugins/plugin-xxx`（暗示动态加载）

### Decision 2：类名后缀方案

**选择**：`*Client` → `*Component`（如 `CacheClient` → `CacheComponent`）

**映射表**：

| 原类名 | 新类名 |
|--------|--------|
| `CacheClient` | `CacheComponent` |
| `AbstractCacheClient` | `AbstractCacheComponent` |
| `CaffeineCacheClient` | `CaffeineCacheComponent` |
| `OssClient` | `OssComponent` |
| `AbstractOssClient` | `AbstractOssComponent` |
| `LocalOssClient` | `LocalOssComponent` |
| `EmailClient` | `EmailComponent` |
| `AbstractEmailClient` | `AbstractEmailComponent` |
| `NoOpEmailClient` | `NoOpEmailComponent` |
| `SmsClient` | `SmsComponent` |
| `AbstractSmsClient` | `AbstractSmsComponent` |
| `NoOpSmsClient` | `NoOpSmsComponent` |
| `SearchClient` | `SearchComponent` |
| `AbstractSearchClient` | `AbstractSearchComponent` |
| `SimpleSearchClient` | `SimpleSearchComponent` |
| `AuthClient` | `AuthComponent` |
| `AbstractAuthClient` | `AbstractAuthComponent` |
| `SaTokenAuthClient` | `SaTokenAuthComponent` |
| `NoOpAuthClient` | `NoOpAuthComponent` |

**理由**：既然模块名改了，类名保持 `Client` 会有概念不一致。`Component` 后缀与 Spring 的 `@Component` 语义一致。

### Decision 3：配置前缀方案

**选择**：`middleware.*` → `component.*`

**映射表**：

| 原前缀 | 新前缀 |
|--------|--------|
| `middleware.cache` | `component.cache` |
| `middleware.object-storage` | `component.oss` |
| `middleware.email` | `component.email` |
| `middleware.sms` | `component.sms` |
| `middleware.search` | `component.search` |
| `middleware.auth` | `component.auth` |
| `middleware.ratelimit` | `component.ratelimit` |

**理由**：`component.*` 与模块名 `component-xxx` 形成统一命名链。`middleware.object-storage` 简化为 `component.oss` 更简洁且与模块名 `component-oss` 对应。

**注意**：`logging` 前缀不变（Spring Boot 惯例），`app`/`thread-pool` 前缀不变（应用配置）。

### Decision 4：YAML 文件重命名

**选择**：`application-optional.yaml` → `application-component.yaml`，内容从 `middleware:` 改为 `component:`

**理由**：文件名直接反映内容——存放 component 相关的可选配置示例。比 `optional` 更具体。

### Decision 5：实施策略 — 分 Chunk 执行

**选择**：按依赖关系分 5 个 Chunk，从底层到上层依次执行：

1. **Chunk 1**：目录重命名（`clients/` → `components/`，`client-*` → `component-*`）
2. **Chunk 2**：Java 包名重命名（`.client.` → `.component.`）+ 类名重命名（`*Client` → `*Component`）
3. **Chunk 3**：配置前缀替换（`middleware.*` → `component.*`）+ YAML 文件重命名
4. **Chunk 4**：POM 文件更新 + AutoConfiguration 注册更新
5. **Chunk 5**：文档更新（AGENTS.md、README.md、docs/ 下所有活跃文档）+ 文件重命名

**理由**：分层执行可以控制每个步骤的变更范围，出问题时更容易定位。每步完成后 `mvn compile` 验证。

## Risks / Trade-offs

- **[Risk] 大范围重命名导致编译失败** → 每个 Chunk 完成后执行 `mvn compile -pl app` 验证
- **[Risk] 类名重命名影响 app 模块中大量 import** → 使用 IDE 的 Refactor Rename 或全局搜索替换，一次性完成
- **[Risk] 配置前缀变更影响已部署环境的兼容性** → 这是一个脚手架项目（archetype），尚无生产部署，影响可控
- **[Trade-off] 不修改归档文件意味着历史文档中 `client-*` 引用会与新代码不一致** → 可接受，归档是历史快照
- **[Trade-off] `ClientException` 保持不变** → 它属于异常体系（`BizException`/`SysException` 并列），不是模块命名
