# Web Quick Start Light

> Spring Boot 4.x + Java 25 多模块四层架构脚手架，开箱即用。

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 25 |
| 框架 | Spring Boot | 4.x |
| ORM | MyBatis-Plus | 3.5.x |
| 对象转换 | MapStruct | 1.6.x |
| 认证 | Sa-Token | 1.45.x |
| 限流 | Bucket4j | 8.17.x |
| 工具库 | Hutool | 5.8.x |
| 数据库 | SQLite | 3.x |
| 测试 | JUnit 5 + Mockito + ArchUnit | - |
| 构建 | Maven 多模块 | - |

> 精确版本号见 `pom.xml` 或 [docs/architecture/system-overview.md](docs/architecture/system-overview.md)。

## 项目结构

```
web-quick-start-light/                     (根 POM, packaging=pom)
├── common/                                (异常体系)
├── components/                               (parent POM, packaging=pom)
│   ├── component-cache/                      (Caffeine 本地缓存)
│   ├── component-oss/                        (本地对象存储)
│   ├── component-email/                      (Jakarta Mail 邮件)
│   ├── component-sms/                        (短信)
│   ├── component-search/                     (内存搜索)
│   ├── component-log/                        (日志组件)
│   ├── component-ratelimit/                  (限流组件)
│   ├── component-idempotent/                 (幂等组件)
│   └── component-auth/                       (认证组件)
└── app/                                   (主应用, 依赖 common + 所有 component-*)
```

> 详细模块依赖和四层架构说明见 [docs/architecture/module-structure.md](docs/architecture/module-structure.md)。

## 快速开始

```bash
# 构建（跳过测试）
mvn clean package -DskipTests

# 运行全部测试
mvn test

# 仅单元测试（*UTest）
mvn test -Dtest="*UTest"

# 仅集成测试（*ITest）
mvn test -Dtest="*ITest"

# 启动应用（开发环境）—— 必须指定 -pl app
mvn spring-boot:run -pl app

# 启动应用（生产环境）
scripts/start.sh prod

# 停止应用
scripts/stop.sh
```

## 多环境配置

```bash
# 开发环境（默认，SQLite）
mvn spring-boot:run -pl app

# 生产环境
scripts/start.sh prod

# 指定可选配置
mvn spring-boot:run -pl app -Dspring-boot.run.profiles=prod
```

## 核心特性

- **四层架构**：Controller → Facade → Service → Repository，ArchUnit 守护依赖规则
- **9 个技术组件**：缓存/存储/邮件/短信/搜索/日志/限流/幂等/认证，Template Method + 条件装配
- **ScopedValue 线程上下文**：基于 Java 25 ScopedValue 的 userId/traceId 全链路传递
- **完善日志体系**：@BusinessLog 注解 + 8 个 Appender + 采样 + 慢 SQL 监控
- **高覆盖率测试**：UTest + ITest 双分类，JaCoCo Instruction ≥ 95%

> 详细架构、编码规范、模块文档见 [docs/](docs/)。

## 骨架使用

骨架安装、使用、故障排查详见 [docs/architecture/archetype-usage.md](docs/architecture/archetype-usage.md)。

## 文档

| 文档 | 说明 |
|------|------|
| [AGENTS.md](AGENTS.md) | AI 编码规范入口 |
| [docs/README.md](docs/README.md) | 文档系统导航 |

> 本项目采用三轨文档体系：**Contract 轨**（docs/，跟随代码变化）、**Constraint 轨**（AGENTS.md + conventions/，驱动代码行为）和 **Intent 轨**（OpenSpec，编码前冻结的设计意图）。Intent 轨文档不在 docs/ 目录下，位于独立的 [openspec/specs/](openspec/specs/) 目录，记录各功能的设计初衷和决策理由。详见 [docs/README.md](docs/README.md)。
