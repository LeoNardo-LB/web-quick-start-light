## ADDED Requirements

### Requirement: API Version Control via Header
系统 SHALL 使用 Spring Boot 4 原生 API Versioning 能力，通过请求头 `API-Version` 传递版本号。所有 Controller 方法 SHALL 支持通过 `version` 属性声明版本约束。

#### Scenario: 请求携带 API-Version Header 匹配特定版本方法
- **WHEN** 客户端发送请求 Header `API-Version: 1.0` 到一个标注了 `@GetMapping(version = "1.0")` 的方法
- **THEN** 系统 SHALL 路由到该版本方法并返回正常响应

#### Scenario: 请求不携带 API-Version Header 访问无版本标注的方法
- **WHEN** 客户端发送请求不携带 `API-Version` Header 到一个没有 `version` 属性的方法
- **THEN** 系统 SHALL 正常路由到该方法

#### Scenario: 请求携带的版本不匹配任何方法
- **WHEN** 客户端发送请求 Header `API-Version: 9.0` 但没有匹配的版本方法
- **THEN** 系统 SHALL 路由到无版本标注的兜底方法（如果存在），否则返回 404

### Requirement: Unified API Prefix
所有业务 API 端点 SHALL 统一使用 `/api` 前缀。Controller 类级别或方法级别的 `@RequestMapping` SHALL 以 `/api` 开头。

#### Scenario: 访问带 /api 前缀的系统配置端点
- **WHEN** 客户端发送 `GET /api/system/configs`
- **THEN** 系统 SHALL 返回所有系统配置列表

#### Scenario: 访问不带 /api 前缀的端点返回 404
- **WHEN** 客户端发送 `GET /system/configs`（不带 /api 前缀）
- **THEN** 系统 SHALL 返回 404

### Requirement: API Version Configuration
系统 SHALL 在 `application.yaml` 中配置 `spring.mvc.apiversion.use-header: "API-Version"`。版本控制策略 SHALL 通过 Spring Boot 4 的 `WebMvcConfigurer.configureApiVersioning()` 配置。

#### Scenario: 应用启动时自动加载 API 版本控制
- **WHEN** 应用启动
- **THEN** 系统 SHALL 注册 `ApiVersionStrategy` 并从 `API-Version` Header 解析版本号
