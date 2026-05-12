# 阶段 5 Spec：公共基础类统一迁入 shared 包

## 概览

### 前置条件
- Phase 1-4 全部完成
- 570 tests, 0 failures
- 三模块（auth/operationlog/systemconfig）已按 Spring Modulith 模式重组

### 核心变更
将分散在 `config/`、`controller/`、`entity/base/`、`generated/` 的公共基础类统一迁入 `shared/` 包，使 app 级别的包结构只保留业务模块和 shared 两个顶层概念。

### 操作统计
| 操作 | 数量 |
|------|------|
| 新增包 | 3（shared/config、shared/dal、shared/web、shared/generated） |
| 迁移文件（main） | 15 |
| 迁移文件（test） | 6 |
| 删除空目录 | ~14 |
| import 更新 | 全项目 |

---

## 一、迁移映射表

### 1.1 config/ → shared/config/

| 旧路径 | 新路径 | 变更类型 |
|--------|--------|---------|
| `config.IdempotentConfigure` | `shared.config.IdempotentConfigure` | 包迁移 |
| `config.LoggingConfigure` | `shared.config.LoggingConfigure` | 包迁移 |
| `config.MybatisPlusConfigure` | `shared.config.MybatisPlusConfigure` | 包迁移 |
| `config.RatelimitConfigure` | `shared.config.RatelimitConfigure` | 包迁移 |
| `config.ThreadPoolConfigure` | `shared.config.ThreadPoolConfigure` | 包迁移 |
| `config.WebConfigure` | `shared.config.WebConfigure` | 包迁移 |
| `config.properties.AppInfoProperties` | `shared.config.properties.AppInfoProperties` | 包迁移 |
| `config.properties.LoggingProperties` | `shared.config.properties.LoggingProperties` | 包迁移 |
| `config.properties.RateLimitProperties` | `shared.config.properties.RateLimitProperties` | 包迁移 |
| `config.properties.ThreadPoolProperties` | `shared.config.properties.ThreadPoolProperties` | 包迁移 |

### 1.2 controller/ → shared/web/

| 旧路径 | 新路径 | 变更类型 |
|--------|--------|---------|
| `controller.global.ContextFillFilter` | `shared.web.ContextFillFilter` | 包迁移 |
| `controller.global.WebExceptionAdvise` | `shared.web.WebExceptionAdvise` | 包迁移 |
| `controller.test.TestController` | `shared.web.TestController` | 包迁移 |

### 1.3 entity/base/ + shared/util/dal/ → shared/dal/

| 旧路径 | 新路径 | 变更类型 |
|--------|--------|---------|
| `entity.base.BaseDO` | `shared.dal.BaseDO` | 包迁移 |
| `shared.util.dal.InstantTypeHandler` | `shared.dal.InstantTypeHandler` | 包迁移 |
| `shared.util.dal.MyMetaObjectHandler` | `shared.dal.MyMetaObjectHandler` | 包迁移 |

### 1.4 generated/ → shared/generated/

| 旧路径 | 新路径 | 变更类型 |
|--------|--------|---------|
| `generated.MybatisPlusGenerator` | `shared.generated.MybatisPlusGenerator` | 包迁移 |

---

## 二、迁移后 shared/ 完整结构

```
shared/
├── package-info.java                 ← 已有（@ApplicationModule OPEN）
├── config/                           ← 新（从 config/ 迁入）
│   ├── IdempotentConfigure.java
│   ├── LoggingConfigure.java
│   ├── MybatisPlusConfigure.java
│   ├── RatelimitConfigure.java
│   ├── ThreadPoolConfigure.java
│   ├── WebConfigure.java
│   └── properties/
│       ├── AppInfoProperties.java
│       ├── LoggingProperties.java
│       ├── RateLimitProperties.java
│       └── ThreadPoolProperties.java
├── dal/                              ← 新（BaseDO + 原 util/dal 合并）
│   ├── BaseDO.java
│   ├── InstantTypeHandler.java
│   └── MyMetaObjectHandler.java
├── generated/                        ← 新（从 generated/ 迁入）
│   └── MybatisPlusGenerator.java
│   └── templates/                    ← 已有（Freemarker 模板）
├── internal/aspect/                  ← 已有，不动
│   ├── idempotent/
│   ├── operationlog/
│   └── ratelimit/
├── pagination/                       ← 已有，不动
│   ├── PageQuery.java
│   └── PageResult.java
├── result/                           ← 已有，不动
│   ├── BaseResult.java
│   └── BasePageResult.java
├── util/                             ← 缩减（dal/ 已提升）
│   ├── IpUtils.java
│   ├── KryoSerializer.java
│   ├── SpringContextUtils.java
│   ├── context/BizContext.java
│   └── logging/
│       ├── LogMarkers.java
│       ├── SamplingTurboFilter.java
│       ├── SensitiveLogUtils.java
│       └── SlowQueryInterceptor.java
└── web/                              ← 新（从 controller/ 迁入）
    ├── ContextFillFilter.java
    ├── WebExceptionAdvise.java
    └── TestController.java
```

---

## 三、import 更新范围

迁移后需更新所有引用旧包路径的文件。主要涉及：

