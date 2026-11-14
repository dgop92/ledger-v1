import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    java
    jacoco
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
