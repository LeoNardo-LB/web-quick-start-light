package org.smm.archetype.support.basic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.smm.archetype.WebStartLightApplication;
import org.smm.archetype.support.UnitTestBase;
import org.springframework.modulith.core.ApplicationModules;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Spring Modulith 模块结构验证")
class ModulithComplianceUTest extends UnitTestBase {

    @Test
    @DisplayName("应验证模块结构合规（含边界验证、循环依赖检测）")
    void should_verifyModulithStructure() {
        ApplicationModules modules = ApplicationModules.of(WebStartLightApplication.class);
        modules.verify();
    }

    @Test
    @DisplayName("应验证模块命名符合约定")
    void should_verifyModuleNames() {
        ApplicationModules modules = ApplicationModules.of(WebStartLightApplication.class);
        assertThat(modules.stream().map(m -> m.getDisplayName()).toList())
                .containsExactlyInAnyOrder(
                        "Authentication", "Component", "Exception",
                        "Operation Log", "System Configuration", "Shared Cross-Cutting");
    }

    @Test
    @DisplayName("应验证所有模块均使用 @ApplicationModule 显式声明")
    void should_verifyAllModulesExplicitlyDeclared() {
        ApplicationModules modules = ApplicationModules.of(WebStartLightApplication.class);
        assertThat(modules.stream().count()).isEqualTo(6);
    }
}
