# 数据库 Schema (Database Schema)

> **职责**: 描述数据库 Schema、种子数据和迁移策略
> **轨道**: Contract
> **维护者**: AI

---

## 目录

- [概述](#概述)
- [公共 API 参考](#公共-api-参考)
  - [表清单](#表清单)
  - [公共字段说明](#公共字段说明)
- [服务流程](#服务流程)
  - [数据库初始化流程](#数据库初始化流程)
  - [代码生成流程](#代码生成流程)
- [Schema 定义](#schema-定义)
  - [system_config 表](#system_config-表)
  - [user 表](#user-表)
  - [operation_log 表](#operation_log-表)
- [种子数据](#种子数据)
  - [系统配置数据](#系统配置数据)
  - [默认管理员账户](#默认管理员账户)
- [索引策略](#索引策略)
- [类型映射](#类型映射)
- [依赖关系](#依赖关系)
- [相关文档](#相关文档)
- [变更历史](#变更历史)

---

## 概述

项目使用 **SQLite** 嵌入式数据库，包含 3 张核心表：

| 表名 | 说明 | 记录数 | 关键特性 |
|------|------|:------:|---------|
| `system_config` | 系统配置表 | 15 | `config_key` 唯一索引，4 个分组 |
| `user` | 用户表 | 1 | `username` 唯一索引，BCrypt 密码 |
| `operation_log` | 操作日志表 | 动态增长 | 按 `module`/`operation_type` 过滤 |

### 双 Schema 策略

| 文件 | 方言 | 用途 |
|------|------|------|
| `schema.sql` | SQLite DDL | 运行时使用（dev/test 环境自动执行） |
| `schema-template.sql` | MySQL DDL 模板 | 新表创建规范模板（含注释和索引规范） |

### 数据库特性

| 特性 | 实现方式 |
|------|---------|
| 主键生成 | MyBatis-Plus `assign_id`（雪花算法） |
| 时间字段 | `datetime('now')` 自动填充 |
| 逻辑删除 | `delete_time` (INTEGER) + `delete_user` (TEXT) |
| 审计字段 | `create_time`/`update_time`/`create_user`/`update_user` 自动填充 |
| Instant 兼容 | `InstantTypeHandler` 支持 ISO-8601 / SQLite datetime / Epoch 三种格式 |

---

## 公共 API 参考

### 表清单

| 表名 | 用途 | 业务模块 |
|------|------|---------|
| `system_config` | 系统配置管理（键值对存储） | 系统管理 |
| `user` | 用户认证与信息 | 认证模块 |
| `operation_log` | 业务操作日志记录 | 日志模块 |

### 公共字段说明

所有表共享以下基础字段（继承自 `BaseDO`）：

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `id` | `INTEGER` | AUTOINCREMENT | 主键 |
| `create_time` | `TEXT` | `datetime('now')` | 创建时间 |
| `update_time` | `TEXT` | `datetime('now')` | 更新时间 |
| `create_user` | `TEXT` | `''` | 创建人（自动从 BizContext 获取） |
| `update_user` | `TEXT` | `''` | 更新人（自动从 BizContext 获取） |
| `delete_time` | `INTEGER` | `0` | 逻辑删除时间（0=未删除） |
| `delete_user` | `TEXT` | `NULL` | 逻辑删除操作人 |

**自动填充机制**：

| 字段 | 填充时机 | 填充来源 |
|------|---------|---------|
| `create_time` | INSERT | SQLite `datetime('now')` |
| `update_time` | INSERT / UPDATE | SQLite `datetime('now')` |
| `create_user` | INSERT | `BizContext.getUserId()`，为空时 `"system"` |
| `update_user` | UPDATE | `BizContext.getUserId()`，为空时 `"system"` |

---

## 服务流程

### 数据库初始化流程

```
应用启动
  │
  ▼
Spring SQL Init 检测
  ├─ spring.sql.init.mode = always (dev/test)
  └─ spring.sql.init.mode = never (prod)
  │
  ▼ (dev/test 环境)
  ├── 1. 执行 schema.sql
  │     ├─ CREATE TABLE IF NOT EXISTS system_config
  │     ├─ CREATE TABLE IF NOT EXISTS user
  │     └─ CREATE TABLE IF NOT EXISTS operation_log
  │
  └── 2. 执行 init.sql
        ├─ INSERT OR IGNORE INTO system_config (15 条)
        └─ INSERT OR IGNORE INTO user (1 条)
```

**关键配置**：
```yaml
spring:
  sql:
    init:
      mode: always                  # dev/test 自动初始化
      schema-locations: classpath:schema.sql
      data-locations: classpath:init.sql
      continue-on-error: false      # SQL 执行失败则启动失败
```

### 代码生成流程

```
MybatisPlusGenerator.main()
  │
  ├── 1. 读取数据库连接配置
  │     ├─ DB_URL 环境变量（默认 SQLite）
  │     ├─ DB_USERNAME 环境变量
  │     └─ DB_PASSWORD 环境变量
  │
  ├── 2. 配置代码生成策略
  │     ├─ Entity 继承 BaseDO
  │     ├─ 命名策略：underline_to_camel
  │     └─ 自动填充字段：create_time, update_time, create_user, update_user
  │
  └── 3. 生成 Entity + Mapper
        ├─ generated/entity/{Table}DO.java
        └── generated/mapper/{Table}Mapper.java
```

> **注意**：`generated` 包下文件禁止手动修改，修改会在下次生成时被覆盖。

---

## Schema 定义

### system_config 表

```sql
CREATE TABLE IF NOT EXISTS system_config (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    config_key      TEXT    NOT NULL,          -- 配置键（唯一）
    config_value    TEXT    NOT NULL DEFAULT '', -- 配置值
    value_type      TEXT    NOT NULL DEFAULT 'STRING', -- 值类型
    group_code      TEXT    NOT NULL,          -- 分组编码
    display_name    TEXT    NOT NULL,          -- 显示名称
    description     TEXT    DEFAULT '',        -- 描述
    input_type      TEXT    NOT NULL DEFAULT 'TEXT', -- 输入类型
    input_config    TEXT    DEFAULT '',        -- 输入配置（JSON）
    sort            INTEGER NOT NULL DEFAULT 0, -- 排序
    create_time     TEXT    DEFAULT (datetime('now')),
    update_time     TEXT    DEFAULT (datetime('now')),
    create_user     TEXT    DEFAULT '',
    update_user     TEXT    DEFAULT '',
    delete_time     INTEGER NOT NULL DEFAULT 0,
    delete_user     TEXT    DEFAULT NULL,
    UNIQUE(config_key)
);
```

**字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `config_key` | TEXT (UK) | 配置键，如 `site.name`、`email.enabled` |
| `config_value` | TEXT | 配置值，如 `QuickStart Light`、`true` |
| `value_type` | TEXT | 值类型：`STRING`、`INTEGER`、`BOOLEAN`、`ENUM` |
| `group_code` | TEXT | 分组编码：`BASIC`、`EMAIL`、`STORAGE`、`SECURITY` |
| `display_name` | TEXT | 前端显示名称 |
| `input_type` | TEXT | 输入控件：`TEXT`、`TEXTAREA`、`NUMBER`、`SWITCH`、`SELECT` |
| `input_config` | TEXT | 输入配置 JSON，如 `{"min":1,"max":100}` |

### user 表

```sql
CREATE TABLE IF NOT EXISTS user (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    username        TEXT    NOT NULL,          -- 用户名（唯一）
    password_hash   TEXT    NOT NULL,          -- BCrypt 密码哈希
    nickname        TEXT    NOT NULL DEFAULT '', -- 昵称
    status          TEXT    NOT NULL DEFAULT 'ACTIVE', -- 状态
    create_time     TEXT    DEFAULT (datetime('now')),
    update_time     TEXT    DEFAULT (datetime('now')),
    create_user     TEXT    DEFAULT '',
    update_user     TEXT    DEFAULT '',
    delete_time     INTEGER NOT NULL DEFAULT 0,
    delete_user     TEXT    DEFAULT NULL,
    UNIQUE(username)
);
```

**字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `username` | TEXT (UK) | 用户名，登录凭证 |
| `password_hash` | TEXT | BCrypt 哈希（Spring Security Crypto 生成） |
| `nickname` | TEXT | 显示昵称 |
| `status` | TEXT | 账户状态：`ACTIVE`、`LOCKED`、`DISABLED` |

### operation_log 表

```sql
CREATE TABLE IF NOT EXISTS operation_log (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    trace_id        TEXT    NOT NULL DEFAULT '',  -- 链路追踪 ID
    user_id         TEXT    NOT NULL DEFAULT '',  -- 操作人 ID
    module          TEXT    NOT NULL DEFAULT '',  -- 所属模块
    operation_type  TEXT    NOT NULL DEFAULT '',  -- 操作类型
    description     TEXT    NOT NULL DEFAULT '',  -- 操作描述
    method          TEXT    NOT NULL DEFAULT '',  -- 方法签名
    params          TEXT    DEFAULT '',           -- 请求参数（JSON）
    result          TEXT    DEFAULT '',           -- 返回结果（JSON）
    execution_time  INTEGER NOT NULL DEFAULT 0,   -- 执行时间（ms）
    ip              TEXT    NOT NULL DEFAULT '',  -- 客户端 IP
    status          TEXT    NOT NULL DEFAULT 'SUCCESS', -- 执行状态
    error_message   TEXT    DEFAULT '',           -- 错误消息
    create_time     TEXT    DEFAULT (datetime('now')),
    update_time     TEXT    DEFAULT (datetime('now')),
    create_user     TEXT    DEFAULT '',
    update_user     TEXT    DEFAULT '',
    delete_time     INTEGER NOT NULL DEFAULT 0,
    delete_user     TEXT    DEFAULT NULL
);
```

**字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `trace_id` | TEXT | OTel traceId，由 LogAspect 从 Span 获取 |
| `user_id` | TEXT | 操作人 ID，由 LogAspect 从 BizContext 获取 |
| `module` | TEXT | 模块名，来自 `@BusinessLog(module = "...")` |
| `operation_type` | TEXT | 操作类型：`CREATE`、`UPDATE`、`DELETE`、`QUERY`、`EXPORT`、`IMPORT` |
| `execution_time` | INTEGER | 方法执行耗时（毫秒） |
| `status` | TEXT | 执行状态：`SUCCESS`、`ERROR` |

---

## 种子数据

### 系统配置数据

| 分组 | 配置键 | 值 | 值类型 | 输入类型 | 排序 |
|------|--------|-----|--------|---------|:----:|
| **BASIC** | `site.name` | `QuickStart Light` | STRING | TEXT | 1 |
| | `site.description` | `A lightweight web application template` | STRING | TEXTAREA | 2 |
| | `site.logo` | `` | STRING | TEXT | 3 |
| | `site.max_upload_size` | `10` | INTEGER | NUMBER | 4 |
| **EMAIL** | `email.enabled` | `false` | BOOLEAN | SWITCH | 1 |
| | `email.from_address` | `noreply@example.com` | STRING | TEXT | 2 |
| | `email.from_alias` | `QuickStart` | STRING | TEXT | 3 |
| | `email.subject_prefix` | `[QuickStart]` | STRING | TEXT | 4 |
| **STORAGE** | `storage.type` | `local` | ENUM | SELECT | 1 |
| | `storage.local.path` | `./uploads` | STRING | TEXT | 2 |
| | `storage.url_prefix` | `/files` | STRING | TEXT | 3 |
| **SECURITY** | `security.login_max_attempts` | `5` | INTEGER | NUMBER | 1 |
| | `security.lock_duration_minutes` | `30` | INTEGER | NUMBER | 2 |
| | `security.password_min_length` | `8` | INTEGER | NUMBER | 3 |
| | `security.enable_captcha` | `true` | BOOLEAN | SWITCH | 4 |

### 默认管理员账户

| 字段 | 值 | 说明 |
|------|-----|------|
| `username` | `admin` | 默认管理员用户名 |
| `password` | `admin123` | 默认密码 |
| `password_hash` | `$2a$10$b9P25YT02WoXL1HpGlcwUeD0E19TUIE6yq.V7QK5iMcWnSSw/.9.a` | BCrypt 哈希 |
| `nickname` | `系统管理员` | 显示昵称 |
| `status` | `ACTIVE` | 账户状态 |

> ⚠️ **安全提示**：生产环境必须修改默认管理员密码。

---

## 索引策略

### SQLite 索引

| 表 | 索引 | 类型 | 说明 |
|----|------|------|------|
| `system_config` | `config_key` | UNIQUE | 配置键唯一约束 |
| `user` | `username` | UNIQUE | 用户名唯一约束 |

### 查询优化

| 查询场景 | 过滤字段 | 索引建议 |
|---------|---------|---------|
| 操作日志按模块查询 | `module` | 可添加 `CREATE INDEX idx_ol_module ON operation_log(module)` |
| 操作日志按操作类型查询 | `operation_type` | 可添加 `CREATE INDEX idx_ol_type ON operation_log(operation_type)` |
| 操作日志按时间范围查询 | `create_time` | 可添加 `CREATE INDEX idx_ol_time ON operation_log(create_time)` |
| 配置按分组查询 | `group_code` | 可添加 `CREATE INDEX idx_sc_group ON system_config(group_code)` |

> 当前索引策略以 UNIQUE 约束为主，具体复合索引根据实际查询性能添加。

---

## 类型映射

### Java ↔ SQLite 类型映射

| Java 类型 | SQLite 类型 | 类型处理器 | 说明 |
|-----------|------------|-----------|------|
| `Long` (id) | `INTEGER` | MyBatis-Plus 内置 | 雪花算法 ID |
| `String` | `TEXT` | 默认 | 字符串 |
| `Integer` | `INTEGER` | 默认 | 数值 |
| `Instant` | `TEXT` | `InstantTypeHandler` | ISO-8601 / SQLite datetime / Epoch |
| `Boolean` | `TEXT` | 默认 | `"true"`/`"false"` |

### InstantTypeHandler 兼容格式

| 格式 | 示例 | 来源 |
|------|------|------|
| ISO-8601 | `2025-01-15T08:00:00Z` | Java 端写入 |
| SQLite datetime | `2025-01-15 08:00:00` | 数据库默认值 |
| Epoch 毫秒 | `1735898400000` | 旧数据兼容 |

---

## 依赖关系

### ORM 层依赖

```
Entity (POJO)
  ├── SystemConfig → ConfigKey, DisplayName, ConfigValue (值对象)
  ├── User → username, passwordHash, nickname, status
  └── OperationLog → traceId, userId, module, ...

DO (Data Object — 生成)
  ├── SystemConfigDO → BaseDO (id, createTime, ...)
  ├── UserDO → BaseDO
  └── OperationLogDO → BaseDO

Mapper (生成)
  ├── SystemConfigMapper extends BaseMapper<SystemConfigDO>
  ├── UserMapper extends BaseMapper<UserDO>
  └── OperationLogMapper extends BaseMapper<OperationLogDO>

Repository (实现)
  ├── SystemConfigRepositoryImpl → SystemConfigMapper + SystemConfigConverter
  ├── UserRepositoryImpl → UserMapper
  └── OperationLogRepositoryImpl → OperationLogMapper + OperationLogRepositoryConverter
```

### 自动填充依赖

```
MyMetaObjectHandler (MetaObjectHandler)
  ├── insertFill → createTime, updateTime, createUser
  └── updateFill → updateTime, updateUser
       └── BizContext.getUserId() → "system" (默认)
```

---

## 相关文档

| 文档 | 关系 | 说明 |
|------|------|------|
| 上下文传播机制 | infrastructure/context-propagation.md | BizContext 自动填充 createUser/updateUser |
| AOP 切面 | infrastructure/aop-aspects.md | LogAspect 自动写入 operation_log |
| 配置参考 | guides/configuration-reference.md | 数据源配置 |
| 部署与运维 | guides/deployment.md | 数据目录管理 |

---

## 变更历史

| 版本 | 日期 | 变更内容 |
|------|------|---------|
| 1.0.0 | 2026-04-25 | 初始版本：3 张表 DDL + 种子数据 + 类型映射 |
