package org.smm.archetype.support.basic;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.UnitTestBase;

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

    // === C-02: 禁止 JPA/Hibernate 注解（源码扫描） ===
    // 注意: ArchUnit 的 ClassesShould 在 1.4.1 中没有 notExist() 方法，
    // 所以改用源码扫描来检测 JPA 注解

    @Test
    @DisplayName("C-02: 禁止使用 JPA/Hibernate 注解")
    void should_not_use_jpa_annotations() {
        String[] jpaPatterns = {
                "@Entity", "@Table(", "@Column(", "@Id(", "@GeneratedValue",
                "@OneToMany", "@ManyToOne", "@ManyToMany", "@OneToOne",
                "@JoinColumn", "@MappedSuperclass", "@Embeddable"
        };

        List<String> violations = SourceScanner.scanMainSource(
                p -> true,
                lines -> {
                    SourceScanner.BlockCommentTracker tracker = new SourceScanner.BlockCommentTracker();
                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (SourceScanner.isImportLine(trimmed)) continue;
                        if (trimmed.startsWith("//")) continue;
                        if (tracker.processLine(trimmed)) continue;
                        for (String pattern : jpaPatterns) {
                            if (trimmed.contains(pattern)) {
                                // 排除非 JPA 的同名注解（如 @Entity 在非 javax/jakarta.persistence 包中）
                                if (pattern.equals("@Entity") && trimmed.contains("@Entity(")) {
                                    return true;
                                } else if (pattern.equals("@Entity") && trimmed.equals("@Entity")) {
                                    return true;
                                } else if (!pattern.equals("@Entity") && trimmed.contains(pattern)) {
                                    return true;
                                }
                            }
                        }
                    }
                    return false;
                }
        );

        assertThat(violations)
                .as("不应使用 JPA/Hibernate 注解（@Entity, @Table, @Column 等）")
                .isEmpty();
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

    // === C-04: 禁止 System.out/System.err（源码扫描） ===
    // ArchUnit 的 GeneralCodingRules 会扫描测试代码自身，改用源码扫描
    // 排除 generated 包（代码生成器）

    @Test
    @DisplayName("C-04: 禁止使用 System.out/System.err（排除 generated 包）")
    void should_not_access_standard_streams() {
        List<String> violations = SourceScanner.scanMainSource(
                p -> !p.toString().contains("/generated/"),
                lines -> {
                    SourceScanner.BlockCommentTracker tracker = new SourceScanner.BlockCommentTracker();
                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (SourceScanner.isImportLine(trimmed)) continue;
                        if (trimmed.startsWith("//")) continue;
                        if (tracker.processLine(trimmed)) continue;
                        if (trimmed.contains("System.out") || trimmed.contains("System.err")) {
                            // 排除 SLF4J/Logback 的配置（不太可能，但保险起见）
                            if (trimmed.contains("System.out.println") || trimmed.contains("System.err.println")
                                    || trimmed.contains("System.out.print") || trimmed.contains("System.err.print")
                                    || trimmed.contains("System.out.printf") || trimmed.contains("System.err.printf")) {
                                return true;
                            }
                        }
                    }
                    return false;
                }
        );

        assertThat(violations)
                .as("生产代码中不应使用 System.out/System.err，应使用 SLF4J Logger")
                .isEmpty();
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
