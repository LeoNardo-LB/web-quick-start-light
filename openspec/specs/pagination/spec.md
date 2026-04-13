## ADDED Requirements

### Requirement: 操作日志分页查询示例
系统 SHALL 以操作日志为示例，展示完整的分页查询链路：Controller → Facade → Service → Repository。

#### Scenario: 操作日志分页查询完整链路
- **WHEN** 客户端发送 `GET /api/system/operation-logs?pageNo=1&pageSize=20`
- **THEN** 请求经过 Controller → OperationLogFacade → OperationLogService → OperationLogRepository，最终通过 MyBatis-Plus IPage 分页查询返回 BasePageResult<OperationLogVO>

### Requirement: 系统配置分页查询
系统配置 SHALL 新增分页查询端点 `GET /api/system/configs/page`，支持按 groupCode 筛选。

#### Scenario: 分页查询系统配置
- **WHEN** 客户端发送 `GET /api/system/configs/page?pageNo=1&pageSize=10`
- **THEN** 系统 SHALL 返回 BasePageResult<SystemConfigVO>，包含 total、pageNo、pageSize 和配置列表

#### Scenario: 按 groupCode 筛选系统配置
- **WHEN** 客户端发送 `GET /api/system/configs/page?groupCode=SITE`
- **THEN** 系统 SHALL 仅返回 SITE 分组的配置

### Requirement: 分页查询参数校验
分页查询参数 SHALL 复用 BasePageRequest 的校验规则（pageNo >= 1，pageSize 1-100，默认 pageNo=1，pageSize=20）。

#### Scenario: pageNo 和 pageSize 使用默认值
- **WHEN** 客户端发送 `GET /api/system/operation-logs`（不带分页参数）
- **THEN** 系统 SHALL 使用默认值 pageNo=1、pageSize=20

#### Scenario: pageSize 超过最大值时校验失败
- **WHEN** 客户端发送 `GET /api/system/operation-logs?pageSize=200`
- **THEN** 系统 SHALL 返回参数校验失败响应
