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
