package org.smm.archetype.support.basic;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.smm.archetype.support.UnitTestBase;

class ArchitectureComplianceUTest extends UnitTestBase {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .importPackages("org.smm.archetype");
    }

    @Test
    void controllerShouldOnlyDependOnServiceLayer() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage("..controller..")
                .and().areNotEnums()
                .should().dependOnClassesThat()
                .resideInAPackage("..repository.mapper..")
                .check(importedClasses);
    }

    @Test
    void serviceShouldNotDependOnControllerLayer() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage("..service..")
                .should().dependOnClassesThat()
                .resideInAPackage("..controller..")
                .allowEmptyShould(true)
                .check(importedClasses);
    }

    @Test
    void repositoryShouldNotDependOnServiceOrControllerLayer() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage("..repository..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..service..", "..controller..")
                .allowEmptyShould(true)
                .check(importedClasses);
    }

    @Test
    void entityShouldNotDependOnSpringFramework() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage("..entity..")
                .and().haveSimpleNameNotEndingWith("Test")
                .should().dependOnClassesThat()
                .resideInAPackage("org.springframework..")
                .check(importedClasses);
    }

    @Test
    void controllerShouldNotDependOnServiceDirectly() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage("..controller..")
                .and().areNotEnums()
                .and().haveSimpleNameNotStartingWith("Login")
                .should().dependOnClassesThat()
                .resideInAPackage("..service..")
                .allowEmptyShould(true)
                .check(importedClasses);
    }

    @Test
    void facadeShouldNotDependOnRepository() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage("..facade..")
                .and().areNotEnums()
                .should().dependOnClassesThat()
                .resideInAPackage("..repository..")
                .allowEmptyShould(true)
                .check(importedClasses);
    }
}
