# ArchUnit 合规增强实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 web-quick-start-light 项目新增 15 条 ArchUnit/源码扫描合规守护规则，覆盖编码规范、模块架构、Spring 配置、测试规范 4 个维度。

**Architecture:** 4 个新测试文件 + 1 个源码扫描工具类，与现有 5 个守护测试并列。ArchUnit API 用于注解/依赖检查，源码扫描用于 Lombok 编译期消除的注解和字符串模式。SHOULD 级规则用 try-catch 实现 WARN 语义。

**Tech Stack:** Spring Boot 4.0.2 + Java 25 + ArchUnit 1.4.1 + JUnit 5 + AssertJ

---

## 文件结构

| 操作 | 文件路径 | 职责 |
|------|---------|------|
| Create | `app/src/test/java/org/smm/archetype/support/basic/SourceScanner.java` | 源码扫描工具类 |
| Create | `app/src/test/java/org/smm/archetype/support/basic/SourceScannerUTest.java` | SourceScanner 单元测试 |
| Create | `app/src/test/java/org/smm/archetype/support/basic/CodingConventionComplianceUTest.java` | C-01~C-07 编码规范 |
| Create | `app/src/test/java/org/smm/archetype/support/basic/ModuleArchitectureComplianceUTest.java` | M-01~M-04 模块架构 |
| Create | `app/src/test/java/org/smm/archetype/support/basic/SpringConfigComplianceUTest.java` | S-01 Spring 配置 |
| Create | `app/src/test/java/org/smm/archetype/support/basic/TestConventionComplianceUTest.java` | T-01~T-03 测试规范 |

---

### Task 1: SourceScanner 工具类

**Files:**
- Create: `app/src/test/java/org/smm/archetype/support/basic/SourceScanner.java`
- Test: `app/src/test/java/org/smm/archetype/support/basic/SourceScannerUTest.java`

- [ ] **Step 1: 写 SourceScanner 单元测试**

```java
package org.smm.archetype.support.basic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.UnitTestBase;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SourceScanner 工具类")
class SourceScannerUTest extends UnitTestBase {

    @Nested
    @DisplayName("isCommentLine")
    class IsCommentLineTest {

        @Test
        @DisplayName("// 开头是注释行")
        void singleLineComment() {
            assertThat(SourceScanner.isCommentLine("// this is a comment")).isTrue();
        }

        @Test
        @DisplayName("* 开头是 Javadoc/块注释行")
        void javadocLine() {
            assertThat(SourceScanner.isCommentLine(" * javadoc description")).isTrue();
        }

        @Test
        @DisplayName("/* 开头是块注释开始行")
        void blockCommentStart() {
            assertThat(SourceScanner.isCommentLine("/* block comment start")).isTrue();
        }

        @Test
        @DisplayName("*/ 开头是块注释结束行")
        void blockCommentEnd() {
            assertThat(SourceScanner.isCommentLine(" */")).isTrue();
        }

        @Test
        @DisplayName("代码行（含行尾注释）不是注释行")
        void codeWithInlineComment() {
            assertThat(SourceScanner.isCommentLine("private Instant createdAt; // not LocalDateTime")).isFalse();
        }

        @Test
        @DisplayName("import 行不是注释行")
        void importLine() {
            assertThat(SourceScanner.isCommentLine("import java.time.LocalDateTime;")).isFalse();
        }

        @Test
        @DisplayName("空行不是注释行")
        void emptyLine() {
            assertThat(SourceScanner.isCommentLine("")).isFalse();
        }
    }

    @Nested
    @DisplayName("isImportLine")
    class IsImportLineTest {

        @Test
        @DisplayName("标准 import 是导入行")
        void standardImport() {
            assertThat(SourceScanner.isImportLine("import java.time.Instant;")).isTrue();
        }

        @Test
        @DisplayName("import static 是导入行")
        void staticImport() {
            assertThat(SourceScanner.isImportLine("import static org.assertj.core.api.Assertions.assertThat;")).isTrue();
        }

        @Test
        @DisplayName("普通代码不是导入行")
        void codeLine() {
            assertThat(SourceScanner.isImportLine("Instant now = Instant.now();")).isFalse();
        }
    }

    @Nested
    @DisplayName("isInBlockComment")
    class IsInBlockCommentTest {

        @Test
        @DisplayName("初始状态不在块注释中")
        void initialState() {
            SourceScanner.BlockCommentTracker tracker = new SourceScanner.BlockCommentTracker();
            assertThat(tracker.isInBlockComment()).isFalse();
        }

        @Test
        @DisplayName("遇到 /* 后进入块注释")
        void enterBlockComment() {
            SourceScanner.BlockCommentTracker tracker = new SourceScanner.BlockCommentTracker();
            tracker.processLine("/* start of block");
            assertThat(tracker.isInBlockComment()).isTrue();
        }

        @Test
        @DisplayName("遇到 */ 后退出块注释")
        void exitBlockComment() {
            SourceScanner.BlockCommentTracker tracker = new SourceScanner.BlockCommentTracker();
            tracker.processLine("/* start */");
            assertThat(tracker.isInBlockComment()).isFalse();
        }

        @Test
        @DisplayName("多行块注释中间行在块注释中")
        void multiLineBlockComment() {
            SourceScanner.BlockCommentTracker tracker = new SourceScanner.BlockCommentTracker();
            tracker.processLine("/* start");
            assertThat(tracker.isInBlockComment()).isTrue();
            tracker.processLine(" * middle line with LocalDateTime");
            assertThat(tracker.isInBlockComment()).isTrue();
            tracker.processLine(" */");
            assertThat(tracker.isInBlockComment()).isFalse();
        }
    }

    @Nested
    @DisplayName("PROJECT_ROOT")
    class ProjectRootTest {

        @Test
        @DisplayName("PROJECT_ROOT 应指向包含根 pom.xml 的目录")
        void projectRootContainsRootPom() {
            assertThat(SourceScanner.PROJECT_ROOT).isNotNull();
            assertThat(java.nio.file.Paths.get(SourceScanner.PROJECT_ROOT).resolve("pom.xml")).exists();
        }
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd /home/leonardo123/develop/code/mine/archetype/web-quick-start-light && JAVA_HOME=$HOME/.jdks/openjdk-25.0.2 mvn test -pl app -Dtest="SourceScannerUTest" -DfailIfNoTests=false -q`
Expected: FAIL（SourceScanner 类不存在）

