---
name: killing-pitest-survivors
compatibility: Requires git and uv (Python, standard library only). Pitest runs through ./gradlew pitest; no Docker needed.
description: Runs Pitest on the classes this branch changed and kills the surviving mutants it introduced by adding or strengthening unit tests. Use after tests are green and before opening a PR, when the user asks to kill survivors, check mutation coverage of the branch, or harden tests with Pitest.
---

# Killing Pitest survivors

Mutation-test only what this branch changed, then close the gaps with unit tests. Scripts do the mechanics; you judge equivalence and write the assertions. Paths below are relative to this folder. Run every script with `uv run --no-project scripts/<name>.py`, from the repo root.

Vocabulary: `NO_COVERAGE` = no unit test executes the line. `SURVIVED` = a test runs it but asserts nothing about the effect. A *line group* is all mutants on one source line.

## Steps

1. **Scope.** `changed.py` lists in-scope changed classes and line ranges (merge-base with `main`, plus uncommitted and untracked work; only `domain` and `application.usecase`). If it prints `nothing in Pitest scope`, report that and stop with no changes.
2. **Snapshot.** `guardrail.py snapshot` (before any edit).
3. **Baseline.** `run.py before`, then `survivors.py build/pitest-skill/before.xml`. Survivors on changed lines are yours; the pre-existing ones are listed and left alone.
4. **Summary.** Print changed classes, survivors on changed lines, planned tests. No confirmation gate; continue.
5. **Triage each line group on a changed line.** `survivors.py <xml> --show path:line` shows one group's mutants.
   - Covered only by a `@QuarkusTest` (`NO_COVERAGE` and the line is reached only from `app/`): report it, write nothing.
   - Looks like a production bug (the code is wrong, not the tests weak): stop the run and report it.
   - Equivalent (no observable behaviour change): list it in the summary with a one-line reason.
   - Otherwise write the smallest unit test or assertion that fails under the mutant, following the neighbouring tests' style and the `GLOSSARY.md` names. Strengthen an existing test by adding assertions; create new tests beside it for new behaviour.
6. **Re-verify.** `run.py after`, then `guardrail.py verify build/pitest-skill/before.xml build/pitest-skill/after.xml`. Exit 1 means the guardrail reverted your edits; read its reasons and fix the approach.
7. **Retry.** At most 2 attempts per line group. A group still surviving after that is reported as unresolved or possibly equivalent.
8. **Finish.** Print the guardrail comparison (killed, still surviving, new) and the reasons for equivalent, unresolved and `@QuarkusTest`-only groups. Run `./gradlew spotlessApply` on the new tests. The test diff is the review point.

## Rules

- Edit files under `src/test` only; production code stays as the snapshot found it.
- Kill each mutant with a real assertion; the guardrail rejects removed, altered or `@Disabled` tests.
- Mention equivalent mutants in the summary only; they get no ignore-list or suppression.
- Keep monetary amounts, account identifiers and transaction data out of logs and examples (AGENTS.md).