### 3.1 BaseDO 引用（影响最大）
所有 DO 类继承 BaseDO，需更新 import：
- `auth/internal/UserDO.java`
- `operationlog/internal/OperationLogDO.java`
- `systemconfig/internal/SystemConfigDO.java`
- `shared/generated/MybatisPlusGenerator.java`
- `shared/dal/MyMetaObjectHandler.java`（引用 BaseDO）

### 3.2 Configure 类引用
每个 Configure 的 import 路径从 `config.*` 变为 `shared.config.*`，影响：
- 各 Configure 类内部的 import（如 `LoggingConfigure` 引用 `LoggingProperties`）
- `WebConfigure` 引用 `ContextFillFilter`
- `IdempotentConfigure` 引用 `IdempotentAspect`（已在 shared/internal/ 下，不变）

### 3.3 Web 基础设施引用
- `WebConfigure` 引用 `ContextFillFilter` → 同在 shared/ 下，相对路径变更
- `WebExceptionAdvise` 引用 `BaseResult` → 同在 shared/ 下

### 3.4 Properties 引用
Properties 前缀不变（如 `@ConfigurationProperties(prefix = "app.info")`），仅 import 路径变更。

---

## 四、测试文件迁移映射

| 旧路径 | 新路径 |
|--------|--------|
| `test/config/LoggingConfigureDirectoryUTest.java` | `test/shared/config/LoggingConfigureDirectoryUTest.java` |
| `test/config/ScheduledExecutorContextPropagationUTest.java` | `test/shared/config/ScheduledExecutorContextPropagationUTest.java` |
| `test/config/TaskDecoratorContextPropagationUTest.java` | `test/shared/config/TaskDecoratorContextPropagationUTest.java` |
| `test/controller/ApiPrefixITest.java` | `test/shared/web/ApiPrefixITest.java` |
| `test/controller/ApiVersioningITest.java` | `test/shared/web/ApiVersioningITest.java` |
| `test/controller/global/ContextFillFilterUTest.java` | `test/shared/web/ContextFillFilterUTest.java` |

---

## 五、空目录清理

迁移后删除以下空目录：

**main 目录（~8 个）：**
- `controller/global/`
- `controller/test/`
- `controller/`
- `entity/base/`
- `entity/`
- `generated/`（模板 resources/templates 保留）
- `config/properties/`
- `config/`
- `shared/util/dal/`（文件已迁至 shared/dal/）

**test 目录（~6 个）：**
- `test/controller/global/`
- `test/controller/operationlog/`（已有的空目录）
- `test/controller/`
- `test/entity/`（已有的空目录）
- `test/config/properties/`
- `test/config/`
- `test/facade/operationlog/`（已有的空目录）
- `test/repository/operationlog/`（已有的空目录）
- `test/repository/user/`（已有的空目录）
- `test/service/auth/`（已有的空目录）

---

## 六、ArchUnit 规则影响分析

### 6.1 M-05（internal/ 包零 Spring 依赖）
- **无影响**：迁移的文件都在 `shared/` 根包或新子包下，不在任何业务模块的 `internal/` 中。

### 6.2 M-04（Controller 路径前缀）
- **需更新**：`NoRedundantConfigureUTest` 或 `WebConfigure` 中如有引用 `controller` 包路径的检测逻辑需更新为 `shared.web`。

### 6.3 NoRedundantConfigureUTest
- **需确认**：检查此测试是否扫描 `config/` 包下的 Configure 类。如果是，需更新扫描路径为 `shared.config/`。

### 6.4 ArchitectureComplianceUTest（四层架构）
- **需确认**：检查四层架构规则中的 Controller 层断言是否硬编码了 `controller` 包名。如果是，需更新为 `shared.web`。

---

## 七、验证清单

### 7.1 编译验证
```bash
mvn clean compile -pl app  # BUILD SUCCESS
```

### 7.2 ArchUnit 规则
```bash
mvn test -Dtest="*ComplianceUTest" -pl app  # 37 tests, 0 failures
```

### 7.3 全量测试
```bash
mvn test -pl app  # 570 tests, 0 failures
```

### 7.4 专项验证
- 旧包路径无残留 import（grep 验证）
- 空目录已清理（find 验证）
- 应用可启动（mvn spring-boot:run -pl app）

---

## 八、任务分解

| # | 任务 | 文件数 |
|---|------|--------|
| 1 | config/ → shared/config/（10 文件 + 更新 import） | 10+ |
| 2 | controller/ → shared/web/（3 文件 + 更新 import） | 3+ |
| 3 | entity/base/ + util/dal/ → shared/dal/（3 文件 + 更新 import） | 3+ |
| 4 | generated/ → shared/generated/（1 文件 + 模板 + 更新 import） | 1+ |
| 5 | 测试文件迁移（6 文件） | 6 |
| 6 | 空目录清理 + import 残留验证 | 0 |
| 7 | ArchUnit 规则适配（如需要） | 1-3 |
| 8 | AGENTS.md + spec 文档更新 | 2 |

---

## 九、风险点

| 风险 | 缓解措施 |
|------|---------|
| import 遗漏导致编译失败 | 编译验证 + grep 残留检查 |
| ArchUnit 规则硬编码旧包名 | 单独运行 ComplianceUTest 验证 |
| Properties 前缀变更 | 仅改 import，不改 @ConfigurationProperties prefix |
| 模板中硬编码旧包名 | 检查 .ftl 模板中的包路径 |
