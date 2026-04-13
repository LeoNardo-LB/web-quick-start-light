## MODIFIED Requirements

### Requirement: AGENTS.md 引用索引模式
- **WHEN** 开发者阅读 AGENTS.md
- **THEN** AGENTS.md MUST 采用引用索引模式，将详细内容委托给 docs/ 目录下的专门文档，AGENTS.md 本身仅保留精简的编码规则摘要和指向详细文档的引用链接，每个引用 MUST 标注引用强度（⛔ MUST / ⚠️ SHOULD / 💡 MAY，采用 RFC 2119 三级体系）

#### Scenario: 编码规则引用链接
- **WHEN** 开发者在 AGENTS.md 中看到"Lombok 规范"条目
- **THEN** 该条目 MUST 包含指向 docs/conventions/java-conventions.md 的引用链接，引用强度 MUST 标注为"⛔ MUST"，链接 MUST 使用相对路径格式

### Requirement: 删除项目结构索引表
- **WHEN** AGENTS.md 被更新为引用索引模式
- **THEN** AGENTS.md MUST 删除现有的"项目结构索引"大表格（约 30 行的模块/目录对照表），该信息 MUST 迁移到 docs/architecture/module-structure.md 中，AGENTS.md 仅保留一句话的项目结构概述

#### Scenario: 项目结构信息迁移验证
- **WHEN** 开发者需要查看 client-cache 模块的说明
- **THEN** AGENTS.md 中 MUST 不再包含 client-cache 的详细说明条目，MUST 通过引用链接指向 docs/modules/client-cache.md

### Requirement: 精确版本号改为大版本号
- **WHEN** 开发者阅读 AGENTS.md 的技术栈表格
- **THEN** 技术栈表格中的精确版本号（如 Java 25、Spring Boot 4.0.2、MyBatis-Plus 3.5.16）MUST 替换为大版本号（如 Java 25、Spring Boot 4.x、MyBatis-Plus 3.5.x），精确版本号 MUST 迁移到 docs/architecture/system-overview.md 的技术栈详细表格中

#### Scenario: 版本号格式变更
- **WHEN** 开发者查看 AGENTS.md 技术栈表格中的 Spring Boot 版本
- **THEN** 显示格式 MUST 为 "Spring Boot 4.x" 而非 "Spring Boot 4.0.2"，且 MUST 包含指向 system-overview.md 的引用链接（标注"⛔ MUST"）

### Requirement: 带引用强度的文档索引
- **WHEN** 开发者需要快速定位编码规范文档
- **THEN** AGENTS.md MUST 包含"文档索引"章节，按主题分类列出所有 docs/ 文档的引用链接，每个引用 MUST 标注引用强度：⛔ MUST（强制阅读，无例外）、⚠️ SHOULD（推荐阅读，允许有充分理由的例外）、💡 MAY（按需阅读，完全可选），索引 MUST 覆盖 architecture/、conventions/、modules/ 三个目录下的全部文档

#### Scenario: 文档索引完整性
- **WHEN** 维护者在 docs/ 下新增了一个文档
- **THEN** AGENTS.md 的文档索引章节 MUST 同步添加该文档的引用条目，包含文档标题、相对路径链接和引用强度标注

#### Scenario: 按引用强度筛选文档
- **WHEN** 新开发者只想阅读 ⛔ MUST 级别的文档
- **THEN** 通过文档索引中的引用强度标注，MUST 能快速识别出所有 ⛔ MUST 文档列表（如 architecture/module-structure.md、conventions/java-conventions.md、conventions/testing-conventions.md）

### Requirement: 删除 JVM 内存配置详表
- **WHEN** AGENTS.md 被更新为引用索引模式
- **THEN** AGENTS.md MUST 删除现有的"JVM 内存配置（1G 总内存）"详细表格（包含堆内存/元空间/代码缓存/直接内存/线程栈/GC 的分区表），该信息 MUST 迁移到 docs/architecture/system-overview.md 的系统设计章节中

#### Scenario: JVM 配置信息迁移验证
- **WHEN** 开发者需要了解 JVM 内存分配细节
- **THEN** AGENTS.md 中 MUST 不再包含 JVM 内存分区表，MUST 通过引用链接指向 docs/architecture/system-overview.md

### Requirement: 删除详细文档路径表
- **WHEN** AGENTS.md 被更新为引用索引模式
- **THEN** AGENTS.md MUST 删除现有的"详细文档"路径表格（包含 docs/ 路径 + 说明的对照表），该表格 MUST 替换为精简的 docs/ 索引（仅包含三轨分类 + 文档数量 + 引用强度标注）

#### Scenario: 文档路径表替换验证
- **WHEN** 开发者在 AGENTS.md 中查找文档路径
- **THEN** AGENTS.md MUST 不再包含逐行列出每个文档路径的表格，MUST 通过精简的三轨分类索引提供导航
