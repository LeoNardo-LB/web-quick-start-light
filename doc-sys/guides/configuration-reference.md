# 配置参考 (Configuration Reference)

> **职责**: 提供所有配置项的完整参考
> **轨道**: Contract
> **维护者**: AI

---

## 目录

- [概述](#概述)
- [公共 API 参考](#公共-api-参考)
  - [应用基础配置](#应用基础配置)
  - [服务端口与路径](#服务端口与路径)
  - [线程池配置](#线程池配置)
  - [认证配置](#认证配置)
  - [日志配置](#日志配置)
  - [MyBatis-Plus 配置](#mybatis-plus-配置)
  - [可观测性配置](#可观测性配置)
  - [组件配置](#组件配置)
- [服务流程](#服务流程)
  - [配置加载顺序](#配置加载顺序)
  - [Profile 激活流程](#profile-激活流程)
- [配置参考](#配置参考)
  - [主配置 (application.yaml)](#主配置-applicationyaml)
  - [开发环境 (application-dev.yaml)](#开发环境-application-devyaml)
  - [生产环境 (application-prod.yaml)](#生产环境-application-prodyaml)
  - [组件配置 (application-component.yaml)](#组件配置-application-componentyaml)
  - [测试环境 (application-test.yaml)](#测试环境-application-testyaml)
- [依赖关系](#依赖关系)
- [相关文档](#相关文档)
- [变更历史](#变更历史)

---

## 概述

项目采用 Spring Boot 多 Profile 配置策略，所有配置文件位于 `app/src/main/resources/`：

| 文件 | 激活条件 | 职责 |
|------|---------|------|
| `application.yaml` | 始终加载 | 基础配置（端口、API 版本、线程池、Sa-Token、认证排除路径） |
| `application-dev.yaml` | `SPRING_PROFILES_ACTIVE=dev` | 开发环境（SQLite + SQL Init + MyBatis 日志 + OTLP Jaeger） |
| `application-prod.yaml` | `SPRING_PROFILES_ACTIVE=prod` | 生产环境（SQLite + 慢 SQL 监控） |
| `application-component.yaml` | 手动引入 | 组件配置模板（默认注释状态） |

**默认 Profile**：`dev`（通过 `${SPRING_PROFILES_ACTIVE:dev}` 控制）

---

## 公共 API 参考

### 应用基础配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `spring.application.name` | `quickstart-light` | 应用名称，影响日志输出、Context Path、Swagger |
| `spring.profiles.active` | `${SPRING_PROFILES_ACTIVE:dev}` | 激活的 Profile |
| `spring.mvc.apiversion.default` | `1.0` | 默认 API 版本 |
| `spring.mvc.apiversion.use.header` | `API-Version` | API 版本 Header 名称 |
| `spring.messages.basename` | `messages` | 国际化消息文件基础名 |
| `spring.messages.encoding` | `UTF-8` | 消息文件编码 |

### 服务端口与路径

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `server.port` | `9201` | 服务端口 |
| `server.servlet.context-path` | `/${spring.application.name}` | Servlet 上下文路径（默认 `/quickstart-light`） |

**完整访问路径**：`http://localhost:9201/quickstart-light/api/...`

### 线程池配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `thread-pool.io-core-size` | `4` | IO 密集型线程池核心线程数 |
| `thread-pool.io-max-size` | `8` | IO 密集型线程池最大线程数 |
| `thread-pool.cpu-core-size` | `3` | CPU 密集型线程池核心线程数 |
| `thread-pool.daemon-core-size` | `2` | 守护线程池核心线程数 |
| `thread-pool.scheduler-pool-size` | `2` | 调度线程池大小 |

### 认证配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `sa-token.token-name` | `Authorization` | Token Header 名称 |
| `sa-token.timeout` | `86400` | Token 超时时间（秒），默认 24 小时 |
| `sa-token.is-concurrent` | `true` | 是否允许同一账号并发登录 |
| `sa-token.is-share` | `true` | 是否允许同端多设备共享登录 |
| `sa-token.token-style` | `uuid` | Token 风格（uuid） |
| `sa-token.is-log` | `false` | 是否打印 Sa-Token 日志 |
| `component.auth.enabled` | `true` | 是否启用认证组件 |
| `component.auth.exclude-paths` | 见下表 | 认证排除路径列表 |

**默认排除路径**：

| 路径 | 说明 |
|------|------|
| `/api/auth/**` | 认证端点（登录/注销） |
| `/api/test/**` | 测试端点 |
| `/actuator/**` | Spring Actuator |
| `/swagger-ui/**` | Swagger UI |
| `/v3/api-docs/**` | OpenAPI 文档 |

### 日志配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `logging.config` | `classpath:logback-spring.xml` | Logback 配置文件 |
| `logging.level.root` | 见 Profile | 根 Logger 级别 |
| `logging.level.org.smm.archetype` | 见 Profile | 项目包 Logger 级别 |
| `logging.file.path` | `.logs` | 日志文件目录（Logback 中使用） |
| `logging.slow-query.enabled` | `false` (dev) / `true` (prod) | 慢查询检测开关 |
| `logging.slow-query.threshold-ms` | `1000` | 慢查询阈值（毫秒） |
| `logging.sampling.enabled` | `false` | 采样过滤开关 |
| `logging.sampling.rate` | `0.1` | 采样率 |

### MyBatis-Plus 配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `mybatis-plus.configuration.map-underscore-to-camel-case` | `true` | 下划线转驼峰 |
| `mybatis-plus.configuration.log-impl` | `StdOutImpl` (dev) / 无 (prod) | SQL 日志实现 |
| `mybatis-plus.global-config.db-config.id-type` | `assign_id` | 主键生成策略（雪花算法） |

### 可观测性配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `management.endpoints.web.exposure.include` | `health,info,prometheus,metrics` | Actuator 暴露端点 |
| `management.tracing.sampling.probability` | `1.0` | 链路追踪采样率 |
| `springdoc.show-actuator` | `true` | Swagger UI 显示 Actuator |
| `springdoc.api-docs.path` | `/v3/api-docs` | OpenAPI JSON 路径 |
| `springdoc.swagger-ui.path` | `/openapi-doc.html` | Swagger UI 路径 |

### 组件配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `component.cache.default-expire-seconds` | `3600` | 缓存默认过期时间（秒） |
| `component.cache.maximum-size` | `10000` | 缓存最大容量 |
| `component.oss.type` | `local` | 对象存储类型 |
| `component.oss.local-storage-path` | `./uploads` | 本地存储路径 |
| `component.email.host` | — | SMTP 服务器地址 |
| `component.email.port` | `587` | SMTP 端口 |
| `component.sms.provider` | `aliyun` | SMS 服务提供商 |

> 组件配置位于 `application-component.yaml`，默认全部注释，需手动取消注释并配置。

---

## 服务流程

### 配置加载顺序

```
1. application.yaml（基础配置 — 始终加载）
   │
2. application-{profile}.yaml（环境配置 — 按 SPRING_PROFILES_ACTIVE 加载）
   │   ├─ dev  → application-dev.yaml
   │   └─ prod → application-prod.yaml
   │
3. application-component.yaml（组件配置 — 需手动激活）
   │
4. 环境变量覆盖（SPRING_PROFILES_ACTIVE, DB_URL 等）
   │
5. JVM 参数覆盖（-Dspring.profiles.active, -Dserver.port 等）
```

**优先级**：JVM 参数 > 环境变量 > Profile 配置 > 主配置

### Profile 激活流程

```
启动命令
  │
  ├─ java -jar app.jar
  │     └─ 默认 SPRING_PROFILES_ACTIVE=dev
  │
  ├─ SPRING_PROFILES_ACTIVE=prod java -jar app.jar
  │     └─ 加载 application-prod.yaml
  │
  └─ ./start.sh prod
        └─ -Dspring.profiles.active=prod
```

---

## 配置参考

### 主配置 (application.yaml)

```yaml
spring:
  application:
    name: quickstart-light
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  mvc:
    apiversion:
      default: "1.0"
      use:
        header: "API-Version"
  messages:
    basename: messages
    encoding: UTF-8

server:
  port: 9201
  servlet:
    context-path: /${spring.application.name}

springdoc:
  show-actuator: true
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /openapi-doc.html

logging:
  config: classpath:logback-spring.xml

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  tracing:
    sampling:
      probability: 1.0

thread-pool:
  io-core-size: 4
  io-max-size: 8
  cpu-core-size: 3
  daemon-core-size: 2
  scheduler-pool-size: 2

sa-token:
  token-name: Authorization
  timeout: 86400
  is-concurrent: true
  is-share: true
  token-style: uuid
  is-log: false

component:
  auth:
    enabled: true
    exclude-paths:
      - /api/auth/**
      - /api/test/**
      - /actuator/**
      - /swagger-ui/**
      - /v3/api-docs/**
```

### 开发环境 (application-dev.yaml)

```yaml
spring:
  datasource:
    url: jdbc:sqlite:./data_sqlite/mydb.db
    driver-class-name: org.sqlite.JDBC
  sql:
    init:
      mode: always                    # 自动执行 schema.sql + init.sql
      schema-locations: classpath:schema.sql
      data-locations: classpath:init.sql
      continue-on-error: false

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl  # 控制台输出 SQL
  global-config:
    db-config:
      id-type: assign_id

logging:
  level:
    root: info
    org.smm.archetype: debug          # 项目代码 DEBUG 级别
  slow-query:
    enabled: false                    # 开发环境关闭慢查询
    threshold-ms: 1000
  sampling:
    enabled: false
    rate: 0.1

management:
  opentelemetry:
    tracing:
      export:
        otlp:
          endpoint: http://localhost:4318/v1/traces  # 本地 Jaeger
    logging:
      export:
        otlp:
          endpoint: http://localhost:4318/v1/logs
  otlp:
    metrics:
      export:
        url: http://localhost:4318/v1/metrics
```

### 生产环境 (application-prod.yaml)

```yaml
spring:
  datasource:
    url: jdbc:sqlite:./data_sqlite/mydb.db
    driver-class-name: org.sqlite.JDBC

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: assign_id

logging:
  level:
    root: warn                        # 生产环境根日志 WARN
    org.smm.archetype: info           # 项目代码 INFO
  slow-query:
    enabled: true                     # 生产环境启用慢查询
    threshold-ms: 1000
  sampling:
    enabled: false
    rate: 0.1
```

### 组件配置 (application-component.yaml)

```yaml
# Component configurations — 按需取消注释
# component:
#   cache:
#     default-expire-seconds: 3600
#     maximum-size: 10000
#   oss:
#     type: local
#     local-storage-path: ./uploads
#   email:
#     host: smtp.example.com
#     port: 587
#     username: ${EMAIL_USERNAME}
#     password: ${EMAIL_PASSWORD}
#     ssl-enable: true
#     from: noreply@example.com
#   sms:
#     provider: aliyun
#     access-key-id: ${SMS_ACCESS_KEY_ID}
#     access-key-secret: ${SMS_ACCESS_KEY_SECRET}
#     sign-name: MyApp
```

### 测试环境 (application-test.yaml)

| 配置项 | 值 | 说明 |
|--------|-----|------|
| 数据源 | 内存 SQLite（`:memory:`） | 测试隔离，不写入文件 |
| 端口 | 随机端口 | 避免端口冲突 |
| SQL Init | schema.sql + test_entity 表 | 测试专用表 |
| 排除自动配置 | MailSenderAutoConfiguration | 测试环境不需要邮件 |

---

## 依赖关系

### 配置文件依赖链

```
application.yaml (基础)
  ├── application-dev.yaml (开发环境)
  │     └── OTLP endpoint → localhost:4318 (Jaeger)
  ├── application-prod.yaml (生产环境)
  │     └── 慢查询 enabled=true
  └── application-component.yaml (组件 — 可选)
        └── cache / oss / email / sms 组件参数
```

### 配置绑定的 Properties 类

| Properties 类 | 配置前缀 | 说明 |
|--------------|---------|------|
| `AppInfoProperties` | `app.*` | 应用信息（启动 Banner 使用） |
| `LoggingProperties` | `logging.*` | 日志配置（采样率、慢查询） |
| `RateLimitProperties` | `ratelimit.*` | 限流全局配置 |

---

## 相关文档

| 文档 | 关系 | 说明 |
|------|------|------|
| 日志体系 | infrastructure/logging-system.md | Logback 配置详解 |
| 部署与运维 | guides/deployment.md | 生产环境 JVM 参数与环境变量 |
| 数据库 Schema | guides/database.md | 数据源配置与初始化 |
| 编码规范 | guides/coding-standards.md | 配置使用规范 |

---

## 变更历史

| 版本 | 日期 | 变更内容 |
|------|------|---------|
| 1.0.0 | 2026-04-25 | 初始版本：4 个配置文件的完整参考 |
