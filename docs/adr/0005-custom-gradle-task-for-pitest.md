# Mutation testing runs through a custom Gradle task, advisory and unit-tests-only

Pitest (pinned `pitest-command-line` 1.30.2 with `pitest-junit5-plugin` 1.2.3) is wired as a `JavaExec` task `pitest` in `build.gradle.kts`, not through `info.solidsoft.pitest` (`gradle-pitest-plugin`). The plugin's default Pitest (1.22.1) lags the current release, its Gradle 9 fix is unreleased and its declared newest Gradle (9.4.1) is older than this repo's 9.6.0, and it is single-maintainer.

The task mutates only `domain` and `application.usecase` and runs only the unit tests in those packages, including the hand-rolled property tests from ADR 0004. `@QuarkusTest` classes and the ArchUnit test never run, so mutants only those would kill report as `NO_COVERAGE`; that is part of the baseline, not a defect. It is advisory: it is not part of `check` or `build`, enforces no threshold, and runs in CI only through a manual `workflow_dispatch` workflow (`mutation-testing.yml`) that uploads the HTML and XML report from `build/reports/pitest/`.

The optional Gradle property `-PpitestTargetClasses=a.B,c.D` restricts the mutated classes (the default stays the full scope); the `killing-pitest-survivors` agent skill uses it so it reuses this task instead of calling Pitest directly.

Baseline (2026-11-16, Java 25, Gradle 9.6.0, no workarounds needed): 101 mutations, 74% killed, test strength 86%, 14 `NO_COVERAGE`, 12 `SURVIVED`, about 40 seconds end to end.

Revisit if `gradle-pitest-plugin` releases Gradle 9.6 support and keeps pace with Pitest.
