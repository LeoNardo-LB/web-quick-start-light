# Phase 3: 全模块迁移 + Spring Modulith Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 Phase 2 的模块化试点模式推广到 auth、operationlog、shared 模块，引入 Spring Modulith 边界验证。

**Architecture:** 每个模块采用 root-package Facade + internal/ 封装结构。Spring Modulith 通过 @ApplicationModule 注解声明边界，ApplicationModules.verify() 自动验证无越界访问。过渡期使用"新文件不加注解"策略避免 Bean 冲突。

**Tech Stack:** Spring Boot 4.x, Spring Modulith 2.0.6+, MyBatis-Plus 3.5.x, Java 25

---

## File Structure

```
新建（Task 1，无 Spring 注解）:
  app/src/main/java/org/smm/archetype/auth/
    AuthFacade.java                                      ← 公开 API 接口（重命名自 LoginFacade）
    internal/
      User.java                                          ← 领域实体
      UserRepository.java                                ← 仓储接口
      UserRepositoryImpl.java                            ← 仓储实现（委托 UserConverter）
      UserConverter.java                                 ← DO→Entity 转换器（提取自 UserRepositoryImpl）
      AuthFacadeImpl.java                                ← Facade 实现（重命名自 LoginFacadeImpl）
      LoginController.java                               ← Controller
      LoginRequest.java                                  ← 请求 record（提取自 Controller 内联）
      LoginVO.java                                       ← 响应 record（新增）
      UserDO.java                                        ← (引用旧 generated 包)
      UserMapper.java                                    ← (引用旧 generated 包)

新建（Task 2，无 Spring 注解）:
  app/src/main/java/org/smm/archetype/operationlog/
    OperationLogFacade.java                              ← 公开 API 接口
    internal/
      OperationLog.java                                  ← 领域实体
      OperationLogPageQuery.java                         ← 分页查询 record
      OperationLogRepository.java                        ← 仓储接口（IPage → PageResult）
      OperationLogRepositoryImpl.java                    ← 仓储实现（IPage → PageResult）
      OperationLogConverter.java                         ← DO→Entity 转换器（toEntity → toModel）
      OperationLogService.java                           ← 业务服务（IPage → PageResult）
      OperationLogFacadeImpl.java                        ← Facade 实现（IPage → PageResult）
      OperationLogController.java                        ← Controller
      OperationLogVO.java                                ← VO record
      OperationLogDO.java                                ← (引用旧 generated 包)
      OperationLogMapper.java                            ← (引用旧 generated 包)

新建（Task 3，无 Spring 注解）:
  app/src/main/java/org/smm/archetype/shared/internal/aspect/
    ratelimit/
      RateLimit.java                                     ← 限流注解（包迁移）
      LimitFallback.java                                 ← 降级枚举（包迁移）
      BucketFactory.java                                 ← Bucket 工厂（包迁移）
      SpelKeyResolver.java                               ← SpEL 解析器（包迁移）
      RateLimitAspect.java                               ← 限流切面（包迁移）
    idempotent/
      Idempotent.java                                    ← 幂等注解（包迁移）
      IdempotentKeyResolver.java                         ← Key 解析器（包迁移）
      IdempotentAspect.java                              ← 幂等切面（包迁移）
    operationlog/
      LogAspect.java                                     ← 日志切面（包迁移）

新建（Task 4）:
  app/src/main/java/org/smm/archetype/
    systemconfig/package-info.java                       ← @ApplicationModule
    auth/package-info.java                               ← @ApplicationModule
    operationlog/package-info.java                       ← @ApplicationModule
    shared/package-info.java                             ← @ApplicationModule(type = OPEN)
  app/src/test/java/org/smm/archetype/support/basic/
    ModulithComplianceUTest.java                         ← ApplicationModules.verify()

新建（Task 5，测试文件）:
  app/src/test/java/org/smm/archetype/auth/internal/
    AuthFacadeITest.java                                 ← 重命名自 LoginFacadeITest
  app/src/test/java/org/smm/archetype/operationlog/internal/
    OperationLogControllerITest.java
    OperationLogPaginationBoundaryITest.java
    OperationLogFacadeITest.java
    OperationLogRepositoryITest.java                     ← IPage → PageResult 断言调整

删除（Task 6）:
  app/src/main/java/org/smm/archetype/controller/auth/LoginController.java
  app/src/main/java/org/smm/archetype/service/auth/LoginFacade.java
  app/src/main/java/org/smm/archetype/service/auth/LoginFacadeImpl.java
  app/src/main/java/org/smm/archetype/entity/user/User.java
  app/src/main/java/org/smm/archetype/repository/user/UserRepository.java
  app/src/main/java/org/smm/archetype/repository/user/UserRepositoryImpl.java
  app/src/main/java/org/smm/archetype/controller/operationlog/OperationLogController.java
  app/src/main/java/org/smm/archetype/facade/operationlog/OperationLogFacade.java
  app/src/main/java/org/smm/archetype/facade/operationlog/OperationLogFacadeImpl.java
  app/src/main/java/org/smm/archetype/facade/operationlog/OperationLogVO.java
  app/src/main/java/org/smm/archetype/service/operationlog/OperationLogService.java
  app/src/main/java/org/smm/archetype/entity/operationlog/OperationLog.java
  app/src/main/java/org/smm/archetype/entity/operationlog/OperationLogPageQuery.java
  app/src/main/java/org/smm/archetype/repository/operationlog/OperationLogRepository.java
  app/src/main/java/org/smm/archetype/repository/operationlog/OperationLogRepositoryImpl.java
  app/src/main/java/org/smm/archetype/repository/operationlog/OperationLogConverter.java
  app/src/main/java/org/smm/archetype/shared/aspect/ratelimit/ (5 files)
  app/src/main/java/org/smm/archetype/shared/aspect/idempotent/ (3 files)
  app/src/main/java/org/smm/archetype/shared/aspect/operationlog/LogAspect.java
  app/src/test/java/org/smm/archetype/service/auth/LoginFacadeITest.java
  app/src/test/java/org/smm/archetype/controller/operationlog/ (2 files)
  app/src/test/java/org/smm/archetype/facade/operationlog/OperationLogFacadeITest.java
  app/src/test/java/org/smm/archetype/repository/operationlog/OperationLogRepositoryITest.java

删除（Task 7，废弃文件清理）:
  app/src/main/java/org/smm/archetype/entity/base/BaseRequest.java
  app/src/main/java/org/smm/archetype/entity/base/BasePageRequest.java
  app/src/main/java/org/smm/archetype/shared/aspect/operationlog/BusinessLog.java (旧存根)
  app/src/main/java/org/smm/archetype/shared/aspect/operationlog/OperationLogRecord.java (旧存根)
  app/src/main/java/org/smm/archetype/shared/aspect/operationlog/OperationLogWriter.java (旧存根)
  app/src/main/java/org/smm/archetype/shared/aspect/operationlog/OperationType.java (旧存根)
  app/src/main/java/org/smm/archetype/generated/entity/UserDO.java
  app/src/main/java/org/smm/archetype/generated/entity/OperationLogDO.java
  app/src/main/java/org/smm/archetype/generated/mapper/UserMapper.java
  app/src/main/java/org/smm/archetype/generated/mapper/OperationLogMapper.java

修改（Task 4）:
  app/pom.xml                                            ← 添加 Spring Modulith 依赖

修改（Task 6）:
  app/src/main/java/org/smm/archetype/auth/internal/*.java      ← 添加 Spring 注解
  app/src/main/java/org/smm/archetype/operationlog/internal/*.java ← 添加 Spring 注解
  app/src/main/java/org/smm/archetype/shared/internal/aspect/*.java ← 添加 Spring 注解

修改（Task 7）:
  app/src/main/java/org/smm/archetype/generated/MybatisPlusGenerator.java ← 多模块输出改造
```

---

## Task 1: auth 模块迁移（源码搬迁，不加 Spring 注解）

**Files:**
- Create: `app/src/main/java/org/smm/archetype/auth/AuthFacade.java`
- Create: `app/src/main/java/org/smm/archetype/auth/internal/User.java`
- Create: `app/src/main/java/org/smm/archetype/auth/internal/UserRepository.java`
- Create: `app/src/main/java/org/smm/archetype/auth/internal/UserRepositoryImpl.java`
- Create: `app/src/main/java/org/smm/archetype/auth/internal/UserConverter.java`
- Create: `app/src/main/java/org/smm/archetype/auth/internal/AuthFacadeImpl.java`
- Create: `app/src/main/java/org/smm/archetype/auth/internal/LoginController.java`
- Create: `app/src/main/java/org/smm/archetype/auth/internal/LoginRequest.java`
- Create: `app/src/main/java/org/smm/archetype/auth/internal/LoginVO.java`
- Create: `app/src/main/java/org/smm/archetype/auth/internal/UserDO.java`
- Create: `app/src/main/java/org/smm/archetype/auth/internal/UserMapper.java`

- [ ] **Step 1.1: 创建目录结构**

```bash
mkdir -p app/src/main/java/org/smm/archetype/auth/internal
```

- [ ] **Step 1.2: 创建 AuthFacade.java（重命名自 LoginFacade）**

```java
package org.smm.archetype.auth;

/**
 * 认证门面接口（模块公开 API）。
 */
public interface AuthFacade {

    /**
     * 登录
     *
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

- [ ] **Step 1.3: 创建 User.java（领域实体）**

```java
package org.smm.archetype.auth.internal;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * 用户实体。
 */
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

- [ ] **Step 1.4: 创建 UserRepository.java（仓储接口）**

```java
package org.smm.archetype.auth.internal;

import java.util.Optional;

/**
 * 用户仓储接口。
 */
interface UserRepository {

    /**
     * 根据用户名查找用户
     *
     * @param username 用户名
     * @return 用户实体，不存在返回 Optional.empty()
     */
    Optional<User> findByUsername(String username);
}
```

- [ ] **Step 1.5: 创建 UserConverter.java（DO→Entity 转换器，提取自 UserRepositoryImpl）**

```java
package org.smm.archetype.auth.internal;

/**
 * 用户 DO → Entity 转换器。
 * 提取自 UserRepositoryImpl 的内联 toEntity() 方法。
 */
class UserConverter {

    User toModel(UserDO userDO) {
        if (userDO == null) {
            return null;
        }
        User user = new User();
        user.setId(userDO.getId());
        user.setUsername(userDO.getUsername());
        user.setPasswordHash(userDO.getPasswordHash());
        user.setNickname(userDO.getNickname());
        user.setStatus(userDO.getStatus());
        user.setCreateTime(userDO.getCreateTime());
        user.setUpdateTime(userDO.getUpdateTime());
        return user;
    }

    UserDO toDataObject(User user) {
        if (user == null) {
            return null;
        }
        return UserDO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .passwordHash(user.getPasswordHash())
                .nickname(user.getNickname())
                .status(user.getStatus())
                .build();
    }
}
```

- [ ] **Step 1.6: 创建 UserDO.java（引用旧 generated 包）**

```java
package org.smm.archetype.auth.internal;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.smm.archetype.entity.base.BaseDO;

/**
 * 用户数据对象。
 * 过渡期：此类在 auth/internal/ 下创建，但 @TableName 和 BaseDO 继承保持不变。
 * Task 6 删除旧 generated/entity/UserDO.java 后，此类成为唯一来源。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("user")
class UserDO extends BaseDO {
    private String username;
    private String passwordHash;
    private String nickname;
    private String status;
}
```

- [ ] **Step 1.7: 创建 UserMapper.java（引用旧 generated 包）**

```java
package org.smm.archetype.auth.internal;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 用户 Mapper。
 * 过渡期：此类在 auth/internal/ 下创建，不添加 @Mapper 注解。
 * Task 6 删除旧 generated/mapper/UserMapper.java 后，添加 @Mapper 注解。
 */
interface UserMapper extends BaseMapper<UserDO> {
}
```

- [ ] **Step 1.8: 创建 UserRepositoryImpl.java（委托 UserConverter）**

```java
package org.smm.archetype.auth.internal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.util.Optional;

/**
 * 用户仓储实现。
 * 过渡期：不添加 @Repository 注解，不添加 @RequiredArgsConstructor。
 */
class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;
    private final UserConverter converter;

    UserRepositoryImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
        this.converter = new UserConverter();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        UserDO userDO = userMapper.selectOne(
                new LambdaQueryWrapper<UserDO>().eq(UserDO::getUsername, username)
        );
        return Optional.ofNullable(userDO).map(converter::toModel);
    }
}
```

- [ ] **Step 1.9: 创建 LoginRequest.java（提取自 Controller 内联 record）**

