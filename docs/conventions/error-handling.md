# 错误处理规范

> 🔵 Constraint 轨 — 团队共识，文档驱动代码

## 📋 目录

- [规范目的](#规范目的)
- [规则](#规则)
- [常见违规场景](#常见违规场景)
- [检查清单](#检查清单)
- [相关文档](#相关文档)

## 规范目的

统一异常体系和错误码管理，支持国际化错误消息，确保前后端错误处理一致。

## 规则

### 规则 1: 三级异常体系

**⛔ MUST**

项目采用三级异常层次结构，全部继承自 `BaseException`（`org.smm.archetype.exception.BaseException`）：

```
RuntimeException
  └── BaseException          (abstract, 持有 ErrorCode)
        ├── BizException     (业务异常 — 可预期，前端展示)
        ├── ClientException  (组件参数异常 — 400 类)
        └── SysException     (系统内部异常 — 500 类)
```

| 异常类型 | HTTP 状态码 | 使用场景 | 日志级别 |
|----------|-------------|----------|----------|
| `BizException` | 200（业务码区分） | 业务规则校验失败、业务流程异常 | WARN |
| `ClientException` | 200（业务码区分） | 外部服务调用失败 | ERROR |
| `SysException` | 200（业务码区分） | 系统内部错误（数据库异常、NPE 等） | ERROR |

> **设计说明**：所有业务异常均返回 HTTP 200，通过 `BaseResult.code` 区分成功/失败。仅 `NotLoginException`（401）和 `NoResourceFoundException`（404）使用 HTTP 状态码。

✅ 正确：
```java
// 业务异常 — 用户可理解，前端直接展示
throw new BizException(CommonErrorCode.AUTH_BAD_CREDENTIALS);

// 系统异常 — 内部错误，前端展示通用提示
throw new SysException(CommonErrorCode.SYS_ERROR);
```

❌ 错误：
```java
// 直接抛 RuntimeException
throw new RuntimeException("配置不存在");  // 禁止

// 使用 HTTP 状态码区分业务异常
throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "参数错误");  // 禁止
```

> **为什么**：`RuntimeException` 无法被 `WebExceptionAdvise` 精确处理，会降级为兜底的 `UNKNOWN_ERROR`，丢失原始错误信息；`ResponseStatusException` 绕过了项目的 HTTP 200 + 业务码统一协议，导致前端需要同时处理两种错误格式。三级异常体系将"用户可理解的业务错误"（BizException）与"需要运维介入的系统错误"（SysException）明确区分，使前端可以针对性地展示友好提示或引导用户重试。

### 规则 2: ErrorCode 接口

**⛔ MUST**

所有错误码必须实现 `ErrorCode` 接口（`org.smm.archetype.exception.ErrorCode`）：

```java
public interface ErrorCode {
    int code();
    String message();

    /**
     * 国际化消息键，格式为 "error." + code()
     * 用于从 messages.properties 中查找对应语言的翻译
     */
    default String messageKey() {
        return "error." + code();
    }
}
```

- `code()` — 错误码数字（唯一标识）
- `message()` — 默认中文消息（当国际化资源未命中时回退）
- `messageKey()` — 国际化键，默认格式 `"error." + code()`

> **为什么**：`ErrorCode` 作为接口而非枚举，允许各模块定义自己的错误码枚举（如 `AuthErrorCode`、`CacheErrorCode`），同时保持统一的 `code()` / `message()` / `messageKey()` 契约。`messageKey()` 的默认实现 `"error." + code()` 提供了约定优于配置的国际化键生成策略，避免每次定义错误码时手动指定 key。

### 规则 3: CommonErrorCode 枚举

**⛔ MUST**

通用错误码定义在 `CommonErrorCode` 枚举中（`org.smm.archetype.exception.CommonErrorCode`），新增通用错误码必须在此枚举中添加，并同步更新国际化资源文件。

| 枚举值 | code | message | messageKey | 分类 |
|--------|------|---------|------------|------|
| `SUCCESS` | 1000 | 成功 | `error.1000` | 通用 |
| `FAIL` | 2000 | 操作失败 | `error.2000` | 通用 |
| `ILLEGAL_ARGUMENT` | 2001 | 参数校验失败 | `error.2001` | 通用 |
| `RPC_EXCEPTION` | 2002 | 外部服务调用失败 | `error.2002` | 通用 |
| `SYS_ERROR` | 5000 | 系统异常 | `error.5000` | 通用 |
| `UNKNOWN_ERROR` | 9999 | 未知异常 | `error.9999` | 通用 |
| `CACHE_OPERATION_FAILED` | 6001 | 缓存操作失败 | `error.6001` | Client-缓存 |
| `OSS_OPERATION_FAILED` | 6101 | 对象存储操作失败 | `error.6101` | Client-存储 |
| `OSS_UPLOAD_FAILED` | 6102 | 文件上传失败 | `error.6102` | Client-存储 |
| `EMAIL_SEND_FAILED` | 6201 | 邮件发送失败 | `error.6201` | Client-邮件 |
| `SMS_SEND_FAILED` | 6301 | 短信发送失败 | `error.6301` | Client-短信 |
| `SEARCH_OPERATION_FAILED` | 6401 | 搜索操作失败 | `error.6401` | Client-搜索 |
| `RATE_LIMIT_EXCEEDED` | 6501 | 请求过于频繁，请稍后再试 | `error.6501` | Client-限流 |
| `AUTH_UNAUTHORIZED` | 6601 | 未登录或登录已过期 | `error.6601` | Client-认证 |
| `AUTH_BAD_CREDENTIALS` | 6602 | 用户名或密码错误 | `error.6602` | Client-认证 |
| `AUTH_USER_NOT_FOUND` | 6603 | 用户不存在 | `error.6603` | Client-认证 |

**错误码分段约定**：

| 范围 | 分类 | 说明 |
|------|------|------|
| 1000 | 成功 | 正确响应 |
| 2000-2999 | 通用业务 | 业务/参数类异常 |
| 5000-5999 | 系统异常 | 内部系统错误 |
| 6000-6999 | Component 中间件 | 各组件模块错误 |
| 9999 | 兜底 | 未知异常 |

**新增错误码流程**：
1. 在 `CommonErrorCode` 枚举中添加新枚举值
2. 在 `messages.properties` 中添加中文翻译（`error.XXXX=中文消息`）
3. 在 `messages_en.properties` 中添加英文翻译（`error.XXXX=English message`）

> **为什么**：统一错误码枚举确保错误码不重复、错误消息可维护。`code()` 用数字编码方便前端 switch 处理，`messageKey()` 支持 i18n 热更新而非硬编码消息。错误码分段约定（4xx/5xx）让调用方能通过码值判断错误类型，无需解析消息文本。

### 规则 4: 国际化

**⛔ MUST**

错误消息必须支持国际化，通过 `messageKey` + `MessageSource` + `Accept-Language` Header 实现自动语言切换。

**国际化资源文件**：

| 文件 | 语言 | 位置 |
|------|------|------|
| `messages.properties` | 中文（默认） | `app/src/main/resources/` |
| `messages_en.properties` | 英文 | `app/src/main/resources/` |
| `ValidationMessages.properties` | 中文（默认） | `app/src/main/resources/` |
| `ValidationMessages_en.properties` | 英文 | `app/src/main/resources/` |

**国际化解析逻辑**（`WebExceptionAdvise.resolveExceptionMessage`）：

1. 当 `BizException` 使用 `new BizException(ErrorCode)` 构造时 → 默认消息 → 走国际化翻译
2. 当 `BizException` 使用 `new BizException(ErrorCode, "自定义消息")` 构造时 → 直接返回自定义消息（不走国际化）
3. 国际化消息找不到时 → 回退到 `messageKey` 本身

✅ 正确：
```java
// 使用默认消息 — 走国际化
throw new BizException(CommonErrorCode.AUTH_BAD_CREDENTIALS);
// 中文环境返回：用户名或密码错误
// 英文环境返回：Invalid username or password

// 使用自定义消息 — 不走国际化
throw new BizException(CommonErrorCode.AUTH_BAD_CREDENTIALS, "该手机号未注册");
```

❌ 错误：
```java
// 硬编码中文消息，不支持国际化
throw new BizException(CommonErrorCode.AUTH_BAD_CREDENTIALS, "用户名或密码错误");
// 应使用默认构造，让国际化机制处理
```

> **为什么**：国际化消息通过 `Accept-Language` Header 自动切换，是面向多语言用户的必要能力。硬编码中文消息使得非中文用户看到的是无法理解的内容，且每次修改文案都需要改代码、重新编译、重新部署。使用 `messageKey` + 资源文件的方式，文案修改只需更新 properties 文件即可热生效。

### 规则 5: 全局异常处理器

**⚠️ SHOULD**

`WebExceptionAdvise`（`org.smm.archetype.controller.global.WebExceptionAdvise`）是全局异常处理器，负责统一捕获异常并返回 `BaseResult<Void>`。

**处理的异常类型**：

| 异常类型 | 处理方式 | HTTP 状态码 | 国际化 |
|----------|----------|-------------|--------|
| `MethodArgumentNotValidException` | 提取字段错误信息 | 200 | 否（字段级消息） |
| `ConstraintViolationException` | 提取约束违反信息 | 200 | 否 |
| `BindException` | 提取绑定错误信息 | 200 | 否 |
| `BizException` | 解析 ErrorCode | 200 | 是（Accept-Language） |
| `ClientException` | 解析 ErrorCode | 200 | 是（Accept-Language） |
| `SysException` | 解析 ErrorCode | 200 | 是（Accept-Language） |
| `NoResourceFoundException` | 资源不存在 | 404 | 否 |
| `NotLoginException` | 未登录 | 401 | 是（Accept-Language） |
| `Exception` | 兜底处理 | 200 | 否 |

**国际化切换机制**：
- 通过 `LocaleResolver` 从 `Accept-Language` Header 解析语言
- `resolveExceptionMessage` 判断是否使用自定义消息
- `resolveMessage` 通过 `MessageSource` 查找翻译，找不到回退到 key

## 常见违规场景

### 场景 1: 直接 throw new RuntimeException 绕过异常体系

❌ 错误做法：
```java
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;

    public void updateConfig(String key, String value) {
        SystemConfig config = systemConfigRepository.getByKey(key);
        if (config == null) {
            throw new RuntimeException("配置不存在: " + key);  // 禁止！绕过异常体系
        }
        // ...
    }
}
```

✅ 正确做法：
```java
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;

    public void updateConfig(String key, String value) {
        SystemConfig config = systemConfigRepository.getByKey(key);
        if (config == null) {
            throw new BizException(CommonErrorCode.ILLEGAL_ARGUMENT, "配置不存在: " + key);
        }
        // ...
    }
}
```

> **后果**：`RuntimeException` 无法被 `WebExceptionAdvise` 精确匹配，降级为兜底的 `UNKNOWN_ERROR(9999)`，前端只看到"未知异常"而非有意义的错误提示。同时该异常不会被 Micrometer 指标系统正确分类，影响错误监控。

### 场景 2: 在 Controller 中 try-catch 后返回自定义格式

❌ 错误做法：
```java
@RestController
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigFacade systemConfigFacade;

    @GetMapping("/{key}")
    public Map<String, Object> getByKey(@PathVariable String key) {
        try {
            SystemConfigVO config = systemConfigFacade.getByKey(key);
            return Map.of("success", true, "data", config);  // 禁止！自定义响应格式
        } catch (BizException e) {
            return Map.of("success", false, "msg", e.getMessage());  // 禁止！绕过全局异常处理
        }
    }
}
```

✅ 正确做法：
```java
@RestController
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigFacade systemConfigFacade;

    @GetMapping("/{key}")
    public BaseResult<SystemConfigVO> getByKey(@PathVariable String key) {
        // 直接返回，异常由 WebExceptionAdvise 统一处理
        return BaseResult.success(systemConfigFacade.getByKey(key));
    }
}
```

> **后果**：每个 Controller 自行处理异常导致响应格式不统一——有的返回 `Map`，有的返回 `BaseResult`，前端需要为每个接口编写不同的错误解析逻辑。同时，手动 catch 会吞掉日志级别、国际化、指标采集等 `WebExceptionAdvise` 提供的横切能力。

## 检查清单

- [ ] 异常使用三级体系（BizException / ClientException / SysException），不直接抛 RuntimeException
- [ ] 所有错误码实现 `ErrorCode` 接口
- [ ] 新增 `CommonErrorCode` 后同步更新 `messages.properties` 和 `messages_en.properties`
- [ ] 错误码分段符合约定（1000 成功 / 2000 通用 / 5000 系统 / 6000 组件 / 9999 兜底）
- [ ] 使用默认消息构造异常时，消息支持国际化
- [ ] `messages.properties` 和 `messages_en.properties` 的 key 与 `CommonErrorCode` 完全一致
- [ ] 参数校验消息在 `ValidationMessages.properties` 和 `ValidationMessages_en.properties` 中配置

## 相关文档

- [Java 编码规范](./java-conventions.md) — 代码风格
- [AGENTS.md](../../AGENTS.md) — 异常体系结构索引
