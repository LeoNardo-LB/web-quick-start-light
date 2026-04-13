## ADDED Requirements

### Requirement: Facade 薄层架构
系统 SHALL 在 Controller 和 Service 之间引入 Facade 层。Controller SHALL 依赖 Facade 接口而非直接依赖 Service。Facade 层负责 DTO 转换（Request → Command，Entity → VO）和多 Service 编排。

#### Scenario: Controller 通过 Facade 调用业务逻辑
- **WHEN** Controller 需要执行业务操作
- **THEN** Controller SHALL 注入 Facade 接口并调用其方法，不直接注入 Service

#### Scenario: Facade 负责将 Entity 转换为 VO
- **WHEN** Facade 从 Service 获取到 Entity
- **THEN** Facade SHALL 将 Entity 转换为 VO 后返回给 Controller

#### Scenario: Facade 编排多个 Service
- **WHEN** 一个业务场景需要调用多个 Service
- **THEN** Facade SHALL 编排多个 Service 调用并组装统一的 VO

### Requirement: Facade 包结构规范
Facade 层 SHALL 位于 `app/.../facade/<module>/` 包下。Facade 接口和实现 SHALL 定义在该包中。所有 DTO/VO/Command record SHALL 放在 facade 包下，不再放在 service 包下。

#### Scenario: VO 从 service 包迁移到 facade 包
- **WHEN** 系统 Config 的 VO 类（SystemConfigVO、ConfigGroupVO、UpdateConfigCommand）
- **THEN** 这些类 SHALL 位于 `facade.system` 包下

#### Scenario: Facade 定义接口和实现
- **WHEN** 为某个业务模块创建 Facade
- **THEN** SHALL 同时创建接口（如 SystemConfigFacade）和实现类（如 SystemConfigFacadeImpl）

### Requirement: Service 层返回 Entity
Service 层 SHALL 返回 Entity（领域对象）而非 VO。VO 组装职责 SHALL 由 Facade 层承担。

#### Scenario: Service 方法返回 Entity
- **WHEN** Facade 调用 Service 的查询方法
- **THEN** Service SHALL 返回 Entity 对象（如 SystemConfig），而非 VO

### Requirement: ArchUnit 架构规则更新
ArchUnit 测试 SHALL 更新以守护四层架构规则：Controller 禁止直接依赖 Service（必须通过 Facade），Facade 禁止直接依赖 Repository（必须通过 Service）。

#### Scenario: Controller 直接注入 Service 时 ArchUnit 报错
- **WHEN** 开发者在 Controller 中直接注入 Service Bean
- **THEN** ArchUnit 测试 SHALL 失败

#### Scenario: Facade 直接注入 Repository 时 ArchUnit 报错
- **WHEN** 开发者在 Facade 中直接注入 Repository 或 Mapper
- **THEN** ArchUnit 测试 SHALL 失败