```java
package org.smm.archetype.auth.internal;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求值对象。
 * 提取自 LoginController 的内联 record。
 */
record LoginRequest(
        @NotBlank(message = "用户名不能为空") String username,
        @NotBlank(message = "密码不能为空") String password
) {}
```

- [ ] **Step 1.10: 创建 LoginVO.java（新增响应 record）**

```java
package org.smm.archetype.auth.internal;

/**
 * 登录响应值对象。
 */
record LoginVO(
        String token
) {}
```

- [ ] **Step 1.11: 创建 AuthFacadeImpl.java（重命名自 LoginFacadeImpl）**

```java
package org.smm.archetype.auth.internal;

import org.smm.archetype.auth.AuthFacade;
import org.smm.archetype.component.auth.AuthComponent;
import org.smm.archetype.exception.BizException;
import org.smm.archetype.exception.CommonErrorCode;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 认证门面实现。
 * 过渡期：不添加 @Service、@Slf4j、@RequiredArgsConstructor 注解。
 */
class AuthFacadeImpl implements AuthFacade {

    private final UserRepository userRepository;
    private final AuthComponent authComponent;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    AuthFacadeImpl(UserRepository userRepository, AuthComponent authComponent) {
        this.userRepository = userRepository;
        this.authComponent = authComponent;
    }

    @Override
    public String login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BizException(CommonErrorCode.AUTH_USER_NOT_FOUND, "用户不存在"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BizException(CommonErrorCode.AUTH_BAD_CREDENTIALS, "用户名或密码错误");
        }

        return authComponent.login(user.getId());
    }

    @Override
    public void logout() {
        authComponent.logout();
    }
}
```

- [ ] **Step 1.12: 创建 LoginController.java**

```java
package org.smm.archetype.auth.internal;

import org.smm.archetype.auth.AuthFacade;
import org.smm.archetype.shared.result.BaseResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

/**
 * 登录控制器。
 * 过渡期：不添加 @RestController、@RequiredArgsConstructor 注解。
 */
@RequestMapping("/api/auth")
public class LoginController {

    private final AuthFacade authFacade;

    LoginController(AuthFacade authFacade) {
        this.authFacade = authFacade;
    }

    @PostMapping("/login")
    public BaseResult<Map<String, String>> login(@RequestBody LoginRequest request) {
        String token = authFacade.login(request.username(), request.password());
        return BaseResult.success(Map.of("token", token));
    }

    @PostMapping("/logout")
    public BaseResult<Void> logout() {
        authFacade.logout();
        return BaseResult.success(null);
    }
}
```

- [ ] **Step 1.13: 验证编译**

Run: `mvn compile -pl app`
Expected: BUILD SUCCESS（旧文件仍有注解，新文件无注解，无冲突）

---

## Task 2: operationlog 模块迁移（源码搬迁，IPage→PageResult，不加 Spring 注解）

**Files:**
- Create: `app/src/main/java/org/smm/archetype/operationlog/OperationLogFacade.java`
- Create: `app/src/main/java/org/smm/archetype/operationlog/internal/OperationLog.java`
- Create: `app/src/main/java/org/smm/archetype/operationlog/internal/OperationLogPageQuery.java`
- Create: `app/src/main/java/org/smm/archetype/operationlog/internal/OperationLogRepository.java`
- Create: `app/src/main/java/org/smm/archetype/operationlog/internal/OperationLogConverter.java`
- Create: `app/src/main/java/org/smm/archetype/operationlog/internal/OperationLogRepositoryImpl.java`
- Create: `app/src/main/java/org/smm/archetype/operationlog/internal/OperationLogService.java`
- Create: `app/src/main/java/org/smm/archetype/operationlog/internal/OperationLogFacadeImpl.java`
- Create: `app/src/main/java/org/smm/archetype/operationlog/internal/OperationLogController.java`
- Create: `app/src/main/java/org/smm/archetype/operationlog/internal/OperationLogVO.java`
- Create: `app/src/main/java/org/smm/archetype/operationlog/internal/OperationLogDO.java`
- Create: `app/src/main/java/org/smm/archetype/operationlog/internal/OperationLogMapper.java`

- [ ] **Step 2.1: 创建目录结构**

```bash
mkdir -p app/src/main/java/org/smm/archetype/operationlog/internal
```

- [ ] **Step 2.2: 创建 OperationLogFacade.java（公开 API 接口）**

```java
package org.smm.archetype.operationlog;

import org.smm.archetype.operationlog.internal.OperationLogPageQuery;
import org.smm.archetype.operationlog.internal.OperationLogVO;
import org.smm.archetype.shared.result.BasePageResult;

/**
 * 操作日志 Facade 接口（模块公开 API）。
 */
public interface OperationLogFacade {

    /**
     * 分页查询操作日志
     *
     * @param query 分页查询参数
     * @return 分页结果（OperationLogVO）
     */
    BasePageResult<OperationLogVO> findByPage(OperationLogPageQuery query);
}
```

- [ ] **Step 2.3: 创建 OperationLog.java（领域实体）**

```java
package org.smm.archetype.operationlog.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * 操作日志领域实体。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
class OperationLog {

    private Long id;
    private String traceId;
    private String userId;
    private String module;
    private String operationType;
    private String description;
    private String method;
    private String params;
    private String result;
    private Long executionTime;
    private String ip;
    private String status;
    private String errorMessage;
    private Instant createTime;
}
```

- [ ] **Step 2.4: 创建 OperationLogPageQuery.java（适配 PageQuery）**

```java
package org.smm.archetype.operationlog.internal;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.smm.archetype.shared.pagination.PageQuery;

/**
 * 操作日志分页查询参数。
 */
public record OperationLogPageQuery(
        @Min(1) int pageNo,
        @Min(1) @Max(100) int pageSize,
        String module,
        String operationType,
        String startTime,
        String endTime
) {
    public OperationLogPageQuery {
        // 使用 PageQuery 做基础分页校验
        PageQuery base = new PageQuery(pageNo, pageSize);
    }

    /**
     * 兼容无参绑定场景（Spring MVC @ModelAttribute）。
     */
    public OperationLogPageQuery() {
        this(1, 10, null, null, null, null);
    }
}
```

- [ ] **Step 2.5: 创建 OperationLogDO.java（引用旧 generated 包）**

```java
package org.smm.archetype.operationlog.internal;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.smm.archetype.entity.base.BaseDO;

/**
 * 操作日志数据对象。
 * 过渡期：此类在 operationlog/internal/ 下创建。
 * Task 6 删除旧 generated/entity/OperationLogDO.java 后，此类成为唯一来源。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("operation_log")
class OperationLogDO extends BaseDO {
    private String traceId;
    private String userId;
    private String module;
    private String operationType;
    private String description;
    private String method;
    private String params;
    private String result;
    private Long executionTime;
    private String ip;
    private String status;
    private String errorMessage;
}
```

- [ ] **Step 2.6: 创建 OperationLogMapper.java**

```java
package org.smm.archetype.operationlog.internal;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 操作日志 Mapper。
 * 过渡期：不添加 @Mapper 注解。
 * Task 6 删除旧 generated/mapper/OperationLogMapper.java 后，添加 @Mapper 注解。
 */
interface OperationLogMapper extends BaseMapper<OperationLogDO> {
}
```

- [ ] **Step 2.7: 创建 OperationLogConverter.java（toEntity → toModel）**

```java
package org.smm.archetype.operationlog.internal;

/**
 * 操作日志 DO → Entity 转换器。
 * 过渡期：不添加 @Component 注解。
 */
class OperationLogConverter {

    OperationLog toModel(OperationLogDO logDO) {
        if (logDO == null) {
            return null;
        }
        return OperationLog.builder()
                .id(logDO.getId())
                .traceId(logDO.getTraceId())
                .userId(logDO.getUserId())
                .module(logDO.getModule())
                .operationType(logDO.getOperationType())
                .description(logDO.getDescription())
                .method(logDO.getMethod())
                .params(logDO.getParams())
                .result(logDO.getResult())
                .executionTime(logDO.getExecutionTime())
                .ip(logDO.getIp())
                .status(logDO.getStatus())
                .errorMessage(logDO.getErrorMessage())
                .createTime(logDO.getCreateTime())
                .build();
    }
}
```

- [ ] **Step 2.8: 创建 OperationLogRepository.java（IPage → PageResult）**

```java
package org.smm.archetype.operationlog.internal;

import org.smm.archetype.shared.pagination.PageResult;

/**
 * 操作日志仓储接口。
 */
interface OperationLogRepository {

    /**
     * 分页查询操作日志
     *
     * @param query 分页查询参数
     * @return 分页结果
     */
    PageResult<OperationLog> findByPage(OperationLogPageQuery query);
}
```

- [ ] **Step 2.9: 创建 OperationLogRepositoryImpl.java（IPage → PageResult）**

```java
package org.smm.archetype.operationlog.internal;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.smm.archetype.shared.pagination.PageResult;

import java.time.Instant;
import java.util.List;

/**
 * 操作日志仓储实现。
 * 过渡期：不添加 @Repository、@Slf4j、@RequiredArgsConstructor 注解。
 */
class OperationLogRepositoryImpl implements OperationLogRepository {

    private final OperationLogMapper operationLogMapper;
    private final OperationLogConverter converter;

    OperationLogRepositoryImpl(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
        this.converter = new OperationLogConverter();
    }

    @Override
    public PageResult<OperationLog> findByPage(OperationLogPageQuery query) {
        Page<OperationLogDO> page = new Page<>(query.pageNo(), query.pageSize());

        LambdaQueryWrapper<OperationLogDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(query.module()), OperationLogDO::getModule, query.module())
                .eq(StrUtil.isNotBlank(query.operationType()), OperationLogDO::getOperationType, query.operationType())
                .ge(StrUtil.isNotBlank(query.startTime()), OperationLogDO::getCreateTime,
                        parseInstant(query.startTime()))
                .le(StrUtil.isNotBlank(query.endTime()), OperationLogDO::getCreateTime,
                        parseInstant(query.endTime()))
                .orderByDesc(OperationLogDO::getId);

        var doPage = operationLogMapper.selectPage(page, wrapper);

        List<OperationLog> entities = doPage.getRecords().stream()
                .map(converter::toModel)
                .toList();

        return PageResult.of(entities, doPage.getTotal(), (int) doPage.getCurrent(), (int) doPage.getSize());
    }

    private Instant parseInstant(String dateTime) {
        if (dateTime == null || dateTime.isBlank()) {
            return null;
        }
        return Instant.parse(dateTime);
    }
}
```

- [ ] **Step 2.10: 创建 OperationLogService.java（IPage → PageResult）**

```java
package org.smm.archetype.operationlog.internal;

import org.smm.archetype.shared.pagination.PageResult;

/**
 * 操作日志服务。
 * 过渡期：不添加 @Service、@RequiredArgsConstructor 注解。
 */
class OperationLogService {

    private final OperationLogRepository operationLogRepository;

    OperationLogService(OperationLogRepository operationLogRepository) {
        this.operationLogRepository = operationLogRepository;
    }

    public PageResult<OperationLog> findByPage(OperationLogPageQuery query) {
        return operationLogRepository.findByPage(query);
    }
}
```

- [ ] **Step 2.11: 创建 OperationLogVO.java**

```java
package org.smm.archetype.operationlog.internal;

import java.time.Instant;

/**
 * 操作日志 VO — 用于前端展示
 *
 * @param id            日志 ID
 * @param traceId       追踪 ID
 * @param userId        操作用户
 * @param module        模块
 * @param operationType 操作类型
 * @param description   描述
 * @param method        方法名
 * @param params        请求参数
 * @param result        返回结果
 * @param executionTime 执行时间（ms）
 * @param ip            IP 地址
 * @param status        状态
 * @param errorMessage  错误信息
 * @param createTime    创建时间
 */
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

- [ ] **Step 2.12: 创建 OperationLogFacadeImpl.java（消除 IPage 泄漏）**

```java
package org.smm.archetype.operationlog.internal;

import org.smm.archetype.operationlog.OperationLogFacade;
import org.smm.archetype.shared.pagination.PageResult;
import org.smm.archetype.shared.result.BasePageResult;

import java.util.List;

/**
 * 操作日志 Facade 实现。
 * 过渡期：不添加 @Service、@RequiredArgsConstructor 注解。
 */
class OperationLogFacadeImpl implements OperationLogFacade {

    private final OperationLogService operationLogService;

