# 架构文档（docs/architecture/）

> 🟢 Contract 轨 — 本目录下所有文档 100% 反映代码现状。代码变更时必须同步更新。

## 📋 目录

- [文档列表](#文档列表)
- [模板结构](#模板结构)
- [编写指南](#编写指南)

## 文档列表

| 文档 | 一句话描述 | 回答的读者问题 |
|------|-----------|--------------|
| [system-overview.md](system-overview.md) | C4 图 + 技术栈概要 | 系统整体长什么样？ |
| [module-structure.md](module-structure.md) | Maven 多模块 + 四层架构 | 代码怎么组织的？ |
| [request-lifecycle.md](request-lifecycle.md) | HTTP 请求完整处理链路 | 一个请求经历了什么？ |
| [design-patterns.md](design-patterns.md) | Template Method + 条件装配 | 用了哪些设计模式？ |
| [thread-context.md](thread-context.md) | ScopedValue 传递链 | 线程上下文怎么传？ |
| [archetype-usage.md](archetype-usage.md) | Maven 骨架安装/使用/故障排查 | 如何基于此项目创建新项目？ |

## 模板结构

architecture/ 下的每个文件有**独立的固定结构**（因为每个文件回答不同的架构问题），但遵循统一的外壳约束：

### 固定层（不可修改）

```
# <标题>

> 🟢 Contract 轨 — 100% 反映代码现状

## 📋 目录
- [概述](#概述)
- [核心章节名](#核心章节名)  ← 根据文档主题命名
- [相关文档](#相关文档)
- [变更历史](#变更历史)

## 概述
（一段话概括本文档回答的问题）

## <核心章节>  ← 至少包含 1 个 Mermaid 图 + 1 个表格

## 相关文档

## 变更历史
| 日期 | 变更内容 |
|------|---------|
| YYYY-MM-DD | 初始创建 |
```

### 半固定层（内容按实际填充）

- **核心章节**：每个文件的章节名不同（如 system-overview.md 的"技术栈概要"、request-lifecycle.md 的"请求时序图"）
- **Mermaid 图**：必须至少 1 个（类型根据内容选择：graph/sequenceDiagram/classDiagram/flowchart）
- **表格**：必须至少 1 个（技术栈/规则/组件等）
- **代码示例**：可选（design-patterns.md 需要，其他可选）
- **变更历史**：新增条目时追加行

#### 可选扩展内容

以下内容为可选扩展，根据文档主题和复杂度选择性添加：

- **💡 设计考量**（"驱动力 → 决策 → 理由"三段式）：说明为什么选择这种架构/设计而非其他方案。适用场景：有明确替代方案需要对比时（如为什么选 Template Method 而非 Strategy）。格式示例：
  > **设计考量**：
  > - **驱动力**：技术组件需要统一的校验/日志骨架
  > - **备选方案**：Strategy 模式（运行时切换）、Template Method 模式（编译期固定）
  > - **选择理由**：组件行为在编译期已确定，不需要运行时切换；Template Method 的 `final` 公开方法还能防止子类跳过校验

- **📐 系统边界**（"内部/外部"分类）：说明系统的覆盖范围和不覆盖的范围。适用场景：system-overview.md 等全景文档。格式示例：
  > **系统边界**：
  > - **包含**：Web 层、Service 层、持久层、缓存、对象存储、邮件、短信、搜索、日志、限流、幂等、认证
  > - **不包含**：消息队列、分布式事务、微服务注册发现、容器编排

### 编写检查清单

- [ ] 包含 🟢 Contract 轨标签
- [ ] 包含 📋 目录（锚点链接到所有二级标题）
- [ ] 概述章节用一段话回答"这个文档解决什么问题"
- [ ] 至少 1 个 Mermaid 图
- [ ] 至少 1 个表格
- [ ] 包含相关文档章节
- [ ] 包含变更历史章节

## 编写指南

（编写指南内容同原有内容，保持不变）

## 相关文档

- [docs/README.md](../README.md) — 文档系统导航和三轨体系说明
- [docs/conventions/README.md](../conventions/README.md) — 编码规范模板
- [docs/modules/README.md](../modules/README.md) — 模块文档模板
- [AGENTS.md](../../AGENTS.md) — AI 编码规范入口
- [OpenSpec specs/](../../openspec/specs/) — 设计意图文档（🔴 Intent 轨，不在 docs/ 下）
