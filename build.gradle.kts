import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    java
    jacoco
    pmd
    id("io.quarkus")
    id("com.diffplug.spotless") version "8.8.0"
}

group = "com.dgop92.ledger_v1"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
    implementation(enforcedPlatform("$quarkusPlatformGroupId:$quarkusPlatformArtifactId:$quarkusPlatformVersion"))

    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-agroal")
    implementation("io.quarkus:quarkus-jdbc-postgresql")
    implementation("io.quarkus:quarkus-flyway")
    implementation("io.quarkus:quarkus-config-yaml")

    implementation("org.jdbi:jdbi3-core:3.54.0")
    implementation("org.jdbi:jdbi3-postgres:3.54.0")
    implementation("com.github.f4b6a3:uuid-creator:6.1.1")

    testImplementation(enforcedPlatform("$quarkusPlatformGroupId:$quarkusPlatformArtifactId:$quarkusPlatformVersion"))
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

extensions.configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Do not set excludeClassLoaders=*QuarkusClassLoader: without the quarkus-jacoco
// extension it drops REST and persistence coverage to 0%.
jacoco {
    toolVersion = "0.8.15"
}

// CDI wiring only; excluded from both the report and the gate.
val coverageClassDirectories =
    files(
        sourceSets.main.map { main ->
            main.output.classesDirs.files.map {
                fileTree(it) { exclude("com/dgop92/ledger_v1/app/config/**") }
            }
        },
    )

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    classDirectories.setFrom(coverageClassDirectories)
    reports {
        html.required = true
        xml.required = true
    }
}

// Coverage ratchet: floors only move up. Lowering one needs justification in the PR.
// Baseline measured 2026-11-14: line 94.7% (floor 94%), branch 72.7% (floor 72%).
tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    // Generate the report first so CI can still upload it when the gate fails.
    mustRunAfter(tasks.jacocoTestReport)
    classDirectories.setFrom(coverageClassDirectories)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.94".toBigDecimal()
            }
        }
        rule {
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.72".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification, tasks.jacocoTestReport)
}

// PMD is report-only for now: findings are printed and written to build/reports/pmd but do
// not fail the build. Enforcement comes after a trial period. Only main sources are analyzed.
// Baseline measured 2026-11-14: 14 findings on main.
// PMD is pinned explicitly: Gradle's default is PMD 6, which cannot parse Java 25.
pmd {
    toolVersion = "7.29.0"
    isConsoleOutput = true
    isIgnoreFailures = true
    ruleSetFiles = files("config/pmd/ruleset.xml")
    ruleSets = emptyList()
}

tasks.named("pmdTest") { enabled = false }

// Mutation testing (Pitest) is advisory: a custom task, not part of check/build, never gated.
// See docs/adr/0005. Versions are pinned explicitly.
val pitestTool by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    pitestTool("org.pitest:pitest-command-line:1.30.2")
    pitestTool("org.pitest:pitest-junit5-plugin:1.2.3")
}

val pitestReportDir = layout.buildDirectory.dir("reports/pitest")
val pitestScopedPackages =
    listOf("com.dgop92.ledger_v1.domain.*", "com.dgop92.ledger_v1.application.usecase.*")

tasks.register<JavaExec>("pitest") {
    group = "verification"
    description = "Advisory mutation testing of domain and application.usecase (unit tests only). Not part of build."
    val mainOutput = sourceSets.main.map { it.output.classesDirs }
    val testRuntime = sourceSets.test.map { it.runtimeClasspath }
    dependsOn("classes", "testClasses")
    classpath = pitestTool
    mainClass = "org.pitest.mutationtest.commandline.MutationCoverageReport"
    doFirst {
        val reportDir = pitestReportDir.get().asFile
        reportDir.mkdirs()
        // Arguments with a mutable classpath are resolved at execution time.
        args(
            "--reportDir",
            reportDir.absolutePath,
            "--outputFormats",
            "HTML,XML",
            "--timestampedReports",
            "false",
            "--targetClasses",
            // Optional -PpitestTargetClasses=a.B,c.D restricts mutation to those classes; default is the full scope.
            providers.gradleProperty("pitestTargetClasses").orNull?.takeIf { it.isNotBlank() }
                ?: pitestScopedPackages.joinToString(","),
            // Same packages only: @QuarkusTest classes (app.*) and ArchitectureTest never run.
            "--targetTests",
            pitestScopedPackages.joinToString(","),
            "--sourceDirs",
            file("src/main/java").absolutePath,
            "--mutableCodePaths",
            mainOutput.get().files.joinToString(",") { it.absolutePath },
            "--classPath",
            testRuntime.get().files.joinToString(",") { it.absolutePath },
            "--threads",
            Runtime.getRuntime().availableProcessors().toString(),
            "--failWhenNoMutations",
            "false",
        )
    }
}

configure<SpotlessExtension> {
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
    }
    java {
        target("src/**/*.java")
        googleJavaFormat()
        cleanthat().sourceCompatibility("21")
        forbidWildcardImports()
        formatAnnotations()
    }
}
