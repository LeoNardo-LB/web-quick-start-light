# shared/ 功能域隔离守护 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增 ArchUnit 规则 S-02，守护 shared/ 下 15 个功能域的分层依赖关系

**Architecture:** 二层分层模型 — 底层 8 包零互依赖 + 底层不依赖上层，上层可依赖底层和上层。新增规则 5 检测未分类功能域防止静默忽略。使用 ArchUnit `noClasses().should().dependOnClassesThat().resideInAnyPackage()` API。

**Tech Stack:** Java 25, ArchUnit 1.4.1, JUnit 5, AssertJ

**Spec:** `docs/superpowers/specs/2026-05-14-shared-isolation-design.md`

---

## File Structure

| 操作 | 文件 | 职责 |
|------|------|------|
| Create | `app/src/test/java/org/smm/archetype/support/basic/SharedIsolationUTest.java` | S-02 规则测试类 |
| Modify | `docs/conventions/java-conventions.md` | 新增 S-02 规则说明 |
| Modify | `docs/architecture/module-structure.md` | ArchUnit 规则列表 44→45 |

---

### Task 1: 创建 SharedIsolationUTest.java

**Files:**
- Create: `app/src/test/java/org/smm/archetype/support/basic/SharedIsolationUTest.java`
- Reference: `app/src/test/java/org/smm/archetype/support/basic/SpringConfigComplianceUTest.java`（S-01 模板）
- Reference: `app/src/test/java/org/smm/archetype/support/basic/ModuleArchitectureComplianceUTest.java`（M-10 slices 用法 + M-07 noClasses 用法）

- [ ] **Step 1: 创建测试文件**

```java
package org.smm.archetype.support.basic;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.UnitTestBase;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L3 单元测试 — 验证 shared/ 功能域分层隔离约束（S-02）。
 * <p>
 * 底层包（8个）之间零互依赖，不依赖上层包。
 * 上层包（6个）可依赖底层和上层。
 * 未分类功能域检测：shared/ 下所有子包必须在 BOTTOM 或 TOP 集合中登记。
 * <p>
 * 覆盖能力: shared-isolation (S-02)
 */
@DisplayName("shared/ 功能域分层隔离")
class SharedIsolationUTest extends UnitTestBase {

    private static final String BASE = "..archetype.shared.";
    private static final Set<String> BOTTOM = Set.of(
            "context", "event", "idempotent", "logging",
            "operationlog", "pagination", "ratelimit", "util"
    );
    private static final Set<String> TOP = Set.of(
            "dal", "generated", "mybatis", "result", "threadpool", "web"
    );

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .importPackages("org.smm.archetype");
    }

    // === 规则 1: 底层包之间零互相依赖 ===

    @Test
    @DisplayName("S-02 规则1: 底层包之间零互相依赖")
    void shared_bottom_packages_should_not_depend_on_each_other() {
        for (String pkg : BOTTOM) {
            Set<String> otherBottom = BOTTOM.stream()
                    .filter(p -> !p.equals(pkg))
                    .map(p -> BASE + p + "..")
                    .collect(Collectors.toSet());

            ArchRuleDefinition.noClasses()
                    .that().resideInAPackage(BASE + pkg + "..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(otherBottom.toArray(String[]::new))
                    .allowEmptyShould(true)
                    .because("底层包 '" + pkg + "' 不得依赖同层其他底层包（S-02 规则1: shared/ 功能域分层隔离）")
                    .check(importedClasses);
        }
    }

    // === 规则 2: 底层包不得依赖上层包 ===

    @Test
    @DisplayName("S-02 规则2: 底层包不得依赖上层包")
    void shared_bottom_packages_should_not_depend_on_top_packages() {
        String[] topPackages = TOP.stream()
                .map(p -> BASE + p + "..")
                .toArray(String[]::new);

        for (String pkg : BOTTOM) {
            ArchRuleDefinition.noClasses()
                    .that().resideInAPackage(BASE + pkg + "..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(topPackages)
                    .allowEmptyShould(true)
                    .because("底层包 '" + pkg + "' 不得依赖上层包（S-02 规则2: shared/ 功能域分层隔离）")
                    .check(importedClasses);
        }
    }

    // === 规则 5: 未分类功能域检测 ===

    @Test
    @DisplayName("S-02 规则5: 所有 shared/ 子包必须被分类")
    void all_shared_subpackages_should_be_classified() {
        // 找到 shared 包下所有有 Java 类的直接子包
        Set<String> actualSubPackages = importedClasses.stream()
                .map(clazz -> clazz.getPackageName())
                .filter(pkg -> pkg.startsWith("org.smm.archetype.shared."))
                .map(pkg -> {
                    // 提取 shared 下的第一级子包名
                    String suffix = pkg.substring("org.smm.archetype.shared.".length());
                    int dotIndex = suffix.indexOf('.');
                    return dotIndex > 0 ? suffix.substring(0, dotIndex) : suffix;
                })
                .filter(subPkg -> !subPkg.isEmpty())
                .collect(Collectors.toSet());

        // 排除 internal（已知的空包/特殊包）
        Set<String> classified = actualSubPackages.stream()
                .filter(p -> !p.equals("internal"))
                .collect(Collectors.toSet());

        Set<String> allKnown = java.util.stream.Stream.concat(
                        BOTTOM.stream(), TOP.stream())
                .collect(Collectors.toUnmodifiableSet());

        assertThat(classified)
                .as("shared/ 下所有有 Java 类的子包（排除 internal）必须在 BOTTOM(底层) 或 TOP(上层) 集合中登记。" +
                        "当前未分类的子包: %s", classified.stream()
                        .filter(p -> !allKnown.contains(p))
                        .collect(Collectors.toSet()))
                .isSubsetOf(allKnown);
    }
}
```

