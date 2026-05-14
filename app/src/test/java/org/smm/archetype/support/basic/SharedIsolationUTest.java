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
        Set<String> allKnown = java.util.stream.Stream.concat(
                        BOTTOM.stream(), TOP.stream())
                .collect(Collectors.toUnmodifiableSet());

        // 找到 shared 包下所有有 Java 类的直接子包
        Set<String> actualSubPackages = importedClasses.stream()
                .map(clazz -> clazz.getPackageName())
                .filter(pkg -> pkg.startsWith("org.smm.archetype.shared."))
                .map(pkg -> {
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

        assertThat(classified)
                .as("shared/ 下所有有 Java 类的子包（排除 internal）必须在 BOTTOM(底层) 或 TOP(上层) 集合中登记。" +
                        "当前未分类的子包: %s", classified.stream()
                        .filter(p -> !allKnown.contains(p))
                        .collect(Collectors.toSet()))
                .isSubsetOf(allKnown);
    }
}