    OperationLogFacadeImpl(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @Override
    public BasePageResult<OperationLogVO> findByPage(OperationLogPageQuery query) {
        PageResult<OperationLog> pageResult = operationLogService.findByPage(query);

        List<OperationLogVO> voList = pageResult.list().stream()
                .map(this::toVO)
                .toList();

        return BasePageResult.from(new PageResult<>(
                voList, pageResult.total(), pageResult.pageNo(), pageResult.pageSize(), pageResult.totalPages()
        ));
    }

    private OperationLogVO toVO(OperationLog log) {
        return new OperationLogVO(
                log.getId(),
                log.getTraceId(),
                log.getUserId(),
                log.getModule(),
                log.getOperationType(),
                log.getDescription(),
                log.getMethod(),
                log.getParams(),
                log.getResult(),
                log.getExecutionTime(),
                log.getIp(),
                log.getStatus(),
                log.getErrorMessage(),
                log.getCreateTime()
        );
    }
}
```

- [ ] **Step 2.13: 创建 OperationLogController.java**

```java
package org.smm.archetype.operationlog.internal;

import org.smm.archetype.operationlog.OperationLogFacade;
import org.smm.archetype.shared.result.BasePageResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.validation.Valid;

/**
 * 操作日志控制器。
 * 过渡期：不添加 @RestController、@RequiredArgsConstructor、@Slf4j、@Tag 注解。
 */
@RequestMapping("/api/system/operation-logs")
@Validated
public class OperationLogController {

    private final OperationLogFacade operationLogFacade;

    OperationLogController(OperationLogFacade operationLogFacade) {
        this.operationLogFacade = operationLogFacade;
    }

    @GetMapping
    public BasePageResult<OperationLogVO> findByPage(
            @Valid @ModelAttribute OperationLogPageQuery query) {
        return operationLogFacade.findByPage(query);
    }
}
```

- [ ] **Step 2.14: 验证编译**

Run: `mvn compile -pl app`
Expected: BUILD SUCCESS

---

## Task 3: shared 模块 AOP 切面迁移（源码搬迁，不加 Spring 注解）

**Files:**
- Create: `app/src/main/java/org/smm/archetype/shared/internal/aspect/ratelimit/RateLimit.java`
- Create: `app/src/main/java/org/smm/archetype/shared/internal/aspect/ratelimit/LimitFallback.java`
- Create: `app/src/main/java/org/smm/archetype/shared/internal/aspect/ratelimit/BucketFactory.java`
- Create: `app/src/main/java/org/smm/archetype/shared/internal/aspect/ratelimit/SpelKeyResolver.java`
- Create: `app/src/main/java/org/smm/archetype/shared/internal/aspect/ratelimit/RateLimitAspect.java`
- Create: `app/src/main/java/org/smm/archetype/shared/internal/aspect/idempotent/Idempotent.java`
- Create: `app/src/main/java/org/smm/archetype/shared/internal/aspect/idempotent/IdempotentKeyResolver.java`
- Create: `app/src/main/java/org/smm/archetype/shared/internal/aspect/idempotent/IdempotentAspect.java`
- Create: `app/src/main/java/org/smm/archetype/shared/internal/aspect/operationlog/LogAspect.java`

- [ ] **Step 3.1: 创建目录结构**

```bash
mkdir -p app/src/main/java/org/smm/archetype/shared/internal/aspect/ratelimit
mkdir -p app/src/main/java/org/smm/archetype/shared/internal/aspect/idempotent
mkdir -p app/src/main/java/org/smm/archetype/shared/internal/aspect/operationlog
```

- [ ] **Step 3.2: 创建 RateLimit.java**

```java
package org.smm.archetype.shared.internal.aspect.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 方法级限流注解。
 * <p>
 * 基于 Bucket4j 令牌桶算法，支持 SpEL 表达式提取限流 Key，
 * 允许不同维度（如用户ID、IP等）独立限流。
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    double capacity() default 10;

    double refillTokens() default 10;

    long refillDuration() default 1;

    TimeUnit refillUnit() default TimeUnit.SECONDS;

    String key() default "";

    LimitFallback fallback() default LimitFallback.REJECT;

    String fallbackMethod() default "";
}
```

- [ ] **Step 3.3: 创建 LimitFallback.java**

```java
package org.smm.archetype.shared.internal.aspect.ratelimit;

/**
 * 限流降级策略枚举。
 */
public enum LimitFallback {
    REJECT,
    WAIT,
    FALLBACK
}
```

- [ ] **Step 3.4: 创建 BucketFactory.java**

```java
package org.smm.archetype.shared.internal.aspect.ratelimit;

import io.github.bucket4j.Bucket;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Bucket4j Bucket 工厂。
 */
public final class BucketFactory {

    private BucketFactory() {
    }

    public static Bucket createBucket(double capacity, double refillTokens,
                                      long refillDuration, TimeUnit refillUnit) {
        Duration refillDurationDuration = Duration.of(refillDuration,
                refillUnit.toChronoUnit());

        return Bucket.builder()
                       .addLimit(limit -> limit
                                                  .capacity(Math.max(1, (long) capacity))
                                                  .refillGreedy(Math.max(1, (long) refillTokens), refillDurationDuration))
                       .build();
    }
}
```

- [ ] **Step 3.5: 创建 SpelKeyResolver.java**

```java
package org.smm.archetype.shared.internal.aspect.ratelimit;

import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;

/**
 * SpEL 表达式解析器。
 */
public final class SpelKeyResolver {

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    private SpelKeyResolver() {
    }

    public static String resolve(Method method, Object[] args, String keyExpression) {
        if (keyExpression == null || keyExpression.isBlank()) {
            return "";
        }

        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            bindParameters(context, method, args);

            Expression expression = PARSER.parseExpression(keyExpression);
            Object value = expression.getValue(context);
            return value != null ? value.toString() : "null";
        } catch (Exception e) {
            return keyExpression;
        }
    }

    private static void bindParameters(StandardEvaluationContext context, Method method, Object[] args) {
        if (args == null) {
            return;
        }

        java.lang.reflect.Parameter[] parameters = method.getParameters();
        for (int i = 0; i < Math.min(parameters.length, args.length); i++) {
            String name = parameters[i].isNamePresent()
                                  ? parameters[i].getName()
                                  : "arg" + i;
            context.setVariable(name, args[i]);
        }
    }
}
```

- [ ] **Step 3.6: 创建 RateLimitAspect.java**

```java
package org.smm.archetype.shared.internal.aspect.ratelimit;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.smm.archetype.exception.BizException;
import org.smm.archetype.exception.CommonErrorCode;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * {@link RateLimit} 注解的 AOP 切面。
 * 过渡期：不添加 @Aspect、@Slf4j 注解。
 */
public class RateLimitAspect {

    private final Map<String, Bucket> bucketCache = new ConcurrentHashMap<>();

    public static String buildBucketKey(Method method, Object[] args, String keyExpression) {
        String baseKey = method.getDeclaringClass().getSimpleName() + "." + method.getName();

        if (keyExpression == null || keyExpression.isBlank()) {
            return baseKey;
        }

        String resolvedKey = SpelKeyResolver.resolve(method, args, keyExpression);
        return baseKey + ":" + resolvedKey;
    }

    public void putBucket(String key, Bucket bucket) {
        bucketCache.put(key, bucket);
    }

    @Pointcut("@annotation(org.smm.archetype.shared.internal.aspect.ratelimit.RateLimit)")
    public void rateLimitCut() {}

    @Around("rateLimitCut()")
    public Object doRateLimit(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

        if (rateLimit == null) {
            return joinPoint.proceed();
        }

        String keyExpression = rateLimit.key();
        String bucketKey = buildBucketKey(method, joinPoint.getArgs(), keyExpression);

        Bucket bucket = bucketCache.computeIfAbsent(bucketKey, k ->
                                                                       BucketFactory.createBucket(rateLimit.capacity(),
                                                                               rateLimit.refillTokens(),
                                                                               rateLimit.refillDuration(), rateLimit.refillUnit()));

        if (bucket.tryConsume(1)) {
            return joinPoint.proceed();
        }

        return handleFallback(joinPoint, rateLimit, bucket);
    }

    private Object handleFallback(ProceedingJoinPoint joinPoint, RateLimit rateLimit, Bucket bucket) throws Throwable {
        switch (rateLimit.fallback()) {
            case REJECT -> {
                throw new BizException(CommonErrorCode.RATE_LIMIT_EXCEEDED);
            }
            case WAIT -> {
                ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
                if (probe.isConsumed()) {
                    return joinPoint.proceed();
                }
                long nanosToWait = probe.getNanosToWaitForRefill();
                try {
                    TimeUnit.NANOSECONDS.sleep(nanosToWait);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new BizException(CommonErrorCode.RATE_LIMIT_EXCEEDED);
                }
                if (bucket.tryConsume(1)) {
                    return joinPoint.proceed();
                }
                throw new BizException(CommonErrorCode.RATE_LIMIT_EXCEEDED);
            }
            case FALLBACK -> {
                String fallbackMethodName = rateLimit.fallbackMethod();
                if (fallbackMethodName == null || fallbackMethodName.isBlank()) {
                    throw new BizException(CommonErrorCode.RATE_LIMIT_EXCEEDED);
                }
                Object target = joinPoint.getTarget();
                try {
                    Method fallbackMethod = target.getClass().getMethod(fallbackMethodName);
                    return fallbackMethod.invoke(target);
                } catch (NoSuchMethodException e) {
                    throw new BizException(CommonErrorCode.RATE_LIMIT_EXCEEDED);
                }
            }
            default -> throw new BizException(CommonErrorCode.RATE_LIMIT_EXCEEDED);
        }
    }
}
```

- [ ] **Step 3.7: 创建 Idempotent.java**

```java
package org.smm.archetype.shared.internal.aspect.idempotent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 幂等防护注解。
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    long timeout() default 3000;

    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

    String field() default "";

    String message() default "请勿重复操作";
}
```

- [ ] **Step 3.8: 创建 IdempotentKeyResolver.java**

```java
package org.smm.archetype.shared.internal.aspect.idempotent;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Arrays;
import java.util.Objects;

/**
 * 幂等 Key 解析器。
 */
public class IdempotentKeyResolver {

    private final SpelExpressionParser parser;

    public IdempotentKeyResolver() {
        this.parser = new SpelExpressionParser();
    }

    public String resolve(String className, String methodName, String[] paramNames, Object[] args, Idempotent idempotent) {
        String field = idempotent.field();
        String fieldValue;

        if (field != null && !field.isEmpty()) {
            fieldValue = resolveSpelField(field, paramNames, args);
        } else {
            fieldValue = String.valueOf(Arrays.hashCode(args));
        }

        return className + "." + methodName + "(" + fieldValue + ")";
    }

    String resolveSpelField(String expression, String[] paramNames, Object[] args) {
        EvaluationContext context = new StandardEvaluationContext();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
                context.setVariable("p" + i, args[i]);
                context.setVariable("a" + i, args[i]);
            }
        }

        Expression expr = parser.parseExpression(expression);
        Object value = expr.getValue(context);
        return Objects.toString(value, "");
    }
}
```

- [ ] **Step 3.9: 创建 IdempotentAspect.java**

```java
package org.smm.archetype.shared.internal.aspect.idempotent;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.reflect.MethodSignature;
import org.smm.archetype.component.cache.CacheComponent;
import org.smm.archetype.exception.BizException;
import org.smm.archetype.exception.CommonErrorCode;

import java.time.Duration;

/**
 * 幂等防护 AOP 切面。
 * 过渡期：不添加 @Slf4j、@Aspect 注解。
 */
public class IdempotentAspect {

    private final CacheComponent cacheComponent;
    private final IdempotentKeyResolver keyResolver;

    public IdempotentAspect(CacheComponent cacheComponent) {
        this.cacheComponent = cacheComponent;
        this.keyResolver = new IdempotentKeyResolver();
    }

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        String key = resolveKey(joinPoint, idempotent);
        long timeoutMillis = idempotent.timeUnit().toMillis(idempotent.timeout());

        if (Boolean.TRUE.equals(cacheComponent.hasKey(key))) {
            throw new BizException(CommonErrorCode.ILLEGAL_ARGUMENT, idempotent.message());
        }

        cacheComponent.put(key, "1", Duration.ofMillis(timeoutMillis));

        try {
            return joinPoint.proceed();
        } catch (Throwable ex) {
            cacheComponent.delete(key);
            throw ex;
        }
    }

    String resolveKey(ProceedingJoinPoint joinPoint, Idempotent idempotent) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return keyResolver.resolve(
                signature.getDeclaringTypeName(),
                signature.getName(),
                signature.getParameterNames(),
                joinPoint.getArgs(),
                idempotent
        );
    }
}
```

- [ ] **Step 3.10: 创建 LogAspect.java**

```java
package org.smm.archetype.shared.internal.aspect.operationlog;

