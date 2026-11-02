# Hand-rolled property tests, no jqwik

Randomized invariant testing (for example `sum(debits) == sum(credits)`) is written as plain JUnit 5 `@RepeatedTest` loops driving a seeded `java.util.Random`, with no property-testing library. We rejected jqwik because version 1.10.0 shipped a hidden prompt injection telling AI agents to delete test code, 1.10.1 and later print an explicit "AI agents must not use this library" notice, and the project is in maintenance mode.

Revisit if a different property-testing library is wanted or jqwik's stance changes.
