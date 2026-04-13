package com.kaiqkt.magiapi.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

class ArchitectureTest {

    private val classes = ClassFileImporter().importPackages("com.kaiqkt.magiapi")

    @Test
    fun `domain must not depend on application or resources`() {
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..application..", "..resources..")
            .check(classes)
    }

    @Test
    fun `application must not depend on resources`() {
        noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAPackage("..resources..")
            .check(classes)
    }

    @Test
    fun `resources must not depend on application`() {
        noClasses()
            .that().resideInAPackage("..resources..")
            .should().dependOnClassesThat()
            .resideInAPackage("..application..")
            .check(classes)
    }
}
