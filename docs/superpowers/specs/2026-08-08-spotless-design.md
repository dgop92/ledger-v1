# Spotless Configuration — Design

## Context

`ledger-v1` is a single-module Quarkus project with no automated formatting today. A prior project's `harness-inspiration.md` (`_bmad-output/planning-artifacts/research/harness-inspiration.md`) documents a much larger Spotless setup: a composite build formatting Java, YAML, SQL, and JSON, with a dedicated `build-logic` convention plugin, npm-shelled Prettier, and custom auto-apply wiring.

`ledger-v1` doesn't have that shape yet — it's a single module with only `.java` and `.gradle.kts` files (plus one `application.properties`, which Spotless doesn't target by default). This design adopts the parts of the inspiration setup that fit the current repo and defers the rest.

## Goal

Add Spotless to `ledger-v1` so Java sources and Gradle Kotlin DSL files have automated, enforced formatting via `./gradlew check`.

## Scope

**In scope:**
- Spotless plugin applied directly in the root `build.gradle.kts`.
- Java formatting for everything under `src/**/*.java` (covers `main`, `test`, `native-test`).
- Kotlin DSL formatting for `build.gradle.kts` and `settings.gradle.kts`.
- Standard `spotlessCheck` / `spotlessApply` behavior (no custom auto-apply wiring).

**Out of scope (revisit when the repo grows into them):**
- YAML/Prettier, SQL/DBeaver, JSON/Jackson formatters — no files of these types exist in the repo yet.
- A separate `build-logic` convention plugin — this project is single-module; there's nothing to share the convention across yet.
- The `prunedTarget` composite-build exclusion helper — `ledger-v1` is not a composite build.
- `AUTO_SPOTLESS_APPLY_ENABLE` env-var toggle / auto-apply-on-check wiring.

## Design

### Plugin

Add to `build.gradle.kts`:

```kotlin
plugins {
    java
    id("io.quarkus")
    id("com.diffplug.spotless") version "8.8.0"
}
```

### Java formatting

```kotlin
spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat()
        cleanthat()
        forbidWildcardImports()
        formatAnnotations()
    }
}
```

Steps run in this order on every `.java` file:
1. **googleJavaFormat()** — reformats to Google's style: 2-space indentation, brace placement, import ordering.
2. **cleanthat()** — applies safe automated refactorings on top (e.g. stream/lambda simplification).
3. **forbidWildcardImports()** — rejects `import com.foo.*`.
4. **formatAnnotations()** — normalizes annotation placement.

### Gradle Kotlin DSL formatting

```kotlin
spotless {
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
    }
}
```

Formats `build.gradle.kts` and `settings.gradle.kts` with ktlint defaults. `target("*.gradle.kts")` at the root is sufficient since this is a single-module project — no nested modules to glob into.

### Check behavior

Plain Spotless default: `./gradlew check` (which depends on `spotlessCheck`) fails and prints a diff for any misformatted file. Developers run `./gradlew spotlessApply` to fix. No custom task wiring or env-var toggles.

### Testing / verification

- `./gradlew spotlessApply` runs cleanly and reformats the existing 3 Java files and 2 `.gradle.kts` files (if needed) with no errors.
- `./gradlew spotlessCheck` (or `./gradlew check`) passes after apply.
- Deliberately misformat a Java file (e.g. bad indentation, a wildcard import) and confirm `spotlessCheck` fails with a clear diagnostic; run `spotlessApply` and confirm it's fixed.

## Future work (not in this design)

When the repo gains YAML, SQL, or JSON files, or grows into a multi-module/composite build, revisit:
- Adding `prettier()` for YAML, `dbeaver()` for SQL, Jackson-based JSON formatting.
- Extracting Spotless config into a `build-logic` convention plugin if multiple modules need to share it.
- The `prunedTarget` file-tree helper if the build becomes composite (nested `includeBuild`s).
