# 配置规范

> 🔵 Constraint 轨 — 团队共识，文档驱动代码

## 📋 目录

- [规范目的](#规范目的)
- [规则](#规则)
- [常见违规场景](#常见违规场景)
- [检查清单](#检查清单)
- [相关文档](#相关文档)

## 规范目的

统一配置管理方式，使用类型安全的 `@ConfigurationProperties` 替代 `@Value`，确保配置可维护、可测试、有提示。

## 规则

### 规则 1: 禁止 @Value

**⛔ MUST**

配置注入必须使用 `@ConfigurationProperties`，禁止使用 `@Value` 注解。

**原因**：
- `@ConfigurationProperties` 提供类型安全、松绑定（relaxed binding）、JSR-303 校验
- `@ConfigurationProperties` 支持 IDE 自动补全（配合 `spring-boot-configuration-processor`）
- `@ConfigurationProperties` 易于单元测试（直接 new 对象即可）

✅ 正确：
```java
@Getter
@Setter
@ConfigurationProperties(prefix = "middleware.cache")
public class CacheProperties {
    private Integer initialCapacity = 1000;
    private Long maximumSize = 10000L;
    private Duration expireAfterWrite = Duration.ofDays(30);
}
```

❌ 错误：
```java
@Component
public class CacheService {
    @Value("${middleware.cache.maximum-size:10000}")  // 禁止！
    private Long maximumSize;
}
```

> **为什么**：`@Value` 将配置散落在各个类中，无法集中管理，修改配置时需要在整个代码库中搜索。`@ConfigurationProperties` 将所有相关配置集中到一个 Properties 类中，配合 `spring-boot-configuration-processor` 提供 IDE 自动补全和文档提示，且支持 JSR-303 校验（如 `@Min`、`@Max`），在应用启动时就能发现配置错误而非运行时才发现。此外，Properties 类可以直接 `new` 出来用于单元测试，而 `@Value` 必须启动 Spring 上下文才能注入。

### 规则 2: 配置前缀统一

**⛔ MUST**

所有客户端模块的配置前缀必须遵循 `middleware.*` 统一命名规范。日志模块使用 Spring Boot 惯例前缀 `logging`。

**客户端模块配置映射表**（从源码验证）：

| 客户端 | 配置前缀 | Properties 类 | 源码位置 |
|--------|----------|---------------|----------|
| 缓存 | `middleware.cache` | `CacheProperties` | `client-cache/...cache/CacheProperties.java` |
| 对象存储 | `middleware.object-storage` | `OssProperties` | `client-oss/...oss/OssProperties.java` |
| 邮件 | `middleware.email` | `EmailProperties` | `client-email/...email/EmailProperties.java` |
| 短信 | `middleware.sms` | `SmsProperties` | `client-sms/...sms/SmsProperties.java` |
| 搜索 | `middleware.search` | `SearchProperties` | `client-search/...search/SearchProperties.java` |
| 日志 | `logging` ⚠️ | `LoggingProperties` | `app/.../config/properties/LoggingProperties.java` |
| 限流 | `middleware.ratelimit` | `RateLimitProperties` | `app/.../config/properties/RateLimitProperties.java` |
| 认证 | `middleware.auth` | `AuthProperties` | `client-auth/...auth/AuthProperties.java` |

> ⚠️ **日志模块特殊**：使用 `logging` 前缀而非 `middleware.logging`，因为日志配置对接 Spring Boot 日志配置惯例（`logging.level`、`logging.file` 等）。

**应用级配置映射表**（从源码验证）：

| 配置域 | 配置前缀 | Properties 类 | 源码位置 |
|--------|----------|---------------|----------|
| 应用信息 | `app` | `AppInfoProperties` | `app/.../config/properties/AppInfoProperties.java` |
| 线程池 | `thread-pool` | `ThreadPoolProperties` | `app/.../config/properties/ThreadPoolProperties.java` |

> **注意**：应用级配置使用独立前缀（`app`、`thread-pool`），不纳入 `middleware.*` 命名空间，因为它们属于应用自身的基础设施配置而非中间件客户端配置。

**配置示例**：

```yaml
app:
  port: "9201"
  context-path: /quickstart-light
  app-name: quickstart-light
  openapi-url: /openapi-doc.html
  api-doc-url: /v3/api-docs

thread-pool:
  io-core-size: 4
  io-max-size: 8
  cpu-core-size: 3
  daemon-core-size: 2
  scheduler-pool-size: 2

middleware:
  cache:
    initial-capacity: 1000
    maximum-size: 10000
    expire-after-write: 30d
    expire-after-access: 30d
  object-storage:
    type: local
    local-storage-path: ./uploads
  email:
    host: smtp.example.com
    port: 587
    username: user@example.com
    password: secret
    ssl-enable: true
    from: noreply@example.com
  sms:
    provider: aliyun
    access-key-id: xxx
    access-key-secret: xxx
    sign-name: 签名
  search:
    default-page-size: 10
    max-page-size: 100
  ratelimit:
    enabled: true
    default-capacity: 10
    default-refill-tokens: 10
    default-refill-duration: 1
  auth:
    enabled: true
    exclude-paths:
      - /api/auth/login
      - /api/test/**

logging:
  slow-query:
    threshold-ms: 1000
    enabled: false
  sampling:
    enabled: false
    rate: 0.1
```

**新增客户端模块时**：
1. 创建 `XxxProperties` 类，使用 `@ConfigurationProperties(prefix = "middleware.xxx")`
2. 所有字段提供合理的默认值
3. 使用 `@Getter` + `@Setter`（禁止 `@Data`）
4. 配套编写 Properties 类的单元测试

> **为什么**：统一的 `middleware.*` 前缀使得所有客户端配置在 YAML 文件中自然聚合在一起，便于运维人员快速定位和批量管理。如果不统一（如有人用 `cache.*`，有人用 `my.cache.*`），配置文件会变得散乱，且无法通过前缀快速识别哪些配置属于中间件层。日志模块使用 `logging` 前缀是唯一例外，因为需要与 Spring Boot 内置的日志配置体系（如 `logging.level`、`logging.file`）无缝对接。

### 规则 3: 多环境配置

**⚠️ SHOULD**

项目支持多环境配置，通过 Spring Profile 切换：

| Profile | 配置文件 | 用途 |
|---------|----------|------|
| `dev` | `application-dev.yaml` | 开发环境（默认） |
| `prod` | `application-prod.yaml` | 生产环境 |
| `test` | `application-test.yaml` | 测试环境（ITest 使用） |
| — | `application-optional.yaml` | 可选配置（可选功能开关） |

**环境切换方式**：

```bash
# 开发环境启动（默认）
mvn spring-boot:run -pl app

# 生产环境启动
scripts/start.sh prod

# 测试环境（ITest 自动激活）
# IntegrationTestBase 已标注 @ActiveProfiles("test")
```

**配置优先级**（高 → 低）：
1. 命令行参数
2. 环境变量（`MIDDLEWARE_CACHE_MAXIMUM_SIZE`）
3. Profile 配置文件（`application-{profile}.yaml`）
4. 主配置文件（`application.yaml`）
5. Properties 类默认值

### 规则 4: JaCoCo 配置

**⚠️ SHOULD**

JaCoCo 覆盖率工具的配置分布如下：

| 配置项 | 位置 | 作用 |
|--------|------|------|
| `<jacoco.version>` | 根 POM `properties` | 版本统一管理 |
| `pluginManagement` | 根 POM | 插件配置继承 |
| `prepare-agent` + `report` | 根 POM `build/plugins` | 所有子模块自动继承 |
| `report-aggregate` + `check` | app 模块 `build/plugins` | 聚合报告 + 门禁检查 |

**关键配置说明**：

```xml
<!-- 根 POM：版本管理 -->
<properties>
    <jacoco.version>0.8.14</jacoco.version>
</properties>

<!-- app 模块：聚合报告 + 门禁 -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>report-aggregate</id>
            <phase>verify</phase>
            <goals><goal>report-aggregate</goal></goals>
        </execution>
        <execution>
            <id>check</id>
            <phase>verify</phase>
            <goals><goal>check</goal></goals>
            <configuration>
                <rules>
                    <rule>
                        <limits>
                            <limit>
                                <counter>INSTRUCTION</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.95</minimum>
                            </limit>
                            <limit>
                                <counter>BRANCH</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.90</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
                <haltOnFailure>false</haltOnFailure>  <!-- 当前仅警告，达标后改为 true -->
            </configuration>
        </execution>
    </executions>
</plugin>
```

**报告输出位置**：`app/target/site/jacoco-aggregate/index.html`

## 常见违规场景

### 场景 1: 使用 @Value 注入配置值

❌ 错误做法：
```java
@Component
public class EmailService {
    @Value("${middleware.email.host:smtp.example.com}")
    private String host;

    @Value("${middleware.email.port:587}")
    private Integer port;

    @Value("${middleware.email.ssl-enable:true}")
    private Boolean sslEnable;
    // 配置散落在 3 个 @Value 中，无法集中管理，无法做类型校验
}
```

✅ 正确做法：
```java
@Getter
@Setter
@ConfigurationProperties(prefix = "middleware.email")
public class EmailProperties {
    private String host = "smtp.example.com";
    private Integer port = 587;
    private Boolean sslEnable = true;

    @Min(1)
    @Max(65535)
    public void setPort(Integer port) {
        this.port = port;
    }
}
// 所有配置集中管理，支持 JSR-303 校验，IDE 有自动补全
```

> **后果**：`@Value` 散落在多个类中时，新增或修改配置需要在整个代码库中搜索，容易遗漏。且 `@Value` 默认是 `String` 类型，需要手动转换，转换失败只在运行时才暴露。`@ConfigurationProperties` 在应用启动时就完成绑定和校验，快速失败。

### 场景 2: 配置前缀不符合 middleware.* 惯例

❌ 错误做法：
```java
@ConfigurationProperties(prefix = "cache.config")         // 应为 middleware.cache
public class CacheProperties { }

@ConfigurationProperties(prefix = "myApp.objectStorage")    // 应为 middleware.object-storage
public class OssProperties { }

@ConfigurationProperties(prefix = "ratelimit")              // 应为 middleware.ratelimit
public class RateLimitProperties { }
```

✅ 正确做法：
```java
@ConfigurationProperties(prefix = "middleware.cache")
public class CacheProperties { }

@ConfigurationProperties(prefix = "middleware.object-storage")
public class OssProperties { }

@ConfigurationProperties(prefix = "middleware.ratelimit")
public class RateLimitProperties { }
```

> **后果**：不统一的配置前缀导致 YAML 文件中同一层的配置散落各处（`cache:`、`myApp:`、`ratelimit:`），运维人员无法通过前缀快速识别哪些配置属于中间件层。团队协作时新成员也无法从配置前缀推断出该配置的归属模块。

## 检查清单

- [ ] 没有使用 `@Value` 注解注入配置
- [ ] 所有配置类使用 `@ConfigurationProperties`，字段有合理默认值
- [ ] 配置前缀遵循 `middleware.*` 统一规范（日志模块除外，使用 `logging`）
- [ ] Properties 类使用 `@Getter` + `@Setter`，不使用 `@Data`
- [ ] 多环境配置文件齐全（dev / prod / test）
- [ ] 新增配置项在 Properties 类中有默认值
- [ ] JaCoCo 覆盖率报告可通过 `mvn clean verify` 生成

## 相关文档

- [Java 编码规范](./java-conventions.md) — Lombok / 编码风格
- [测试规范](./testing-conventions.md) — 覆盖率标准
- [AGENTS.md](../../AGENTS.md) — 技术栈版本与 JVM 配置