import com.alibaba.fastjson2.JSON;
import io.opentelemetry.api.trace.Span;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @BusinessLog 注解的 AOP 切面。
 * <p>
 * 注意：此切面在过渡期不添加 @Aspect 注解。
 * Pointcut 引用的是 common/operationlog/BusinessLog（已迁移的注解）。
 */
public class LogAspect {

    private static final int                   MAX_LENGTH       = 2048;
    private static final String                TRUNCATED_SUFFIX = "...(truncated)";
    private static final Map<Class<?>, Logger> LOGGER_MAP       = new ConcurrentHashMap<>();

    private final OperationLogWriter operationLogWriter;

    public LogAspect() {
        this(null);
    }

    public LogAspect(OperationLogWriter operationLogWriter) {
        this.operationLogWriter = operationLogWriter;
    }

    private static String toSafeJson(Object obj) {
        if (obj == null)
            return "null";
        try {
            String json = JSON.toJSONString(obj);
            if (json.length() > MAX_LENGTH) {
                return json.substring(0, MAX_LENGTH - TRUNCATED_SUFFIX.length()) + TRUNCATED_SUFFIX;
            }
            return json;
        } catch (Exception e) {
            return obj.getClass().getSimpleName() + "@" + Integer.toHexString(obj.hashCode());
        }
    }

    @Pointcut("@annotation(org.smm.archetype.operationlog.BusinessLog)")
    public void logCut() {}

    @Around("logCut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        org.smm.archetype.operationlog.BusinessLog businessLog = signature.getMethod()
                .getAnnotation(org.smm.archetype.operationlog.BusinessLog.class);
        Class<?> declaringType = signature.getDeclaringType();
        String methodName = signature.getMethod().getName();
        long startTime = System.currentTimeMillis();
        String businessDesc = businessLog != null ? businessLog.value() : "-";

        Logger logger = LOGGER_MAP.computeIfAbsent(declaringType, k -> LoggerFactory.getLogger(k));

        String status = "SUCCESS";
        String errorMessage = "";
        Object result = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            status = "ERROR";
            errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            throw e;
        } finally {
            long durationMs = System.currentTimeMillis() - startTime;

            if (status.equals("SUCCESS")) {
                logger.info("[方法执行] {}#{} | {} | {}ms | {} | {} | {}",
                        declaringType.getSimpleName(), methodName, businessDesc,
                        durationMs, Thread.currentThread().getName(),
                        toSafeJson(joinPoint.getArgs()), toSafeJson(result));
            } else {
                logger.error("[方法执行] {}#{} | {} | {}ms | {} | {} | ERROR",
                        declaringType.getSimpleName(), methodName, businessDesc,
                        durationMs, Thread.currentThread().getName(),
                        toSafeJson(joinPoint.getArgs()), errorMessage);
            }

            writeOperationLog(businessLog, declaringType, methodName,
                    joinPoint.getArgs(), result, durationMs, status, errorMessage);
        }
    }

    private void writeOperationLog(org.smm.archetype.operationlog.BusinessLog businessLog,
                                   Class<?> declaringType,
                                   String methodName, Object[] args, Object result,
                                   long durationMs, String status, String errorMessage) {
        if (operationLogWriter == null || businessLog == null) {
            return;
        }

        double samplingRate = businessLog.samplingRate();
        if (samplingRate < 1.0 && ThreadLocalRandom.current().nextDouble() >= samplingRate) {
            return;
        }

        String operationType = businessLog.operation().code();
        String module = businessLog.module();
        String description = businessLog.value();

        org.smm.archetype.operationlog.OperationLogRecord record =
                new org.smm.archetype.operationlog.OperationLogRecord(
                Span.current().getSpanContext().getTraceId(),
                "",
                module,
                operationType,
                description,
                declaringType.getSimpleName() + "#" + methodName,
                toSafeJson(args),
                toSafeJson(result),
                durationMs,
                "",
                status,
                errorMessage
        );

        try {
            operationLogWriter.write(record);
        } catch (Exception e) {
            Logger logger = LOGGER_MAP.getOrDefault(declaringType, LoggerFactory.getLogger(LogAspect.class));
            logger.warn("[操作日志写入失败] {} | {}", record.method(), e.getMessage());
        }
    }
}
```

**注意**：`LogAspect` 的 `@Pointcut` 引用的是 `org.smm.archetype.operationlog.BusinessLog`（已在 common 模块中的注解），而非旧的 `shared.aspect.operationlog.BusinessLog`。`OperationLogWriter` 和 `OperationLogRecord` 也引用 `common.operationlog` 包。这是因为在 Phase 1 中这些类型已迁移至 common 模块。但由于 LogAspect 是编译期的类，需确认这些 common 包下的类确实存在。如果 `common/operationlog/` 包不存在，则 Pointcut 仍需引用旧路径。

- [ ] **Step 3.11: 验证编译**

Run: `mvn compile -pl app`
Expected: BUILD SUCCESS

**注意**：如果编译失败，检查 `org.smm.archetype.operationlog.BusinessLog` 是否存在。如果不存在，LogAspect 的 Pointcut 应改回 `org.smm.archetype.shared.aspect.operationlog.BusinessLog`（旧路径，因旧文件仍在）。

---

## Task 4: Spring Modulith 依赖引入 + 模块声明 + 验证测试

**Files:**
- Modify: `app/pom.xml`
- Create: `app/src/main/java/org/smm/archetype/systemconfig/package-info.java`
- Create: `app/src/main/java/org/smm/archetype/auth/package-info.java`
- Create: `app/src/main/java/org/smm/archetype/operationlog/package-info.java`
- Create: `app/src/main/java/org/smm/archetype/shared/package-info.java`
- Create: `app/src/test/java/org/smm/archetype/support/basic/ModulithComplianceUTest.java`

- [ ] **Step 4.1: 在 app/pom.xml 添加 Spring Modulith 依赖**

在 `<!-- 测试 -->` 注释之后添加：

```xml
        <!-- Spring Modulith — 模块边界验证 -->
        <dependency>
            <groupId>org.springframework.modulith</groupId>
            <artifactId>spring-modulith-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <!-- Spring Modulith — 事件持久化 -->
        <dependency>
            <groupId>org.springframework.modulith</groupId>
            <artifactId>spring-modulith-starter-jdbc</artifactId>
        </dependency>
```

如果 Spring Boot 4.x BOM 未管理 modulith 版本，需在根 pom.xml 的 `<properties>` 中添加：

```xml
<spring-modulith.version>2.0.6</spring-modulith.version>
```

- [ ] **Step 4.2: 创建 systemconfig/package-info.java**

```java
@org.springframework.modulith.ApplicationModule(
        displayName = "System Configuration",
        allowedDependencies = {"shared"}
)
package org.smm.archetype.systemconfig;
```

- [ ] **Step 4.3: 创建 auth/package-info.java**

```java
@org.springframework.modulith.ApplicationModule(
        displayName = "Authentication",
        allowedDependencies = {"shared"}
)
package org.smm.archetype.auth;
```

- [ ] **Step 4.4: 创建 operationlog/package-info.java**

```java
@org.springframework.modulith.ApplicationModule(
        displayName = "Operation Log",
        allowedDependencies = {"shared"}
)
package org.smm.archetype.operationlog;
```

- [ ] **Step 4.5: 创建 shared/package-info.java**

```java
@org.springframework.modulith.ApplicationModule(
        displayName = "Shared Cross-Cutting",
        type = org.springframework.modulith.ApplicationModule.Type.OPEN
)
package org.smm.archetype.shared;
```

- [ ] **Step 4.6: 创建 ModulithComplianceUTest.java**

```java
package org.smm.archetype.support.basic;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Spring Modulith 模块边界验证。
 * <p>
 * 验证规则：
 * - 无循环依赖
 * - 模块间仅通过 allowedDependencies 访问
 * - internal/ 包不对外暴露
 */
class ModulithComplianceUTest {

    @Test
    void should_verifyModulithStructure() {
        ApplicationModules modules = ApplicationModules.of(org.smm.archetype.App.class);
        modules.verify();
    }

    @Test
    void should_generateModulithDocumentation() {
        ApplicationModules modules = ApplicationModules.of(org.smm.archetype.App.class);
        new Documenter(modules)
                .writeDocumentation()
                .writeIndividualModulesAsPlantUml();
    }
}
```

- [ ] **Step 4.7: 验证编译**

Run: `mvn compile -pl app`
Expected: BUILD SUCCESS

**注意**：如果 Spring Modulith BOM 版本不兼容，需要手动指定版本。如果 `ApplicationModules.of()` 的参数类 `App.class` 路径不对，需要确认主类的完整路径。

---

## Task 5: 测试文件迁移（auth + operationlog）

**Files:**
- Create: `app/src/test/java/org/smm/archetype/auth/internal/AuthFacadeITest.java`
- Create: `app/src/test/java/org/smm/archetype/operationlog/internal/OperationLogControllerITest.java`
- Create: `app/src/test/java/org/smm/archetype/operationlog/internal/OperationLogPaginationBoundaryITest.java`
- Create: `app/src/test/java/org/smm/archetype/operationlog/internal/OperationLogFacadeITest.java`
- Create: `app/src/test/java/org/smm/archetype/operationlog/internal/OperationLogRepositoryITest.java`

- [ ] **Step 5.1: 创建测试目录**

```bash
mkdir -p app/src/test/java/org/smm/archetype/auth/internal
mkdir -p app/src/test/java/org/smm/archetype/operationlog/internal
```

- [ ] **Step 5.2: 创建 AuthFacadeITest.java（重命名自 LoginFacadeITest）**

```java
package org.smm.archetype.auth.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.auth.AuthFacade;
import org.smm.archetype.exception.BizException;
import org.smm.archetype.exception.CommonErrorCode;
import org.smm.archetype.support.IntegrationTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AuthFacade 集成测试（重命名自 LoginFacadeITest）
 * <p>
 * 使用真实 Spring 上下文 + 内存 SQLite，init.sql 加载 admin/admin123 用户。
 */
@DisplayName("AuthFacade")
class AuthFacadeITest extends IntegrationTestBase {

    @Autowired
    private AuthFacade authFacade;

    @Nested
    @DisplayName("login 成功场景")
    class LoginSuccessTest {

        @Test
        @DisplayName("MFT: 正确用户名密码登录成功返回非空 token")
        void should_loginSuccessfully_andReturnToken() {
            webTestClient.post().uri("/api/auth/login")
                    .bodyValue(Map.of("username", "admin", "password", "admin123"))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.data.token").isNotEmpty();
        }
    }

    @Nested
    @DisplayName("login 失败场景")
    class LoginFailureTest {

        @Test
        @DisplayName("DIR: 用户不存在时抛 BizException 且 errorCode 为 AUTH_USER_NOT_FOUND")
        void should_throwWhenUserNotFound() {
            assertThatThrownBy(() -> authFacade.login("nonexistent", "password"))
                    .isInstanceOf(BizException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.AUTH_USER_NOT_FOUND);
        }

        @Test
        @DisplayName("DIR: 密码错误时抛 BizException 且 errorCode 为 AUTH_BAD_CREDENTIALS")
        void should_throwWhenBadPassword() {
            assertThatThrownBy(() -> authFacade.login("admin", "wrong-password"))
                    .isInstanceOf(BizException.class)
                    .extracting("errorCode")
                    .isEqualTo(CommonErrorCode.AUTH_BAD_CREDENTIALS);
        }
    }

    @Nested
    @DisplayName("logout")
    class LogoutTest {

        @Test
        @DisplayName("MFT: 注销成功不抛异常")
        void should_logoutSuccessfully() {
            webTestClient.post().uri("/api/auth/logout")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true);
        }
    }
}
```

- [ ] **Step 5.3: 创建 OperationLogControllerITest.java**

```java
package org.smm.archetype.operationlog.internal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.generated.entity.OperationLogDO;
import org.smm.archetype.generated.mapper.OperationLogMapper;
import org.smm.archetype.support.IntegrationTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 操作日志控制器集成测试 — 验证 API 端点
 */
class OperationLogControllerITest extends IntegrationTestBase {

    @Autowired
    private OperationLogMapper operationLogMapper;

    @BeforeEach
    void setUpTestData() {
        operationLogMapper.delete(new LambdaQueryWrapper<>());

        for (int i = 1; i <= 5; i++) {
            OperationLogDO logDO = new OperationLogDO();
            logDO.setTraceId("trace-" + i);
            logDO.setUserId("user-" + i);
            logDO.setModule(i <= 3 ? "USER" : "SYSTEM");
            logDO.setOperationType(i % 2 == 1 ? "CREATE" : "UPDATE");
            logDO.setDescription("测试操作 " + i);
            logDO.setMethod("com.example.Test.method" + i);
            logDO.setParams("{}");
            logDO.setResult("success");
            logDO.setExecutionTime(100L * i);
            logDO.setIp("127.0.0.1");
            logDO.setStatus("SUCCESS");
            logDO.setCreateTime(Instant.parse("2025-01-" + String.format("%02d", i) + "T10:00:00Z"));
            operationLogMapper.insert(logDO);
        }
    }