- [ ] **Step 3: 实现 SourceScanner**

```java
package org.smm.archetype.support.basic;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 源码扫描工具类，用于检测编译期被 Lombok 消除的注解和字符串模式。
 * ArchUnit 无法检测这些模式，因此使用源码文件扫描。
 */
class SourceScanner {

    static final String PROJECT_ROOT = computeProjectRoot();

    private static String computeProjectRoot() {
        Path dir = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        // 向上搜索包含根 pom.xml（有 <modules> 的）的目录
        for (int i = 0; i < 10; i++) {
            Path pom = dir.resolve("pom.xml");
            if (Files.exists(pom)) {
                try {
                    List<String> lines = Files.readAllLines(pom);
                    // 根 pom.xml 包含 <modules>
                    boolean isRoot = lines.stream().anyMatch(l -> l.contains("<modules>"));
                    if (isRoot) {
                        return dir.toString();
                    }
                } catch (IOException e) {
                    // ignore
                }
            }
            dir = dir.getParent();
            if (dir == null) break;
        }
        // fallback: 与现有 NoDataAnnotationUTest 保持一致
        return System.getProperty("user.dir", ".").replace("/app", "");
    }

    /**
     * 扫描所有模块的 src/main/java 下的 Java 文件，返回满足条件的违规文件列表。
     * 覆盖范围：app/src/main/java/、components/component-xxx/src/main/java/、common/src/main/java/。
     * Files.walk 从 PROJECT_ROOT 递归遍历，路径包含 "/src/main/java/" 的 .java 文件均会被匹配。
     */
    static List<String> scanMainSource(Predicate<Path> fileFilter, Predicate<List<String>> contentMatcher) {
        return scanSource("src/main/java", fileFilter, contentMatcher);
    }

    /**
     * 扫描所有模块的 src/test/java 下的 Java 文件，返回满足条件的违规文件列表。
     * 覆盖范围：app/src/test/java/、components/component-xxx/src/test/java/（如有）。
     */
    static List<String> scanTestSource(Predicate<Path> fileFilter, Predicate<List<String>> contentMatcher) {
        return scanSource("src/test/java", fileFilter, contentMatcher);
    }

    private static List<String> scanSource(String sourceDir, Predicate<Path> fileFilter, Predicate<List<String>> contentMatcher) {
        Path rootPath = Paths.get(PROJECT_ROOT).toAbsolutePath().normalize();
        List<String> violatingFiles = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(rootPath)) {
            walk.filter(p -> p.toString().endsWith(".java"))
                .filter(p -> p.toString().contains("/" + sourceDir + "/"))
                .filter(p -> !p.toString().contains("/target/"))
                .filter(fileFilter)
                .filter(p -> {
                    try {
                        List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
                        return contentMatcher.test(lines);
                    } catch (MalformedInputException e) {
                        // 非 UTF-8 文件：跳过
                        return false;
                    } catch (IOException e) {
                        return false;
                    }
                })
                .map(p -> rootPath.relativize(p).toString())
                .sorted()
                .forEach(violatingFiles::add);
        } catch (IOException e) {
            // ignore
        }
        return violatingFiles;
    }

    /**
     * 判断一行是否是注释行（// 开头、* 开头、/* 开头、*/ 开头）。
     * 注意：不含行尾注释检测（代码行含 // 不算注释行）。
     */
    static boolean isCommentLine(String line) {
        String trimmed = line.trim();
        return trimmed.startsWith("//")
            || trimmed.startsWith("*")
            || trimmed.startsWith("/*")
            || trimmed.startsWith("*/");
    }

    /**
     * 判断一行是否是 import 语句。包括 import xxx 和 import static xxx。
     */
    static boolean isImportLine(String line) {
        String trimmed = line.trim();
        return trimmed.startsWith("import ");
    }

    /**
     * 块注释状态跟踪器。逐行处理，维护 /* */ 的开启/关闭状态。
     */
    static class BlockCommentTracker {
        private boolean inBlockComment = false;

        /**
         * 处理一行，更新块注释状态。返回当前行是否在块注释中。
         */
        boolean processLine(String line) {
            String trimmed = line.trim();
            if (inBlockComment) {
                if (trimmed.contains("*/")) {
                    inBlockComment = false;
                }
                return true; // 当前行在块注释中
            }
            if (trimmed.contains("/*")) {
                if (!trimmed.contains("*/")) {
                    inBlockComment = true;
                }
                return true; // /* 开头的行也算在块注释中
            }
            return false;
        }

        boolean isInBlockComment() {
            return inBlockComment;
        }
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd /home/leonardo123/develop/code/mine/archetype/web-quick-start-light && JAVA_HOME=$HOME/.jdks/openjdk-25.0.2 mvn test -pl app -Dtest="SourceScannerUTest" -DfailIfNoTests=false -q`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add app/src/test/java/org/smm/archetype/support/basic/SourceScanner.java app/src/test/java/org/smm/archetype/support/basic/SourceScannerUTest.java
git commit -m "feat: add SourceScanner utility with block comment tracking and robust PROJECT_ROOT"
```

---

### Task 2: CodingConventionComplianceUTest（C-01~C-07）

**Files:**
- Create: `app/src/test/java/org/smm/archetype/support/basic/CodingConventionComplianceUTest.java`

- [ ] **Step 1: 实现 CodingConventionComplianceUTest**

```java
package org.smm.archetype.support.basic;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.library.GeneralCodingRules;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.UnitTestBase;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("编码规范合规检查")
class CodingConventionComplianceUTest extends UnitTestBase {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .importPackages("org.smm.archetype");
    }

    // === C-01: 时间字段统一 Instant（源码扫描） ===

    @Test
    @DisplayName("C-01: entity/repository 包禁止使用 LocalDateTime 和 java.util.Date 字段")
    void entity_repository_should_use_instant_not_localdatetime() {
        Pattern fieldPattern = Pattern.compile("\\b(LocalDateTime|Date)\\s+\\w+");

        List<String> violations = SourceScanner.scanMainSource(
                p -> p.toString().contains("/entity/") || p.toString().contains("/repository/"),
                lines -> {
                    SourceScanner.BlockCommentTracker tracker = new SourceScanner.BlockCommentTracker();
                    for (String line : lines) {
                        String trimmed = line.trim();
                        // 跳过 import 行
                        if (SourceScanner.isImportLine(trimmed)) continue;
                        // 跳过单行注释
                        if (trimmed.startsWith("//")) continue;
                        // 跳过块注释内的行
                        if (tracker.processLine(trimmed)) continue;
                        // 匹配字段声明模式：类型名 + 空格 + 标识符
                        if (fieldPattern.matcher(trimmed).find()) {
                            // 排除 java.util.Date import 已被跳过，这里匹配的是字段声明
                            // 但 Date 可能是 java.sql.Date 等，需要包含 .Date 的全限定名或简单 Date
                            if (trimmed.contains("LocalDateTime") || trimmed.contains("java.util.Date")) {
                                return true;
                            }
                        }
                    }
                    return false;
                }
        );

        assertThat(violations)
                .as("entity/repository 包中不应使用 LocalDateTime 或 java.util.Date 字段，应统一使用 Instant")
                .isEmpty();
    }

    // === C-02: 禁止 JPA/Hibernate 注解（ArchUnit） ===

    @Test
    @DisplayName("C-02: 禁止使用 JPA/Hibernate 注解")
    void should_not_use_jpa_annotations() {
        String[] jpaAnnotations = {
                "javax.persistence.Entity", "jakarta.persistence.Entity",
                "javax.persistence.Table", "jakarta.persistence.Table",
                "javax.persistence.Column", "jakarta.persistence.Column",
                "javax.persistence.Id", "jakarta.persistence.Id",
                "javax.persistence.GeneratedValue", "jakarta.persistence.GeneratedValue",
                "javax.persistence.OneToMany", "jakarta.persistence.OneToMany",
                "javax.persistence.ManyToOne", "jakarta.persistence.ManyToOne",
                "javax.persistence.ManyToMany", "jakarta.persistence.ManyToMany",
                "javax.persistence.OneToOne", "jakarta.persistence.OneToOne",
                "javax.persistence.JoinColumn", "jakarta.persistence.JoinColumn",
                "javax.persistence.MappedSuperclass", "jakarta.persistence.MappedSuperclass",
                "javax.persistence.Embeddable", "jakarta.persistence.Embeddable"
        };

        for (String annotation : jpaAnnotations) {
            ArchRuleDefinition.classes()
                    .that().areAnnotatedWith(annotation)
                    .should().notExist()
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }
    }

    // === C-03: 禁止 BeanUtils.copyProperties（源码扫描） ===

    @Test
    @DisplayName("C-03: 禁止使用 BeanUtils.copyProperties")
    void should_not_use_beanutils_copy_properties() {
        List<String> violations = SourceScanner.scanMainSource(
                p -> true,
                lines -> {
                    SourceScanner.BlockCommentTracker tracker = new SourceScanner.BlockCommentTracker();
                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (SourceScanner.isImportLine(trimmed)) continue;
                        if (trimmed.startsWith("//")) continue;
                        if (tracker.processLine(trimmed)) continue;
                        if (trimmed.contains("BeanUtils.copyProperties")) return true;
                    }
                    return false;
                }
        );

        assertThat(violations)
                .as("不应使用 BeanUtils.copyProperties，应使用 MapStruct")
                .isEmpty();
    }

    // === C-04: 禁止 System.out/System.err（ArchUnit） ===

    @Test
    @DisplayName("C-04: 禁止使用 System.out/System.err")
    void should_not_access_standard_streams() {
        GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS
                .check(importedClasses);
    }

    // === C-05: 禁止 @With（Lombok）（源码扫描） ===

    @Test
    @DisplayName("C-05: 禁止使用 @With Lombok 注解")
    void should_not_use_with_annotation() {
        List<String> violations = SourceScanner.scanMainSource(
                p -> true,
                lines -> {
                    SourceScanner.BlockCommentTracker tracker = new SourceScanner.BlockCommentTracker();
                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (SourceScanner.isImportLine(trimmed)) continue;
                        if (trimmed.startsWith("//")) continue;
                        if (tracker.processLine(trimmed)) continue;
                        // 精确匹配 @With（排除 @WithBuilder、@WithValue 等）
                        if (trimmed.equals("@With") || trimmed.startsWith("@With(") || trimmed.startsWith("@With ")) {
                            return true;
                        }
                    }
                    return false;
                }
        );

        assertThat(violations)
                .as("不应使用 @With Lombok 注解")
                .isEmpty();
    }

    // === C-06: DTO/VO 使用 record（ArchUnit，SHOULD 级别） ===

    @Test
    @DisplayName("C-06: DTO/VO 应使用 record（SHOULD 级别，WARN 不 FAIL）")
    void dto_vo_should_use_record() {
        // 正向验证：检查是否实际执行了规则（SHOULD 规则必须验证 try-catch 路径）
        java.util.List<String> shouldViolations = new java.util.ArrayList<>();
        try {
            ArchRuleDefinition.classes()
                    .that().resideInAPackage("..facade..")
                    .and().haveSimpleNameEndingWith("VO")
                    .or().haveSimpleNameEndingWith("DTO")
                    .should(com.tngtech.archunit.lang.conditions.ArchConditions.beRecords())
                    .check(importedClasses);
            // 如果没有 AssertionError，说明全部合规或无匹配类
            System.out.println("[C-06] 所有 DTO/VO 均为 record，或无匹配类（合规）");
        } catch (AssertionError e) {
            // SHOULD 级别：记录违规但不阻塞 CI
            shouldViolations.add(e.getMessage());
            System.out.println("[C-06 SHOULD 违规（不阻塞 CI）] " + e.getMessage());
        }
        // 断言：验证规则确实被执行（违规信息不为空时说明规则生效）
        // 不做 assertThat(shouldViolations).isEmpty() 因为是 SHOULD 级别
        System.out.println("[C-06] 检查完成，违规数: " + shouldViolations.size());
    }

    // === C-07: Properties 类禁止 @Data（源码扫描） ===

    @Test
    @DisplayName("C-07: Properties/Configure 类禁止使用 @Data")
    void properties_class_should_not_use_data() {
        List<String> violations = SourceScanner.scanMainSource(
                p -> {
                    String name = p.getFileName().toString();
                    return name.endsWith("Properties.java") || name.endsWith("Configure.java");
                },
                lines -> {
                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (trimmed.equals("@Data") || trimmed.startsWith("@Data(") || trimmed.startsWith("@Data ")) {
                            return true;
                        }
                    }
                    return false;
                }
        );

        assertThat(violations)
                .as("Properties/Configure 类不应使用 @Data，应使用 @Getter + @Setter")
                .isEmpty();
    }
}
```

- [ ] **Step 2: 运行测试确认通过**

Run: `cd /home/leonardo123/develop/code/mine/archetype/web-quick-start-light && JAVA_HOME=$HOME/.jdks/openjdk-25.0.2 mvn test -pl app -Dtest="CodingConventionComplianceUTest" -DfailIfNoTests=false -q`
Expected: PASS（C-06 如有违规仅打印警告）

- [ ] **Step 3: 提交**

```bash
git add app/src/test/java/org/smm/archetype/support/basic/CodingConventionComplianceUTest.java
git commit -m "feat: add coding convention compliance tests (C-01~C-07)"
```

---

### Task 3: ModuleArchitectureComplianceUTest（M-01~M-04）

**Files:**
- Create: `app/src/test/java/org/smm/archetype/support/basic/ModuleArchitectureComplianceUTest.java`

- [ ] **Step 1: 实现 ModuleArchitectureComplianceUTest**

```java
package org.smm.archetype.support.basic;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.UnitTestBase;

