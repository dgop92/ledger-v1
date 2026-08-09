# ledger-v1

Quarkus (Java 25) backend for a financial ledger. Early-stage: currently a bare Quarkus skeleton (single placeholder REST resource) — no domain code yet.

## Build and Test Commands

| Scope | Command |
|-------|---------|
| Dev mode (live reload) | `./gradlew quarkusDev` |
| Build (JVM jar) | `./gradlew build` |
| Run all tests | `./gradlew test` |
| Reformat code (Spotless) | `./gradlew spotlessApply` |
| Build native executable | `./gradlew build -Dquarkus.native.enabled=true` |

Run commands from the repo root; Gradle wrapper (`./gradlew`) is checked in, don't rely on a system Gradle install.

`./gradlew build` is the main verification command when implementing tasks — it runs compilation, Spotless formatting checks, and all tests (`build` depends on `check`, so a separate `./gradlew check` is redundant).

## Coding Conventions and Style

- **Language:** Java 25 (`sourceCompatibility`/`targetCompatibility` in `build.gradle.kts`)
- **Build:** Gradle with Kotlin DSL (`build.gradle.kts`, `settings.gradle.kts`), not Groovy
- **Package root:** `com.dgop92.ledger_v1`

## Security and Compliance

- Never hard-code secrets; use `application.properties` placeholders resolved from environment variables.
- This is a financial ledger — treat monetary amounts, account identifiers, and transaction data as sensitive; do not log them.
