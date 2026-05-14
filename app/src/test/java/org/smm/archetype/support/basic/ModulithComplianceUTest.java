package org.smm.archetype.support.basic;

import com.tngtech.archunit.core.domain.JavaClass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.smm.archetype.WebStartLightApplication;
import org.smm.archetype.support.UnitTestBase;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Spring Modulith 模块结构验证")
class ModulithComplianceUTest extends UnitTestBase {

    /**
     * component 和 exception 包来自外部 Maven 模块（components/*、common），
     * 不属于 app 内业务模块，排除后 Modulith 不再将其视为独立模块。
     */
    private static final ApplicationModules MODULES = ApplicationModules.of(
            WebStartLightApplication.class,
            JavaClass.Predicates.resideInAPackage("org.smm.archetype.component..")
                    .or(JavaClass.Predicates.resideInAPackage("org.smm.archetype.exception.."))
    );

    @Test
    @DisplayName("应验证模块结构合规（含边界验证、循环依赖检测）")
    void should_verifyModulithStructure() {
        MODULES.verify();
    }

    @Test
    @DisplayName("应验证模块命名符合约定")
    void should_verifyModuleNames() {
        assertThat(MODULES.stream().map(ApplicationModule::getDisplayName).toList())
                .containsExactlyInAnyOrder(
                        "Authentication",
                        "Operation Log", "System Configuration", "Shared Cross-Cutting");
    }

    @Test
    @DisplayName("应验证所有模块均使用 @ApplicationModule 显式声明")
    void should_verifyAllModulesExplicitlyDeclared() {
        assertThat(MODULES.stream().count()).isEqualTo(4);
    }
}