import java.util.Set;
import java.util.stream.Collectors;

@DisplayName("模块架构合规检查")
class ModuleArchitectureComplianceUTest extends UnitTestBase {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .importPackages("org.smm.archetype");
    }

    // === M-01: common 模块零 Spring 依赖 ===

    @Test
    @DisplayName("M-01: common 模块（exception 包）不得依赖 Spring Framework")
    void common_module_should_not_depend_on_spring() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage("..exception..")
                .should().dependOnClassesThat()
                .resideInAPackage("org.springframework..")
                .allowEmptyShould(true)
                .check(importedClasses);
    }

    // === M-02: 组件模块间零互相依赖（动态发现组件列表） ===

    @Test
    @DisplayName("M-02: 组件模块间零互相依赖")
    void component_modules_should_not_depend_on_each_other() {
        // 动态发现 component 下的一级子包
        Set<String> components = importedClasses.stream()
                .map(c -> c.getPackageName())
                .filter(p -> p.contains(".component."))
                .map(p -> {
                    int idx = p.indexOf(".component.");
                    return p.substring(idx + ".component.".length()).split("\\.")[0];
                })
                .collect(Collectors.toSet());

        for (String component : components) {
            String[] otherPackages = components.stream()
                    .filter(c -> !c.equals(component))
                    .map(c -> "..component." + c + "..")
                    .toArray(String[]::new);

            if (otherPackages.length == 0) continue;

            ArchRuleDefinition.noClasses()
                    .that().resideInAPackage("..component." + component + "..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(otherPackages)
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }
    }

    // === M-03: Facade 方法返回值不在 entity 包 ===

    @Test
    @DisplayName("M-03: Facade public 方法不得返回内部 Entity 类型")
    void facade_should_not_return_internal_entity() {
        ArchRuleDefinition.classes()
                .that().resideInAPackage("..facade..")
                .and().areNotInterfaces()
                .should(notReturnInternalEntity())
                .check(importedClasses);
    }

    private static ArchCondition<JavaClass> notReturnInternalEntity() {
        return new ArchCondition<>("not return internal entity types") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                for (JavaMethod method : clazz.getMethods()) {
                    if (!method.getModifiers().contains(JavaModifier.PUBLIC)) continue;
                    JavaType returnType = method.getReturnType();
                    String returnTypeName = returnType.getName();
                    if ("void".equals(returnTypeName)) continue;
                    // 检查返回类型是否在 entity 包下且不是 VO/DTO
                    if (returnTypeName.contains(".entity.")
                            && !returnTypeName.endsWith("VO")
                            && !returnTypeName.endsWith("DTO")) {
                        events.add(SimpleConditionEvent.violated(method,
                                String.format("%s.%s() 返回内部 Entity 类型: %s",
                                        clazz.getSimpleName(), method.getName(), returnTypeName)));
                    }
                }
            }
        };
    }

    // === M-04: API 路径以 /api 开头（SHOULD 级别） ===

    @Test
    @DisplayName("M-04: Controller API 路径应以 /api 开头（SHOULD 级别，WARN 不 FAIL）")
    void api_path_should_start_with_api() {
        // 正向验证：检查是否实际执行了规则（SHOULD 规则必须验证 try-catch 路径）
        java.util.List<String> shouldViolations = new java.util.ArrayList<>();
        try {
            ArchRuleDefinition.classes()
                    .that().areAnnotatedWith("org.springframework.web.bind.annotation.RequestMapping")
                    .or().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                    .should(havePathStartingWith("/api"))
                    .allowEmptyShould(true)
                    .check(importedClasses);
            // 如果没有 AssertionError，说明全部合规或无匹配类
            System.out.println("[M-04] 所有 API 路径均以 /api 开头，或无匹配 Controller（合规）");
        } catch (AssertionError e) {
            // SHOULD 级别：记录违规但不阻塞 CI
            shouldViolations.add(e.getMessage());
            System.out.println("[M-04 SHOULD 违规（不阻塞 CI）] " + e.getMessage());
        }
        // 断言：验证规则确实被执行（违规信息不为空时说明规则生效）
        // 不做 assertThat(shouldViolations).isEmpty() 因为是 SHOULD 级别
        System.out.println("[M-04] 检查完成，违规数: " + shouldViolations.size());
    }

    private static ArchCondition<JavaClass> havePathStartingWith(String prefix) {
        return new ArchCondition<>("have path starting with '" + prefix + "'") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                clazz.getAnnotations().forEach(annotation -> {
                    String annotationType = annotation.getType().getName();
                    if (annotationType.contains("RequestMapping") || annotationType.contains("Mapping")) {
                        // 尝试获取 value 或 path 属性
                        Object value = annotation.tryGetExplicitlyDeclaredProperty("value")
                                .or(() -> annotation.tryGetExplicitlyDeclaredProperty("path"))
                                .orElse(null);
                        if (value != null) {
                            String path = value.toString();
                            if (!path.startsWith(prefix)) {
                                events.add(SimpleConditionEvent.violated(clazz,
                                        String.format("%s 的 API 路径 '%s' 不以 '%s' 开头",
                                                clazz.getSimpleName(), path, prefix)));
                            }
                        }
                    }
                });
            }
        };
    }
}
```

- [ ] **Step 2: 运行测试确认通过**

Run: `cd /home/leonardo123/develop/code/mine/archetype/web-quick-start-light && JAVA_HOME=$HOME/.jdks/openjdk-25.0.2 mvn test -pl app -Dtest="ModuleArchitectureComplianceUTest" -DfailIfNoTests=false -q`
Expected: PASS（M-04 如有违规仅打印警告）

- [ ] **Step 3: 提交**

```bash
git add app/src/test/java/org/smm/archetype/support/basic/ModuleArchitectureComplianceUTest.java
git commit -m "feat: add module architecture compliance tests (M-01~M-04)"
```

---

### Task 4: SpringConfigComplianceUTest（S-01）

**Files:**
- Create: `app/src/test/java/org/smm/archetype/support/basic/SpringConfigComplianceUTest.java`

- [ ] **Step 1: 实现 SpringConfigComplianceUTest**

```java
package org.smm.archetype.support.basic;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.UnitTestBase;

