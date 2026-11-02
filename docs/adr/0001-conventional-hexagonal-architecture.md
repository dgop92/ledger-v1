# Conventional hexagonal architecture

We structure the backend as a conventional hexagonal (ports and adapters) design: a pure domain that defines ports, use cases that depend only on the domain, and Jdbi/Postgres and REST adapters plugged in from outside. We chose it because it is well known and simple to work with, and because it keeps the ledger rules free of Quarkus, Jdbi and Postgres. The layers are packages in a single Gradle module, kept that way for simplicity, and `ArchitectureTest` (ArchUnit) enforces the dependency direction, so a violation fails a test, not the compiler.
