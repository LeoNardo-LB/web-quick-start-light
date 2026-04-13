## Context

web-quick-start-light 是一个 Maven 多模块项目（root → common + clients/6子模块 + app），包含 115 个测试，但当前没有代码覆盖率工具。后开发验证时只能通过 grep 断言计数估算覆盖率，缺乏精确的语句/分支/行级数据。

项目模块结构：
- `common/` — 异常体系，无测试
- `clients/client-cache|oss|email|sms|search|log/` — 中间件客户端，仅 client-search(17) 和 client-log(8) 有测试
- `app/` — 主应用，90 个测试（UTest + ITest）

构建：Maven，父 POM `spring-boot-starter-parent:4.0.2`，Java 25。

## Goals / Non-Goals

**Goals:**
- 在根 POM 统一配置 JaCoCo 插件，所有子模块自动继承
- `mvn verify` 自动生成每个模块的覆盖率报告（HTML + XML）
- app 模块聚合所有子模块覆盖率数据（`report-aggregate`）
- 配置合理的排除规则（测试类、生成代码、配置类、主启动类）
- 配置覆盖率阈值检查（`check` goal）
- 更新项目文档

**Non-Goals:**
- 不配置 CI/CD 集成（仅本地 Maven 命令）
- 不配置 SonarQube 集成
- 不修改任何业务代码或测试代码
- 不添加新的测试用例（仅配置工具）
- 不调整现有测试框架或依赖

## Decisions

### D1: JaCoCo 版本选择 — 0.8.12

**选择**: `jacoco-maven-plugin:0.8.12`

**理由**: 0.8.12 是当前最新稳定版（2025年3月发布），支持 Java 25+，与 Maven 3.9+ 兼容。Spring Boot 4.0.2 的 BOM 中不包含 JaCoCo，需手动指定版本。

**替代方案**: 
- 0.8.11 — 可用但缺少对最新 Java 的部分支持
- 0.8.13-SNAPSHOT — 不稳定

### D2: 插件配置层级 — 根 POM pluginManagement + 子模块继承

**选择**: 根 POM 的 `<pluginManagement>` 声明 JaCoCo 插件及版本，根 POM `<build><plugins>` 中配置 `prepare-agent` 和 `report`（绑定默认生命周期），所有子模块自动继承。

**理由**: 多模块项目标准模式。`pluginManagement` 统一版本，`build/plugins` 中的 `prepare-agent` 和 `report` 绑定到 `default` 生命周期，子模块无需重复配置。

**替代方案**:
- 每个子模块单独配置 → 重复代码多，易遗漏
- 仅在 app 配置 → 无法覆盖 common 和 clients 模块的覆盖率

### D3: 聚合报告 — app 模块 `report-aggregate`

**选择**: 在 app 模块的 `<build><plugins>` 中添加 `report-aggregate` goal，依赖所有子模块。

**理由**: app 已经依赖 common + 所有 client-* 模块，天然适合做聚合点。`report-aggregate` 从依赖模块的 `target/jacoco.exec` 收集数据，生成全项目聚合报告。

**输出路径**: `app/target/site/jacoco-aggregate/`

### D4: 排除规则

**选择**: 排除以下类：
- `**/*Test*` — 测试类
- `**/*UTest` — 单元测试
- `**/*ITest` — 集成测试
- `**/generated/**` — 代码生成器产物
- `**/*Application*` — Spring Boot 主启动类
- `**/*Properties*` — 配置属性类（纯 POJO，无逻辑）
- `**/*Configuration*` — Spring 自动配置类（条件装配框架代码）
- `**/mapper/**` — MyBatis-Plus 生成的 Mapper 接口

**理由**: 这些类要么是测试基础设施、框架胶水代码，要么是生成的代码，覆盖它们没有业务价值，反而会稀释覆盖率数据。

### D5: 覆盖率阈值 — post-dev-verification 标准

**选择**: 阈值对齐 post-dev-verification 技能标准：
- `INSTRUCTION` 覆盖率 ≥ 95%（对应 Stmt/Line ≥ 95%）
- `BRANCH` 覆盖率 ≥ 90%

当前 `haltOnFailure=false`，未达标时仅输出 WARNING 不中断构建。达标后改为 `true` 启用门禁。

**理由**: post-dev-verification 定义了代码覆盖率的软目标（SHOULD），AI 生成代码缺陷率是人类代码的 1.7 倍，需要更高的覆盖率保障。当前实际覆盖率（instruction=0.74, branch=0.54）未达标，属于 clients 模块（cache/oss/email/sms）尚未有测试覆盖的已知差距。先配好标准，通过警告驱动后续补充测试。

**替代方案**:
- 设置宽松阈值（70%/50%）→ 无法发现覆盖率回退
- 直接 haltOnFailure=true → 阻塞构建，不实际

### D6: report goal 绑定阶段 — verify

**选择**: `report` 和 `report-aggregate` 绑定到 `verify` 阶段。

**理由**: `mvn test` 执行测试（`surefire`），JaCoCo `prepare-agent` 在 `initialize` 阶段注入探针，`report` 在 `verify` 阶段生成报告。这样 `mvn verify` 即可一步完成测试+报告。

## Risks / Trade-offs

- **[风险] JaCoCo 与 Java 25 兼容性** → JaCoCo 0.8.12 支持 Java 25，但若出现问题可降级至 0.8.11
- **[风险] 构建时间增加** → JaCoCo 字节码注入和报告生成约增加 2-5 秒构建时间，影响可忽略
- **[权衡] haltOnFailure=false** → 当前覆盖率（instruction=0.74, branch=0.54）未达 95%/90% 目标，仅警告不中断。达标后需改为 true 启用门禁
- **[权衡] 排除配置类** → 排除 *Configuration 和 *Properties 会降低总覆盖率，但聚焦业务代码质量