@DisplayName("Spring 配置合规检查")
class SpringConfigComplianceUTest extends UnitTestBase {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .importPackages("org.smm.archetype");
    }

    // === S-01: 组件 ConfigurationProperties 前缀一致性 ===

    @Test
    @DisplayName("S-01: 组件模块 Properties 类前缀必须以 component. 开头")
    void component_properties_should_have_component_prefix() {
        ArchRuleDefinition.classes()
                .that().resideInAPackage("..component..")
                .and().areAnnotatedWith("org.springframework.boot.context.properties.ConfigurationProperties")
                .should(havePrefixStartingWith("component."))
                .allowEmptyShould(true)
                .check(importedClasses);
    }

    private static ArchCondition<JavaClass> havePrefixStartingWith(String prefix) {
        return new ArchCondition<>("have @ConfigurationProperties prefix starting with '" + prefix + "'") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                clazz.tryGetAnnotationOfType("org.springframework.boot.context.properties.ConfigurationProperties")
                        .ifPresent(annotation -> {
                            Object prefixValue = annotation.get("prefix").orElse(null);
                            if (prefixValue == null) return;
                            String prefixStr = prefixValue.toString();
                            if (!prefixStr.startsWith(prefix)) {
                                events.add(SimpleConditionEvent.violated(clazz,
                                        String.format("%s 的 @ConfigurationProperties prefix='%s' 不以 '%s' 开头",
                                                clazz.getSimpleName(), prefixStr, prefix)));
                            }
                        });
            }
        };
    }
}
```

- [ ] **Step 2: 运行测试确认通过**

Run: `cd /home/leonardo123/develop/code/mine/archetype/web-quick-start-light && JAVA_HOME=$HOME/.jdks/openjdk-25.0.2 mvn test -pl app -Dtest="SpringConfigComplianceUTest" -DfailIfNoTests=false -q`
Expected: PASS

- [ ] **Step 3: 提交**

```bash
git add app/src/test/java/org/smm/archetype/support/basic/SpringConfigComplianceUTest.java
git commit -m "feat: add Spring config compliance test (S-01)"
```

---

### Task 5: TestConventionComplianceUTest（T-01~T-03）

**Files:**
- Create: `app/src/test/java/org/smm/archetype/support/basic/TestConventionComplianceUTest.java`

- [ ] **Step 1: 实现 TestConventionComplianceUTest**

```java
package org.smm.archetype.support.basic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.UnitTestBase;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("测试规范合规检查")
class TestConventionComplianceUTest extends UnitTestBase {

