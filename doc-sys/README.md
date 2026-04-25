# 文档体系说明

> **职责**: 描述文档系统本身的结构、生成规则和维护方式
>
> 本文档面向文档维护者（人类和 AI），说明 `doc-sys/` 下 22 份文档的组织逻辑、三轨哲学和持续维护策略。

## 文档体系概述

本项目文档体系基于**三轨哲学**构建，将文档按职责和生命周期分为三条轨道：

| 轨道 | 英文 | 职责 | 生命周期 | 维护者 |
|------|------|------|---------|--------|
| **意图** | Intent | 记录"为什么这样设计" | 创建后冻结，变更通过新 ADR | Human |
| **契约** | Contract | 描述"代码是什么" | 跟随代码自动同步 | AI |
| **约束** | Constraint | 定义"必须怎么做" | 人类编写，驱动代码 | Human |

三轨之间的关系：**Intent → Constraint → Contract**

```
Intent（设计意图）── 架构决策时的思考过程
     │
     ▼
Constraint（约束规则）── 从意图中提炼的可执行规则
     │
     ▼
Contract（代码契约）── 规则落地后的代码行为描述
```

## 目录结构

```
doc-sys/
├── architecture/                    — Intent 轨道，系统级设计文档
│   ├── arch-overview.md             #   架构总览（分层、模块、技术栈）
│   └── arch-decisions.md            #   架构决策记录（10 个 ADR）
│
├── business/                        — Contract 轨道，业务领域文档
│   ├── base-types.md                #   基础类型（BaseResult / BaseDO）
│   ├── exception-system.md          #   异常体系（三级分类 + 错误码）
│   ├── api-reference.md             #   REST API 参考（18 个端点）
│   ├── domain-user.md               #   用户领域（认证链路）
│   ├── domain-system-config.md      #   系统配置领域（DDD 值对象）
│   └── domain-operation-log.md      #   操作日志领域（查询 + AOP 写入）
│
├── components/                      — Contract 轨道，组件文档
│   ├── component-pattern.md         #   组件设计模式（Template Method 规范）
│   ├── component-auth.md            #   认证组件
│   ├── component-cache.md           #   缓存组件
│   ├── component-oss.md             #   对象存储组件
│   ├── component-search.md          #   搜索组件
│   └── component-messaging.md       #   邮件与短信组件
│
├── infrastructure/                  — Intent + Contract 轨道，基础设施文档
│   ├── context-propagation.md       #   上下文传播机制（BizContext + ScopedValue）
│   ├── aop-aspects.md               #   AOP 切面（幂等/日志/限流）
│   └── logging-system.md            #   日志体系（logback + 采样 + 脱敏）
│
├── guides/                          — Constraint 轨道，规范和指南
│   ├── coding-standards.md          #   编码规范（10 条规则 + ArchUnit 守护）
│   ├── configuration-reference.md   #   配置参考（全部 YAML 配置项）
│   ├── database.md                  #   数据库 Schema（DDL + 种子数据）
│   ├── testing-guide.md             #   测试指南（分层 + 命名 + 标签）
│   └── deployment.md                #   部署与运维（JVM 参数 + 脚本）
│
├── .generation/                     — 文档生成过程的中间文件（不纳入正式文档）
│   ├── phase1-analysis/             #   Phase 1: 源码分析报告
│   └── phase2-3-checklist/          #   Phase 2-3: 分组与完整性检查
│
└── README.md                        #   本文件
```

## 三轨体系详解

### Intent 轨道 — 设计意图

**定位**: 回答"为什么这样设计"，记录架构决策的背景、动机和权衡。

**特征**:
- 创建后视为**不可变**（Frozen），记录设计历史
- 变更通过新增 ADR（supersede 原记录）而非修改原文
- 包含 Mermaid 图表、对比分析、决策矩阵等深度内容
- 面向架构师和资深开发者

**文档清单**:

| 文档 | 目录 | 核心内容 |
|------|------|---------|
| 架构总览 | `architecture/arch-overview.md` | 分层架构、模块拓扑 DAG、技术栈全景、5 种设计模式 |
| 架构决策记录 | `architecture/arch-decisions.md` | 10 个 ADR（Template Method、DDD-lite、ScopedValue、SQLite、Serial GC 等） |
| 上下文传播机制 | `infrastructure/context-propagation.md` | ScopedValue 替代 ThreadLocal 的完整设计 |
| 组件设计模式 | `components/component-pattern.md` | 6 组件统一的 Template Method + Strategy 骨架 |

### Contract 轨道 — 代码契约

**定位**: 回答"代码是什么"，描述代码的实际行为和公共 API。

**特征**:
- **跟随代码同步** — 代码变更时自动或半自动更新
- 包含公共 API 签名、类型定义、流程图、依赖关系
- 面向使用代码的开发者和 AI Agent
- 格式统一：公共 API 参考 → 核心类型 → 服务流程 → 依赖关系 → 相关文档

**文档清单**:

