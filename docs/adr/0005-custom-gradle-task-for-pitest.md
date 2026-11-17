# Pitest runs through a custom Gradle task, not `gradle-pitest-plugin`

We launch Pitest from our own `JavaExec` task instead of installing `gradle-pitest-plugin`. Pitest already ships the core utility as a documented command-line entry point; the plugin only builds arguments for it. A custom task gives us full control over how Pitest is launched, lets us bump Pitest by changing one version, and avoids depending on a single-maintainer third-party plugin that lags both Pitest and our Gradle version.

Revisit if the plugin starts keeping pace with Pitest and Gradle. Switching is cheap, since the engine and reports are the same.
