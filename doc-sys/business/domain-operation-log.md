# 操作日志领域

> **职责**: 描述操作日志领域的 API 和查询流程
> **轨道**: Contract
> **维护者**: AI

## 目录

- [概述](#概述)

## 概述

操作日志领域负责系统操作日志的查询与展示。`OperationLogController` 提供 `/api/system/operation-logs` 路径下的分页查询端点，支持按模块、操作类型、时间范围等多条件动态过滤。`OperationLog` 领域实体使用 Builder 模式，包含 14 个业务字段（traceId、userId、module、operationType 等）。日志的写入由 `@BusinessLog` AOP 切面自动完成，本领域仅负责查询。

## 公共 API 参考

### OperationLogController — 操作日志控制器

```java
@Slf4j
@RestController
@RequestMapping("/api/system/operation-logs")
@RequiredArgsConstructor
@Tag(name = "操作日志")
@Validated
public class OperationLogController {

    private final OperationLogFacade operationLogFacade;

    @GetMapping
    @Operation(summary = "分页查询操作日志")
    public BasePageResult<OperationLogVO> findByPage(
            @Valid @ModelAttribute OperationLogPageQuery query);
}
```

#### REST 端点

| HTTP 方法 | 路径 | 参数 | 返回类型 | 说明 |
|:---------:|------|------|---------|------|
| GET | `/api/system/operation-logs` | `OperationLogPageQuery` (query) | `BasePageResult<OperationLogVO>` | 分页查询操作日志 |

### OperationLogFacade — 操作日志 Facade 接口

```java
package org.smm.archetype.facade.operationlog;

public interface OperationLogFacade {
    /**
     * 分页查询操作日志
     * @param query 分页查询参数
     * @return 分页结果（OperationLogVO）
     */
    BasePageResult<OperationLogVO> findByPage(OperationLogPageQuery query);
}
```

### OperationLogRepository — 操作日志仓储接口

```java
package org.smm.archetype.repository.operationlog;

public interface OperationLogRepository {
    IPage<OperationLog> findByPage(OperationLogPageQuery query);
}
```

### OperationLogService — 操作日志服务

```java
package org.smm.archetype.service.operationlog;

@Transactional(readOnly = true)
public class OperationLogService {
    IPage<OperationLog> findByPage(OperationLogPageQuery query);
}
```

**设计要点**：标注 `@Transactional(readOnly = true)` 只读事务，当前无写入操作（日志写入在 AOP 切面中完成）。

## 核心类型定义

### OperationLog — 操作日志领域实体

```java
package org.smm.archetype.entity.operationlog;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationLog {
    private Long id;                  // 日志 ID
    private String traceId;           // 追踪 ID
    private String userId;            // 操作用户 ID
    private String module;            // 模块名称
    private String operationType;     // 操作类型
    private String description;       // 操作描述
    private String method;            // 方法名
    private String params;            // 请求参数
    private String result;            // 返回结果
    private Long executionTime;       // 执行时间（ms）
    private String ip;                // IP 地址
    private String status;            // 状态
    private String errorMessage;      // 错误信息
    private Instant createTime;       // 创建时间
}
```

**设计要点**：使用 `@Builder` 模式，适用于 AOP 切面中灵活构建日志对象。`@NoArgsConstructor` + `@AllArgsConstructor` 为 MyBatis-Plus 和序列化提供支持。

### OperationLogVO — 操作日志 VO

```java
package org.smm.archetype.facade.operationlog;

public record OperationLogVO(
    Long id,
    String traceId,
    String userId,
    String module,
    String operationType,
    String description,
    String method,
    String params,
    String result,
    Long executionTime,
    String ip,
    String status,
    String errorMessage,
    Instant createTime
) {}
```

**设计要点**：Java record，不可变，面向前端展示。与 Entity 字段一一对应，但类型为纯 String/Long/Instant，无值对象封装。

### OperationLogPageQuery — 分页查询参数

```java
package org.smm.archetype.entity.operationlog;

public record OperationLogPageQuery(
    @Min(1) int pageNo,               // 当前页码，最小值 1
    @Min(1) @Max(100) int pageSize,   // 每页大小，范围 1-100
    String module,                     // 模块名称（可选过滤）
    String operationType,              // 操作类型（可选过滤）
    String startTime,                  // 开始时间（可选，ISO 8601 格式）
    String endTime                     // 结束时间（可选，ISO 8601 格式）
) {}
```

**校验规则**：

| 参数 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `pageNo` | int | `@Min(1)` | 当前页码 |
| `pageSize` | int | `@Min(1)`, `@Max(100)` | 每页大小 |
| `module` | String | 可选 | 模块名称过滤 |
| `operationType` | String | 可选 | 操作类型过滤 |
| `startTime` | String | 可选 | 开始时间（ISO 8601） |
| `endTime` | String | 可选 | 结束时间（ISO 8601） |

## 服务流程

### 分页查询流程

```
GET /api/system/operation-logs?pageNo=1&pageSize=20&module=system&startTime=2026-04-01T00:00:00Z
    │
    ▼
OperationLogController.findByPage(query)
    │
    ▼
OperationLogFacadeImpl.findByPage(query)
    │
    ▼
OperationLogService.findByPage(query)
    │  @Transactional(readOnly = true)
    ▼
OperationLogRepositoryImpl.findByPage(query)
    │
    ├── 1. 构建分页参数：Page<OperationLogDO>(pageNo, pageSize)
    │
    ├── 2. 构建动态 WHERE 条件（LambdaQueryWrapper）：
    │       ├── module != null     → WHERE module = ?
    │       ├── operationType != null → AND operation_type = ?
    │       ├── startTime != null  → AND create_time >= ?
    │       └── endTime != null    → AND create_time <= ?
    │
    ├── 3. 排序：ORDER BY id DESC
    │
    ├── 4. 执行查询：OperationLogMapper.selectPage()
    │
    └── 5. 转换：OperationLogConverter.toEntity() (DO → Entity)
            │
            ▼
    IPage<OperationLog>
            │
            ▼
OperationLogFacadeImpl (Entity → VO 转换)
    │
    ▼
BasePageResult<OperationLogVO>.fromPage(page)
```

### 查询结果示例

```json
{
  "code": 1000,
  "success": true,
  "message": "success",
  "data": [
    {
      "id": 1001,
      "traceId": "0192a3b4c5d6e7f8a9b0c1d2e3f4a5b6",
      "userId": "1234567890",
      "module": "system",
      "operationType": "UPDATE",
      "description": "更新系统配置",
      "method": "SystemConfigController.updateConfig",
      "params": "{\"key\":\"site.name\",\"value\":\"MyApp\"}",
      "result": "success",
      "executionTime": 15,
      "ip": "192.168.1.100",
      "status": "SUCCESS",
      "errorMessage": null,
      "createTime": "2026-04-25T10:30:00Z"
    }
  ],
  "total": 150,
  "pageNo": 1,
  "pageSize": 20,
  "traceId": "0192a3b4c5d6e7f8a9b0c1d2e3f4a5b6",
  "time": "2026-04-25T10:35:00Z"
}
```

### 日志写入流程（AOP 切面，非本领域直接管理）

```
业务方法 (标注 @BusinessLog)
    │
    ▼
LogAspect (AOP 切面)
    │
    ├── 1. 解析 @BusinessLog 注解获取描述
    ├── 2. 收集上下文信息（userId, module, method, params, ip, traceId）
    ├── 3. 执行目标方法，记录 executionTime
    ├── 4. 构建 OperationLog entity (Builder 模式)
    └── 5. OperationLogWriter 策略写入（支持采样率控制）
```

## 依赖关系

### 内部依赖

| 依赖组件 | 类型 | 用途 |
|---------|------|------|
| `OperationLogRepository` | 接口 | 数据访问抽象 |
| `OperationLogConverter` | 组件 | DO→Entity 单向转换 |
| `OperationLogService` | 服务 | 只读事务管理 |
| `BasePageResult` | 基础类型 | 分页响应包装 |

### 外部框架依赖

| 框架 | 用途 |
|------|------|
| Spring Boot Web | `@RestController`, `@GetMapping`, `@ModelAttribute` |
| MyBatis-Plus | `IPage`, `LambdaQueryWrapper`, 分页查询 |
| Hutool | `StrUtil.isNotBlank()` 条件式 WHERE 拼接 |
| Jakarta Validation | `@Min`, `@Max` 参数校验 |
| Swagger/OpenAPI | `@Tag`, `@Operation` API 文档注解 |
| Lombok | `@RequiredArgsConstructor`, `@Getter`, `@Setter`, `@Builder`, `@Slf4j` |

### 模块间依赖

```
OperationLogController (controller/operationlog)
    └── OperationLogFacade (facade/operationlog 接口)
            └── OperationLogFacadeImpl (facade/operationlog 实现)
                    ├── OperationLogService (service/operationlog)
                    │       └── OperationLogRepository (repository/operationlog 接口)
                    │               └── OperationLogRepositoryImpl (repository/operationlog 实现)
                    │                       ├── OperationLogMapper (generated)
                    │                       └── OperationLogConverter (DO→Entity)
                    └── Entity → VO 转换
```

## 相关文档

| 文档 | 关系 | 说明 |
|------|------|------|
| 基础类型 | business/base-types.md | BasePageResult 分页响应格式 |
| REST API 参考 | business/api-reference.md | 完整端点列表 |
| AOP 切面 | infrastructure/aop-aspects.md | @BusinessLog 切面写入日志 |
| 日志体系 | infrastructure/logging-system.md | 日志基础设施 |

## 变更历史

| 日期 | 版本 | 变更内容 |
|------|------|---------|
| 2026-04-25 | v1.0 | 初始版本：操作日志领域文档生成 |
