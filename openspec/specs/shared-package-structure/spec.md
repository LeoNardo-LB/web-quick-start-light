## ADDED Requirements

### Requirement: shared 包组织规范

app 模块 SHALL 包含 `shared` 包作为跨层共享基础设施的统一存放位置，采用职责子包模式组织：

- `shared/aspect/`：AOP 切面及配套组件，按业务再分子包（ratelimit/、idempotent/、operationlog/）
- `shared/logging/`：日志基础设施工具（慢查询拦截器、采样过滤器、脱敏工具、Marker 常量、日志目录验证）
- `shared/util/`：通用工具类（线程上下文传递、DAL 横切处理、序列化、IP 工具等）

#### Scenario: 新增横切关注点放置位置

- **WHEN** 开发者需要新增一个 AOP 横切关注点（如 @Permission）
- **THEN** 该关注点的注解、切面、辅助类 SHALL 放置在 `shared/aspect/<业务名>/` 下

#### Scenario: util 顶级包不存在

- **WHEN** 检查 app 模块的顶级包结构
- **THEN** 不存在 `util` 顶级包，所有工具类 SHALL 在 `shared/util/` 或其子包下

### Requirement: shared 包不参与四层流转

`shared` 包下的组件 SHALL NOT 参与 Controller→Facade→Service→Repository 的四层业务流转。shared 中的切面、工具类是被四层使用的横切基础设施。

#### Scenario: shared 包不依赖 controller/facade/service/repository

- **WHEN** ArchUnit 检查包间依赖
- **THEN** `shared` 包下的类 SHALL NOT 依赖 `controller`、`facade`、`service`、`repository` 包中的具体业务类

### Requirement: aspect 子包自包含

`shared/aspect/` 下的每个业务子包 SHALL 是自包含的——包含该横切功能所需的全部组件（注解、切面、辅助类、枚举），不跨子包引用。

#### Scenario: ratelimit 子包完整性

- **WHEN** 查看 `shared/aspect/ratelimit/` 目录
- **THEN** 该目录 SHALL 包含 RateLimit.java（注解）、RateLimitAspect.java（切面）、SpelKeyResolver.java、BucketFactory.java、LimitFallback.java

#### Scenario: idempotent 子包完整性

- **WHEN** 查看 `shared/aspect/idempotent/` 目录
- **THEN** 该目录 SHALL 包含 Idempotent.java（注解）、IdempotentAspect.java（切面）、IdempotentKeyResolver.java

#### Scenario: operationlog 子包完整性

- **WHEN** 查看 `shared/aspect/operationlog/` 目录
- **THEN** 该目录 SHALL 包含
  BusinessLog.java（注解）、LogAspect.java（切面）、OperationLogWriter.java（接口）、OperationLogRecord.java（record）、OperationType.java（枚举）
