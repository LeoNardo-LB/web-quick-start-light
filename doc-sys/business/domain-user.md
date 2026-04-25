# 用户领域

> **职责**: 描述用户领域的 API、实体和服务流程
> **轨道**: Contract
> **维护者**: AI

## 目录

- [概述](#概述)

## 概述

用户领域是应用的认证核心，涵盖从 HTTP 入口到数据访问的完整链路。`LoginController` 提供 `/api/auth` 路径下的登录/登出 REST 端点，通过 `LoginFacade` 接口编排认证流程：`UserRepository` 查询用户 → `BCryptPasswordEncoder` 校验密码 → `AuthComponent` 生成/注销 Token。`User` 是纯 POJO 领域实体，与 `UserDO`（持久化对象）通过 Repository 层分离。

## 公共 API 参考

### LoginController — 认证控制器

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class LoginController {

    private final LoginFacade loginFacade;

    @PostMapping("/login")
    public BaseResult<Map<String, String>> login(@RequestBody LoginRequest request);

    @PostMapping("/logout")
    public BaseResult<Void> logout();
}
```

#### REST 端点

| HTTP 方法 | 路径 | 请求体 | 返回类型 | 说明 |
|:---------:|------|--------|---------|------|
| POST | `/api/auth/login` | `LoginRequest` | `BaseResult<Map<String, String>>` | 登录，返回 `{ "token": "xxx" }` |
| POST | `/api/auth/logout` | 无 | `BaseResult<Void>` | 登出 |

#### LoginRequest

```java
record LoginRequest(
    @NotBlank(message = "用户名不能为空") String username,
    @NotBlank(message = "密码不能为空") String password
) {}
```

**校验规则**：`username` 和 `password` 均不允许为空，违反时抛出 `MethodArgumentNotValidException`，由全局异常处理器返回 `ILLEGAL_ARGUMENT(2001)` 错误码。

#### 登录成功响应示例

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

### LoginFacade — 登录门面接口

```java
package org.smm.archetype.service.auth;

public interface LoginFacade {
    /**
     * 登录
     * @param username 用户名
     * @param password 密码
     * @return token
     */
    String login(String username, String password);

    /**
     * 注销
     */
    void logout();
}
```

### LoginFacadeImpl — 登录门面实现

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginFacadeImpl implements LoginFacade {

    private final UserRepository userRepository;
    private final AuthComponent authComponent;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public String login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BizException(CommonErrorCode.AUTH_USER_NOT_FOUND, "用户不存在"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BizException(CommonErrorCode.AUTH_BAD_CREDENTIALS, "用户名或密码错误");
        }

        log.info("用户登录成功: username={}", username);
        return authComponent.login(user.getId());
    }

    @Override
    public void logout() {
        log.info("用户注销");
        authComponent.logout();
    }
}
```

### UserRepository — 用户仓储接口

```java
package org.smm.archetype.repository.user;

public interface UserRepository {
    Optional<User> findByUsername(String username);
}
```

## 核心类型定义

### User — 用户领域实体

```java
package org.smm.archetype.entity.user;

@Getter
@Setter
public class User {
    private Long id;
    private String username;
    private String passwordHash;
    private String nickname;
    private String status;
    private Instant createTime;
    private Instant updateTime;
}
```

**设计要点**：

- 纯 POJO 领域实体，不含任何 JPA/MyBatis-Plus 注解
- 与 `UserDO`（generated 层的持久化对象）通过 Repository 层的转换方法分离
- `passwordHash` 存储 BCrypt 哈希值，明文密码仅在 `LoginFacadeImpl` 中用于校验

### 认证相关错误码

| 错误码 | 值 | 触发场景 | 异常类型 |
|--------|:---:|---------|---------|
| `AUTH_UNAUTHORIZED` | 6601 | 未登录或登录已过期（Sa-Token 拦截） | `NotLoginException` |
| `AUTH_BAD_CREDENTIALS` | 6602 | 密码校验失败 | `BizException` |
| `AUTH_USER_NOT_FOUND` | 6603 | 用户名不存在 | `BizException` |

## 服务流程

### 登录流程

```
POST /api/auth/login
  Body: { "username": "admin", "password": "123456" }
    │
    ▼
LoginController.login()
    │
    ▼
LoginFacadeImpl.login(username, password)
    │
    ├── 1. UserRepository.findByUsername(username)
    │       ├── 找到用户 → User entity
    │       └── 未找到 → throw BizException(AUTH_USER_NOT_FOUND)
    │                        → HTTP 200 + { code: 6603, message: "用户不存在" }
    │
    ├── 2. BCryptPasswordEncoder.matches(password, user.passwordHash)
    │       ├── 匹配 → 继续
    │       └── 不匹配 → throw BizException(AUTH_BAD_CREDENTIALS)
    │                      → HTTP 200 + { code: 6602, message: "用户名或密码错误" }
    │
    └── 3. AuthComponent.login(user.id)
            │
            └── 返回 token 字符串
                │
                ▼
        BaseResult.success(Map.of("token", token))
        → HTTP 200 + { code: 1000, data: { "token": "xxx" } }
```

### 登出流程

```
POST /api/auth/logout
    │
    ▼
LoginController.logout()
    │
    ▼
LoginFacadeImpl.logout()
    │
    └── AuthComponent.logout()
            │
            ▼
    BaseResult.success(null)
    → HTTP 200 + { code: 1000, data: null }
```

### 数据转换路径

```
MySQL user 表
    │
    ▼
UserDO (generated, extends BaseDO)
    │  MyBatis-Plus 自动映射
    ▼
UserRepositoryImpl.findByUsername()
    │  内联 toEntity() 转换
    ▼
User (Entity, 纯 POJO)
    │
    ▼
LoginFacadeImpl (业务逻辑)
    │
    ▼
LoginController → BaseResult.success(data)
```

## 依赖关系

### 内部依赖

| 依赖组件 | 类型 | 用途 |
|---------|------|------|
| `UserRepository` | 接口 | 用户数据访问抽象 |
| `AuthComponent` | 组件接口 | Token 生成/注销（来自 component-auth） |
| `BCryptPasswordEncoder` | Spring Security | 密码哈希校验 |
| `BizException` + `CommonErrorCode` | 异常体系 | 认证失败时抛出 |
| `BaseResult` | 基础类型 | 统一响应包装 |

### 外部框架依赖

| 框架 | 用途 |
|------|------|
| Spring Boot Web | `@RestController`, `@PostMapping`, `@RequestBody` |
| Spring Security | `BCryptPasswordEncoder` 密码校验 |
| Jakarta Validation | `@NotBlank` 参数校验 |
| Lombok | `@RequiredArgsConstructor`, `@Getter`, `@Setter`, `@Slf4j` |

### 模块间依赖

```
LoginController (controller/auth)
    └── LoginFacade (service/auth 接口)
            └── LoginFacadeImpl (service/auth 实现)
                    ├── UserRepository (repository/user 接口)
                    │       └── UserRepositoryImpl (repository/user 实现)
                    │               ├── UserMapper (generated 层)
                    │               └── UserDO → User (内联转换)
                    ├── AuthComponent (component-auth)
                    └── BCryptPasswordEncoder (Spring Security)
```

## 相关文档

| 文档 | 关系 | 说明 |
|------|------|------|
| 异常体系 | business/exception-system.md | 认证错误码定义 |
| 基础类型 | business/base-types.md | BaseResult 统一响应格式 |
| REST API 参考 | business/api-reference.md | 完整端点列表 |
| 认证组件 | components/component-auth.md | AuthComponent 接口与配置 |
| 操作日志领域 | business/domain-operation-log.md | 登录操作可被 @BusinessLog 记录 |

## 变更历史

| 日期 | 版本 | 变更内容 |
|------|------|---------|
| 2026-04-25 | v1.0 | 初始版本：用户领域文档生成 |
