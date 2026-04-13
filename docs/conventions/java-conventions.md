# Java 编码规范

> 🔵 Constraint 轨 — 团队共识，文档驱动代码

## 📋 目录

- [规范目的](#规范目的)
- [规则](#规则)
- [常见违规场景](#常见违规场景)
- [检查清单](#检查清单)
- [相关文档](#相关文档)

## 规范目的

统一 Java 编码风格，确保代码一致性、可读性和可维护性，减少因风格差异导致的协作摩擦。

## 规则

### 规则 1: Lombok 注解规范

**⛔ MUST**

禁止使用 `@Data` 注解，因其隐式包含 `@EqualsAndHashCode` 和 `@ToString`，可能导致集合操作意外行为（如 HashSet 重复、循环引用栈溢出等）。

允许使用的 Lombok 注解：

| 注解 | 用途 |
|------|------|
| `@Builder` | 构建者模式 |
| `@RequiredArgsConstructor` | 必填参数构造器（配合 `final` 字段） |
| `@Getter` / `@Setter` | 显式控制字段访问 |
| `@Slf4j` | 日志声明 |
| `@AllArgsConstructor` | 全参构造器 |
| `@NoArgsConstructor` | 无参构造器 |

禁止使用的 Lombok 注解：

| 注解 | 原因 |
|------|------|
| `@Data` | 隐含 `@EqualsAndHashCode` + `@ToString`，行为不可控 |
| `@Value` | 不可变语义与项目 Entity 体系冲突 |
| `@With` | 隐式生成 with 方法，语义不明确 |

✅ 正确：
```java
@Builder
@RequiredArgsConstructor
@Getter
@Setter
public class SystemConfig {
    private Long id;
    private String configKey;
    private String configValue;
}
```

❌ 错误：
```java
@Data  // 禁止！隐含 @EqualsAndHashCode / @ToString
public class SystemConfig {
    private Long id;
    private String configKey;
    private String configValue;
}
```

> **为什么**：`@Data` 隐含的 `@EqualsAndHashCode` 仅使用当前类字段生成 `equals/hashCode`，在继承链中会忽略父类字段，导致 `HashSet` 去重失效、`HashMap` 查找失败等隐蔽 bug；`@ToString` 在双向关联实体间会触发循环引用导致 `StackOverflowError`。显式使用 `@Getter` + `@Setter` 虽然多一些代码，但行为完全可控，避免了这些运行时陷阱。

### 规则 2: 时间类型规范

**⛔ MUST**

所有时间存储与传输统一使用 `java.time.Instant`，禁止使用 `LocalDateTime` 和 `Long`（时间戳毫秒）作为时间字段类型。

- 数据库中 `Instant` 存储为 TEXT（ISO 8601 格式），由 MyBatis-Plus 自动转换
- API 响应中的时间字段统一使用 ISO 8601 字符串格式

✅ 正确：
```java
public class BaseDO {
    private Instant createdAt;
    private Instant updatedAt;
}
```

❌ 错误：
```java
public class BaseDO {
    private LocalDateTime createdAt;  // 禁止
    private Long updatedAt;           // 禁止
}
```

> **为什么**：`LocalDateTime` 不携带时区信息，在不同时区的服务器间传输时会产生歧义（同一时刻在不同时区表现为不同的 `LocalDateTime`）；`Long` 时间戳虽然精确但可读性差，且容易混淆秒级与毫秒级精度。`Instant` 是 UTC 绝对时间点，配合 ISO 8601 序列化格式（如 `2025-01-15T08:30:00Z`）既有时区确定性又有良好的可读性。

### 规则 3: Record 规范

**⚠️ SHOULD**

根据场景选择 `record` 或 `class`：

| 场景 | 类型 | 示例 |
|------|------|------|
| DTO / VO / Result | `record` | `SystemConfigVO`、`UpdateConfigCommand` |
| 值对象 | `record` + `of()` 工厂方法 | `ConfigKey`、`ConfigValue` |
| 有继承链的基类 | `class` | `BaseResult`、`BaseRequest`、`BaseDO` |

✅ 正确：
```java
// DTO/VO 用 record
public record SystemConfigVO(
    String configKey,
    String configValue,
    String displayName
) {}

// 值对象用 record + of()
public record ConfigKey(String value) {
    public static ConfigKey of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("configKey 不能为空");
        }
        return new ConfigKey(value.trim());
    }
}

// 有继承链的基类保持 class
public abstract class BaseRequest {
    // ...
}
```

❌ 错误：
```java
// 基类用 record（record 不能被继承）
public record BaseRequest() {}  // 错误！
```

### 规则 4: 命名规范

**⚠️ SHOULD**

#### 测试命名

| 类型 | 后缀 | 基类 | 运行环境 |
|------|------|------|----------|
| 单元测试 | `*UTest` | `UnitTestBase`（Mockito） | 纯内存 |
| 集成测试 | `*ITest` | `IntegrationTestBase`（Spring 上下文） | 含 SQLite |

#### 包名分层

| 层 | 包路径 |
|----|--------|
| Controller | `...controller` |
| Facade | `...facade` |
| Service | `...service` |
| Repository | `...repository` |
| Entity | `...entity` |
| Config | `...config` |
| Util | `...util` |

#### 类名约定

| 类型 | 后缀 | 示例 |
|------|------|------|
| 配置属性 | `Properties` | `CacheProperties` |
| 枚举 | 无后缀（大驼峰） | `OperationType`、`ConfigGroup` |
| 异常 | `Exception` | `BizException` |
| 工具类 | `Utils` | `ScopedThreadContext` |

### 规则 5: 依赖约束

**⛔ MUST**

| 类别 | 允许 | 禁止 |
|------|------|------|
| ORM | MyBatis-Plus | JPA / Hibernate |
| 工具库 | Hutool / Apache Commons / Guava | 自行实现已有轮子 |
| 对象转换 | MapStruct | BeanUtils.copyProperties（编译期不安全） |
| 序列化 | Jackson（JSON）、Kryo（二进制） | - |

✅ 正确：
```java
// 使用 MapStruct 转换
@Mapper(componentModel = "spring")
public interface SystemConfigConverter {
    SystemConfig toEntity(SystemConfigDO dto);
}
```

❌ 错误：
```java
// 禁止使用 JPA 注解
@Entity  // 禁止
@Table(name = "system_config")  // 禁止
public class SystemConfig {}
```

> **为什么**：混用 JPA 和 MyBatis-Plus 会导致事务管理混乱（JPA 使用 EntityManager，MyBatis-Plus 使用 SqlSession），且 JPA 的懒加载代理与 MyBatis-Plus 的实体模型不兼容，容易引发 `LazyInitializationException`。统一使用 MyBatis-Plus 可以避免两套 ORM 机制的冲突，降低维护成本。

### 规则 6: API 路径规范

**⚠️ SHOULD**

- 所有控制器使用 `/api` 路径前缀
- API 版本通过 HTTP Header `API-Version` 控制，不在 URL 路径中嵌入版本号
- 配置：`spring.mvc.apiversion.use-header: "API-Version"`

✅ 正确：
```java
@RestController
@RequestMapping("/api/system/configs")
public class SystemConfigController {
    // GET /api/system/configs
    // GET /api/system/configs/page
}
```

❌ 错误：
```java
@RestController
@RequestMapping("/system/configs")  // 缺少 /api 前缀
public class SystemConfigController {}

// URL 中嵌入版本号
@RequestMapping("/api/v1/system/configs")  // 版本应通过 Header 控制
```

### 规则 7: 日志规范

**⛔ MUST**

- 使用 `@Slf4j` 声明日志对象
- 使用参数化日志（`{}` 占位符），禁止字符串拼接
- 禁止 `System.out.println` / `System.err.println`
- 业务方法日志使用 `@BusinessLog` 注解（`org.smm.archetype.client.log.BusinessLog`）

✅ 正确：
```java
@Slf4j
@Service
public class SystemConfigService {
    public void updateConfig(String key, String value) {
        log.info("更新配置: key={}, value={}", key, value);
    }
}
```

❌ 错误：
```java
@Service
public class SystemConfigService {
    public void updateConfig(String key, String value) {
        System.out.println("更新配置: " + key);  // 禁止
    }

    public void queryConfig(String key) {
        log.info("查询配置: key=" + key);  // 禁止字符串拼接
    }
}
```

> **为什么**：`System.out.println` 输出到标准输出，不受日志框架管控，无法按级别过滤、无法输出到文件、无法与日志聚合系统（如 ELK）对接。字符串拼接（`"key=" + key`）在日志级别未启用时仍会执行拼接操作，产生不必要的对象分配；而参数化日志（`log.info("key={}", key)`）在对应级别未启用时不会拼接字符串，性能更优。

### 规则 8: 代码生成器规范

**⛔ MUST**

- 代码生成器生成的代码位于 `app/.../generated/` 包下
- 生成的文件头部有注释标注，**禁止手动修改**
- 如需修改生成逻辑，修改 `MybatisPlusGenerator.java` 后重新生成

生成的代码包括：
- `SystemConfigDO` / `SystemConfigMapper`
- `OperationLogDO` / `OperationLogMapper`
- `UserDO` / `UserMapper`

✅ 正确：
```java
// generated/ 包下的文件 — 禁止手动修改
// 由 MybatisPlusGenerator 自动生成，任何手动修改都会被覆盖
package org.smm.archetype.generated;
```

❌ 错误：
```java
// 手动修改 generated/ 包下的 DO 或 Mapper 文件  // 禁止！
```

> **为什么**：`generated/` 包下的代码由 `MybatisPlusGenerator` 根据数据库表结构自动生成，每次重新生成时会覆盖整个文件。手动修改会在下次生成时丢失，且与代码审查流程脱节。如需调整生成逻辑，应修改 Generator 配置后重新生成，确保代码始终与数据库结构一致。

## 常见违规场景

### 场景 1: Controller 直接注入 Mapper（层次穿透）

❌ 错误做法：
```java
@RestController
@RequestMapping("/api/system/configs")
public class SystemConfigController {

    @Autowired
    private SystemConfigMapper systemConfigMapper;  // 禁止！Controller 不应直接依赖 Repository 层

    @GetMapping("/{key}")
    public BaseResult<SystemConfig> getByKey(@PathVariable String key) {
        return BaseResult.success(systemConfigMapper.selectByKey(key));
    }
}
```

✅ 正确做法：
```java
@RestController
@RequestMapping("/api/system/configs")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigFacade systemConfigFacade;  // Controller → Facade

    @GetMapping("/{key}")
    public BaseResult<SystemConfigVO> getByKey(@PathVariable String key) {
        return BaseResult.success(systemConfigFacade.getByKey(key));
    }
}
```

> **后果**：Controller 直接依赖 Mapper 绕过了 Facade 的 Entity→VO 转换，导致内部数据结构（如 Entity 含敏感字段）直接暴露给前端，且 ArchUnit 规则会阻止编译。

### 场景 2: 使用 @Data 注解 Entity 导致 Lombok 陷阱

❌ 错误做法：
```java
@Data
public class BaseDO {
    private Instant createdAt;
    private Instant updatedAt;
}

@Data
@EqualsAndHashCode(callSuper = true)
public class SystemConfig extends BaseDO {
    private String configKey;
}
// 问题：如果另一个类也继承 BaseDO 且有相同 createdAt/updatedAt，
// 放入 HashSet 后 equals/hashCode 行为不可预期
```

✅ 正确做法：
```java
@Builder
@RequiredArgsConstructor
@Getter
@Setter
public class BaseDO {
    private Instant createdAt;
    private Instant updatedAt;
}

@Builder
@RequiredArgsConstructor
@Getter
@Setter
public class SystemConfig extends BaseDO {
    private String configKey;
}
// equals/hashCode 由开发者显式控制，避免隐式行为
```

### 场景 3: 使用 LocalDateTime 导致时区问题

❌ 错误做法：
```java
// 服务部署在北京时间服务器（UTC+8）
public class OrderDO {
    private LocalDateTime createdAt;  // 存储为 "2025-01-15T16:30:00"
}
// 同一时刻在 UTC 服务器上存储为 "2025-01-15T08:30:00"
// 数据库中同一时刻出现两种不同的值！
```

✅ 正确做法：
```java
public class OrderDO {
    private Instant createdAt;  // 存储为 "2025-01-15T08:30:00Z"（UTC 绝对时间）
}
// 无论服务器在哪个时区，存储的值始终一致
```

## 检查清单

- [ ] 没有使用 `@Data` 注解
- [ ] 时间字段统一使用 `Instant`，没有 `LocalDateTime` 或 `Long` 时间戳
- [ ] DTO/VO 使用 `record`，基类保持 `class`
- [ ] 单元测试命名为 `*UTest`，集成测试命名为 `*ITest`
- [ ] 没有使用 JPA / Hibernate 注解
- [ ] 对象转换使用 MapStruct
- [ ] API 路径以 `/api` 开头
- [ ] 使用 `@Slf4j` + 参数化日志，没有 `System.out.println`
- [ ] `generated/` 包下的文件未被手动修改

## 相关文档

- [系统全景](../architecture/system-overview.md) — 四层架构说明
- [测试规范](./testing-conventions.md) — 测试分类与命名
- [错误处理规范](./error-handling.md) — 异常体系说明
- [AGENTS.md](../../AGENTS.md) — 项目技术栈与结构索引
