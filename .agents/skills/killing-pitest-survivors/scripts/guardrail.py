"""Mechanical guardrail: snapshot production code and tests, then verify the skill's edits.

Usage:
  uv run --no-project guardrail.py snapshot
  uv run --no-project guardrail.py verify BEFORE.xml AFTER.xml

`verify` exits 1 and restores src/main and src/test from the snapshot when:
  - production code changed;
  - an existing test file, @Test method or assertion line was removed or altered, or @Disabled was added;
  - a mutant counts as killed but no test file was added or changed.
On success it prints the before/after comparison.
"""

import argparse
import re
import shutil
import sys
from collections import Counter

from common import parse_mutations, repo_root, state_dir, survivor_counter

TREES = ("src/main", "src/test")
ASSERTION = re.compile(r"\b(assert\w*|verify\w*|fail|assertThat|assertThrows)\s*\(|^\.\w+\(")
TEST_METHOD = re.compile(r"@(?:Test|ParameterizedTest|RepeatedTest|Property)\b[^{;]*?\bvoid\s+(\w+)\s*\(", re.S)


def read_tree(base, tree):
    """Map relative file path -> text for one tree under base."""
    top = base / tree
    if not top.exists():
        return {}
    return {
        str(p.relative_to(base)): p.read_text()
        for p in sorted(top.rglob("*"))
        if p.is_file()
    }


def assertion_lines(text):
    lines = (" ".join(line.split()) for line in text.splitlines())
    return Counter(line for line in lines if ASSERTION.search(line))


def test_methods(text):
    return set(TEST_METHOD.findall(text))


def check_tests(before, after):
    """Reasons an existing test file was removed or weakened. Pure function over two path->text maps."""
    reasons = []
    for path, old in before.items():
        if path not in after:
            reasons.append(f"existing test file removed: {path}")
            continue
        new = after[path]
        for name in sorted(test_methods(old) - test_methods(new)):
            reasons.append(f"test method removed or renamed: {path}#{name}")
        missing = assertion_lines(old) - assertion_lines(new)
        for line, count in missing.items():
            reasons.append(f"assertion removed or altered in {path} (x{count}): {line}")
        if new.count("@Disabled") > old.count("@Disabled"):
            reasons.append(f"@Disabled added in {path}")
    return reasons


def check_production(before, after):
    changed = sorted(p for p in set(before) | set(after) if before.get(p) != after.get(p))
    return [f"production code changed: {p}" for p in changed]


def restore(root, snapshot):
    for tree in TREES:
        saved = read_tree(snapshot, tree)
        for path in read_tree(root, tree):
            if path not in saved:
                (root / path).unlink()
        for path, text in saved.items():
            target = root / path
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(text)


def snapshot_command(root):
    snapshot = state_dir(root) / "snapshot"
    if snapshot.exists():
        shutil.rmtree(snapshot)
    for tree in TREES:
        if (root / tree).exists():
            shutil.copytree(root / tree, snapshot / tree)
    print(f"snapshot saved to {snapshot}")
    return 0


def compare(before_mutations, after_mutations):
    before, after = survivor_counter(before_mutations), survivor_counter(after_mutations)
    return {
        "killed": sorted((before - after).elements()),
        "still_surviving": sorted((before & after).elements()),
        "new": sorted((after - before).elements()),
    }


def verify(root, before_xml, after_xml):
    """Return (reasons, comparison). Restores the snapshot when reasons is non-empty."""
    snapshot = state_dir(root) / "snapshot"
    if not snapshot.exists():
        return ["no snapshot: run `guardrail.py snapshot` before editing"], None
    comparison = compare(parse_mutations(before_xml), parse_mutations(after_xml))
    reasons = check_production(read_tree(snapshot, "src/main"), read_tree(root, "src/main"))
    before_tests, after_tests = read_tree(snapshot, "src/test"), read_tree(root, "src/test")
    reasons += check_tests(before_tests, after_tests)
    tests_touched = any(before_tests.get(p) != t for p, t in after_tests.items())
    if comparison["killed"] and not tests_touched:
        reasons.append("mutants counted as killed but no test was added or changed")
    if reasons:
        restore(root, snapshot)
    return reasons, comparison


def print_comparison(comparison):
    for title, key in (("Killed", "killed"), ("Still surviving", "still_surviving"), ("New survivors", "new")):
        print(f"{title}: {len(comparison[key])}")
        for path, method, line, mutator, description in comparison[key]:
            print(f"  {path}:{line} ({method}) {mutator}: {description}")


def main():
    parser = argparse.ArgumentParser()
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("snapshot")
    check = sub.add_parser("verify")
    check.add_argument("before")
    check.add_argument("after")
    args = parser.parse_args()

    root = repo_root()
    if args.command == "snapshot":
        return snapshot_command(root)
    reasons, comparison = verify(root, args.before, args.after)
    if reasons:
        print("GUARDRAIL FAILED, edits reverted to the snapshot:")
        for reason in reasons:
            print(f"  - {reason}")
        return 1
    print_comparison(comparison)
    return 0


if __name__ == "__main__":
    sys.exit(main())
