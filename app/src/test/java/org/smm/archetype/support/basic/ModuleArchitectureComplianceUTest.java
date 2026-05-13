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
    // 注意: package-info.java 中的 @ApplicationModule 是 Modulith 基础设施元数据，
    // 不属于业务代码对 Spring 的依赖，因此排除 package-info。

    @Test
    @DisplayName("M-01: common 模块（exception 包）不得依赖 Spring Framework")
    void common_module_should_not_depend_on_spring() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage("..exception..")
                .and().doNotHaveSimpleName("package-info")
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

    // === M-05: 模块 internal/ 包零 Spring 依赖（infrastructure/ 包 + Controller/Service/RepositoryImpl/FacadeImpl/EventHandler/测试类除外） ===

    @Test
    @DisplayName("M-05: 模块 internal/ 包零 Spring 依赖（infrastructure/ 包 + Controller/Service/RepositoryImpl/FacadeImpl/EventHandler/测试类除外）")
    void module_internal_should_not_depend_on_spring_except_allowed() {
        List<String> modules = discoverBusinessModules();

        for (String module : modules) {
            ArchRuleDefinition.noClasses()
                    .that().resideInAPackage("..archetype." + module + ".internal..")
                    .and().resideOutsideOfPackage("..archetype." + module + ".internal.infrastructure..")
                    .and().haveSimpleNameNotEndingWith("Controller")
                    .and().haveSimpleNameNotEndingWith("Service")
                    .and().haveSimpleNameNotEndingWith("RepositoryImpl")
                    .and().haveSimpleNameNotEndingWith("FacadeImpl")
                    .and().haveSimpleNameNotEndingWith("EventHandler")
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

    // === C-08: 禁止 @Autowired 字段注入 ===

    @Test
    @DisplayName("C-08: 禁止 @Autowired 字段注入 — 推荐使用构造器注入 + @RequiredArgsConstructor")
    void should_not_use_autowired_field_injection() {
        ArchRuleDefinition.fields()
                .that().areDeclaredInClassesThat()
                .resideInAPackage("org.smm.archetype..")
                .and().areDeclaredInClassesThat().resideOutsideOfPackage("..support.basic..")
                // ITest/ETest 集成测试允许 @Autowired
                .and().areDeclaredInClassesThat().resideOutsideOfPackage("..cases..")
                .and().areDeclaredInClassesThat().haveSimpleNameNotEndingWith("ITest")
                .and().areDeclaredInClassesThat().haveSimpleNameNotEndingWith("ETest")
                // WebConfigure 等基础设施 Configure 类允许 @Autowired
                .and().areDeclaredInClassesThat().haveSimpleNameNotEndingWith("Configure")
                // IntegrationTestBase 允许 @Autowired
                .and().areDeclaredInClassesThat().haveSimpleNameNotEndingWith("TestBase")
                .should().notBeAnnotatedWith(org.springframework.beans.factory.annotation.Autowired.class)
                .allowEmptyShould(true)
                .check(importedClasses);
    }

    // === C-09: 禁止抛出泛型异常 ===

    @Test
    @DisplayName("C-09: 禁止抛出泛型异常 — 必须使用 BizException/ClientException/SysException")
    void should_not_throw_generic_exceptions() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage("org.smm.archetype..")
                .and().resideOutsideOfPackage("..exception..")
                .and().resideOutsideOfPackage("..support.basic..")
                .and().resideOutsideOfPackage("..shared.web..")
                .and().resideOutsideOfPackage("..component..")
                .should().callConstructor(RuntimeException.class)
                .orShould().callConstructor(Exception.class)
                .orShould().callConstructor(Throwable.class)
                .allowEmptyShould(true)
                .check(importedClasses);
    }

    // === C-10: Controller 公开方法返回值必须 BaseResult/BasePageResult ===

    @Test
    @DisplayName("C-10: Controller 公开方法返回值必须为 BaseResult 或 BasePageResult")
    void controller_public_methods_should_return_base_result() {
        ArchRuleDefinition.methods()
                .that().arePublic()
                .and().areDeclaredInClassesThat()
                .resideInAPackage("org.smm.archetype..")
                .and().areDeclaredInClassesThat().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                .and().areDeclaredInClassesThat().resideOutsideOfPackage("..shared.web..")
                .should(new ArchCondition<JavaMethod>("return BaseResult or BasePageResult") {
                    @Override
                    public void check(JavaMethod method, ConditionEvents events) {
                        JavaClass returnType = method.getRawReturnType();
                        String name = returnType.getName();
                        if ("void".equals(name)) return;
                        if (name.startsWith("org.springframework.")) return;
                        if (name.equals("java.util.Map")) return;
                        if (!name.contains(".result.BaseResult") && !name.contains(".result.BasePageResult")) {
                            events.add(SimpleConditionEvent.violated(method,
                                method.getFullName() + " returns " + name
                                + " — must return BaseResult or BasePageResult"));
                        }
                    }
                })
                .allowEmptyShould(true)
                .check(importedClasses);
    }

    // === C-11: 非 DO 类禁止 MyBatis-Plus 持久化注解 ===

    @Test
    @DisplayName("C-11: 非 DO 类禁止 MyBatis-Plus 持久化注解 — 仅 DO（infrastructure/）可使用")
    void non_do_classes_should_not_have_mybatis_plus_annotations() {
        ArchRuleDefinition.noClasses()
                .that().haveSimpleNameNotEndingWith("DO")
                .and().resideInAPackage("org.smm.archetype..")
                .and().resideOutsideOfPackage("..shared.dal..")
                .should().beAnnotatedWith("com.baomidou.mybatisplus.annotation.TableName")
                .orShould().beAnnotatedWith("com.baomidou.mybatisplus.annotation.TableId")
                .orShould().beAnnotatedWith("com.baomidou.mybatisplus.annotation.TableField")
                .orShould().beAnnotatedWith("com.baomidou.mybatisplus.annotation.TableLogic")
                .orShould().beAnnotatedWith("com.baomidou.mybatisplus.annotation.Version")
                .allowEmptyShould(true)
                .check(importedClasses);
    }

    // === C-12: 禁止 java.util.logging ===

    @Test
    @DisplayName("C-12: 禁止 java.util.logging — 统一使用 SLF4J (@Slf4j)")
    void should_not_use_java_util_logging() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage("org.smm.archetype..")
                .and().resideOutsideOfPackage("..support.basic..")
                .should().dependOnClassesThat()
                .resideInAPackage("java.util.logging..")
                .allowEmptyShould(true)
                .check(importedClasses);
    }

    // === C-13: 禁止使用 @Deprecated API ===

    @Test
    @DisplayName("C-13: 禁止使用 @Deprecated API")
    void should_not_use_deprecated_api() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage("org.smm.archetype..")
                .and().resideOutsideOfPackage("..support.basic..")
                .should().dependOnClassesThat()
                .areAnnotatedWith(Deprecated.class)
                .allowEmptyShould(true)
                .check(importedClasses);
    }

    // === M-10: 业务模块间零循环依赖 ===

    @Test
    @DisplayName("M-10: 业务模块间零循环依赖（ArchUnit slices 独立检测）")
    void modules_should_be_free_of_cycles() {
        // 只检测业务模块（auth/operationlog/systemconfig）之间的循环依赖
        // 排除 shared/support/component/exception 等基础设施包，它们之间有合理的横向依赖
        com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices()
                .matching("org.smm.archetype.(auth|operationlog|systemconfig)..")
                .should().beFreeOfCycles()
                .allowEmptyShould(true)
                .check(importedClasses);
    }

    // === C-14: @Service 类字段必须 final ===

    @Test
    @DisplayName("C-14: @Service 类字段必须 final — 确保使用构造器注入")
    void service_fields_should_be_final() {
        ArchRuleDefinition.classes()
                .that().areAnnotatedWith("org.springframework.stereotype.Service")
                .and().resideInAPackage("org.smm.archetype..")
                .should(new ArchCondition<JavaClass>("have no non-final instance fields") {
                    @Override
                    public void check(JavaClass clazz, ConditionEvents events) {
                        for (var field : clazz.getFields()) {
                            if (field.getModifiers().contains(JavaModifier.STATIC)) continue;
                            if (field.getModifiers().contains(JavaModifier.FINAL)) continue;
                            events.add(SimpleConditionEvent.violated(field,
                                field.getFullName() + " is not final in @Service class "
                                + clazz.getSimpleName()));
                        }
                    }
                })
                .allowEmptyShould(true)
                .check(importedClasses);
    }

    // === C-15: Utility 类方法必须 static ===

    @Test
    @DisplayName("C-15: Utility 类方法必须 static")
    void utility_class_methods_should_be_static() {
        ArchRuleDefinition.methods()
                .that().arePublic()
                .and().areDeclaredInClassesThat()
                .haveSimpleNameEndingWith("Utils")
                .and().areDeclaredInClassesThat()
                .resideInAPackage("org.smm.archetype..")
                // 排除 Spring 回调方法（如 setApplicationContext）
                .and().doNotHaveName("setApplicationContext")
                .should().beStatic()
                .allowEmptyShould(true)
                .check(importedClasses);
    }

    // === C-16: Logger 字段必须是 private static final ===

    @Test
    @DisplayName("C-16: SLF4J Logger 字段必须是 private static final")
    void logger_fields_should_be_private_static_final() {
        ArchRuleDefinition.fields()
                .that().haveRawType(org.slf4j.Logger.class)
                .and().areDeclaredInClassesThat()
                .resideInAPackage("org.smm.archetype..")
                .and().areDeclaredInClassesThat()
                .resideOutsideOfPackage("..support.basic..")
                .should().bePrivate()
                .andShould().beStatic()
                .andShould().beFinal()
                .allowEmptyShould(true)
                .check(importedClasses);
    }
}