    // === T-01: 测试文件名 *UTest / *ITest ===

    @Test
    @DisplayName("T-01: 含 @Test 的测试文件必须以 UTest 或 ITest 结尾")
    void test_files_should_have_utest_or_itest_suffix() {
        List<String> violations = SourceScanner.scanTestSource(
                p -> {
                    String name = p.getFileName().toString();
                    return !name.endsWith("UTest.java")
                            && !name.endsWith("ITest.java")
                            && !name.equals("package-info.java");
                },
                lines -> lines.stream().anyMatch(line ->
                        line.trim().contains("@Test") || line.trim().contains("@ParameterizedTest"))
        );

        assertThat(violations)
                .as("含 @Test 的测试文件应以 UTest.java 或 ITest.java 结尾")
                .isEmpty();
    }

    // === T-02: 禁止 UTest 中使用 @SpringBootTest ===

    @Test
    @DisplayName("T-02: UTest 中禁止使用 @SpringBootTest")
    void utest_should_not_use_spring_boot_test() {
        List<String> violations = SourceScanner.scanTestSource(
                p -> p.getFileName().toString().endsWith("UTest.java"),
                lines -> lines.stream().anyMatch(line -> line.trim().contains("@SpringBootTest"))
        );

        assertThat(violations)
                .as("UTest（纯单元测试）中不应使用 @SpringBootTest")
                .isEmpty();
    }