| 文档 | 目录 | 核心内容 |
|------|------|---------|
| 基础类型 | `business/base-types.md` | BaseResult / BasePageResult / BaseRequest / BaseDO |
| 异常体系 | `business/exception-system.md` | BaseException → BizException / ClientException / SysException |
| REST API 参考 | `business/api-reference.md` | 全部 18 个端点的请求/响应定义 |
| 用户领域 | `business/domain-user.md` | LoginController → LoginFacade → UserRepository |
| 系统配置领域 | `business/domain-system-config.md` | DDD 值对象 + 6 个 CRUD 端点 |
| 操作日志领域 | `business/domain-operation-log.md` | 分页查询 + AOP 自动写入 |
| 认证组件 | `components/component-auth.md` | AuthComponent 接口 + Sa-Token 实现 |
| 缓存组件 | `components/component-cache.md` | CacheComponent 接口 + Caffeine 实现 |
| 对象存储组件 | `components/component-oss.md` | OssComponent 接口 + 本地文件系统实现 |
| 搜索组件 | `components/component-search.md` | SearchComponent 接口 + 内存实现 |
| 邮件与短信组件 | `components/component-messaging.md` | EmailComponent + SmsComponent NoOp 实现 |
| AOP 切面 | `infrastructure/aop-aspects.md` | @Idempotent / @BusinessLog / @RateLimit |
| 日志体系 | `infrastructure/logging-system.md` | logback-spring.xml + 采样过滤 + 脱敏 |
| 配置参考 | `guides/configuration-reference.md` | 全部 Properties 类与 YAML 配置项 |
| 数据库 Schema | `guides/database.md` | SQLite DDL + 种子数据 + 代码生成器 |

### Constraint 轨道 — 约束规则

**定位**: 回答"必须怎么做"，定义编码规范、测试规则和运维约束。

**特征**:
- **人类驱动** — 规则变更优先更新文档，然后修改代码
- 包含正确示例 ✅ 和违规示例 ❌
- 由 ArchUnit 自动化测试守护执行
- 面向所有代码贡献者

**文档清单**:

| 文档 | 目录 | 核心内容 |
|------|------|---------|
| 编码规范 | `guides/coding-standards.md` | 10 条规则 + ArchUnit 守护 + 违规示例 |
| 测试指南 | `guides/testing-guide.md` | 测试分层 + 命名规范 + 标签体系 |
| 部署与运维 | `guides/deployment.md` | start.sh / stop.sh + JVM 参数 + 环境变量 |

## 维护规则

### 各轨道的维护触发条件

| 轨道 | 触发条件 | 维护方式 | 验证方式 |
|------|---------|---------|---------|
| **Intent** | 重大架构变更（新增/废弃 ADR） | 人工编写 + AI 辅助整理 | 人工审核 |
| **Contract** | 代码变更（新增类、修改方法签名） | AI 自动同步（doc-sys-sync） | CI 校验文档-代码一致性 |
| **Constraint** | 规则变更（新增编码规范、修改 ArchUnit 规则） | 人类编写 | ArchUnit 测试 + CI |

### 文档同步策略

本项目使用 `doc-sys-sync` 工具检测代码与文档之间的漂移：

| 级别 | 说明 | 适用场景 |
|:----:|------|---------|
| **L0** | 提交前快速检查 | `git commit` 前的轻量检查 |
| **L1** | 快速同步（文件级） | 小规模代码变更 |
| **L2** | 共识同步（章节级） | 中等规模变更（默认） |
| **L3** | 深度同步（语义级） | 大规模重构后 |

### 文档格式规范

所有文档遵循统一格式：

```markdown
# 文档标题

> **职责**: 一句话描述文档的核心内容
> **轨道**: Intent / Contract / Constraint
> **维护者**: Frozen / AI / Human

## 目录
- [概述](#概述)
- [公共 API 参考](#公共-api-参考)      ← Contract 轨道必须有
- [编码规则](#编码规则)                ← Constraint 轨道必须有
- [服务流程](#服务流程)
- [依赖关系](#依赖关系)
- [相关文档](#相关文档)               ← 交叉引用其他文档
- [变更历史](#变更历史)               ← 所有文档必须有
```

### 引用强度

项目入口文件 `AGENTS.md` 中为每份文档标注引用强度，指导 AI Agent 的阅读优先级：

| 强度 | 图标 | 含义 |
|------|:----:|------|
| **必读** | 🔴 | AI Agent 在修改对应领域代码前必须阅读 |
| **推荐** | 🟡 | 理解上下文、编写新功能时建议阅读 |
| **参考** | 🟢 | 按需查阅，不要求预先阅读 |

## 排除清单

以下内容**不在文档体系的管理范围内**：

| 路径 | 说明 |
|------|------|
| `app/src/main/java/.../generated/` | MyBatis-Plus 代码生成器产物，可随时重新生成 |
| `doc-sys/.generation/` | 文档生成过程的中间分析文件，仅供调试 |
| `openspec/` | OpenSpec 变更管理文件，独立于文档体系 |
| `.mvn/`、`.idea/`、`.settings/` | IDE 和构建工具配置文件 |

## 统计概览

| 维度 | 数量 |
|------|:----:|
| 文档总数 | 22 份 |
| Intent 轨道 | 4 份 |
| Contract 轨道 | 15 份 |
| Constraint 轨道 | 3 份 |
| 架构决策记录（ADR） | 10 个 |
| 编码规则 | 10 条 |
| ArchUnit 守护测试 | 10 个 |
| REST API 端点 | 18 个 |
| 组件模块 | 6 个 |
