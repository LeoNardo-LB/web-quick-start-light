## Why

当前项目中 `clients` 模块及其 `client-*` 子模块的命名采用"客户端"语义，但实际承载的是可插拔的技术组件（缓存、对象存储、邮件、短信、搜索、认证）。名称与实际职责不匹配，且配置前缀 `middleware.*` 与模块命名无关联。需要将模块名、目录名、Java 包名、类名、配置前缀统一重命名为 `component` 语义，提升概念一致性和代码可读性。

## What Changes

- **BREAKING**：目录 `clients/` 重命名为 `components/`，6 个子目录 `client-*` 重命名为 `component-*`
- **BREAKING**：Java 包名 `org.smm.archetype.client` 全部重命名为 `org.smm.archetype.component`
- **BREAKING**：Java 类名中 `Client` 后缀全部改为 `Component`（如 `CacheClient` → `CacheComponent`，`AbstractCacheClient` → `AbstractCacheComponent`）
- **BREAKING**：配置前缀从 `middleware.*` 统一改为 `component.*`（如 `middleware.cache` → `component.cache`）
- **BREAKING**：`middleware.object-storage` 简化为 `component.oss`
- **BREAKING**：`application-optional.yaml` 重命名为 `application-component.yaml`
- Maven POM 中所有 `client-xxx` artifactId 改为 `component-xxx`，`clients` 改为 `components`
- Spring AutoConfiguration.imports 中全限定类名同步更新
- 所有文档（AGENTS.md、README.md、docs/）中的中文"客户端"改为"组件"，模块名/路径同步更新
- OpenSpec 归档文件（`openspec/changes/archive/`、`openspec/specs/`）**不修改**

## Capabilities

### New Capabilities

- `component-naming-convention`: 定义 component 模块命名规范，覆盖目录名、artifactId、Java 包名、类名、配置前缀的统一命名规则

### Modified Capabilities

- `client-modules-replacement`: 原有的 client 模块替换规范，需要将所有 `client` 语义更新为 `component`，类名从 `*Client` 更新为 `*Component`
- `auth`: AuthClient/AuthAutoConfiguration 等类重命名为 AuthComponent/AuthAutoConfiguration，包路径和配置前缀同步变更

## Impact

- **POM 文件**：9 个 pom.xml 需修改（根 POM、clients 父 POM、app POM、6 个子模块 POM）
- **Java 源码**：约 60 个 .java 文件需修改（package 声明、import、类名、注释、测试断言）
- **配置文件**：3 个 YAML 文件 + 1 个重命名（application-optional.yaml → application-component.yaml）
- **AutoConfiguration 注册**：7 个 .imports 文件
- **文档**：约 15 个活跃 MD 文件需修改 + 9 个 MD 文件需重命名
- **不涉及**：OpenSpec 归档、YAML/Properties 中无 middleware 引用的文件、scripts/、Docker/CI 配置
