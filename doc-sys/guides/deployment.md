# 部署与运维 (Deployment & Operations)

> **职责**: 定义部署流程和运维操作规范
> **轨道**: Constraint
> **维护者**: Human

---

## 目录

- [概述](#概述)
- [编码规则](#编码规则)
  - [部署前置条件](#部署前置条件)
  - [启动规则](#启动规则)
  - [停止规则](#停止规则)
  - [JVM 参数规则](#jvm-参数规则)
  - [目录结构规则](#目录结构规则)
  - [环境变量规则](#环境变量规则)
- [代码示例](#代码示例)
  - [标准启动流程](#标准启动流程)
  - [优雅停止流程](#优雅停止流程)
  - [日志查看示例](#日志查看示例)
  - [健康检查示例](#健康检查示例)
- [配置参考](#配置参考)
  - [JVM 内存分配方案](#jvm-内存分配方案)
  - [GC 配置](#gc-配置)
  - [诊断配置](#诊断配置)
  - [环境变量参考](#环境变量参考)
  - [目录结构参考](#目录结构参考)
- [检查清单](#检查清单)
- [相关文档](#相关文档)
- [变更历史](#变更历史)

---

## 概述

项目提供标准化的生产环境部署脚本，基于 **1G JVM 内存** 配置，适用于轻量级 Spring Boot 应用：

| 能力 | 实现 | 说明 |
|------|------|------|
| 启动 | `scripts/start.sh` | PID 管理、JVM 参数、GC 配置、OOM 诊断 |
| 停止 | `scripts/stop.sh` | SIGTERM 优雅停机 → SIGKILL 强制终止 |
| GC | Serial GC | ≤1G 堆最优，零额外开销 |
| 诊断 | HeapDump + GC 日志 | OOM 自动 dump，GC 日志轮转 |
| 数据库 | SQLite | 嵌入式，无需外部数据库 |
| 日志 | Logback 8 Appender | 按环境 Profile 路由 |

---

## 编码规则

### 部署前置条件

| # | 规则 | 说明 |
|---|------|------|
| D1 | 必须先执行 `mvn clean package -DskipTests` | 生成可执行 JAR |
| D2 | JRE 版本必须 ≥ 25 | 项目使用 Java 25 ScopedValue |
| D3 | 操作系统必须为 Linux | 脚本使用 `/proc/{pid}/cmdline` 验证 |
| D4 | 磁盘可用空间 ≥ 2GB | 日志文件 + SQLite 数据 + JVM 运行 |

### 启动规则

| # | 规则 | 说明 |
|---|------|------|
| D5 | 必须通过 `start.sh` 启动 | 不允许直接 `java -jar`（缺少 JVM 参数） |
| D6 | 启动前检查 PID 文件 | 防止重复启动 |
| D7 | 启动前检查 JAR 文件存在 | 友好错误提示 |
| D8 | 自动创建 `logs/` 目录 | 确保日志目录存在 |

### 停止规则

| # | 规则 | 说明 |
|---|------|------|
| D9 | 必须通过 `stop.sh` 停止 | 先 SIGTERM 优雅停机，超时后 SIGKILL |
| D10 | 默认等待 30 秒 | 可通过参数自定义超时 |
| D11 | 停止前验证进程为 Java | 防止 PID 回收导致误杀 |

### JVM 参数规则

| # | 规则 | 说明 |
|---|------|------|
| D12 | 堆内存固定 512MB | `-Xms512m -Xmx512m`（避免动态扩缩） |
| D13 | 使用 Serial GC | ≤1G 堆最优，零额外开销 |
| D14 | 启用 OOM 诊断 | HeapDump + ExitOnOutOfMemoryError |
| D15 | 启用 GC 日志 | 轮转策略：5 文件 × 10MB |

### 目录结构规则

| # | 规则 | 说明 |
|---|------|------|
| D16 | 数据库目录：`./data_sqlite/` | SQLite 数据库文件 |
| D17 | 日志目录：`./logs/` | 应用日志、GC 日志、Heap Dump |
| D18 | PID 文件：`./logs/web-quick-start-light.pid` | 进程管理 |

### 环境变量规则

| # | 规则 | 说明 |
|---|------|------|
| D19 | Profile 通过 `SPRING_PROFILES_ACTIVE` 或 `start.sh` 参数指定 | 不允许硬编码 |
| D20 | 数据库连接通过配置文件管理 | 开发环境支持 `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` |

---

## 代码示例

### 标准启动流程

```bash
# 1. 构建项目
mvn clean package -DskipTests

# 2. 启动（默认 prod 环境）
./start.sh

# 3. 指定环境启动
./start.sh dev

# 4. 指定环境和 JAR 路径
./start.sh prod /path/to/custom.jar

# 5. 查看启动日志
tail -f logs/app.log        # prod 环境
tail -f logs/current.log    # dev 环境
```

**start.sh 输出示例**：
```
==========================================
  web-quick-start-light 启动中...
  Profile : prod
  JAR     : app/target/app-0.0.1-SNAPSHOT.jar
  PID File: logs/web-quick-start-light.pid
  JVM 最大: 1G
==========================================
[OK] 应用已启动 (PID: 12345)
     日志目录: logs/
     查看日志: tail -f logs/app.log
```

### 优雅停止流程

```bash
# 1. 标准停止（默认 30 秒超时）
./stop.sh

# 2. 自定义超时（60 秒）
./stop.sh 60

# 3. 检查是否停止
ps -p $(cat logs/web-quick-start-light.pid)
```

**stop.sh 输出示例**：
```
[INFO] 正在停止 web-quick-start-light (PID: 12345)...
[OK] 应用已优雅停止 (PID: 12345)，耗时 3s
```

**超时强制终止示例**：
```
[INFO] 正在停止 web-quick-start-light (PID: 12345)...
[WARN] 等待超时 (30s)，强制终止...
[OK] 已强制终止 (PID: 12345)
```

### 日志查看示例

```bash
# prod 环境
tail -f logs/app.log              # 应用日志
tail -f logs/error.log            # 错误日志
tail -f logs/warn.log             # 警告日志
tail -f logs/json.log             # JSON 结构化日志
tail -f logs/slow-query.log       # 慢查询日志
tail -f logs/audit.log            # 审计日志
tail -f logs/gc.log               # GC 日志

# dev 环境
tail -f logs/current.log          # 会话日志（覆盖写）

# 搜索错误
grep -i "error" logs/app.log | tail -20

# 统计错误数量
grep -c "ERROR" logs/app.log
```

### 健康检查示例

```bash
# Actuator 健康检查
curl -s http://localhost:9201/quickstart-light/actuator/health | python3 -m json.tool

# Prometheus 指标
curl -s http://localhost:9201/quickstart-light/actuator/prometheus | head -20

# 检查进程状态
ps -p $(cat logs/web-quick-start-light.pid) -o pid,ppid,cmd,%mem,%cpu
```

---

## 配置参考

### JVM 内存分配方案

| 区域 | 大小 | JVM 参数 | 占比 |
|------|------|---------|------|
| 堆内存 (Heap) | 512MB | `-Xms512m -Xmx512m` | 50% |
| 元空间 (Metaspace) | 128MB | `-XX:MaxMetaspaceSize=128m` | 13% |
| 代码缓存 (CodeCache) | 64MB | `-XX:ReservedCodeCacheSize=64m` | 6% |
| 直接内存 (DirectMem) | 64MB | `-XX:MaxDirectMemorySize=64m` | 6% |
| 线程栈 (ThreadStack) | ~100MB | `-Xss512k` × ~200 线程 | 10% |
| GC + JVM 内部 | ~90MB | Serial GC 零额外开销 | 9% |
| **合计** | **~958MB ≈ 1G** | | |

### GC 配置

| 参数 | 值 | 说明 |
|------|-----|------|
| `-XX:+UseSerialGC` | — | Serial GC：小堆最优，零额外开销 |

**选择理由**：
- ≤1G 堆内存下零额外内存开销（无 remembered sets / region tables / barriers）
- 吞吐量最高（无并发协调开销）
- 512MB 堆下 Full GC 暂停通常 <100ms
- Oracle 官方推荐小数据集使用 Serial GC

### 诊断配置

| 参数 | 值 | 说明 |
|------|-----|------|
| `-XX:+HeapDumpOnOutOfMemoryError` | — | OOM 时自动生成堆转储 |
| `-XX:HeapDumpPath` | `logs/heapdump.hprof` | 堆转储文件路径 |
| `-XX:+ExitOnOutOfMemoryError` | — | OOM 时立即退出（不挂起） |
| `-XX:+PrintCommandLineFlags` | — | 启动时打印 JVM 参数 |

### GC 日志配置

| 参数 | 值 | 说明 |
|------|-----|------|
| `-Xlog:gc*` | `file=logs/gc.log` | GC 日志文件 |
| `time,uptime,level,tags` | — | 日志内容格式 |
| `filecount=5` | — | 最多 5 个轮转文件 |
| `filesize=10M` | — | 每个文件最大 10MB |

### 环境变量参考

| 环境变量 | 默认值 | 说明 |
|---------|--------|------|
| `SPRING_PROFILES_ACTIVE` | `dev` | Spring Boot 激活 Profile |
| `DB_URL` | `jdbc:sqlite:./data_sqlite/mydb.db` | 数据库连接 URL（代码生成器使用） |
| `DB_USERNAME` | — | 数据库用户名（代码生成器使用） |
| `DB_PASSWORD` | — | 数据库密码（代码生成器使用） |

**start.sh 设置的 JVM 系统属性**：

| 属性 | 值 | 说明 |
|------|-----|------|
| `spring.profiles.active` | 命令行参数 | 激活 Profile |
| `file.encoding` | `UTF-8` | 文件编码 |
| `user.timezone` | `Asia/Shanghai` | 时区 |
| `java.security.egd` | `file:/dev/./urandom` | 随机数生成器（加速启动） |

### 目录结构参考

```
项目根目录/
├── scripts/
│   ├── start.sh              # 生产启动脚本
│   └── stop.sh               # 优雅停止脚本
├── logs/                      # 运行时日志目录（自动创建）
│   ├── web-quick-start-light.pid  # PID 文件
│   ├── app.log               # 应用日志（prod）
│   ├── app-2026-04-25.0.log.gz    # 轮转日志
│   ├── json.log              # JSON 结构化日志（prod）
│   ├── error.log             # 错误日志
│   ├── warn.log              # 警告日志（prod）
│   ├── current.log           # 会话日志（dev/test，覆盖写）
│   ├── slow-query.log        # 慢查询日志
│   ├── audit.log             # 审计日志
│   ├── gc.log                # GC 日志
│   └── heapdump.hprof        # OOM 堆转储（仅 OOM 时生成）
├── data_sqlite/               # SQLite 数据目录
│   └── mydb.db               # 数据库文件
└── app/target/
    └── app-0.0.1-SNAPSHOT.jar  # 可执行 JAR
```

---

## 检查清单

> 以下清单适用于生产部署前检查。

### 部署前

- [ ] JRE 版本 ≥ 25 已安装
- [ ] `mvn clean package -DskipTests` 构建成功
- [ ] JAR 文件存在于 `app/target/` 目录
- [ ] 磁盘可用空间 ≥ 2GB
- [ ] `application-prod.yaml` 配置已确认

### 启动后

- [ ] `./start.sh` 执行成功，输出 PID
- [ ] `tail -f logs/app.log` 确认启动完成
- [ ] Actuator 健康检查通过：`curl /actuator/health`
- [ ] Swagger UI 可访问：`http://host:9201/quickstart-light/openapi-doc.html`

### 停止验证

- [ ] `./stop.sh` 执行成功
- [ ] PID 文件已清理
- [ ] 进程已退出：`ps -p {pid}` 无结果

### 运维监控

- [ ] 错误日志正常：`grep ERROR logs/error.log | wc -l`
- [ ] GC 日志无异常：Full GC 频率 < 1次/小时
- [ ] 磁盘空间充足：`df -h` 确认日志目录
- [ ] 慢查询日志：`tail logs/slow-query.log` 检查

---

## 相关文档

| 文档 | 关系 | 说明 |
|------|------|------|
| 配置参考 | guides/configuration-reference.md | Spring Boot 多环境配置 |
| 日志体系 | infrastructure/logging-system.md | Logback Appender 详解 |
| 数据库 Schema | guides/database.md | SQLite 数据初始化 |

---

## 变更历史

| 版本 | 日期 | 变更内容 |
|------|------|---------|
| 1.0.0 | 2026-04-25 | 初始版本：20 条规则 + start.sh/stop.sh + JVM 参数 |