    @Nested
    @DisplayName("GET /api/system/operation-logs")
    class FindByPage {

        @Test
        @DisplayName("MFT: 默认分页返回所有记录")
        void should_returnPaginatedData() {
            webTestClient.get().uri("/api/system/operation-logs?pageNo=1&pageSize=10")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo(1000)
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.total").isEqualTo(5)
                    .jsonPath("$.pageNo").isEqualTo(1)
                    .jsonPath("$.pageSize").isEqualTo(10)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(5);
        }

        @Test
        @DisplayName("MFT: 分页参数正确工作")
        void should_respectPaginationParams() {
            webTestClient.get().uri("/api/system/operation-logs?pageNo=1&pageSize=2")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.total").isEqualTo(5)
                    .jsonPath("$.pageNo").isEqualTo(1)
                    .jsonPath("$.pageSize").isEqualTo(2)
                    .jsonPath("$.data.length()").isEqualTo(2);
        }

        @Test
        @DisplayName("MFT: 按模块过滤")
        void should_filterByModule() {
            webTestClient.get().uri("/api/system/operation-logs?pageNo=1&pageSize=10&module=USER")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.total").isEqualTo(3)
                    .jsonPath("$.data.length()").isEqualTo(3)
                    .jsonPath("$.data[0].module").isEqualTo("USER");
        }

        @Test
        @DisplayName("MFT: 按操作类型过滤")
        void should_filterByOperationType() {
            webTestClient.get().uri("/api/system/operation-logs?pageNo=1&pageSize=10&operationType=CREATE")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.total").isEqualTo(3)
                    .jsonPath("$.data.length()").isEqualTo(3)
                    .jsonPath("$.data[0].operationType").isEqualTo("CREATE");
        }

        @Test
        @DisplayName("DIR: pageSize 超过 100 返回校验失败")
        void should_returnValidationError_whenPageSizeExceeds100() {
            webTestClient.get().uri("/api/system/operation-logs?pageNo=1&pageSize=101")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(false);
        }

        @Test
        @DisplayName("MFT: 返回数据包含正确字段")
        void should_returnCorrectFields() {
            webTestClient.get().uri("/api/system/operation-logs?pageNo=1&pageSize=10")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.data[0].id").isNumber()
                    .jsonPath("$.data[0].traceId").exists()
                    .jsonPath("$.data[0].userId").exists()
                    .jsonPath("$.data[0].module").isEqualTo("SYSTEM")
                    .jsonPath("$.data[0].operationType").isEqualTo("CREATE")
                    .jsonPath("$.data[0].description").exists()
                    .jsonPath("$.data[0].method").exists()
                    .jsonPath("$.data[0].executionTime").isNumber()
                    .jsonPath("$.data[0].ip").exists()
                    .jsonPath("$.data[0].status").isEqualTo("SUCCESS")
                    .jsonPath("$.data[0].createTime").exists();
        }

        @Test
        @DisplayName("MFT: 按时间范围过滤")
        void should_filterByTimeRange() {
            webTestClient.get().uri("/api/system/operation-logs?pageNo=1&pageSize=10"
                            + "&startTime=2025-01-02T00:00:00Z&endTime=2025-01-04T23:59:59Z")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.total").isEqualTo(3)
                    .jsonPath("$.data.length()").isEqualTo(3);
        }
    }
}
```

- [ ] **Step 5.4: 创建 OperationLogPaginationBoundaryITest.java**

```java
package org.smm.archetype.operationlog.internal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.generated.entity.OperationLogDO;
import org.smm.archetype.generated.mapper.OperationLogMapper;
import org.smm.archetype.support.IntegrationTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 操作日志分页边界值测试 — 验证分页参数边界行为
 */
class OperationLogPaginationBoundaryITest extends IntegrationTestBase {

    @Autowired
    private OperationLogMapper operationLogMapper;

    @BeforeEach
    void setUpTestData() {
        operationLogMapper.delete(new LambdaQueryWrapper<>());

        for (int i = 1; i <= 5; i++) {
            OperationLogDO logDO = new OperationLogDO();
            logDO.setTraceId("trace-" + i);
            logDO.setUserId("user-" + i);
            logDO.setModule(i <= 3 ? "USER" : "SYSTEM");
            logDO.setOperationType(i % 2 == 1 ? "CREATE" : "UPDATE");
            logDO.setDescription("测试操作 " + i);
            logDO.setMethod("com.example.Test.method" + i);
            logDO.setParams("{}");
            logDO.setResult("success");
            logDO.setExecutionTime(100L * i);
            logDO.setIp("127.0.0.1");
            logDO.setStatus("SUCCESS");
            logDO.setCreateTime(Instant.parse("2025-01-" + String.format("%02d", i) + "T10:00:00Z"));
            operationLogMapper.insert(logDO);
        }
    }

    @Nested
    @DisplayName("GET /api/system/operation-logs — 边界值测试")
    class PaginationBoundary {

        @Test
        @DisplayName("BND: pageNo=100（超出范围）返回空 data 数组，total 仍正确")
        void should_returnEmptyData_whenPageNoExceedsRange() {
            webTestClient.get().uri("/api/system/operation-logs?pageNo=100&pageSize=10")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.total").isEqualTo(5)
                    .jsonPath("$.pageNo").isEqualTo(100)
                    .jsonPath("$.pageSize").isEqualTo(10)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(0);
        }

        @Test
        @DisplayName("BND: pageSize=1 返回恰好 1 条记录")
        void should_returnExactlyOneRecord_whenPageSizeIs1() {
            webTestClient.get().uri("/api/system/operation-logs?pageNo=1&pageSize=1")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.total").isEqualTo(5)
                    .jsonPath("$.pageNo").isEqualTo(1)
                    .jsonPath("$.pageSize").isEqualTo(1)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(1);
        }

        @Test
        @DisplayName("BND: pageSize=100（边界值）返回最多 5 条记录（数据不足 100）")
        void should_returnAllRecords_whenPageSizeIs100() {
            webTestClient.get().uri("/api/system/operation-logs?pageNo=1&pageSize=100")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.total").isEqualTo(5)
                    .jsonPath("$.pageNo").isEqualTo(1)
                    .jsonPath("$.pageSize").isEqualTo(100)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(5);
        }

        @Test
        @DisplayName("BND: 无分页参数（缺失 pageNo/pageSize）返回校验失败")
        void should_returnValidationError_whenPaginationParamsMissing() {
            webTestClient.get().uri("/api/system/operation-logs")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(false);
        }

        @Test
        @DisplayName("BND: module 过滤 + pageSize=1 返回过滤后分页结果")
        void should_returnFilteredPaginatedResult_whenModuleFilterWithPageSize1() {
            webTestClient.get().uri("/api/system/operation-logs?pageNo=1&pageSize=1&module=USER")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.total").isEqualTo(3)
                    .jsonPath("$.pageNo").isEqualTo(1)
                    .jsonPath("$.pageSize").isEqualTo(1)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(1)
                    .jsonPath("$.data[0].module").isEqualTo("USER");

            webTestClient.get().uri("/api/system/operation-logs?pageNo=1&pageSize=1&module=SYSTEM")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.total").isEqualTo(2)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(1)
                    .jsonPath("$.data[0].module").isEqualTo("SYSTEM");
        }

        @Test
        @DisplayName("BND: module 过滤 + pageNo 超出范围返回空 data")
        void should_returnEmptyData_whenFilteredPageNoExceedsRange() {
            webTestClient.get().uri("/api/system/operation-logs?pageNo=100&pageSize=10&module=USER")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.total").isEqualTo(3)
                    .jsonPath("$.data").isArray()
                    .jsonPath("$.data.length()").isEqualTo(0);
        }
    }
}
```

- [ ] **Step 5.5: 创建 OperationLogFacadeITest.java**

```java
package org.smm.archetype.operationlog.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.operationlog.OperationLogFacade;
import org.smm.archetype.shared.result.BasePageResult;
import org.smm.archetype.support.IntegrationTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OperationLogFacade 集成测试
 */
@DisplayName("OperationLogFacade")
class OperationLogFacadeITest extends IntegrationTestBase {

    @Autowired
    private OperationLogFacade operationLogFacade;

    @Nested
    @DisplayName("findByPage")
    class FindByPage {

        @Test
        @DisplayName("MFT: 分页查询返回正确元数据（空数据场景）")
        void should_returnPagedResult() {
            OperationLogPageQuery query = new OperationLogPageQuery(1, 10, null, null, null, null);
            BasePageResult<OperationLogVO> result = operationLogFacade.findByPage(query);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isNotNull();
            assertThat(result.getPageNo()).isEqualTo(1);
            assertThat(result.getPageSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("MFT: 查询不存在的模块返回空结果")
        void should_returnEmptyResult_whenNoRecords() {
            OperationLogPageQuery query = new OperationLogPageQuery(1, 10, "NONEXISTENT", null, null, null);
            BasePageResult<OperationLogVO> result = operationLogFacade.findByPage(query);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getTotal()).isEqualTo(0);
            assertThat(result.getData()).isEmpty();
        }

        @Test
        @DisplayName("MFT: 返回结果的 VO 结构正确，isSuccess=true 且 data 不为 null")
        void should_mapFieldsToVO() {
            OperationLogPageQuery query = new OperationLogPageQuery(1, 10, null, null, null, null);
            BasePageResult<OperationLogVO> result = operationLogFacade.findByPage(query);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isNotNull();
            assertThat(result.getTotal()).isGreaterThanOrEqualTo(0);
            assertThat(result.getPageNo()).isEqualTo(1);
            assertThat(result.getPageSize()).isEqualTo(10);
            assertThat(result.getCode()).isNotNull();
            assertThat(result.getMessage()).isNotNull();
            assertThat(result.getTime()).isNotNull();
        }

        @Test
        @DisplayName("MFT: 分页参数正确传递，pageNo 和 pageSize 与请求一致")
        void should_passQueryParameters() {
            OperationLogPageQuery query = new OperationLogPageQuery(2, 5, "USER", null, null, null);
            BasePageResult<OperationLogVO> result = operationLogFacade.findByPage(query);

            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getPageNo()).isEqualTo(2);
            assertThat(result.getPageSize()).isEqualTo(5);
        }
    }
}
```

- [ ] **Step 5.6: 创建 OperationLogRepositoryITest.java（IPage → PageResult 断言调整）**

```java
package org.smm.archetype.operationlog.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.generated.entity.OperationLogDO;
import org.smm.archetype.generated.mapper.OperationLogMapper;
import org.smm.archetype.shared.pagination.PageResult;
import org.smm.archetype.support.IntegrationTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 操作日志仓储集成测试 — 验证分页查询（真实 SQLite 数据库）
 * <p>
 * 断言已从 IPage API 调整为 PageResult API：
 * - IPage.getRecords() → PageResult.list()
 * - IPage.getTotal() → PageResult.total()
 * - IPage.getCurrent() → PageResult.pageNo()
 * - IPage.getSize() → PageResult.pageSize()
 */
class OperationLogRepositoryITest extends IntegrationTestBase {

    @Autowired
    private OperationLogRepository operationLogRepository;

    @Autowired
    private OperationLogMapper operationLogMapper;

    @BeforeEach
    void setUpTestData() {
        operationLogMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>());

