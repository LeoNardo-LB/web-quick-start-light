# 阶段 5b Spec：shared/ 按功能域自包含重组

## 概览

### 前置条件
- Phase 5 完成（config/controller/entity/base/generated 已迁入 shared/）
- 570 tests, 0 failures

### 核心变更
将 shared/ 从按技术层分组（config/internal/aspect/util）改为按功能域自包含分组。每个横切关注点是一个独立包，包含注解+切面+配置+属性。

### 操作统计
| 操作 | 数量 |
|------|------|
| 移动文件 | ~30 |
| 新建功能域包 | 6（ratelimit/idempotent/logging/operationlog/threadpool/context） |
| 消除的旧包 | 4（config/、internal/、util/context/、util/logging/） |

---

## 一、迁移映射表

### 1.1 限流功能域 → shared/ratelimit/

| 旧路径 | 新路径 |
|--------|--------|
| `shared.internal.aspect.ratelimit.RateLimit` | `shared.ratelimit.RateLimit` |
| `shared.internal.aspect.ratelimit.RateLimitAspect` | `shared.ratelimit.RateLimitAspect` |
| `shared.internal.aspect.ratelimit.BucketFactory` | `shared.ratelimit.BucketFactory` |
| `shared.internal.aspect.ratelimit.SpelKeyResolver` | `shared.ratelimit.SpelKeyResolver` |
| `shared.internal.aspect.ratelimit.LimitFallback` | `shared.ratelimit.LimitFallback` |
| `shared.config.RatelimitConfigure` | `shared.ratelimit.RatelimitConfigure` |
| `shared.config.properties.RateLimitProperties` | `shared.ratelimit.RateLimitProperties` |

### 1.2 幂等功能域 → shared/idempotent/

| 旧路径 | 新路径 |
|--------|--------|
| `shared.internal.aspect.idempotent.Idempotent` | `shared.idempotent.Idempotent` |
| `shared.internal.aspect.idempotent.IdempotentAspect` | `shared.idempotent.IdempotentAspect` |
| `shared.internal.aspect.idempotent.IdempotentKeyResolver` | `shared.idempotent.IdempotentKeyResolver` |
| `shared.config.IdempotentConfigure` | `shared.idempotent.IdempotentConfigure` |

### 1.3 操作日志功能域 → shared/operationlog/

| 旧路径 | 新路径 |
|--------|--------|
| `shared.internal.aspect.operationlog.LogAspect` | `shared.operationlog.LogAspect` |

### 1.4 日志功能域 → shared/logging/

| 旧路径 | 新路径 |
|--------|--------|
| `shared.config.LoggingConfigure` | `shared.logging.LoggingConfigure` |
| `shared.config.properties.LoggingProperties` | `shared.logging.LoggingProperties` |
| `shared.util.logging.LogMarkers` | `shared.logging.LogMarkers` |
| `shared.util.logging.SamplingTurboFilter` | `shared.logging.SamplingTurboFilter` |
| `shared.util.logging.SensitiveLogUtils` | `shared.logging.SensitiveLogUtils` |
| `shared.util.logging.SlowQueryInterceptor` | `shared.logging.SlowQueryInterceptor` |

### 1.5 上下文提升 → shared/context/

| 旧路径 | 新路径 |
|--------|--------|
| `shared.util.context.BizContext` | `shared.context.BizContext` |

### 1.6 线程池域 → shared/threadpool/

| 旧路径 | 新路径 |
|--------|--------|
| `shared.config.ThreadPoolConfigure` | `shared.threadpool.ThreadPoolConfigure` |
| `shared.config.properties.ThreadPoolProperties` | `shared.threadpool.ThreadPoolProperties` |

### 1.7 Web 域扩展 → shared/web/

| 旧路径 | 新路径 |
|--------|--------|
| `shared.config.WebConfigure` | `shared.web.WebConfigure` |
| `shared.config.properties.AppInfoProperties` | `shared.web.AppInfoProperties` |
| `shared.util.IpUtils` | `shared.web.IpUtils` |

### 1.8 不动的包

| 包 | 说明 |
|----|------|
| `shared.dal/` | 数据访问基础设施 |
| `shared.generated/` | 代码生成器 |
| `shared.pagination/` | 分页 |
| `shared.result/` | 响应模型 |
| `shared.util.KryoSerializer` | 序列化工具（保留原位） |
| `shared.util.SpringContextUtils` | Spring 工具（保留原位） |

---

## 二、重组后 shared/ 完整结构

