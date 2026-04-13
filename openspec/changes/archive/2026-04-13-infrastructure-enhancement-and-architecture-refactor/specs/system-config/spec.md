## MODIFIED Requirements

### Requirement: 系统配置 Controller 使用 Facade 层
SystemConfigController SHALL 依赖 SystemConfigFacade 接口而非 SystemConfigService。所有 VO/Command 类 SHALL 从 service.system 包迁移到 facade.system 包。

#### Scenario: Controller 通过 Facade 操作配置
- **WHEN** SystemConfigController 处理请求
- **THEN** Controller SHALL 注入 SystemConfigFacade 并调用其方法

## ADDED Requirements

### Requirement: 系统配置分页查询端点
系统 SHALL 新增 `GET /api/system/configs/page` 分页查询端点，支持按 groupCode 筛选。

#### Scenario: 分页查询系统配置
- **WHEN** 客户端发送 `GET /api/system/configs/page?pageNo=1&pageSize=10`
- **THEN** 系统 SHALL 返回 BasePageResult<SystemConfigVO>