        for (int i = 1; i <= 5; i++) {
            OperationLogDO logDO = new OperationLogDO();
            logDO.setTraceId("trace-" + i);
            logDO.setUserId("user-" + i);
            logDO.setModule(i <= 3 ? "USER" : "SYSTEM");
            logDO.setOperationType(i % 2 == 1 ? "CREATE" : "UPDATE");
            logDO.setDescription("测试操作 " + i);
            logDO.setMethod("com.example.Test.method" + i);
            logDO.setParams("{}");
            logDO.setResult("success");
            logDO.setExecutionTime(100L * i);
            logDO.setIp("127.0.0.1");
            logDO.setStatus("SUCCESS");
            logDO.setCreateTime(Instant.parse("2025-01-" + String.format("%02d", i) + "T10:00:00Z"));
            operationLogMapper.insert(logDO);
        }
    }

    @Nested
    @DisplayName("findByPage — 无过滤条件")
    class FindByPageNoFilter {

        @Test
        @DisplayName("MFT: 无过滤条件返回所有记录，分页正确")
        void should_returnAllRecords_paginated() {
            OperationLogPageQuery query = new OperationLogPageQuery(1, 3, null, null, null, null);

            PageResult<OperationLog> result = operationLogRepository.findByPage(query);

            assertThat(result.total()).isEqualTo(5);
            assertThat(result.list()).hasSize(3);
            assertThat(result.pageNo()).isEqualTo(1);
            assertThat(result.pageSize()).isEqualTo(3);
        }

        @Test
        @DisplayName("MFT: 第二页返回剩余记录")
        void should_returnSecondPage() {
            OperationLogPageQuery query = new OperationLogPageQuery(2, 3, null, null, null, null);

            PageResult<OperationLog> result = operationLogRepository.findByPage(query);

            assertThat(result.total()).isEqualTo(5);
            assertThat(result.list()).hasSize(2);
            assertThat(result.pageNo()).isEqualTo(2);
        }

        @Test
        @DisplayName("DIR: pageSize 超过总记录数时返回所有记录")
        void should_returnAllRecords_whenPageSizeExceedsTotal() {
            OperationLogPageQuery query = new OperationLogPageQuery(1, 100, null, null, null, null);

            PageResult<OperationLog> result = operationLogRepository.findByPage(query);

            assertThat(result.total()).isEqualTo(5);
            assertThat(result.list()).hasSize(5);
        }
    }

    @Nested
    @DisplayName("findByPage — 按模块过滤")
    class FindByPageModuleFilter {

        @Test
        @DisplayName("MFT: 按 USER 模块过滤返回 3 条记录")
        void should_filterByUserModule() {
            OperationLogPageQuery query = new OperationLogPageQuery(1, 10, "USER", null, null, null);

            PageResult<OperationLog> result = operationLogRepository.findByPage(query);

            assertThat(result.total()).isEqualTo(3);
            assertThat(result.list()).allMatch(log -> "USER".equals(log.getModule()));
        }

        @Test
        @DisplayName("MFT: 按 SYSTEM 模块过滤返回 2 条记录")
        void should_filterBySystemModule() {
            OperationLogPageQuery query = new OperationLogPageQuery(1, 10, "SYSTEM", null, null, null);

            PageResult<OperationLog> result = operationLogRepository.findByPage(query);

            assertThat(result.total()).isEqualTo(2);
            assertThat(result.list()).allMatch(log -> "SYSTEM".equals(log.getModule()));
        }

        @Test
        @DisplayName("DIR: 不存在的模块返回空列表")
        void should_returnEmpty_whenModuleNotFound() {
            OperationLogPageQuery query = new OperationLogPageQuery(1, 10, "NONEXISTENT", null, null, null);

            PageResult<OperationLog> result = operationLogRepository.findByPage(query);

            assertThat(result.total()).isEqualTo(0);
            assertThat(result.list()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByPage — 按操作类型过滤")
    class FindByPageOperationTypeFilter {

        @Test
        @DisplayName("MFT: 按 CREATE 类型过滤返回奇数 ID 记录")
        void should_filterByCreateType() {
            OperationLogPageQuery query = new OperationLogPageQuery(1, 10, null, "CREATE", null, null);

            PageResult<OperationLog> result = operationLogRepository.findByPage(query);

            assertThat(result.total()).isEqualTo(3);
            assertThat(result.list()).allMatch(log -> "CREATE".equals(log.getOperationType()));
        }

        @Test
        @DisplayName("MFT: 按 UPDATE 类型过滤返回偶数 ID 记录")
        void should_filterByUpdateType() {
            OperationLogPageQuery query = new OperationLogPageQuery(1, 10, null, "UPDATE", null, null);

            PageResult<OperationLog> result = operationLogRepository.findByPage(query);

            assertThat(result.total()).isEqualTo(2);
            assertThat(result.list()).allMatch(log -> "UPDATE".equals(log.getOperationType()));
        }
    }

    @Nested
    @DisplayName("findByPage — 按时间范围过滤")
    class FindByPageTimeRangeFilter {

        @Test
        @DisplayName("MFT: 时间范围内返回匹配记录")
        void should_filterByTimeRange() {
            OperationLogPageQuery query = new OperationLogPageQuery(1, 10, null, null,
                    "2025-01-02T00:00:00Z", "2025-01-04T23:59:59Z");

            PageResult<OperationLog> result = operationLogRepository.findByPage(query);

            assertThat(result.total()).isEqualTo(3);
        }

        @Test
        @DisplayName("DIR: 超出范围返回空列表")
        void should_returnEmpty_whenTimeRangeOutOfRange() {
            OperationLogPageQuery query = new OperationLogPageQuery(1, 10, null, null,
                    "2030-01-01T00:00:00Z", "2030-12-31T23:59:59Z");

            PageResult<OperationLog> result = operationLogRepository.findByPage(query);

            assertThat(result.total()).isEqualTo(0);
            assertThat(result.list()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByPage — 组合过滤")
    class FindByPageCombinedFilter {

        @Test
        @DisplayName("MFT: 模块 + 操作类型组合过滤")
        void should_filterByModuleAndType() {
            OperationLogPageQuery query = new OperationLogPageQuery(1, 10, "USER", "CREATE", null, null);

            PageResult<OperationLog> result = operationLogRepository.findByPage(query);

            assertThat(result.total()).isEqualTo(2);
            assertThat(result.list()).allMatch(log ->
                    "USER".equals(log.getModule()) && "CREATE".equals(log.getOperationType()));
        }
    }

    @Nested
    @DisplayName("findByPage — 分页参数验证")
    class FindByPagePaginationParams {

        @Test
        @DisplayName("MFT: pageNo=1,pageSize=1 返回 1 条记录，total=5")
        void should_returnOneRecord_perPage() {
            OperationLogPageQuery query = new OperationLogPageQuery(1, 1, null, null, null, null);

            PageResult<OperationLog> result = operationLogRepository.findByPage(query);

            assertThat(result.total()).isEqualTo(5);
            assertThat(result.list()).hasSize(1);
        }
    }
}
```

- [ ] **Step 5.7: 验证编译**

Run: `mvn compile -pl app`
Expected: BUILD SUCCESS（测试文件新增不影响编译；新 ITest 的 import 在 Task 6 激活前不会生效，因为新位置的类还没有 Spring 注解）

**注意**：此时新测试文件虽然已创建，但由于新位置的 Service/Repository 等 Bean 未注册（无 @Service/@Repository 注解），Spring 上下文会注入旧的 Bean。因此 Task 5 完成后，旧测试仍然可以运行：

Run: `mvn test -Dtest="*ITest" -pl app`
Expected: 全部通过（仍使用旧代码路径）

---

## Task 6: 激活（删除旧文件 + 添加 Spring 注解 + 更新 import）

**Files:**
- Modify: `app/src/main/java/org/smm/archetype/auth/internal/*.java` — 添加 Spring 注解
- Modify: `app/src/main/java/org/smm/archetype/operationlog/internal/*.java` — 添加 Spring 注解
- Modify: `app/src/main/java/org/smm/archetype/shared/internal/aspect/**/*.java` — 添加 Spring 注解
- Delete: 所有旧位置文件

**此 Task 是原子操作。每个 Step 完成后必须编译验证。**

- [ ] **Step 6.1: 为 auth/internal/ 新文件添加 Spring 注解**

**UserRepositoryImpl.java** — 添加 `@Repository` + `@RequiredArgsConstructor`，注入 UserConverter 为 Bean：

```java
package org.smm.archetype.auth.internal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户仓储实现。
 */
@Repository
@RequiredArgsConstructor
class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;
    private final UserConverter converter;

    @Override
    public Optional<User> findByUsername(String username) {
        UserDO userDO = userMapper.selectOne(
                new LambdaQueryWrapper<UserDO>().eq(UserDO::getUsername, username)
        );
        return Optional.ofNullable(userDO).map(converter::toModel);
    }
}
```

**UserConverter.java** — 添加 `@Component`：

```java
package org.smm.archetype.auth.internal;

import org.springframework.stereotype.Component;

/**
 * 用户 DO → Entity 转换器。
 */
@Component
class UserConverter {

    User toModel(UserDO userDO) {
        if (userDO == null) {
            return null;
        }
        User user = new User();
        user.setId(userDO.getId());
        user.setUsername(userDO.getUsername());
        user.setPasswordHash(userDO.getPasswordHash());
        user.setNickname(userDO.getNickname());
        user.setStatus(userDO.getStatus());
        user.setCreateTime(userDO.getCreateTime());
        user.setUpdateTime(userDO.getUpdateTime());
        return user;
    }

    UserDO toDataObject(User user) {
        if (user == null) {
            return null;
        }
        return UserDO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .passwordHash(user.getPasswordHash())
                .nickname(user.getNickname())
                .status(user.getStatus())
                .build();
    }
}
```

**AuthFacadeImpl.java** — 添加 `@Slf4j` + `@Service` + `@RequiredArgsConstructor`：

```java
package org.smm.archetype.auth.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.smm.archetype.auth.AuthFacade;
import org.smm.archetype.component.auth.AuthComponent;
import org.smm.archetype.exception.BizException;
import org.smm.archetype.exception.CommonErrorCode;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证门面实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
class AuthFacadeImpl implements AuthFacade {

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

**LoginController.java** — 添加 `@RestController` + `@RequiredArgsConstructor`：

```java
package org.smm.archetype.auth.internal;

import lombok.RequiredArgsConstructor;
import org.smm.archetype.auth.AuthFacade;
import org.smm.archetype.shared.result.BaseResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 登录控制器。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class LoginController {

    private final AuthFacade authFacade;

    @PostMapping("/login")
    public BaseResult<Map<String, String>> login(@RequestBody LoginRequest request) {
        String token = authFacade.login(request.username(), request.password());
        return BaseResult.success(Map.of("token", token));
    }

    @PostMapping("/logout")
    public BaseResult<Void> logout() {
        authFacade.logout();
        return BaseResult.success(null);
    }
}
```

**UserMapper.java** — 添加 `@Mapper`：

```java
package org.smm.archetype.auth.internal;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper。
 */
@Mapper
interface UserMapper extends BaseMapper<UserDO> {
}
```

- [ ] **Step 6.2: 为 operationlog/internal/ 新文件添加 Spring 注解**

**OperationLogConverter.java** — 添加 `@Component`：

```java
package org.smm.archetype.operationlog.internal;

import org.springframework.stereotype.Component;

/**
 * 操作日志 DO → Entity 转换器。
 */
@Component
class OperationLogConverter {

    OperationLog toModel(OperationLogDO logDO) {
        if (logDO == null) {
            return null;
        }
        return OperationLog.builder()
                .id(logDO.getId())
                .traceId(logDO.getTraceId())
                .userId(logDO.getUserId())
                .module(logDO.getModule())
                .operationType(logDO.getOperationType())
                .description(logDO.getDescription())
                .method(logDO.getMethod())
                .params(logDO.getParams())
                .result(logDO.getResult())
                .executionTime(logDO.getExecutionTime())
                .ip(logDO.getIp())
                .status(logDO.getStatus())
                .errorMessage(logDO.getErrorMessage())
                .createTime(logDO.getCreateTime())
                .build();
    }
}
```

**OperationLogRepositoryImpl.java** — 添加 `@Repository` + `@RequiredArgsConstructor`，注入 Converter 为 Bean：

```java
package org.smm.archetype.operationlog.internal;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.smm.archetype.shared.pagination.PageResult;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * 操作日志仓储实现。
 */
@Repository
@RequiredArgsConstructor
class OperationLogRepositoryImpl implements OperationLogRepository {

    private final OperationLogMapper operationLogMapper;
    private final OperationLogConverter converter;

    @Override
    public PageResult<OperationLog> findByPage(OperationLogPageQuery query) {
        Page<OperationLogDO> page = new Page<>(query.pageNo(), query.pageSize());

        LambdaQueryWrapper<OperationLogDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(query.module()), OperationLogDO::getModule, query.module())
                .eq(StrUtil.isNotBlank(query.operationType()), OperationLogDO::getOperationType, query.operationType())
                .ge(StrUtil.isNotBlank(query.startTime()), OperationLogDO::getCreateTime,
                        parseInstant(query.startTime()))
                .le(StrUtil.isNotBlank(query.endTime()), OperationLogDO::getCreateTime,
                        parseInstant(query.endTime()))
                .orderByDesc(OperationLogDO::getId);

        var doPage = operationLogMapper.selectPage(page, wrapper);

        List<OperationLog> entities = doPage.getRecords().stream()
                .map(converter::toModel)
                .toList();

        return PageResult.of(entities, doPage.getTotal(), (int) doPage.getCurrent(), (int) doPage.getSize());
    }

    private Instant parseInstant(String dateTime) {
        if (dateTime == null || dateTime.isBlank()) {
            return null;
        }
        return Instant.parse(dateTime);
    }
}
```

**OperationLogService.java** — 添加 `@Service` + `@RequiredArgsConstructor` + `@Transactional`：

```java
package org.smm.archetype.operationlog.internal;

import lombok.RequiredArgsConstructor;
import org.smm.archetype.shared.pagination.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 操作日志服务。
 */
@Service
@RequiredArgsConstructor
class OperationLogService {

    private final OperationLogRepository operationLogRepository;

    @Transactional(readOnly = true)
    public PageResult<OperationLog> findByPage(OperationLogPageQuery query) {
        return operationLogRepository.findByPage(query);
    }
}
```

**OperationLogFacadeImpl.java** — 添加 `@Service` + `@RequiredArgsConstructor`：

```java
package org.smm.archetype.operationlog.internal;

import lombok.RequiredArgsConstructor;
import org.smm.archetype.operationlog.OperationLogFacade;
import org.smm.archetype.shared.pagination.PageResult;
import org.smm.archetype.shared.result.BasePageResult;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 操作日志 Facade 实现。
 */
@Service
@RequiredArgsConstructor
class OperationLogFacadeImpl implements OperationLogFacade {

    private final OperationLogService operationLogService;

    @Override
    public BasePageResult<OperationLogVO> findByPage(OperationLogPageQuery query) {
        PageResult<OperationLog> pageResult = operationLogService.findByPage(query);

        List<OperationLogVO> voList = pageResult.list().stream()
                .map(this::toVO)
                .toList();

        return BasePageResult.from(new PageResult<>(
                voList, pageResult.total(), pageResult.pageNo(), pageResult.pageSize(), pageResult.totalPages()
        ));
    }

    private OperationLogVO toVO(OperationLog log) {
        return new OperationLogVO(
                log.getId(),
                log.getTraceId(),
                log.getUserId(),
                log.getModule(),
                log.getOperationType(),
                log.getDescription(),
                log.getMethod(),
                log.getParams(),
                log.getResult(),
                log.getExecutionTime(),
                log.getIp(),
                log.getStatus(),
                log.getErrorMessage(),
                log.getCreateTime()
        );
    }
}
```

**OperationLogController.java** — 添加 `@RestController` + `@RequiredArgsConstructor` + `@Slf4j` + `@Tag`：

```java
package org.smm.archetype.operationlog.internal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.smm.archetype.operationlog.OperationLogFacade;
import org.smm.archetype.shared.result.BasePageResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志控制器
 */
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
            @Valid @ModelAttribute OperationLogPageQuery query) {
        return operationLogFacade.findByPage(query);
    }
}
```

**OperationLogMapper.java** — 添加 `@Mapper`：

```java
package org.smm.archetype.operationlog.internal;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志 Mapper。
 */
@Mapper
interface OperationLogMapper extends BaseMapper<OperationLogDO> {
}
```

- [ ] **Step 6.3: 为 shared/internal/aspect/ 新文件添加 Spring 注解**

**RateLimitAspect.java** — 添加 `@Aspect` + `@Slf4j`：

在类声明前添加 `@Aspect` 和 `@Slf4j` 注解。Pointcut 中的注解路径改为新路径：
`@annotation(org.smm.archetype.shared.internal.aspect.ratelimit.RateLimit)` → 保持不变（因为此类已在新路径下）

```java
// 在 Step 3.6 的代码基础上，只需在类声明前添加：
@Aspect
@Slf4j
public class RateLimitAspect {
```

**IdempotentAspect.java** — 添加 `@Slf4j` + `@Aspect`：

```java
// 在 Step 3.9 的代码基础上，只需在类声明前添加：
@Slf4j
@Aspect
public class IdempotentAspect {
```

**LogAspect.java** — 添加 `@Aspect`（已有 `@Pointcut` 和 `@Around`，但类上缺 `@Aspect`）：

```java
// 在 Step 3.10 的代码基础上，只需在类声明前添加：
@Aspect
public class LogAspect {
```

- [ ] **Step 6.4: 删除旧源文件**

```bash
# auth 旧文件
rm app/src/main/java/org/smm/archetype/controller/auth/LoginController.java
rm app/src/main/java/org/smm/archetype/service/auth/LoginFacade.java
rm app/src/main/java/org/smm/archetype/service/auth/LoginFacadeImpl.java
rm app/src/main/java/org/smm/archetype/entity/user/User.java
rm app/src/main/java/org/smm/archetype/repository/user/UserRepository.java
rm app/src/main/java/org/smm/archetype/repository/user/UserRepositoryImpl.java

# operationlog 旧文件
rm app/src/main/java/org/smm/archetype/controller/operationlog/OperationLogController.java
rm app/src/main/java/org/smm/archetype/facade/operationlog/OperationLogFacade.java
rm app/src/main/java/org/smm/archetype/facade/operationlog/OperationLogFacadeImpl.java
rm app/src/main/java/org/smm/archetype/facade/operationlog/OperationLogVO.java
rm app/src/main/java/org/smm/archetype/service/operationlog/OperationLogService.java
rm app/src/main/java/org/smm/archetype/entity/operationlog/OperationLog.java
rm app/src/main/java/org/smm/archetype/entity/operationlog/OperationLogPageQuery.java
rm app/src/main/java/org/smm/archetype/repository/operationlog/OperationLogRepository.java
rm app/src/main/java/org/smm/archetype/repository/operationlog/OperationLogRepositoryImpl.java
rm app/src/main/java/org/smm/archetype/repository/operationlog/OperationLogConverter.java

# shared 旧切面文件
rm app/src/main/java/org/smm/archetype/shared/aspect/ratelimit/RateLimitAspect.java
rm app/src/main/java/org/smm/archetype/shared/aspect/ratelimit/RateLimit.java
rm app/src/main/java/org/smm/archetype/shared/aspect/ratelimit/LimitFallback.java
rm app/src/main/java/org/smm/archetype/shared/aspect/ratelimit/BucketFactory.java
rm app/src/main/java/org/smm/archetype/shared/aspect/ratelimit/SpelKeyResolver.java
rm app/src/main/java/org/smm/archetype/shared/aspect/idempotent/IdempotentAspect.java
rm app/src/main/java/org/smm/archetype/shared/aspect/idempotent/Idempotent.java
rm app/src/main/java/org/smm/archetype/shared/aspect/idempotent/IdempotentKeyResolver.java
rm app/src/main/java/org/smm/archetype/shared/aspect/operationlog/LogAspect.java

# generated 旧文件
rm app/src/main/java/org/smm/archetype/generated/entity/UserDO.java
rm app/src/main/java/org/smm/archetype/generated/entity/OperationLogDO.java
rm app/src/main/java/org/smm/archetype/generated/mapper/UserMapper.java
rm app/src/main/java/org/smm/archetype/generated/mapper/OperationLogMapper.java

# 清理空目录
rmdir app/src/main/java/org/smm/archetype/controller/auth 2>/dev/null || true
rmdir app/src/main/java/org/smm/archetype/controller/operationlog 2>/dev/null || true
rmdir app/src/main/java/org/smm/archetype/entity/user 2>/dev/null || true
rmdir app/src/main/java/org/smm/archetype/entity/operationlog 2>/dev/null || true
rmdir app/src/main/java/org/smm/archetype/repository/user 2>/dev/null || true
rmdir app/src/main/java/org/smm/archetype/repository/operationlog 2>/dev/null || true
rmdir app/src/main/java/org/smm/archetype/service/auth 2>/dev/null || true
rmdir app/src/main/java/org/smm/archetype/facade/operationlog 2>/dev/null || true
rmdir app/src/main/java/org/smm/archetype/generated/entity 2>/dev/null || true
rmdir app/src/main/java/org/smm/archetype/generated/mapper 2>/dev/null || true
```

- [ ] **Step 6.5: 删除旧测试文件**

```bash
rm app/src/test/java/org/smm/archetype/service/auth/LoginFacadeITest.java
rm app/src/test/java/org/smm/archetype/controller/operationlog/OperationLogControllerITest.java
rm app/src/test/java/org/smm/archetype/controller/operationlog/OperationLogPaginationBoundaryITest.java
rm app/src/test/java/org/smm/archetype/facade/operationlog/OperationLogFacadeITest.java
rm app/src/test/java/org/smm/archetype/repository/operationlog/OperationLogRepositoryITest.java

# 清理空目录
rmdir app/src/test/java/org/smm/archetype/service/auth 2>/dev/null || true
rmdir app/src/test/java/org/smm/archetype/controller/operationlog 2>/dev/null || true
rmdir app/src/test/java/org/smm/archetype/facade/operationlog 2>/dev/null || true
rmdir app/src/test/java/org/smm/archetype/repository/operationlog 2>/dev/null || true
```

- [ ] **Step 6.6: 更新跨模块测试中的 import**

检查 `cases/integrationtest/LoginControllerITest.java`，确认它不直接 import `controller.auth.LoginController`（它通过 HTTP 端点测试，不需要修改 import）。如果确实 import 了旧路径，需更新。

检查所有 `cases/integrationtest/` 下的测试文件，搜索旧包路径引用：

```bash
grep -r "org.smm.archetype.controller.auth" app/src/test/ --include="*.java" || true
grep -r "org.smm.archetype.service.auth" app/src/test/ --include="*.java" || true
grep -r "org.smm.archetype.repository.user" app/src/test/ --include="*.java" || true
grep -r "org.smm.archetype.entity.user" app/src/test/ --include="*.java" || true
grep -r "org.smm.archetype.facade.operationlog" app/src/test/ --include="*.java" || true
grep -r "org.smm.archetype.repository.operationlog" app/src/test/ --include="*.java" || true
grep -r "org.smm.archetype.controller.operationlog" app/src/test/ --include="*.java" || true
grep -r "org.smm.archetype.entity.operationlog" app/src/test/ --include="*.java" || true
grep -r "org.smm.archetype.service.operationlog" app/src/test/ --include="*.java" || true
grep -r "org.smm.archetype.shared.aspect.ratelimit" app/src/test/ --include="*.java" || true
grep -r "org.smm.archetype.shared.aspect.idempotent" app/src/test/ --include="*.java" || true
grep -r "org.smm.archetype.shared.aspect.operationlog" app/src/test/ --include="*.java" || true
```

如果发现任何引用，更新为新路径：
- `org.smm.archetype.controller.auth.*` → `org.smm.archetype.auth.internal.*`
- `org.smm.archetype.service.auth.*` → `org.smm.archetype.auth.internal.*` 或 `org.smm.archetype.auth.AuthFacade`
- `org.smm.archetype.facade.operationlog.*` → `org.smm.archetype.operationlog.OperationLogFacade` 或 `org.smm.archetype.operationlog.internal.*`
- `org.smm.archetype.shared.aspect.ratelimit.*` → `org.smm.archetype.shared.internal.aspect.ratelimit.*`
- `org.smm.archetype.shared.aspect.idempotent.*` → `org.smm.archetype.shared.internal.aspect.idempotent.*`

- [ ] **Step 6.7: 检查并更新源码中的旧 import**

搜索所有源码中的旧包引用：

```bash
grep -r "org.smm.archetype.controller.auth" app/src/main/ --include="*.java" || true
grep -r "org.smm.archetype.service.auth" app/src/main/ --include="*.java" || true
grep -r "org.smm.archetype.repository.user" app/src/main/ --include="*.java" || true
grep -r "org.smm.archetype.entity.user" app/src/main/ --include="*.java" || true
grep -r "org.smm.archetype.facade.operationlog" app/src/main/ --include="*.java" || true
grep -r "org.smm.archetype.repository.operationlog" app/src/main/ --include="*.java" || true
grep -r "org.smm.archetype.controller.operationlog" app/src/main/ --include="*.java" || true
grep -r "org.smm.archetype.entity.operationlog" app/src/main/ --include="*.java" || true
grep -r "org.smm.archetype.service.operationlog" app/src/main/ --include="*.java" || true
grep -r "org.smm.archetype.generated.entity.UserDO" app/src/main/ --include="*.java" || true
grep -r "org.smm.archetype.generated.entity.OperationLogDO" app/src/main/ --include="*.java" || true
grep -r "org.smm.archetype.generated.mapper.UserMapper" app/src/main/ --include="*.java" || true
grep -r "org.smm.archetype.generated.mapper.OperationLogMapper" app/src/main/ --include="*.java" || true
grep -r "org.smm.archetype.shared.aspect.ratelimit" app/src/main/ --include="*.java" || true
grep -r "org.smm.archetype.shared.aspect.idempotent" app/src/main/ --include="*.java" || true
grep -r "org.smm.archetype.shared.aspect.operationlog" app/src/main/ --include="*.java" || true
```

修复所有找到的旧引用。

- [ ] **Step 6.8: 编译验证**

Run: `mvn compile -pl app`
Expected: BUILD SUCCESS — 无 duplicate class 错误，无 missing import 错误

- [ ] **Step 6.9: 运行新位置的测试**

Run: `mvn test -Dtest="*ITest" -pl app`
Expected: 全部通过（新位置的 ITest 使用新的 Bean）

Run: `mvn test -Dtest="*UTest" -pl app`
Expected: 全部通过

---

## Task 7: 最终清理 + 全量验证

**Files:**
- Delete: `app/src/main/java/org/smm/archetype/entity/base/BaseRequest.java`
- Delete: `app/src/main/java/org/smm/archetype/entity/base/BasePageRequest.java`
- Delete: `app/src/main/java/org/smm/archetype/shared/aspect/operationlog/BusinessLog.java` (旧存根)
- Delete: `app/src/main/java/org/smm/archetype/shared/aspect/operationlog/OperationLogRecord.java` (旧存根)
- Delete: `app/src/main/java/org/smm/archetype/shared/aspect/operationlog/OperationLogWriter.java` (旧存根)
- Delete: `app/src/main/java/org/smm/archetype/shared/aspect/operationlog/OperationType.java` (旧存根)
- Modify: `app/src/main/java/org/smm/archetype/generated/MybatisPlusGenerator.java` — 多模块输出改造

- [ ] **Step 7.1: 删除废弃的 entity/base 文件**

```bash
rm app/src/main/java/org/smm/archetype/entity/base/BaseRequest.java
rm app/src/main/java/org/smm/archetype/entity/base/BasePageRequest.java
```

**注意**：如果 `BaseResult.java` 和 `BasePageResult.java` 仍在 `entity/base/` 下有旧版本，也需确认 Phase 1/2 是否已迁移至 `shared/result/`。如果 `entity/base/BaseResult.java` 仍有旧版本（@Deprecated 标记），则一并删除。

- [ ] **Step 7.2: 删除 shared/aspect/operationlog/ 下的旧存根**

```bash
rm app/src/main/java/org/smm/archetype/shared/aspect/operationlog/BusinessLog.java
rm app/src/main/java/org/smm/archetype/shared/aspect/operationlog/OperationLogRecord.java
rm app/src/main/java/org/smm/archetype/shared/aspect/operationlog/OperationLogWriter.java
rm app/src/main/java/org/smm/archetype/shared/aspect/operationlog/OperationType.java
```

**注意**：删除前需确认这些文件的 `@Deprecated` 标记中的新位置已存在。具体来说，需确认 `common/operationlog/` 包下有对应的替代类：
- `BusinessLog` → `common/operationlog/BusinessLog.java`（或仍在旧位置但已不被引用）
- `OperationLogRecord` → `common/operationlog/OperationLogRecord.java`
- `OperationLogWriter` → `common/operationlog/OperationLogWriter.java`
- `OperationType` → `common/operationlog/OperationType.java`

如果 `common/operationlog/` 不存在这些类，则不能删除旧存根，需先确认迁移状态。

- [ ] **Step 7.3: 改造 MybatisPlusGenerator.java（多模块输出）**

```java
package org.smm.archetype.generated;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.baomidou.mybatisplus.generator.fill.Column;
import org.smm.archetype.entity.base.BaseDO;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

/**
 * MyBatis Plus 代码生成器。
 * 支持多模块输出：通过 --module 参数指定目标模块。
 * <p>
 * 使用示例：
 * <pre>
 * java org.smm.archetype.generated.MybatisPlusGenerator --module systemconfig
 * java org.smm.archetype.generated.MybatisPlusGenerator --module auth
 * java org.smm.archetype.generated.MybatisPlusGenerator --module operationlog
 * </pre>
 */
public class MybatisPlusGenerator {

    private static final String DATABASE_URL = System.getenv().getOrDefault("DB_URL",
            "jdbc:sqlite:./data/app.db");
    private static final String USERNAME = System.getenv().getOrDefault("DB_USERNAME", "");
    private static final String PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "");
    private static final String SOURCE_DIR = Paths.get(System.getProperty("user.dir")) + "/app/src/main/java";

    /**
     * 模块 → (parentPackage, tableName) 映射
     */
    private static final Map<String, ModuleConfig> MODULES = Map.of(
            "systemconfig", new ModuleConfig("org.smm.archetype.systemconfig.internal", new String[]{"system_config"}),
            "auth", new ModuleConfig("org.smm.archetype.auth.internal", new String[]{"user"}),
            "operationlog", new ModuleConfig("org.smm.archetype.operationlog.internal", new String[]{"operation_log"})
    );

    public static void main(String[] args) {
        String moduleName = args.length > 0 ? args[0].replace("--module=", "") : "systemconfig";

        if (moduleName.startsWith("--")) {
            moduleName = moduleName.substring(2);
        }

        ModuleConfig config = MODULES.get(moduleName);
        if (config == null) {
            System.err.println("未知模块: " + moduleName + "，可用模块: " + MODULES.keySet());
            return;
        }

        FastAutoGenerator.create(DATABASE_URL, USERNAME, PASSWORD)
                .globalConfig(builder -> builder
                        .author("CodeGenerator")
                        .outputDir(SOURCE_DIR)
                        .dateType(DateType.TIME_PACK)
                        .disableOpenDir())
                .packageConfig(builder -> builder
                        .parent(config.packageName)
                        .entity("entity")
                        .mapper("mapper")
                        .pathInfo(Collections.singletonMap(OutputFile.xml, "")))
                .strategyConfig(builder -> builder
                        .addInclude(config.tables)
                        .addTablePrefix("")
                        .entityBuilder()
                        .superClass(BaseDO.class)
                        .naming(NamingStrategy.underline_to_camel)
                        .columnNaming(NamingStrategy.underline_to_camel)
                        .enableLombok()
                        .enableTableFieldAnnotation()
                        .addSuperEntityColumns("id", "create_time", "update_time", "create_user", "update_user", "delete_time", "delete_user")
                        .formatFileName("%sDO")
                        .idType(IdType.ASSIGN_ID)
                        .addTableFills(
                                new Column("create_time", FieldFill.INSERT),
                                new Column("update_time", FieldFill.INSERT_UPDATE),
                                new Column("create_user", FieldFill.INSERT),
                                new Column("update_user", FieldFill.INSERT_UPDATE)
                        )
                        .enableFileOverride()
                        .mapperBuilder()
                        .superClass(com.baomidou.mybatisplus.core.mapper.BaseMapper.class)
                        .mapperAnnotation(org.apache.ibatis.annotations.Mapper.class)
                        .formatMapperFileName("%sMapper")
                        .enableFileOverride()
                        .controllerBuilder()
                        .disable())
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();

        System.out.println("代码生成完成！模块: " + moduleName + "，输出目录: " + SOURCE_DIR);
    }

    private record ModuleConfig(String packageName, String[] tables) {}
}
```

- [ ] **Step 7.4: 清理空目录**

```bash
rmdir app/src/main/java/org/smm/archetype/shared/aspect/operationlog 2>/dev/null || true
rmdir app/src/main/java/org/smm/archetype/shared/aspect/ratelimit 2>/dev/null || true
rmdir app/src/main/java/org/smm/archetype/shared/aspect/idempotent 2>/dev/null || true
rmdir app/src/main/java/org/smm/archetype/shared/aspect 2>/dev/null || true
rmdir app/src/main/java/org/smm/archetype/generated/entity 2>/dev/null || true
rmdir app/src/main/java/org/smm/archetype/generated/mapper 2>/dev/null || true
```

- [ ] **Step 7.5: 残留引用检查**

搜索所有源码和测试中是否还有旧路径引用：

```bash
# 搜索所有可能残留的旧 import
grep -r "org.smm.archetype.entity.user" app/ --include="*.java" || echo "OK: no references"
grep -r "org.smm.archetype.entity.operationlog" app/ --include="*.java" || echo "OK: no references"
grep -r "org.smm.archetype.repository.user" app/ --include="*.java" || echo "OK: no references"
grep -r "org.smm.archetype.repository.operationlog" app/ --include="*.java" || echo "OK: no references"
grep -r "org.smm.archetype.service.auth" app/ --include="*.java" || echo "OK: no references"
grep -r "org.smm.archetype.service.operationlog" app/ --include="*.java" || echo "OK: no references"
grep -r "org.smm.archetype.facade.operationlog" app/ --include="*.java" || echo "OK: no references"
grep -r "org.smm.archetype.controller.auth" app/ --include="*.java" || echo "OK: no references"
grep -r "org.smm.archetype.controller.operationlog" app/ --include="*.java" || echo "OK: no references"
grep -r "org.smm.archetype.generated.entity.UserDO" app/ --include="*.java" || echo "OK: no references"
grep -r "org.smm.archetype.generated.entity.OperationLogDO" app/ --include="*.java" || echo "OK: no references"
grep -r "org.smm.archetype.generated.mapper.UserMapper" app/ --include="*.java" || echo "OK: no references"
grep -r "org.smm.archetype.generated.mapper.OperationLogMapper" app/ --include="*.java" || echo "OK: no references"
grep -r "org.smm.archetype.shared.aspect.operationlog.BusinessLog" app/ --include="*.java" || echo "OK: no references"
grep -r "org.smm.archetype.shared.aspect.operationlog.OperationLogRecord" app/ --include="*.java" || echo "OK: no references"
grep -r "org.smm.archetype.shared.aspect.operationlog.OperationLogWriter" app/ --include="*.java" || echo "OK: no references"
grep -r "org.smm.archetype.shared.aspect.operationlog.OperationType" app/ --include="*.java" || echo "OK: no references"
grep -r "org.smm.archetype.shared.aspect.ratelimit" app/ --include="*.java" || echo "OK: no references"
grep -r "org.smm.archetype.shared.aspect.idempotent" app/ --include="*.java" || echo "OK: no references"
grep -r "org.smm.archetype.entity.base.BaseRequest" app/ --include="*.java" || echo "OK: no references"
grep -r "org.smm.archetype.entity.base.BasePageRequest" app/ --include="*.java" || echo "OK: no references"
```

如发现残留引用，逐一修复。

- [ ] **Step 7.6: 全量编译验证**

Run: `mvn clean compile -pl app`
Expected: BUILD SUCCESS

- [ ] **Step 7.7: 全量单元测试**

Run: `mvn test -Dtest="*UTest" -pl app`
Expected: 全部通过

- [ ] **Step 7.8: 全量集成测试**

Run: `mvn test -Dtest="*ITest" -pl app`
Expected: 全部通过

- [ ] **Step 7.9: Modulith 合规验证**

Run: `mvn test -Dtest="ModulithComplianceUTest" -pl app`
Expected: PASS

**注意**：如果 Modulith verify 失败，检查以下可能原因：
1. `@ApplicationModule` 注解的 `allowedDependencies` 配置是否正确
2. `internal/` 包中的类是否被其他模块直接引用
3. Spring Modulith 版本是否与 Spring Boot 4.x 兼容

- [ ] **Step 7.10: ArchUnit 合规验证**

Run: `mvn test -Dtest="*ComplianceUTest" -pl app`
Expected: 全部通过

- [ ] **Step 7.11: 全量测试 + 覆盖率**

Run: `mvn clean verify`
Expected: BUILD SUCCESS

- [ ] **Step 7.12: 应用启动验证**

Run: `mvn spring-boot:run -pl app`
Expected: 应用正常启动，无 Bean 冲突错误

---

## 总结

| Task | 文件数 | KEY 变更 | 风险 |
|------|--------|---------|------|
| 1 | 11 新源文件 | LoginFacade→AuthFacade 重命名 + UserConverter 提取 | 低（无注解，无冲突） |
| 2 | 12 新源文件 | IPage→PageResult 全链路 + toEntity→toModel | 低（无注解，无冲突） |
| 3 | 9 新源文件 | AOP 切面包迁移 | 低（无注解，无冲突） |
| 4 | 4 package-info + 1 测试 + pom 修改 | Spring Modulith 引入 | 中（版本兼容性） |
| 5 | 5 新测试文件 | IPage→PageResult 断言 + LoginFacade→AuthFacade | 低 |
| 6 | 删除 30+ 旧文件 + 添加注解 | 原子切换 | 高（Bean 冲突 + import 修复） |
| 7 | 删除 6 废弃文件 + Generator 改造 | 清理 | 低 |
