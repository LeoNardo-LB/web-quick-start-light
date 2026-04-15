## 1. 准备工作

- [x] 1.1 确认技能已加载
- [x] 1.2 创建 feature 分支

## 2. Chunk 1：目录重命名

- [x] 2.1-2.2 目录重命名 clients/ → components/ 完成

## 3. Chunk 2：Java 包名和类名重命名

- [x] 3.1-3.8 全部 6 个子模块 Java 包名+类名重命名完成，app 模块 import 替换完成，编译和测试通过

## 4. Chunk 3：配置前缀替换 + YAML 文件重命名

- [x] 4.1-4.5 所有 Properties/AutoConfiguration 前缀 middleware.* → component.* 替换完成，YAML 文件更新完成

## 5. Chunk 4：POM 文件 + AutoConfiguration 注册更新

- [x] 5.1-5.6 所有 POM 和 AutoConfiguration.imports 更新完成，全量构建通过

## 6. Chunk 5：文档更新

- [x] 6.1.1 AGENTS.md 全面更新完成
- [x] 6.1.2 README.md 更新完成
- [x] 6.2.1 docs/architecture/module-structure.md 更新完成
- [x] 6.2.2 docs/architecture/system-overview.md 更新完成
- [x] 6.2.3 docs/architecture/design-patterns.md 更新完成
- [x] 6.3.1 docs/conventions/configuration.md 更新完成
- [x] 6.3.2 docs/conventions/testing-conventions.md 更新完成
- [x] 6.3.3 docs/conventions/error-handling.md 更新完成
- [x] 6.4.1-6.4.6 docs/modules/ 下 6 个文件重命名+内容更新完成
- [x] 6.5.1-6.5.5 docs/modules/README.md、auth.md、operation-log.md、system-config.md、docs/README.md 更新完成
- [x] 6.6.1-6.6.2 文档残留验证通过（活跃文档无 client-*/middleware. 残留）

## 7. 最终验证

- [x] 7.1 `mvn clean package -DskipTests` BUILD SUCCESS
- [x] 7.2 `mvn clean test` — 484 tests, 0 failures, BUILD SUCCESS
- [x] 7.3 全局搜索 `org.smm.archetype.client` — 0 结果
- [x] 7.4 全局搜索 `middleware.` — 0 结果
- [x] 7.5 全局搜索 `client-*` 模块名 — 0 结果（活跃代码/文档中）
- [x] 7.6 `application-optional.yaml` 不存在，`application-component.yaml` 存在

## 8. 一致性检查

- [x] 8.1 进行 artifact 文档、讨论结果的一致性检查

## 9. 收尾清理

- [x] 9.1 修复 ContextFillFilterUTest.java 中 5 处 @DisplayName 残留（AuthClient → AuthComponent）
- [x] 9.2 清理 .idea/encodings.xml 旧路径引用（删除 20 条已不存在的 archetype-clients/ 和 clients/ 条目）
- [x] 9.3 清理 .idea/compiler.xml 旧模块引用（删除 10 条 bytecodeTargetLevel + 10 条 javacSettings）
- [x] 9.4 清理 .idea/modules.xml 已删除模块引用（client-idempotent, client-log, client-ratelimit）
- [x] 9.5 修复 docs/conventions/configuration.md 中 application-optional.yaml 残留引用
- [x] 9.6 `mvn clean test` — 484 tests, 0 failures, BUILD SUCCESS ✅
