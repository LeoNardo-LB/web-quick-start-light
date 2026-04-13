# 模块文档（docs/modules/）

> 🟢 Contract 轨 — 本目录下所有文档 100% 反映代码现状。代码变更时必须同步更新。

## 📋 目录

- [文档列表](#文档列表)
- [模板结构](#模板结构)
- [编写指南](#编写指南)

## 文档列表

### 业务模块

| 文档 | 一句话描述 |
|------|-----------|
| [auth.md](auth.md) | 认证模块：Sa-Token 登录/注销/拦截 |
| [system-config.md](system-config.md) | 系统配置模块：CRUD + 分页 + 值对象 |
| [operation-log.md](operation-log.md) | 操作日志模块：@BusinessLog 注解 + 分页查询 |

### 技术客户端

| 文档 | 一句话描述 |
|------|-----------|
| [client-cache.md](client-cache.md) | 缓存客户端：Caffeine + 10 方法 |
| [client-oss.md](client-oss.md) | 对象存储客户端：本地存储 + NIO + 日期分层 |
| [client-email.md](client-email.md) | 邮件客户端：Jakarta Mail + NoOp 默认 |
| [client-sms.md](client-sms.md) | 短信客户端：3 方法 + NoOp 默认 |
| [client-search.md](client-search.md) | 搜索客户端：内存搜索 + 15 方法 |
| [client-log.md](client-log.md) | 日志客户端：@BusinessLog + 8 Appender |
| [client-ratelimit.md](client-ratelimit.md) | 限流客户端：Bucket4j + SpEL |
| [client-idempotent.md](client-idempotent.md) | 幂等客户端：@Idempotent + Caffeine |
| [client-auth.md](client-auth.md) | 认证客户端：AuthClient 接口 + Sa-Token |

## 模板结构

modules/ 下所有文件使用**统一的 8 章节模板**：

### 固定层（不可修改）

```
# <模块名> — Contract 轨

> 代码变更时必须同步更新本文档

## 📋 目录
- [概述](#概述)
- [业务场景](#业务场景)
- [技术设计](#技术设计)
- [API 参考](#api-参考)
- [配置参考](#配置参考)
- [使用指南](#使用指南)
- [相关文档](#相关文档)
- [变更历史](#变更历史)

## 概述
（模块定位和职责，一句话概括）

## 业务场景
（核心用例列表，每个用例包含场景名称和 1-3 句流程描述，至少 3 个用例）

## 技术设计
（类图/时序图 + 关键类说明，至少 1 个 Mermaid 图）

## API 参考
（公开方法/接口/端点列表，含参数和返回值）

## 配置参考
（配置项表格。无配置项时写"本模块无独立配置项"）

## 使用指南
（至少 2 个不同场景的完整代码示例）

## 相关文档

### 上游依赖
（本模块依赖的其他模块/文档）

### 下游消费者
（依赖本模块的其他模块/文档）

### 设计依据
（OpenSpec Intent、架构文档等相关设计文档）

## 变更历史
| 日期 | 变更内容 |
|------|---------|
| YYYY-MM-DD | 初始创建 |
```

### 半固定层（内容按实际填充）

- **概述**：固定一句话概括模块职责
- **业务场景**：列出 3+ 个核心用例，每个用例包含场景名称和 1-3 句流程描述
- **技术设计**：
  - 至少 1 个 Mermaid 图（classDiagram 或 sequenceDiagram）
  - 关键类说明表格（类名、职责、关键方法）
  - 技术客户端模块需展示 Template Method 模式（详见 [design-patterns.md](../architecture/design-patterns.md)）
- **API 参考**：列出全部公开方法/端点，格式：方法签名 | 参数 | 返回值 | 说明
- **配置参考**：列出配置前缀和全部配置项，格式：配置项 | 类型 | 默认值 | 说明
- **使用指南**：至少 2 个不同场景的完整代码示例（如基础用法 + 高级用法，或集成步骤 + 常见操作）
- **相关文档**：分三类（上游依赖/下游消费者/设计依据）列出关联文档链接
- **变更历史**：使用追加式表格，仅记录结构性变更（新增 API、删除字段等），不记录琐碎变更

### 编写检查清单

- [ ] 包含 `# <模块名> — Contract 轨` 标题格式
- [ ] 标题下方包含 blockquote `> 代码变更时必须同步更新本文档`
- [ ] 包含 📋 目录（8 个章节的锚点链接）
- [ ] 概述用一句话概括模块职责
- [ ] 业务场景列出 3+ 个用例，每个有流程描述
- [ ] 技术设计包含至少 1 个 Mermaid 图
- [ ] API 参考列出全部公开方法
- [ ] 配置参考列出全部配置项（或注明"无独立配置项"）
- [ ] 使用指南包含至少 2 个不同场景的代码示例
- [ ] 包含相关文档章节（分上游依赖/下游消费者/设计依据）
- [ ] 包含变更历史章节（至少一条初始记录）

## 编写指南

（编写指南内容同原有内容，保持不变）

## 相关文档

- [docs/README.md](../README.md) — 文档系统导航和三轨体系说明
- [docs/architecture/README.md](../architecture/README.md) — 架构文档模板
- [docs/conventions/README.md](../conventions/README.md) — 编码规范模板
- [AGENTS.md](../../AGENTS.md) — AI 编码规范入口
- [OpenSpec specs/](../../openspec/specs/) — 设计意图文档（🔴 Intent 轨，不在 docs/ 下）