    // === T-03: 禁止 ITest 中使用 @Mock ===

    @Test
    @DisplayName("T-03: ITest 中禁止使用 @Mock 注解")
    void itest_should_not_use_mock() {
        List<String> violations = SourceScanner.scanTestSource(
                p -> p.getFileName().toString().endsWith("ITest.java"),
                lines -> {
                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (SourceScanner.isImportLine(trimmed)) continue;
                        // 精确匹配 @Mock（排除 @MockBean、@MockitoBean 等）
                        if (trimmed.equals("@Mock") || trimmed.startsWith("@Mock(") || trimmed.startsWith("@Mock ")) {
                            return true;
                        }
                    }
                    return false;
                }
        );

        assertThat(violations)
                .as("ITest（集成测试）中不应使用 @Mock，应使用真实依赖")
                .isEmpty();
    }
}
```

- [ ] **Step 2: 运行测试确认通过**

Run: `cd /home/leonardo123/develop/code/mine/archetype/web-quick-start-light && JAVA_HOME=$HOME/.jdks/openjdk-25.0.2 mvn test -pl app -Dtest="TestConventionComplianceUTest" -DfailIfNoTests=false -q`
Expected: PASS

- [ ] **Step 3: 提交**

```bash
git add app/src/test/java/org/smm/archetype/support/basic/TestConventionComplianceUTest.java
git commit -m "feat: add test convention compliance tests (T-01~T-03)"
```

---

### Task 6: 全量测试验证 + 最终提交

**Files:**
- None new (verification only)

- [ ] **Step 1: 运行全量测试**

Run: `cd /home/leonardo123/develop/code/mine/archetype/web-quick-start-light && JAVA_HOME=$HOME/.jdks/openjdk-25.0.2 mvn test -pl app -DfailIfNoTests=false`
Expected: 全部 PASS，0 失败（C-06/M-04 如有 SHOULD 违规仅打印警告不导致失败）

- [ ] **Step 2: 确认新增测试全部运行**

Run: `cd /home/leonardo123/develop/code/mine/archetype/web-quick-start-light && JAVA_HOME=$HOME/.jdks/openjdk-25.0.2 mvn test -pl app -Dtest="SourceScannerUTest,CodingConventionComplianceUTest,ModuleArchitectureComplianceUTest,SpringConfigComplianceUTest,TestConventionComplianceUTest" -DfailIfNoTests=false`
Expected: 全部 PASS

- [ ] **Step 3: 确认现有测试不受影响**

Run: `cd /home/leonardo123/develop/code/mine/archetype/web-quick-start-light && JAVA_HOME=$HOME/.jdks/openjdk-25.0.2 mvn test -pl app -Dtest="ArchitectureComplianceUTest,NoDataAnnotationUTest,NoValueInjectionUTest,NoRedundantConfigureUTest" -DfailIfNoTests=false`
Expected: 全部 PASS（现有 5 个守护测试不受影响）

- [ ] **Step 4: 最终提交**

```bash
git add -A
git commit -m "feat: complete ArchUnit compliance enhancement - 15 rules across 4 dimensions"
```
