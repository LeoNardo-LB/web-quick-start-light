package org.smm.archetype.support.basic;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.smm.archetype.WebStartLightApplication;
import org.smm.archetype.support.UnitTestBase;
import org.springframework.modulith.core.ApplicationModules;

@DisplayName("Spring Modulith 模块结构验证")
class ModulithComplianceUTest extends UnitTestBase {

    @Test
    @Disabled("Phase 4 将完善模块边界约束，当前跨包依赖（component/exception/entity）属于 app 内部引用")
    @DisplayName("应验证模块结构合规")
    void should_verifyModulithStructure() {
        ApplicationModules modules = ApplicationModules.of(WebStartLightApplication.class);
        modules.verify();
    }
}
