# 基础类型

> **职责**: 描述项目中的基础类型和通用响应包装
> **轨道**: Contract
> **维护者**: AI

## 目录

- [概述](#概述)

## 概述

基础类型模块位于 `entity/base` 包下，提供贯穿整个应用分层架构的公共类型定义。包括统一响应包装器 `BaseResult<T>` / `BasePageResult<T>`（封装返回码、traceId、时间戳）、请求基类 `BaseRequest` / `BasePageRequest`（含分页参数约束）、以及数据对象基类 `BaseDO`（审计字段 + 逻辑删除）。所有 Controller 均通过 `BaseResult` 返回统一格式的响应，前端据此解析业务状态。

## 公共 API 参考

### BaseResult\<T\> — 统一响应包装器

```java
package org.smm.archetype.entity.base;

@Getter
@Setter
public class BaseResult<T> {
    private int code;               // 业务状态码（对应 ErrorCode.code()）
    private boolean success;        // 是否成功
    private String message;         // 消息描述
    private T data;                 // 响应数据
    private String traceId;         // OTel W3C 标准 32 字符 hex traceId
    private Instant time;           // 响应时间戳

    // 工厂方法
    public static BaseResult<Void> fail();
    public static BaseResult<Void> fail(ErrorCode errorCode);
    public static BaseResult<Void> fail(ErrorCode errorCode, String msg);
    public static <T> BaseResult<T> success(T data);
}
```

**契约说明**：

- `fail()` 无参版本使用 `CommonErrorCode.FAIL` 作为默认错误码
- `fail(ErrorCode)` 使用错误码的默认 `message()`
- `fail(ErrorCode, String)` 使用自定义消息覆盖默认值
- `success(T)` 自动填充 `code=1000`、`message="success"`、`traceId`（从 OTel Span 上下文获取）、`time`（当前时间）

**成功响应示例**：

```json
{
  "code": 1000,
  "success": true,
  "message": "success",
  "data": { "token": "abc123" },
  "traceId": "0192a3b4c5d6e7f8a9b0c1d2e3f4a5b6",
  "time": "2026-04-25T10:30:00Z"
}
```

**失败响应示例**：

```json
{
  "code": 2000,
  "success": false,
  "message": "操作失败",
  "data": null,
  "traceId": "0192a3b4c5d6e7f8a9b0c1d2e3f4a5b6",
  "time": "2026-04-25T10:30:00Z"
}
```

### BasePageResult\<T\> — 分页响应包装器

```java
package org.smm.archetype.entity.base;

@Getter
@Setter
public class BasePageResult<T> extends BaseResult<List<T>> {
    private long total;          // 总记录数
    private int pageNo;          // 当前页码
    private int pageSize;        // 每页大小

    // 从 MyBatis-Plus 分页结果转换
    public static <T> BasePageResult<T> fromPage(IPage<T> page);
}
```

**契约说明**：

- 继承 `BaseResult<List<T>>`，`data` 字段为当前页的记录列表
- `fromPage()` 是唯一的创建入口，从 MyBatis-Plus 的 `IPage<T>` 对象自动提取分页元数据
- 自动填充 `code`、`message`、`traceId`、`time`（与 `BaseResult.success()` 行为一致）

**分页响应示例**：

```json
{
  "code": 1000,
  "success": true,
  "message": "success",
  "data": [
    { "id": 1, "configKey": "site.name", "configValue": "MyApp" },
    { "id": 2, "configKey": "site.logo", "configValue": "/logo.png" }
  ],
  "total": 25,
  "pageNo": 1,
  "pageSize": 20,
  "traceId": "0192a3b4c5d6e7f8a9b0c1d2e3f4a5b6",
  "time": "2026-04-25T10:30:00Z"
}
```

### BaseRequest — 请求基类

```java
package org.smm.archetype.entity.base;

@Getter
@Setter
public class BaseRequest {
    private String requestId;     // 请求序列号
    private String traceId;       // 链路追踪 ID
}
```

### BasePageRequest — 分页请求基类

```java
package org.smm.archetype.entity.base;

@Getter
@Setter
public class BasePageRequest extends BaseRequest {
    @Min(1)
    private int pageNo = 1;       // 当前页码，最小值 1

    @Min(1)
    @Max(100)
    private int pageSize = 20;    // 每页大小，范围 1-100
}
```

**契约说明**：

- 使用 JSR-380 注解（`@Min`, `@Max`）做参数校验，校验失败由 `WebExceptionAdvise` 捕获并返回 `ILLEGAL_ARGUMENT(2001)` 错误码
- 默认值：`pageNo=1`、`pageSize=20`

### BaseDO — 数据对象基类

```java
package org.smm.archetype.entity.base;

@Getter
@Setter
public abstract class BaseDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;                       // 雪花算法主键

    @TableField(fill = FieldFill.INSERT)
    private Instant createTime;            // 创建时间（自动填充）

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Instant updateTime;            // 更新时间（自动填充）

    @TableField(fill = FieldFill.INSERT)
    private String createUser;             // 创建人（自动填充）

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateUser;             // 更新人（自动填充）

    private Long deleteTime;               // 逻辑删除标记：0 = 未删除，非 0 = 删除时间戳
    private String deleteUser;             // 删除人 ID
}
```

**契约说明**：

- 所有 MyBatis-Plus 数据对象（DO）必须继承此基类
- 主键使用 `ASSIGN_ID` 策略（雪花算法），保证分布式唯一性
- `createTime` / `updateTime` 使用 `Instant` 类型（UTC 时间戳）
- 逻辑删除通过 `deleteTime` 时间戳实现（非 0 表示已删除），而非 Boolean 标志位

## 核心类型定义

### 类型层级关系

```
BaseResult<T>
  └── BasePageResult<T> extends BaseResult<List<T>>

BaseRequest
  └── BasePageRequest extends BaseRequest

BaseDO (abstract)
  ├── UserDO (generated)
  ├── SystemConfigDO (generated)
  └── OperationLogDO (generated)
```

### DO / Entity / VO 模式

项目采用三层对象模型分离策略：

| 层级 | 类型 | 职责 | 示例 |
|------|------|------|------|
| **DO** (Data Object) | MyBatis-Plus 映射 | 持久化层，含 ORM 注解，继承 `BaseDO` | `UserDO`, `SystemConfigDO` |
| **Entity** | 领域模型 | 业务逻辑层，纯 POJO，不含 ORM 注解 | `User`, `SystemConfig`, `OperationLog` |
| **VO** (View Object) | 视图对象 | Controller 层输出，Java record，面向前端 | `SystemConfigVO`, `OperationLogVO` |

**转换路径**：

```
DO ←→ Entity (通过 Repository Converter)
Entity → VO  (通过 Facade 实现)
```

## 服务流程

### 统一响应创建流程

```
Controller 方法
    │
    ├── 正常流程
    │   ├── Facade/Service 返回 Entity
    │   ├── Facade 转换 Entity → VO
    │   └── BaseResult.success(voList)
    │       ├── code = 1000
    │       ├── success = true
    │       ├── message = "success"
    │       ├── data = voList
    │       ├── traceId = Span.current().getSpanContext().getTraceId()
    │       └── time = Instant.now()
    │
    └── 分页查询流程
        ├── Repository 返回 IPage<Entity>
        ├── Facade 转换 Entity → VO
        ├── BasePageResult.fromPage(page)
        │   ├── total = page.getTotal()
        │   ├── pageNo = (int) page.getCurrent()
        │   ├── pageSize = (int) page.getSize()
        │   └── data = voList
        └── 自动填充 code/success/message/traceId/time
```

### 分页参数校验流程

```
HTTP 请求（GET /api/system/configs/page?pageNo=1&pageSize=50&groupCode=BASIC）
    │
    ▼
BasePageRequest / PageQuery record（JSR-380 注解）
    │
    ├── pageNo < 1     → ConstraintViolationException → ILLEGAL_ARGUMENT(2001)
    ├── pageSize < 1   → ConstraintViolationException → ILLEGAL_ARGUMENT(2001)
    ├── pageSize > 100 → ConstraintViolationException → ILLEGAL_ARGUMENT(2001)
    │
    └── 校验通过 → Repository 分页查询
```

## 依赖关系

### 外部依赖

| 依赖 | 用途 |
|------|------|
| `com.baomidou.mybatisplus.annotation` | `BaseDO` 的 `@TableId`、`@TableField`、`IdType`、`FieldFill` |
| `com.baomidou.mybatisplus.core.metadata.IPage` | `BasePageResult.fromPage()` 的参数类型 |
| `io.opentelemetry.api.trace.Span` | `traceId` 自动获取（W3C 标准 32 字符 hex） |
| `org.smm.archetype.exception.CommonErrorCode` | `BaseResult` / `BasePageResult` 的成功/失败码 |
| `org.smm.archetype.exception.ErrorCode` | `BaseResult.fail()` 的参数类型 |
| `jakarta.validation.constraints` | `BasePageRequest` 的 `@Min` / `@Max` 校验注解 |
| `lombok` | `@Getter` / `@Setter` / `@Builder` |
| `java.time.Instant` | 时间戳类型 |

### 模块间依赖

| 依赖方 | 使用类型 | 场景 |
|--------|---------|------|
| **所有 Controller** | `BaseResult`, `BasePageResult` | 统一响应包装 |
| **所有 Facade** | `BaseResult`, `BasePageResult` | 返回包装后的 VO |
| **generated 层 DO** | `BaseDO` | 继承审计字段和逻辑删除 |
| **分页查询 DTO** | `BasePageRequest` | 继承分页参数约束 |

## 相关文档

| 文档 | 关系 | 说明 |
|------|------|------|
| 异常体系 | business/exception-system.md | BaseResult.fail() 使用 ErrorCode |
| REST API 参考 | business/api-reference.md | 所有端点使用 BaseResult/BasePageResult |
| 用户领域 | business/domain-user.md | User Entity 使用领域模型模式 |
| 系统配置领域 | business/domain-system-config.md | SystemConfig Entity 使用值对象模式 |
| 操作日志领域 | business/domain-operation-log.md | OperationLog Entity 使用 Builder 模式 |

## 变更历史

| 日期 | 版本 | 变更内容 |
|------|------|---------|
| 2026-04-25 | v1.0 | 初始版本：基础类型文档生成 |