- [ ] **Step 2: 运行测试验证全部通过**

Run: `JAVA_HOME="/home/leonardo123/.jdks/openjdk-25.0.2" mvn test -pl app -Dtest="SharedIsolationUTest" -DfailIfNoTests=false`
Expected: 3 tests passed, 0 failures

- [ ] **Step 3: 运行全量测试确认无回归**

Run: `JAVA_HOME="/home/leonardo123/.jdks/openjdk-25.0.2" mvn test -pl app`
Expected: 589 tests (586 + 3), 0 failures, 0 errors, 4 skipped

- [ ] **Step 4: Commit**

```bash
git add app/src/test/java/org/smm/archetype/support/basic/SharedIsolationUTest.java
git commit -m "feat: add S-02 shared/ domain isolation ArchUnit rules (bottom-zero-dep + unclassified-detection)"
```

---

### Task 2: 同步文档 — java-conventions.md

**Files:**
- Modify: `docs/conventions/java-conventions.md`

- [ ] **Step 1: 在 java-conventions.md 中新增 S-02 规则说明**

先查看当前结构：
```bash
scripts/md-sections docs/conventions/java-conventions.md
```

在 ArchUnit 规则章节的 S-01 后面追加 S-02 规则说明。内容：

```
#### S-02: shared/ 功能域分层隔离
- **强度**: ⛔ MUST
- **规则**:
  1. 底层包（context/event/idempotent/logging/operationlog/pagination/ratelimit/util）之间零互依赖
  2. 底层包不得依赖上层包（dal/generated/mybatis/result/threadpool/web）
  3. 所有 shared/ 子包（排除 internal）必须在 BOTTOM 或 TOP 集合中登记
- **测试文件**: `SharedIsolationUTest.java`
```

- [ ] **Step 2: Commit**

```bash
git add docs/conventions/java-conventions.md
git commit -m "docs: add S-02 shared/ domain isolation rule to java-conventions"
```

---

### Task 3: 同步文档 — module-structure.md

**Files:**
- Modify: `docs/architecture/module-structure.md`

- [ ] **Step 1: 在 module-structure.md 的 ArchUnit 规则列表中追加 S-02**

先查看当前结构：
```bash
scripts/md-sections docs/architecture/module-structure.md
```

在 ArchUnit 守护规则表格的 S-01 行后面追加：
```
| S-02 | shared/ 功能域分层隔离 | `SharedIsolationUTest` | 底层包零互依赖 + 底层不依赖上层 + 未分类检测 | ⛔ MUST |
```

更新规则总数：44→45。

- [ ] **Step 2: Commit**

```bash
git add docs/architecture/module-structure.md
git commit -m "docs: add S-02 to ArchUnit rules table in module-structure (44→45)"
```

---

### Task 4: 最终验证

- [ ] **Step 1: 全量测试**

Run: `JAVA_HOME="/home/leonardo123/.jdks/openjdk-25.0.2" mvn test -pl app`
Expected: 589 tests, 0 failures, 0 errors, 4 skipped

- [ ] **Step 2: 破坏性测试验证（手动确认后恢复）**

在底层包（如 `shared/context/BizContext.java`）中临时添加一行 `import org.smm.archetype.shared.event.DomainEvent;`，运行 `SharedIsolationUTest`，确认规则 1 报错。然后删除该 import 恢复原状。

- [ ] **Step 3: 清理审查报告**

```bash
rm doc-review-report.md
git add -A && git rebase --autosquash HEAD~3  # 如果需要的话合并 commits
```
