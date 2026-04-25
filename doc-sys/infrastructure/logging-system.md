# 日志体系 (Logging System)

> **职责**: 描述日志体系的架构和配置
> **轨道**: Contract
> **维护者**: AI

---

## 目录

- [概述](#概述)
- [公共 API 参考](#公共-api-参考)
  - [日志标记常量](#日志标记常量)
  - [采样过滤器](#采样过滤器)
  - [脱敏工具](#脱敏工具)
  - [慢查询拦截器](#慢查询拦截器)
  - [内存日志收集器（测试用）](#内存日志收集器测试用)
- [服务流程](#服务流程)
  - [日志输出流程](#日志输出流程)
  - [采样过滤流程](#采样过滤流程)
  - [慢查询检测流程](#慢查询检测流程)
  - [日志脱敏流程](#日志脱敏流程)
- [Appender 详解](#appender-详解)
- [日志配置](#日志配置)
  - [Profile 路由策略](#profile-路由策略)
  - [日志格式](#日志格式)
  - [日志级别策略](#日志级别策略)
- [依赖关系](#依赖关系)
- [相关文档](#相关文档)
- [变更历史](#变更历史)

---

## 概述

日志体系基于 **Logback** 构建，提供 8 个 Appender、3 个专用 Logger、4 个日志工具类，支持多环境 Profile 路由：

| 能力 | 实现 | 说明 |
|------|------|------|
| 控制台输出 | CONSOLE Appender | dev/test 环境开发调试 |
| 应用日志 | FILE + ASYNC_FILE | 异步写入，100MB 轮转，7 天保留 |
| JSON 结构化日志 | JSON_FILE + ASYNC_JSON_FILE | prod 环境 ELK 接入 |
| 会话日志 | CURRENT + ASYNC_CURRENT | 覆盖写，每次启动刷新 |
| 错误日志 | ERROR_FILE | ERROR 级别专用，60 天保留 |
| 警告日志 | WARN_FILE | WARN+ 级别，prod 环境，30 天保留 |
| 慢查询日志 | SLOW_QUERY_FILE | MyBatis 慢 SQL 专用，30 天保留 |
| 审计日志 | AUDIT_FILE | 审计专用，180 天保留 |
| 采样过滤 | SamplingTurboFilter | 按比例过滤非 ERROR 日志 |
| 日志脱敏 | SensitiveLogUtils | 敏感信息遮盖 |
| 慢查询拦截 | SlowQueryInterceptor | MyBatis SQL 超时检测 |

---

## 公共 API 参考

### 日志标记常量

```java
// LogMarkers.java
public static final Marker USER;      // 用户相关日志
public static final Marker SECURITY;  // 安全相关日志
public static final Marker AUDIT;     // 审计相关日志
public static final Marker SYSTEM;    // 系统相关日志
```

**使用方式**：
```java
private static final Logger log = LoggerFactory.getLogger(MyService.class);
log.info(LogMarkers.USER, "用户登录成功: userId={}", userId);
log.warn(LogMarkers.SECURITY, "登录失败尝试: ip={}", ip);
```

### 采样过滤器

```java
// SamplingTurboFilter.java — Logback TurboFilter
public FilterReply decide(Marker marker, Logger logger, Level level,
                          String format, Object[] params, Throwable t);
public void setSampleRate(double sampleRate);  // 0.0 ~ 1.0
```

| 参数 | 说明 |
|------|------|
| `sampleRate` | 采样率，1.0 = 全部通过，0.1 = 仅 10% 通过 |
| ERROR 日志 | 始终通过，不受采样率影响 |

**配置方式**（通过 LoggingProperties）：
```yaml
logging:
  sampling:
    enabled: true
    rate: 0.1  # 仅 10% 的非 ERROR 日志通过
```

### 脱敏工具

```java
// SensitiveLogUtils.java
public static String mask(String value);                // 默认 75% 遮盖
public static String mask(String value, double ratio);  // 自定义遮盖比例
```

**脱敏规则**：保留首尾各一部分字符，中间用 `*` 替代。

| 输入 | 输出（默认 75%） |
|------|-----------------|
| `"13812345678"` | `"138****5678"` |
| `"hello world"` | `"h****d"` |

### 慢查询拦截器

```java
// SlowQueryInterceptor.java — MyBatis Interceptor
public Object intercept(Invocation invocation);  // 拦截 query/update
public Object plugin(Object target);             // 包装目标对象
```

**配置参数**（通过 LoggingProperties）：

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `logging.slow-query.enabled` | `false` | 是否启用慢查询检测 |
| `logging.slow-query.threshold-ms` | `1000` | 慢查询阈值（毫秒） |

**日志输出**：
- Logger: `SLOW_QUERY`
- 级别: `WARN`
- 格式: 含 SQL 语句、执行时间、traceId

### 内存日志收集器（测试用）

```java
// MemoryLogAppender.java — Logback AppenderBase（仅测试使用）
// 线程安全的 synchronized list，用于在测试中捕获和断言日志输出
```

**使用方式**（测试代码中）：
```java
MemoryLogAppender appender = new MemoryLogAppender();
appender.start();
// ... 执行被测代码 ...
assertThat(appender.getMessages()).anyMatch(msg -> msg.contains("预期内容"));
```

---

## 服务流程

### 日志输出流程

```
业务代码 log.info(...)
  │
  ▼
SamplingTurboFilter（TurboFilter — 最先执行）
  ├─ level == ERROR → DECIDE_NEUTRAL（始终通过）
  └─ random() >= sampleRate → DECIDE_DENY（拒绝）
  └─ random() < sampleRate → DECIDE_NEUTRAL（通过）
  │
  ▼
Logger 层级判断（root logger 或专用 logger）
  ├─ AUDIT_LOGGER → AUDIT_FILE
  ├─ SLOW_QUERY  → SLOW_QUERY_FILE
  └─ root logger → 根据 Profile 路由
       ├─ dev/test → CONSOLE + ASYNC_FILE + ASYNC_CURRENT
       └─ prod    → ERROR_FILE + ASYNC_FILE + ASYNC_JSON_FILE
```

### 采样过滤流程

```
1. Logback 初始化时注册 SamplingTurboFilter
2. 每条日志在到达 Logger 之前先经过 TurboFilter.decide()
3. DECIDE_NEUTRAL → 继续后续 Logger 判断
4. DECIDE_DENY → 日志被丢弃，不写入任何 Appender
5. ERROR 级别 → 始终 DECIDE_NEUTRAL，不受采样率影响
```

**适用场景**：高并发接口的 INFO/DEBUG 日志降采样，减少日志量。

### 慢查询检测流程

```
MyBatis 执行 SQL
  │
  ▼
SlowQueryInterceptor.intercept()
  ├─ enabled == false → 直接执行
  └─ enabled == true ↓
       │
       ├─ 记录开始时间
       ├─ invocation.proceed() 执行 SQL
       ├─ 计算执行时间
       │
       ├─ executionTime >= threshold
       │     └─ SLOW_QUERY Logger.warn(SQL + executionTime + traceId)
       │
       └─ executionTime < threshold
             └─ 正常返回（不记录）
```

### 日志脱敏流程

```
敏感数据（手机号、身份证等）
  │
  ▼
SensitiveLogUtils.mask(value)
  ├─ 计算遮盖长度: totalLength * ratio
  ├─ 保留前缀: (1 - ratio) / 2 的字符
  ├─ 保留后缀: (1 - ratio) / 2 的字符
  └─ 中间用 * 替代
```

---

## Appender 详解

### Appender 总览

| Appender | 类型 | 输出文件 | 文件大小 | 保留天数 | 总大小上限 | Profile |
|----------|------|---------|---------|---------|-----------|---------|
| CONSOLE | ConsoleAppender | — | — | — | — | dev, test, default |
| FILE → ASYNC_FILE | RollingFileAppender | `app.log` | 100MB | 7 天 | 2GB | all |
| CURRENT → ASYNC_CURRENT | FileAppender | `current.log` | — | — | — | dev, test, default |
| ERROR_FILE | RollingFileAppender | `error.log` | 100MB | 60 天 | 3GB | all |
| WARN_FILE | RollingFileAppender | `warn.log` | 100MB | 30 天 | 2GB | prod |
| JSON_FILE → ASYNC_JSON_FILE | RollingFileAppender | `json.log` | 100MB | 7 天 | 2GB | prod |
| SLOW_QUERY_FILE | RollingFileAppender | `slow-query.log` | 100MB | 30 天 | 3GB | AUDIT_LOGGER |
| AUDIT_FILE | RollingFileAppender | `audit.log` | 100MB | 180 天 | 10GB | SLOW_QUERY |

### 异步 Appender 配置

所有异步 Appender 统一配置：
- `queueSize`: 1024（队列容量）
- `discardingThreshold`: 0（队列满时不丢弃 TRACE/DEBUG/INFO 级别日志）

### JSON 日志格式

prod 环境使用 LogstashEncoder 输出 JSON 格式日志：
```json
{
  "@timestamp": "2026-04-25T10:30:00.000Z",
  "app_name": "quickstart-light",
  "level": "INFO",
  "logger_name": "org.smm.archetype.service.MyService",
  "message": "处理请求成功",
  "traceId": "abc123...",
  "thread_name": "http-nio-9201-exec-1"
}
```

---

## 日志配置

### Profile 路由策略

| Profile | Root Logger 关联 Appender | 说明 |
|---------|-------------------------|------|
| `dev`, `test`, `default` | CONSOLE + ERROR_FILE + ASYNC_FILE + ASYNC_CURRENT | 开发调试友好 |
| `prod` | ERROR_FILE + ASYNC_FILE + ASYNC_JSON_FILE | 生产环境结构化日志 |

**设计考量**：
- prod 环境不输出 CONSOLE（避免容器标准输出堆积）
- prod 环境增加 JSON_FILE（ELK 采集）和 WARN_FILE（告警分析）
- CURRENT 在每次应用启动时覆盖写，适合查看最近一次请求日志

### 日志格式

| 变量 | 含义 |
|------|------|
| `%d{yyyy-MM-dd HH:mm:ss.SSS}` | 时间戳（毫秒精度） |
| `${springApplicationName}` | 应用名（从 Spring 配置读取） |
| `%thread` | 线程名 |
| `%X{traceId:--}` | MDC 中的 traceId（无则显示 `--`） |
| `%-5level` | 日志级别（左对齐 5 字符） |
| `%logger{36}` | Logger 名（最长 36 字符，缩写包名） |
| `%msg%xEx` | 日志消息 + 异常堆栈 |

**完整格式**：
```
2026-04-25 10:30:00.123 | quickstart-light | http-nio-9201-exec-1 | abc123def456 | INFO  | o.s.a.service.MyService | 处理请求成功
```

### 日志级别策略

| 环境 | Root 级别 | 项目包级别 | 说明 |
|------|----------|-----------|------|
| dev | INFO | DEBUG | 开发环境项目代码 DEBUG |
| test | INFO | DEBUG | 测试环境项目代码 DEBUG |
| prod | WARN | INFO | 生产环境仅 WARN+ 和项目代码 INFO+ |

**配置位置**：`application-{profile}.yaml` 中的 `logging.level.*`

---

## 依赖关系

### 内部依赖

```
shared.util.logging
  ├── LogMarkers → SLF4J Marker API
  ├── SamplingTurboFilter → Logback TurboFilter
  ├── SensitiveLogUtils → Hutool StrUtil
  └── SlowQueryInterceptor → MyBatis Interceptor + LoggingProperties

config/LoggingConfigure
  ├── 装配 LogAspect
  ├── 装配 SlowQueryInterceptor
  └── 装配 SamplingTurboFilter
```

### 外部依赖

| 框架 | 版本 | 用途 |
|------|------|------|
| Logback | Boot 4.x 管理 | 日志框架核心 |
| SLF4J | Boot 4.x 管理 | 日志门面 |
| Logstash Logback Encoder | 9.0 | JSON 日志输出 |
| Hutool Core | 5.8.44 | 字符串工具（脱敏） |

---

## 相关文档

| 文档 | 关系 | 说明 |
|------|------|------|
| 上下文传播机制 | infrastructure/context-propagation.md | traceId 传播到 MDC |
| AOP 切面 | infrastructure/aop-aspects.md | LogAspect 使用 SLF4J + traceId |
| 配置参考 | guides/configuration-reference.md | 日志相关配置项 |
| 部署与运维 | guides/deployment.md | 日志目录与文件管理 |

---

## 变更历史

| 版本 | 日期 | 变更内容 |
|------|------|---------|
| 1.0.0 | 2026-04-25 | 初始版本：8 Appender + 4 日志工具类 + Profile 路由 |
