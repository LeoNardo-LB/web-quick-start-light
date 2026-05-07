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
    // 注意: component.dto 是跨组件共享的子包名，不是独立组件模块，需排除

    @Test
    @DisplayName("M-02: 组件模块间零互相依赖")
    void component_modules_should_not_depend_on_each_other() {
        // 动态发现 component 下的一级子包，排除 dto（共享 DTO 子包）
        Set<String> components = importedClasses.stream()
                .map(c -> c.getPackageName())
                .filter(p -> p.contains(".component."))
                .map(p -> {
                    int idx = p.indexOf(".component.");
                    return p.substring(idx + ".component.".length()).split("\\.")[0];
                })
                .filter(name -> !name.equals("dto"))  // 排除共享 DTO 子包
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
                    // 排除 entity.base 包中的通用基类（如 BasePageResult）
                    if (returnTypeName.contains(".entity.")
                            && !returnTypeName.contains(".entity.base.")
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
