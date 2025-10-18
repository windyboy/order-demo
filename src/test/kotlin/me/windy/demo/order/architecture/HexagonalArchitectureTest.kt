package me.windy.demo.order.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import io.kotest.core.spec.style.StringSpec

/**
 * Architecture tests that enforce hexagonal architecture rules.
 * These tests prove that the dependency rules are followed and prevent violations.
 *
 * ## Hexagonal Architecture Rules:
 * 1. Domain layer has NO dependencies on outer layers (adapters, framework)
 * 2. Application layer depends ONLY on domain and ports
 * 3. Adapters depend on ports (interfaces), NOT on services (implementations)
 * 4. All dependencies point INWARD toward the domain
 */
class HexagonalArchitectureTest : StringSpec({

    val classes: JavaClasses =
        ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages("me.windy.demo.order")

    "domain layer should not depend on adapter layer" {
        noClasses()
            .that().resideInAPackage("..core.domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..adapter..")
            .check(classes)
    }

    "domain layer should not depend on Micronaut framework" {
        noClasses()
            .that().resideInAPackage("..core.domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("io.micronaut..", "jakarta.inject..")
            .check(classes)
    }

    "domain layer should not depend on application layer" {
        noClasses()
            .that().resideInAPackage("..core.domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..core.application..")
            .check(classes)
    }

    "adapters should not depend on application services directly" {
        noClasses()
            .that().resideInAPackage("..adapter..")
            .should().dependOnClassesThat()
            .resideInAPackage("..core.application.service..")
            .because("Adapters should depend on ports (interfaces), not services (implementations)")
            .check(classes)
    }

    "application layer should not depend on adapters" {
        noClasses()
            .that().resideInAPackage("..core.application..")
            .should().dependOnClassesThat()
            .resideInAPackage("..adapter..")
            .check(classes)
    }

    "port interfaces should not depend on adapters" {
        noClasses()
            .that().resideInAPackage("..core.port..")
            .should().dependOnClassesThat()
            .resideInAPackage("..adapter..")
            .check(classes)
    }
})
