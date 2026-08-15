import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    id("com.diffplug.spotless") version "8.8.0"
}

allprojects {
    apply(plugin = "com.diffplug.spotless")

    repositories {
        mavenCentral()
        mavenLocal()
    }

    configure<SpotlessExtension> {
        kotlinGradle {
            target("*.gradle.kts")
            ktlint()
        }
    }
}

subprojects {
    apply(plugin = "java")

    group = "com.dgop92.ledger_v1"
    version = "1.0.0-SNAPSHOT"

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

    configure<SpotlessExtension> {
        java {
            target("src/**/*.java")
            googleJavaFormat()
            cleanthat().sourceCompatibility("21")
            forbidWildcardImports()
            formatAnnotations()
        }
    }
}
