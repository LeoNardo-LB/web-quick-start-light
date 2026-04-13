## ADDED Requirements

### Requirement: MyBatis-Plus 代码生成器移植
SHALL 从 speccoding 移植 MybatisPlusGenerator 到 `app/.../generated/` 包下，适配 SQLite 数据库连接（默认 `jdbc:sqlite:./data/app.db`）。

#### Scenario: 代码生成器可运行
- **WHEN** 配置 DB_URL 环境变量指向 SQLite 数据库并运行 MybatisPlusGenerator
- **THEN** 在 `app/.../generated/entity/` 和 `app/.../generated/mapper/` 下生成 DO 和 Mapper 文件

### Requirement: 生成的代码禁止手动修改
AGENTS.md SHALL 新增规范：`generated/` 包下的代码由 MybatisPlusGenerator 自动生成，禁止手动修改。如需变更表结构 SHALL 修改 schema.sql 后重新运行代码生成器。

#### Scenario: AGENTS.md 包含代码生成器规范
- **WHEN** 阅读 AGENTS.md
- **THEN** 包含"禁止手动修改生成代码"的明确规范

### Requirement: schema.sql DDL 文件
SHALL 新增 `app/src/main/resources/schema.sql`，包含 system_config 表的 DDL（SQLite 语法）。DDL SHALL 包含表模板注释和建表规范说明。

#### Scenario: system_config 表自动创建
- **WHEN** 应用首次启动（SQLite 数据库不存在）
- **THEN** system_config 表被自动创建，字段与 speccoding 一致（适配 SQLite 类型）

### Requirement: init.sql DML 文件
SHALL 新增 `app/src/main/resources/init.sql`，包含 15 条系统配置初始数据（BASIC 4 条、EMAIL 4 条、STORAGE 3 条、SECURITY 4 条），使用 SQLite 语法（`datetime('now')` 替代 `NOW()`）。

#### Scenario: 初始数据自动插入
- **WHEN** 应用首次启动（system_config 表为空）
- **THEN** 15 条初始配置数据被自动插入

### Requirement: SQL 自动执行配置
application.yaml SHALL 配置 Spring Boot 的 SQL 自动执行机制（`spring.sql.init`），确保 schema.sql 和 init.sql 在应用启动时执行。

#### Scenario: SQL 文件自动执行
- **WHEN** 应用启动
- **THEN** schema.sql 和 init.sql 被自动执行（仅当表不存在时建表，仅当表为空时插入数据）

### Requirement: SQLite 语法适配
所有 SQL 语句 SHALL 适配 SQLite 语法：INTEGER 替代 BIGINT、TEXT 替代 JSON/TIMESTAMP、无 ENGINE/CHARSET 声明、datetime('now') 替代 NOW()、应用层处理 ON UPDATE CURRENT_TIMESTAMP。

#### Scenario: SQL 在 SQLite 中可执行
- **WHEN** 使用 SQLite 执行 schema.sql
- **THEN** 所有 DDL 语句成功执行，无语法错误
