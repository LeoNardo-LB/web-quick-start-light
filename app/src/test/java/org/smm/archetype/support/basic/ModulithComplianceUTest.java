package org.smm.archetype.support.basic;

import com.tngtech.archunit.core.domain.JavaClass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.smm.archetype.WebStartLightApplication;
import org.smm.archetype.support.UnitTestBase;
import org.springframework.modulith.core.ApplicationModules;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Spring Modulith 模块结构验证")
class ModulithComplianceUTest extends UnitTestBase {

    /**
     * component 包来自外部 Maven 模块（components/*），不属于 app 内业务模块，
     * 排除后 Modulith 不再将其视为独立模块，避免对 AuthComponent/CacheComponent 等
     * 的跨模块引用报违规。
     */
    private static final ApplicationModules MODULES = ApplicationModules.of(
            WebStartLightApplication.class,
            JavaClass.Predicates.resideInAPackage("org.smm.archetype.component..")
    );

    @Test
    @DisplayName("应验证模块结构合规（含边界验证、循环依赖检测）")
    void should_verifyModulithStructure() {
        MODULES.verify();
    }

    @Test
    @DisplayName("应验证模块命名符合约定")
    void should_verifyModuleNames() {
        assertThat(MODULES.stream().map(m -> m.getDisplayName()).toList())
                .containsExactlyInAnyOrder(
                        "Authentication", "Exception",
                        "Operation Log", "System Configuration", "Shared Cross-Cutting");
    }

    @Test
    @DisplayName("应验证所有模块均使用 @ApplicationModule 显式声明")
    void should_verifyAllModulesExplicitlyDeclared() {
        assertThat(MODULES.stream().count()).isEqualTo(5);
    }
}
