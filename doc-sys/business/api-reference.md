# REST API 参考

> **职责**: 提供所有 REST API 端点的完整参考
> **轨道**: Contract
> **维护者**: AI

## 目录

- [概述](#概述)

## 概述

本文档汇总了应用中所有 REST API 端点，涵盖认证（LoginController）、系统配置（SystemConfigController）、操作日志（OperationLogController）和测试接口（TestController）四个业务领域，共计 18 个端点。所有端点遵循统一响应格式 `BaseResult<T>` / `BasePageResult<T>`，通过 `WebExceptionAdvise` 全局异常处理器保证错误响应格式一致。

## 公共 API 参考

## 公共 API 参考

### API 前缀规范

所有业务 API 以 `/api` 为前缀，按领域分子路径：

| 领域 | 前缀 | Controller |
|------|------|-----------|
| 认证 | `/api/auth` | `LoginController` |
| 系统配置 | `/api/system/configs` | `SystemConfigController` |
| 操作日志 | `/api/system/operation-logs` | `OperationLogController` |
| 测试接口 | `/api/test` | `TestController` |

---

### 认证 API — `/api/auth`

#### POST `/api/auth/login` — 登录

| 属性 | 值 |
|------|-----|
| **标签** | 认证 |
| **描述** | 用户名密码登录，返回认证 Token |
| **请求方法** | POST |
| **Content-Type** | application/json |

**请求体** (`LoginRequest`)：

```json
{
  "username": "admin",
  "password": "123456"
}
```

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `username` | String | `@NotBlank` | 用户名 |
| `password` | String | `@NotBlank` | 密码 |

**成功响应** (`200 OK`)：

```json
{
  "code": 1000,
  "success": true,
  "message": "success",
  "data": { "token": "eyJhbGciOiJIUzI1NiJ9..." },
  "traceId": "0192a3b4c5d6e7f8a9b0c1d2e3f4a5b6",
  "time": "2026-04-25T10:30:00Z"
}
```

**失败响应**：

| code | message | 触发条件 |
|:----:|---------|---------|
| 2001 | `username: 用户名不能为空` | 用户名为空 |
| 2001 | `password: 密码不能为空` | 密码为空 |
| 6603 | `用户不存在` | 用户名未注册 |
| 6602 | `用户名或密码错误` | 密码不匹配 |

---

#### POST `/api/auth/logout` — 登出

| 属性 | 值 |
|------|-----|
| **描述** | 注销当前登录会话 |
| **请求方法** | POST |
| **请求体** | 无 |

**成功响应** (`200 OK`)：

```json
{
  "code": 1000,
  "success": true,
  "message": "success",
  "data": null,
  "traceId": "...",
  "time": "..."
}
```

---

### 系统配置 API — `/api/system/configs`

#### GET `/api/system/configs` — 获取所有配置

| 属性 | 值 |
|------|-----|
| **标签** | 系统配置 |
| **描述** | 获取全部系统配置列表 |
| **请求方法** | GET |
| **参数** | 无 |

**成功响应** (`200 OK`)：`BaseResult<List<SystemConfigVO>>`

---

#### GET `/api/system/configs/groups` — 获取配置分组

| 属性 | 值 |
|------|-----|
| **描述** | 获取所有配置分组（枚举，不查数据库） |
| **请求方法** | GET |
| **参数** | 无 |

**成功响应** (`200 OK`)：

```json
{
  "code": 1000,
  "success": true,
  "data": [
    { "code": "BASIC", "displayName": "基础配置", "icon": "SettingOutlined", "color": "#1890ff" },
    { "code": "EMAIL", "displayName": "邮件配置", "icon": "MailOutlined", "color": "#52c41a" },
    { "code": "STORAGE", "displayName": "存储配置", "icon": "CloudOutlined", "color": "#faad14" },
    { "code": "SECURITY", "displayName": "安全配置", "icon": "LockOutlined", "color": "#722ed1" }
  ]
}
```

---

#### GET `/api/system/configs/page` — 分页查询系统配置

| 属性 | 值 |
|------|-----|
| **描述** | 按条件分页查询系统配置 |
| **请求方法** | GET |
| **参数类型** | Query String |

**查询参数** (`SystemConfigPageQuery`)：

| 参数 | 类型 | 默认值 | 约束 | 说明 |
|------|------|:------:|------|------|
| `pageNo` | Integer | 1 | `@Min(1)` | 当前页码 |
| `pageSize` | Integer | 20 | `@Min(1)`, `@Max(100)` | 每页大小 |
| `groupCode` | String | null | 可选 | 按分组编码过滤 |

**成功响应** (`200 OK`)：`BasePageResult<SystemConfigVO>`

---

#### GET `/api/system/configs/{key}` — 按 Key 获取配置

| 属性 | 值 |
|------|-----|
| **描述** | 根据配置键获取单条配置 |
| **请求方法** | GET |

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `key` | String | 配置键 |

**成功响应** (`200 OK`)：`BaseResult<SystemConfigVO>`

**失败响应**：

| code | 触发条件 |
|:----:|---------|
| 2000 | 配置键不存在 |

---

#### GET `/api/system/configs/group/{code}` — 按分组获取配置

| 属性 | 值 |
|------|-----|
| **描述** | 获取指定分组下的所有配置 |
| **请求方法** | GET |

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `code` | String | 分组编码（BASIC/EMAIL/STORAGE/SECURITY） |

**成功响应** (`200 OK`)：`BaseResult<List<SystemConfigVO>>`

---

#### PUT `/api/system/configs/{key}` — 更新配置

| 属性 | 值 |
|------|-----|
| **描述** | 更新指定配置的值 |
| **请求方法** | PUT |
| **Content-Type** | application/json |

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `key` | String | 配置键 |

**请求体** (`UpdateConfigRequest`)：

```json
{
  "configValue": "new value"
}
```

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `configValue` | String | `@NotBlank` | 新的配置值 |

**成功响应** (`200 OK`)：`BaseResult<SystemConfigVO>`（返回更新后的配置）

**失败响应**：

| code | 触发条件 |
|:----:|---------|
| 2001 | `configValue: 配置值不能为空` |
| 2000 | 配置键不存在 |

---

### 操作日志 API — `/api/system/operation-logs`

#### GET `/api/system/operation-logs` — 分页查询操作日志

| 属性 | 值 |
|------|-----|
| **标签** | 操作日志 |
| **描述** | 按条件分页查询操作日志 |
| **请求方法** | GET |
| **参数类型** | Query String |

**查询参数** (`OperationLogPageQuery`)：

| 参数 | 类型 | 默认值 | 约束 | 说明 |
|------|------|:------:|------|------|
| `pageNo` | int | 无 | `@Min(1)` | 当前页码 |
| `pageSize` | int | 无 | `@Min(1)`, `@Max(100)` | 每页大小 |
| `module` | String | null | 可选 | 按模块过滤 |
| `operationType` | String | null | 可选 | 按操作类型过滤 |
| `startTime` | String | null | 可选 | 开始时间（ISO 8601） |
| `endTime` | String | null | 可选 | 结束时间（ISO 8601） |

**成功响应** (`200 OK`)：`BasePageResult<OperationLogVO>`

---

### 测试 API — `/api/test`

> ⚠️ 测试接口仅用于开发/调试环境，生产环境应禁用。

| HTTP 方法 | 路径 | 参数 | 返回类型 | 说明 |
|:---------:|------|------|---------|------|
| GET | `/api/test/hello` | 无 | `BaseResult<List<String>>` | Hello World |
| GET | `/api/test/exception` | 无 | `BaseResult<Void>` | 测试 BizException |
| GET | `/api/test/validate` | `name` (`@NotBlank`) | `BaseResult<String>` | 参数校验测试 |
| GET | `/api/test/bizlog` | 无 | `BaseResult<String>` | 测试 @BusinessLog 注解 |
| GET | `/api/test/client-exception` | 无 | `BaseResult<Void>` | 测试 ClientException |
| GET | `/api/test/sys-exception` | 无 | `BaseResult<Void>` | 测试 SysException |
| GET | `/api/test/generic-exception` | 无 | `BaseResult<Void>` | 测试通用 RuntimeException |
| GET | `/api/test/not-login` | 无 | `BaseResult<Void>` | 测试 NotLoginException |
| POST | `/api/test/bind-test` | `TestForm` (`name`: `@NotBlank`) | `BaseResult<String>` | 测试 BindException |

---

### 端点汇总表

| # | 方法 | 路径 | 领域 | 返回类型 |
|:-:|:----:|------|------|---------|
| 1 | POST | `/api/auth/login` | 认证 | `BaseResult<Map<String, String>>` |
| 2 | POST | `/api/auth/logout` | 认证 | `BaseResult<Void>` |
| 3 | GET | `/api/system/configs` | 系统配置 | `BaseResult<List<SystemConfigVO>>` |
| 4 | GET | `/api/system/configs/groups` | 系统配置 | `BaseResult<List<ConfigGroupVO>>` |
| 5 | GET | `/api/system/configs/page` | 系统配置 | `BasePageResult<SystemConfigVO>` |
| 6 | GET | `/api/system/configs/{key}` | 系统配置 | `BaseResult<SystemConfigVO>` |
| 7 | GET | `/api/system/configs/group/{code}` | 系统配置 | `BaseResult<List<SystemConfigVO>>` |
| 8 | PUT | `/api/system/configs/{key}` | 系统配置 | `BaseResult<SystemConfigVO>` |
| 9 | GET | `/api/system/operation-logs` | 操作日志 | `BasePageResult<OperationLogVO>` |
| 10 | GET | `/api/test/hello` | 测试 | `BaseResult<List<String>>` |
| 11 | GET | `/api/test/exception` | 测试 | `BaseResult<Void>` |
| 12 | GET | `/api/test/validate` | 测试 | `BaseResult<String>` |
| 13 | GET | `/api/test/bizlog` | 测试 | `BaseResult<String>` |
| 14 | GET | `/api/test/client-exception` | 测试 | `BaseResult<Void>` |
| 15 | GET | `/api/test/sys-exception` | 测试 | `BaseResult<Void>` |
| 16 | GET | `/api/test/generic-exception` | 测试 | `BaseResult<Void>` |
| 17 | GET | `/api/test/not-login` | 测试 | `BaseResult<Void>` |
| 18 | POST | `/api/test/bind-test` | 测试 | `BaseResult<String>` |

## 核心类型定义

### 请求类型

| 类型 | 字段 | 约束 | 使用端点 |
|------|------|------|---------|
| `LoginRequest` | `username: String`, `password: String` | `@NotBlank` | POST `/api/auth/login` |
| `UpdateConfigRequest` | `configValue: String` | `@NotBlank` | PUT `/api/system/configs/{key}` |
| `OperationLogPageQuery` | `pageNo: int`, `pageSize: int`, `module: String`, `operationType: String`, `startTime: String`, `endTime: String` | `@Min(1)`, `@Max(100)` | GET `/api/system/operation-logs` |
| `SystemConfigPageQuery` | `pageNo: Integer`, `pageSize: Integer`, `groupCode: String` | `@Min(1)`, `@Max(100)` | GET `/api/system/configs/page` |
| `TestForm` | `name: String` | `@NotBlank` | POST `/api/test/bind-test` |

### 响应类型

| 类型 | 字段 | 使用端点 |
|------|------|---------|
| `BaseResult<Map<String, String>>` | `code`, `success`, `message`, `data`, `traceId`, `time` | POST `/api/auth/login` |
| `BaseResult<Void>` | `code`, `success`, `message`, `data`, `traceId`, `time` | POST `/api/auth/logout` |
| `BaseResult<List<SystemConfigVO>>` | 同上 + `data: SystemConfigVO[]` | GET configs, GET group/{code} |
| `BaseResult<List<ConfigGroupVO>>` | 同上 + `data: ConfigGroupVO[]` | GET configs/groups |
| `BaseResult<SystemConfigVO>` | 同上 + `data: SystemConfigVO` | GET configs/{key}, PUT configs/{key} |
| `BasePageResult<SystemConfigVO>` | 同上 + `total`, `pageNo`, `pageSize` | GET configs/page |
| `BasePageResult<OperationLogVO>` | 同上 + `total`, `pageNo`, `pageSize` | GET operation-logs |

## 服务流程

### 统一请求处理流程

```
HTTP 请求
    │
    ▼
ContextFillFilter
    │  解析 userId → BizContext + OTel Baggage
    ▼
Controller 方法
    │  参数校验 (@Valid / @Validated)
    │
    ├── 校验失败 → WebExceptionAdvise → HTTP 200 + ILLEGAL_ARGUMENT(2001)
    │
    └── 校验通过 → 调用 Facade/Service
            │
            ├── 正常 → BaseResult.success(data) → HTTP 200
            │
            └── 异常 → WebExceptionAdvise
                    ├── BizException       → HTTP 200 + 业务错误码
                    ├── ClientException    → HTTP 200 + 外部错误码
                    ├── SysException       → HTTP 200 + 系统错误码
                    ├── NotLoginException  → HTTP 401 + AUTH_UNAUTHORIZED
                    ├── NoResourceFound    → HTTP 404 + FAIL
                    └── Exception          → HTTP 200 + UNKNOWN_ERROR(9999)
```

### HTTP 状态码规范

| HTTP 状态码 | 触发条件 | 说明 |
|:-----------:|---------|------|
| **200** | 所有业务异常 | 统一返回 200，业务状态通过 `code` 字段区分 |
| **401** | `NotLoginException` | 未登录或登录已过期 |
| **404** | `NoResourceFoundException` | 资源路径不存在 |

**设计原则**：绝大多数响应返回 HTTP 200，业务错误通过 `BaseResult.code` 区分。仅认证过期（401）和资源不存在（404）使用非 200 状态码。

## 依赖关系

### Controller 依赖汇总

| Controller | 依赖 Facade/Service | 依赖组件 |
|-----------|-------------------|---------|
| `LoginController` | `LoginFacade` | — |
| `SystemConfigController` | `SystemConfigFacade` | Swagger (`@Tag`, `@Operation`) |
| `OperationLogController` | `OperationLogFacade` | Swagger (`@Tag`, `@Operation`) |
| `TestController` | — | `@BusinessLog` AOP 注解 |

### 全局基础设施

| 组件 | 位置 | 职责 |
|------|------|------|
| `WebExceptionAdvise` | `controller/global` | `@RestControllerAdvice` 全局异常处理 |
| `ContextFillFilter` | `controller/global` | 请求上下文桥接（userId → BizContext + OTel） |

## 配置参考

### 分页参数通用约束

| 参数 | 最小值 | 最大值 | 默认值 |
|------|:------:|:------:|:------:|
| `pageNo` | 1 | — | 1 |
| `pageSize` | 1 | 100 | 20 |

### API 路由前缀

| 配置项 | 值 | 来源 |
|--------|-----|------|
| 认证 API | `/api/auth` | `LoginController` 硬编码 |
| 系统配置 API | `/api/system/configs` | `SystemConfigController` 硬编码 |
| 操作日志 API | `/api/system/operation-logs` | `OperationLogController` 硬编码 |
| 测试 API | `/api/test` | `TestController` 硬编码 |

## 相关文档

| 文档 | 关系 | 说明 |
|------|------|------|
| 异常体系 | business/exception-system.md | 错误码与异常类型定义 |
| 基础类型 | business/base-types.md | BaseResult/BasePageResult 响应格式 |
| 用户领域 | business/domain-user.md | 认证 API 的业务实现 |
| 系统配置领域 | business/domain-system-config.md | 系统配置 API 的业务实现 |
| 操作日志领域 | business/domain-operation-log.md | 操作日志 API 的业务实现 |

## 变更历史

| 日期 | 版本 | 变更内容 |
|------|------|---------|
| 2026-04-25 | v1.0 | 初始版本：REST API 参考文档生成 |
