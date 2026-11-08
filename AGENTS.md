# ledger-v1

Quarkus (Java 25) backend for a financial ledger. Hexagonal (ports and adapters) design: domain, use cases, Jdbi/Postgres adapters, Flyway migrations, REST resources.

Domain terms: use the exact names in [GLOSSARY.md](GLOSSARY.md). Why key design decisions were made: [docs/adr/](docs/adr/). Setup and local database: [README.md](README.md). Opening PRs: use the `creating-pull-requests` skill in `.agents/skills`.

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

Tests and `build` require Docker running (Quarkus Dev Services starts a Testcontainers Postgres). Dev mode uses a local Postgres from `local-env/docker-compose.yml` (see README.md).

## Architecture

Packages under `com.dgop92.ledger_v1`:

- `domain` — pure Java; must not depend on Quarkus, Jdbi, or Postgres. Holds entities, value objects, domain exceptions, and ports (`domain.port`).
- `application.usecase` — one use case per operation, depending only on domain.
- `adapters.{rest,persistence,idgen}` — implement ports / expose REST; REST exception mappers translate domain exceptions.
- `app.config` — `AdapterConfig` wires ports and use cases manually via CDI `@Produces`; register new use cases/adapters there.

Dependency direction (domain <- application <- adapters <- app) is enforced by `src/test/java/com/dgop92/ledger_v1/architecture/ArchitectureTest.java`.

Schema changes go in new Flyway migrations under `src/main/resources/db/migration`; never edit applied ones.

## Coding Conventions and Style

- **Language:** Java 25 (`sourceCompatibility`/`targetCompatibility` in `build.gradle.kts`)
- **Build:** Gradle with Kotlin DSL (`build.gradle.kts`, `settings.gradle.kts`), not Groovy
- **Package root:** `com.dgop92.ledger_v1`

## Security and Compliance

- Never hard-code secrets; use `application.properties` placeholders resolved from environment variables.
- This is a financial ledger — treat monetary amounts, account identifiers, and transaction data as sensitive; do not log them.

## Agent skills

### Issue tracker

Issues live in GitHub Issues for `dgop92/ledger-v1` (via the `gh` CLI). See `docs/agents/issue-tracker.md`.

### Triage labels

Default five-label vocabulary (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`). See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `GLOSSARY.md` + `docs/adr/` at the repo root. See `docs/agents/domain.md`.
