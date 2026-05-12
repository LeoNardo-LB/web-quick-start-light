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
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.UnitTestBase;

import java.util.ArrayList;
import java.util.List;
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
        // 检查 facade 包（旧布局）+ internal 包中的 FacadeImpl 类（新布局）
        ArchRuleDefinition.classes()
                .that().resideInAPackage("..facade..")
                .and().areNotInterfaces()
                .or().resideInAPackage("..internal..")
                .and().haveSimpleNameEndingWith("FacadeImpl")
                .should(notReturnInternalEntity())
                .allowEmptyShould(true)
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
                    // 检查返回类型是否在 entity 包下且不是 VO/DTO/枚举
                    // 排除 entity.base 包中的通用基类（如 BasePageResult）
                    // 排除枚举类型（枚举在 entity 包下是合理的业务值对象）
                    if (returnTypeName.contains(".entity.")
                            && !returnTypeName.contains(".entity.base.")
                            && !returnTypeName.endsWith("VO")
                            && !returnTypeName.endsWith("DTO")
                            && !isEnum(returnTypeName)) {
                        events.add(SimpleConditionEvent.violated(method,
                                String.format("%s.%s() 返回内部 Entity 类型: %s",
                                        clazz.getSimpleName(), method.getName(), returnTypeName)));
                    }
                }
            }
        };
    }

    /**
     * 检查给定类名是否为枚举类型。
     * 枚举在 entity 包下是合理的业务值对象，不算"内部 Entity 泄露"。
     */
    private static boolean isEnum(String className) {
        try {
            JavaClass jc = importedClasses.get(className);
            return jc != null && jc.isEnum();
        } catch (Exception e) {
            // 类不在导入范围内，保守返回 false
            return false;
        }
    }

    // === M-04: Controller 路径前缀规范（MUST 强制） ===
    // API Controller 必须 /api，Web 页面 Controller 必须 /web，Admin 必须 /api/admin

    @Test
    @DisplayName("M-04: Controller 路径前缀必须符合规范（API→/api, Web→/web）")
    void controller_path_should_follow_prefix_convention() {
        // API Controller（非 web 页面）必须以 /api 开头
        ArchRuleDefinition.classes()
                .that().areAnnotatedWith("org.springframework.web.bind.annotation.RequestMapping")
                .and().resideOutsideOfPackage("..controller.web..")
                .should(havePathStartingWith("/api"))
                .allowEmptyShould(true)
                .check(importedClasses);

        // Web 页面 Controller 必须以 /web 开头
        ArchRuleDefinition.classes()
                .that().areAnnotatedWith("org.springframework.web.bind.annotation.RequestMapping")
                .and().resideInAPackage("..controller.web..")
                .should(havePathStartingWith("/web"))
                .allowEmptyShould(true)
                .check(importedClasses);
    }

    private static ArchCondition<JavaClass> havePathStartingWith(String prefix) {
        return new ArchCondition<>("have path starting with '" + prefix + "'") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                // 只检查 @RequestMapping 注解上的路径
                clazz.tryGetAnnotationOfType("org.springframework.web.bind.annotation.RequestMapping")
                        .ifPresent(annotation -> {
                            // 尝试获取 value 或 path 属性
                            Object rawValue = annotation.tryGetExplicitlyDeclaredProperty("value")
                                    .or(() -> annotation.tryGetExplicitlyDeclaredProperty("path"))
                                    .orElse(null);
                            if (rawValue == null) return;

                            // 处理 String 和 String[] 两种类型
                            String[] paths;
                            if (rawValue instanceof String[]) {
                                paths = (String[]) rawValue;
                            } else if (rawValue instanceof String) {
                                paths = new String[]{(String) rawValue};
                            } else {
                                paths = new String[]{rawValue.toString()};
                            }

                            for (String path : paths) {
                                if (!path.startsWith(prefix)) {
                                    events.add(SimpleConditionEvent.violated(clazz,
                                            String.format("%s 的 API 路径 '%s' 不以 '%s' 开头",
                                                    clazz.getSimpleName(), path, prefix)));
                                }
                            }
                        });
            }
        };
    }

    // ========== 辅助方法：动态发现业务模块 ==========

    /**
     * 动态发现业务模块名称列表。
     * <p>
     * 业务模块定义：org.smm.archetype 下的一级子包中，包含 internal 子包的模块。
     * 排除 shared/config/generated/support/controller/entity 等非业务包。
     */
    private static List<String> discoverBusinessModules() {
        Set<String> excludedModules = Set.of(
                "shared", "config", "generated", "support", "controller", "entity"
        );

        return importedClasses.stream()
                .map(JavaClass::getPackageName)
                .filter(p -> p.startsWith("org.smm.archetype."))
                .filter(p -> p.contains(".internal"))
                .map(p -> {
                    // 从 "org.smm.archetype.auth.internal" 提取 "auth"
                    String afterBase = p.substring("org.smm.archetype.".length());
                    return afterBase.split("\\.")[0];
                })
                .filter(name -> !excludedModules.contains(name))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    // === M-05: 模块 internal/ 包零 Spring 依赖（Controller/Service/Converter/RepositoryImpl/FacadeImpl/测试类除外） ===

    @Test
    @DisplayName("M-05: 模块 internal/ 包零 Spring 依赖（Controller/Service/Converter/RepositoryImpl/FacadeImpl/测试类除外）")
    void module_internal_should_not_depend_on_spring_except_allowed() {
        List<String> modules = discoverBusinessModules();

        for (String module : modules) {
            // internal 包中，排除需要 Spring 的四层架构组件和测试类
            ArchRuleDefinition.noClasses()
                    .that().resideInAPackage("..archetype." + module + ".internal..")
                    .and().haveSimpleNameNotEndingWith("Controller")
                    .and().haveSimpleNameNotEndingWith("Service")
                    .and().haveSimpleNameNotEndingWith("Converter")
                    .and().haveSimpleNameNotEndingWith("RepositoryImpl")
                    .and().haveSimpleNameNotEndingWith("FacadeImpl")
                    .and(new DescribedPredicate<>("is not a test class") {
                        @Override
                        public boolean test(JavaClass javaClass) {
                            return !isTestClass(javaClass);
                        }
                    })
                    .should().dependOnClassesThat()
                    .resideInAPackage("org.springframework..")
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }
    }

    /**
     * 判断是否为测试类（ITest/ETest 及其内部类）。
     * 内部类的全限定名形如 pkg.OuterITest$Inner，因此检查全限定名包含 ITest$ / ETest$ 或以 ITest / ETest 结尾。
     */
    private static boolean isTestClass(JavaClass javaClass) {
        String fqn = javaClass.getName();
        return fqn.endsWith("ITest") || fqn.contains("ITest$")
                || fqn.endsWith("ETest") || fqn.contains("ETest$");
    }

    // === M-06: Repository 接口方法签名不得出现 MyBatis-Plus 类型 ===

    @Test
    @DisplayName("M-06: Repository 接口方法签名不得出现 MyBatis-Plus 类型")
    void repository_interface_should_not_depend_on_mybatis_plus() {
        ArchRuleDefinition.noClasses()
                .that().haveSimpleNameEndingWith("Repository")
                .and().areInterfaces()
                .should().dependOnClassesThat()
                .resideInAPackage("com.baomidou.mybatisplus..")
                .allowEmptyShould(true)
                .check(importedClasses);
    }

    // === M-07/M-09: 模块间不得直接访问其他模块的 internal/ 包 ===

    @Test
    @DisplayName("M-07/M-09: 模块间不得直接访问其他模块的 internal/ 包")
    void modules_should_not_access_other_module_internal() {
        List<String> modules = discoverBusinessModules();

        for (String sourceModule : modules) {
            // 收集其他所有模块的 internal 包路径
            List<String> otherInternalPackages = new ArrayList<>();
            for (String targetModule : modules) {
                if (!targetModule.equals(sourceModule)) {
                    otherInternalPackages.add("..archetype." + targetModule + ".internal..");
                }
            }

            if (otherInternalPackages.isEmpty()) continue;

            ArchRuleDefinition.noClasses()
                    .that().resideInAPackage("..archetype." + sourceModule + "..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(otherInternalPackages.toArray(new String[0]))
                    .allowEmptyShould(true)
                    .check(importedClasses);
        }
    }

    // === M-08: Facade 接口不得依赖 MyBatis-Plus 类型 ===

    @Test
    @DisplayName("M-08: Facade 接口不得依赖 MyBatis-Plus 类型")
    void facade_interface_should_not_depend_on_mybatis_plus() {
        ArchRuleDefinition.noClasses()
                .that().haveSimpleNameEndingWith("Facade")
                .and().areInterfaces()
                .should().dependOnClassesThat()
                .resideInAPackage("com.baomidou.mybatisplus..")
                .allowEmptyShould(true)
                .check(importedClasses);
    }
}
