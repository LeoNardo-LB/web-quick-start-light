## ADDED Requirements

### Requirement: AGENTS.md 入口文件
项目根目录 SHALL 包含 `AGENTS.md` 文件，作为 AI 编码助手的入口指引。文件内容 SHALL 少于 200 行，包含以下核心部分：
1. 项目概述（技术栈、架构模式）
2. 快速开始命令（构建、测试、运行）
3. 核心编码规则（三级异常、测试命名、禁止 @Data 等）
4. 项目结构索引（关键目录位置）
5. 详细文档引用（指向 docs/ 下的详细规范）

#### Scenario: AI 助手首次进入项目
- **WHEN** AI 编码助手首次打开项目
- **THEN** 通过读取 AGENTS.md 了解项目技术栈（Java 25 + Spring Boot 3.5.6 + 三层架构）、核心规则和文档索引

#### Scenario: AGENTS.md 长度控制
- **WHEN** 统计 AGENTS.md 行数
- **THEN** 总行数不超过 200 行（符合 Anthropic 官方推荐）

### Requirement: CLAUDE.md 符号链接
项目根目录 SHALL 包含 `CLAUDE.md` 文件，作为指向 `AGENTS.md` 的符号链接，兼容 Claude Code 工具。

#### Scenario: Claude Code 读取项目指令
- **WHEN** Claude Code 打开项目
- **THEN** 读取 CLAUDE.md（符号链接）获得与 AGENTS.md 相同的内容

### Requirement: docs/ 目录结构
项目 SHALL 包含 `docs/` 目录，结构如下：
```
docs/
├── README.md                       # 文档导航索引
├── architecture/
│   ├── overview.md                 # 三层架构总览
│   └── dependency-rules.md         # 依赖规则
├── backend/
│   ├── coding-standards.md         # 编码规范
│   ├── testing-guide.md            # 测试规范
│   ├── error-handling.md           # 错误处理规范
│   └── performance-rules.md        # 性能规则
├── workflow/
│   ├── tdd-guide.md                # TDD 指南
│   └── openspec-guide.md           # OpenSpec 工作流
└── guides/
    └── getting-started.md          # 快速开始
```

#### Scenario: 文档体系完整性
- **WHEN** 列出 docs/ 目录下所有文件
- **THEN** 包含 README.md + 4 个子目录共 10 个文档文件

### Requirement: docs/README.md 文档导航
docs/README.md SHALL 作为文档导航索引，按分类列出所有文档，包含文档名称、简介、适用场景和文件路径。

#### Scenario: 查找编码规范
- **WHEN** 开发者需要了解编码规范
- **THEN** 在 docs/README.md 中找到 `backend/coding-standards.md` 的引用和简介

### Requirement: docs/architecture/overview.md 三层架构总览
overview.md SHALL 描述本项目的三层架构（Controller → Service → Repository），包含：
1. 各层职责说明
2. 各层允许使用的注解
3. 层间调用规则（Controller 不直接调用 Repository）
4. 包结构约定

#### Scenario: 新开发者理解项目架构
- **WHEN** 新开发者阅读 overview.md
- **THEN** 理解 Controller 负责参数校验和响应封装、Service 负责业务逻辑、Repository 负责数据访问的三层分工

### Requirement: docs/backend/coding-standards.md 编码规范
coding-standards.md SHALL 包含：
1. 允许的依赖库清单（MyBatis-Plus、Hutool、Jackson、Lombok）
2. Lombok 使用规范（禁止 @Data，使用 @Builder + @RequiredArgsConstructor）
3. 命名规范（类名、方法名、常量名）
4. 配置类位置约定

#### Scenario: 检查是否使用了禁止的依赖
- **WHEN** 开发者需要确认是否可以使用 Guava
- **THEN** 在 coding-standards.md 的依赖清单中查找，发现不在允许列表中

### Requirement: docs/backend/testing-guide.md 测试规范
testing-guide.md SHALL 包含：
1. 测试命名规范（`XxxUTest` 单元测试 / `XxxITest` 集成测试）
2. 测试基类使用说明（UnitTestBase / IntegrationTestBase）
3. Mock 规范
4. ArchUnit 架构合规测试说明

#### Scenario: 编写新的集成测试
- **WHEN** 开发者需要为新的 API 端点编写集成测试
- **THEN** 参照 testing-guide.md 创建继承 IntegrationTestBase 的 `XxxITest` 类

### Requirement: README.md 更新
README.md SHALL 反映当前项目真实状态：
1. 技术栈表格包含 SQLite（而非 H2）
2. 多环境配置命令更新（dev 使用 SQLite，而非 H2）
3. 项目特性列表包含日志系统能力（MDC traceId、慢查询拦截、审计日志等）
4. 骨架使用方法保持不变

#### Scenario: 新用户查看 README
- **WHEN** 新用户阅读 README.md 的技术栈表格
- **THEN** 看到数据库为 SQLite（开发/测试/生产环境），而非 H2

### Requirement: 前一变更 tasks.md 追加记录
enhance-architecture-from-backend 变更的 tasks.md SHALL 追加 Phase 2 精简任务记录，包括：
1. 删除 BizLogDto、PersistenceHandler 等过度设计文件
2. 重写 @BizLog 注解和 BizLogAspect
3. 重写 TestController
4. 移除 @MapperScan、恢复 ArchUnit 规则
5. 更新 ApplicationStartupITest

#### Scenario: 查看完整变更历史
- **WHEN** 查看 enhance-architecture-from-backend/tasks.md
- **THEN** 包含原始 70 个任务 + Phase 2 精简任务，共约 80+ 个任务项，全部标记 [x]
