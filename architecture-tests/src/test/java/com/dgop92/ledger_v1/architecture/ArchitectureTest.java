package com.dgop92.ledger_v1.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

/** Enforces AD-1 (domain-first boundary) and AD-2 (layered dependency direction). */
@AnalyzeClasses(
    packages = "com.dgop92.ledger_v1",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

  @ArchTest
  static final ArchRule domainMustNotDependOnFrameworks =
      noClasses()
          .that()
          .resideInAPackage("com.dgop92.ledger_v1.domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("io.quarkus..", "org.jdbi..", "org.postgresql..");

  @ArchTest
  static final ArchRule layeredArchitectureIsRespected =
      Architectures.layeredArchitecture()
          .consideringOnlyDependenciesInLayers()
          .layer("Domain")
          .definedBy("com.dgop92.ledger_v1.domain..")
          .layer("Application")
          .definedBy("com.dgop92.ledger_v1.application..")
          .layer("Adapters")
          .definedBy("com.dgop92.ledger_v1.adapters..")
          .layer("App")
          .definedBy("com.dgop92.ledger_v1.app..")
          .whereLayer("Domain")
          .mayOnlyBeAccessedByLayers("Application", "Adapters", "App")
          .whereLayer("Application")
          .mayOnlyBeAccessedByLayers("Adapters", "App")
          .whereLayer("Adapters")
          .mayOnlyBeAccessedByLayers("App")
          .whereLayer("App")
          .mayNotBeAccessedByAnyLayer();
}
