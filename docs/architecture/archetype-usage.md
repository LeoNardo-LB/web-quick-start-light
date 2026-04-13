# 骨架使用

> 🟢 Contract 轨 — 100% 反映代码现状

## 📋 目录

- [概述](#概述)
- [骨架元数据](#骨架元数据)
- [本地安装骨架](#本地安装骨架)
- [使用骨架创建新项目](#使用骨架创建新项目)
- [骨架排除文件](#骨架排除文件)
- [故障排查](#故障排查)
- [相关文档](#相关文档)
- [变更历史](#变更历史)

## 概述

本文档说明如何将本项目作为 Maven Archetype 骨架使用，快速创建基于相同技术栈的新项目。

### 骨架使用流程

```mermaid
flowchart LR
    A[安装骨架到本地仓库] --> B[从骨架创建新项目]
    B --> C[调整包名和配置]
    C --> D[删除不需要的模块]
    D --> E[启动并验证]
```

## 骨架元数据

| 参数 | 值 |
|------|-----|
| groupId | `org.smm.archetype` |
| artifactId | `web-quick-start-light` |
| version | `0.0.1-SNAPSHOT` |
| Java | 25 |
| Spring Boot | 4.x |
| ORM | MyBatis-Plus 3.5.x |
| 数据库 | SQLite 3.x |
| 认证 | Sa-Token 1.45.x |
| 限流 | Bucket4j 8.17.x |

> 精确版本号见 `pom.xml` 或 [system-overview.md](system-overview.md)。

## 本地安装骨架

### 1. 引入骨架构建插件

在项目根 `pom.xml` 中添加：

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-archetype-plugin</artifactId>
            <version>3.4.0</version>
        </plugin>
    </plugins>
</build>
```

### 2. 创建骨架文件

**macOS / Linux：**

```shell
mvn archetype:create-from-project
```

**Windows：**

```shell
mvn archetype:create-from-project -s <你的 settings.xml 完整路径>
```

执行完毕后，项目中生成 `target/generated-sources/archetype` 目录。

### 3. 本地安装骨架

```shell
cd target/generated-sources/archetype
mvn install
```

安装成功后，本地 Maven 仓库根目录下会出现 `archetype-catalog.xml`，其中包含骨架坐标。

## 使用骨架创建新项目

### 方式一：命令行（推荐）

**macOS / Linux：**

```shell
mvn archetype:generate \
  -DarchetypeCatalog=local \
  -DinteractiveMode=false \
  -DarchetypeGroupId=org.smm.archetype \
  -DarchetypeArtifactId=web-quick-start-light \
  -DarchetypeVersion=0.0.1-SNAPSHOT \
  -DgroupId=org.example \
  -DartifactId=my-project \
  -Dversion=1.0.0-SNAPSHOT
```

**Windows：**

```shell
mvn archetype:generate -DarchetypeCatalog=local -DinteractiveMode=false -DarchetypeGroupId=org.smm.archetype -DarchetypeArtifactId=web-quick-start-light -DarchetypeVersion=0.0.1-SNAPSHOT -DgroupId=org.example -DartifactId=my-project -Dversion=1.0.0-SNAPSHOT
```

> **Windows 注意**：如果使用 `^` 续行符，`^` 后不能有任何字符（包括空格）。

### 方式二：IDEA 界面

1. 在 IDEA 中打开 `Settings → Build → Maven → Runner → VM Options` 或在新建项目向导中选择「Create from archetype」
2. 添加本地 archetype-catalog.xml（位于本地 Maven 仓库根目录）
3. 选择 `org.smm.archetype:web-quick-start-light` 骨架
4. 填写新项目的 groupId / artifactId / version

## 骨架排除文件

骨架生成时会自动排除以下文件，不会出现在新项目中：

| 排除规则 | 说明 |
|----------|------|
| `**/.idea/**` | IDE 配置 |
| `**/target/*` | 构建输出 |
| `logs/**` | 日志目录 |
| `**/*.iml` | IntelliJ 模块文件 |
| `README.md` | 项目 README（骨架自带） |
| `data_h2/**` | H2 数据文件 |

## 故障排查

| 错误 | 原因 | 解决方案 |
|------|------|----------|
| `ResourceManager: unable to find resource 'archetype-resources/.../pom.xml'` | `archetype-metadata.xml` 中子模块的 `dir` 路径不正确 | 编辑 `target/generated-sources/archetype/src/main/resources/META-INF/maven/archetype-metadata.xml`，为多模块子项目添加正确的前缀（如 `app/`） |
| `The specified user settings file does not exist` | Maven 找不到 settings.xml | 使用 `-s` 参数指定 settings.xml 完整路径 |
| `Archetype IT 'basic' failed` | 集成测试阶段找不到资源 | 在 `archetype-metadata.xml` 中修正 fileset/module 的目录路径后重新 `mvn install` |
| `Cannot resolve org.smm.archetype:common:...` | 骨架安装未完成或本地仓库缓存过期 | 先在骨架项目根目录执行 `mvn clean install -DskipTests`，确保所有模块安装到本地仓库 |
| `Java version 25 required` | 项目要求 Java 25，当前 JDK 版本不满足 | 安装 JDK 25 并设置 `JAVA_HOME`，运行 `java -version` 确认版本 |
| `Spring Boot 应用启动失败: No qualifying bean of type AuthClient` | 认证客户端未正确装配 | 检查 `middleware.auth.enabled` 配置（默认 `true`）；如使用 Sa-Token，确认 `sa-token` 依赖在 classpath 中；如不需要认证，确认 NoOp 自动配置生效 |
| `生成的项目编译失败: package xxx does not exist` | 骨架生成时包名替换异常 | 检查 archetype-metadata.xml 中的 `package` 属性是否正确；手动检查生成项目的 import 语句是否与实际包名一致 |

## 相关文档

- [system-overview.md](system-overview.md) — 系统全景和技术栈概要
- [module-structure.md](module-structure.md) — 多模块结构说明
- [docs/README.md](../README.md) — 文档系统导航
- [README.md](../../README.md) — 项目概述

## 变更历史

| 日期 | 变更内容 |
|------|---------|
| 2026-04-14 | 从 ARCHETYPE_README.md 迁移至 docs/architecture/archetype-usage.md |