```
shared/
├── package-info.java
├── context/                     ← BizContext（从 util/context/ 提升）
│   └── BizContext.java
├── dal/                         ← 不动
│   ├── BaseDO.java
│   ├── InstantTypeHandler.java
│   └── MyMetaObjectHandler.java
├── generated/                   ← 不动
│   └── MybatisPlusGenerator.java
├── idempotent/                  ← 幂等功能域
│   ├── Idempotent.java
│   ├── IdempotentAspect.java
│   ├── IdempotentKeyResolver.java
│   └── IdempotentConfigure.java
├── logging/                     ← 日志功能域
│   ├── LoggingConfigure.java
│   ├── LoggingProperties.java
│   ├── LogMarkers.java
│   ├── SamplingTurboFilter.java
│   ├── SensitiveLogUtils.java
│   └── SlowQueryInterceptor.java
├── mybatis/                     ← MyBatis 基础设施
│   ├── MybatisPlusConfigure.java
│   └── MybatisPlusProperties.java  （如有）
├── operationlog/                ← 操作日志功能域
│   └── LogAspect.java
├── pagination/                  ← 不动
│   ├── PageQuery.java
│   └── PageResult.java
├── ratelimit/                   ← 限流功能域
│   ├── RateLimit.java
│   ├── RateLimitAspect.java
│   ├── BucketFactory.java
│   ├── SpelKeyResolver.java
│   ├── LimitFallback.java
│   ├── RatelimitConfigure.java
│   └── RateLimitProperties.java
├── result/                      ← 不动
│   ├── BaseResult.java
│   └── BasePageResult.java
├── threadpool/                  ← 线程池域
│   ├── ThreadPoolConfigure.java
│   └── ThreadPoolProperties.java
├── util/                        ← 缩减为纯工具
│   ├── KryoSerializer.java
│   └── SpringContextUtils.java
└── web/                         ← Web 域
    ├── WebConfigure.java
    ├── AppInfoProperties.java
    ├── ContextFillFilter.java
    ├── WebExceptionAdvise.java
    ├── TestController.java
    └── IpUtils.java
```

---

## 三、import 更新范围

### 3.1 限流注解 @RateLimit
Controller 使用此注解，需更新 import：
- 搜索 `import org.smm.archetype.shared.internal.aspect.ratelimit.RateLimit`
- 替换为 `import org.smm.archetype.shared.ratelimit.RateLimit`

### 3.2 幂等注解 @Idempotent
同理更新。

### 3.3 BizContext 引用
ThreadPoolConfigure 和其他异步传播代码引用：
- `import org.smm.archetype.shared.util.context.BizContext` → `import org.smm.archetype.shared.context.BizContext`

### 3.4 日志工具引用
SlowQueryInterceptor 被用于 MybatisPlusConfigure：
- `import org.smm.archetype.shared.util.logging.*` → `import org.smm.archetype.shared.logging.*`

---

## 四、测试文件迁移

### 4.1 限流测试
- `test/shared/internal/aspect/ratelimit/*` → `test/shared/ratelimit/*`

### 4.2 幂等测试
- `test/shared/internal/aspect/idempotent/*` → `test/shared/idempotent/*`

### 4.3 日志测试
- `test/shared/util/logging/*` → `test/shared/logging/*`

### 4.4 上下文测试
- `test/shared/util/context/*` → `test/shared/context/*`

### 4.5 配置测试
- `test/shared/config/*` → 对应功能域测试包

### 4.6 Web 测试
- `test/shared/web/*` 基本不动，但 import 可能需更新

---

## 五、空目录清理

迁移后删除：
- `shared/internal/aspect/ratelimit/`
- `shared/internal/aspect/idempotent/`
- `shared/internal/aspect/operationlog/`
- `shared/internal/aspect/`
- `shared/internal/`
- `shared/util/context/`
- `shared/util/logging/`
- `shared/config/properties/`
- `shared/config/`
- 对应的 test 空目录

---

## 六、ArchUnit 规则影响

### M-05（internal/ 包零 Spring 依赖）
Configure 类不再在 `internal/` 中，而是在功能域包中。需确认 M-05 例外列表更新（不再需要 Configure 例外，因为 Configure 不在 internal/ 中了）。

### 架构合规测试
检查四层架构测试中是否有硬编码 `shared.internal.aspect` 或 `shared.config` 的路径，需更新。

---

## 七、验证清单

```bash
mvn clean compile -pl app          # BUILD SUCCESS
mvn test -Dtest="*ComplianceUTest" # 全部通过
mvn test -pl app                   # 570 tests, 0 failures
```

---

## 八、任务分解

| # | 任务 | 文件数 |
|---|------|--------|
| 1 | ratelimit/ 功能域重组（7 文件 + 测试） | 7+ |
| 2 | idempotent/ 功能域重组（4 文件 + 测试） | 4+ |
| 3 | operationlog/ 功能域重组（1 文件 + 测试） | 1+ |
| 4 | logging/ 功能域重组（6 文件 + 测试） | 6+ |
| 5 | context/ + threadpool/ + web/ 扩展 | 6+ |
| 6 | mybatis/ 独立包（MybatisPlusConfigure） | 1+ |
| 7 | 测试文件迁移 + 空 test 目录清理 | ~15 |
| 8 | 空 main 目录清理 + import 残留验证 | 0 |
| 9 | ArchUnit 规则适配 + AGENTS.md 更新 | 1-3 |
