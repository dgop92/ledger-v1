"""List Pitest survivors grouped by source line, split into on-changed-lines and pre-existing.

Usage:
  uv run --no-project survivors.py REPORT.xml [--changed FILE] [--json]
  uv run --no-project survivors.py REPORT.xml --show path:line

Status vocabulary: NO_COVERAGE = no unit test executes the line;
SURVIVED = a test runs it but asserts nothing about the effect.
"""

import argparse
import json
import sys
from collections import defaultdict

from common import (
    SURVIVOR_STATUSES,
    line_is_changed,
    load_json,
    parse_mutations,
    repo_root,
    state_dir,
)


def group_by_line(mutations, changed_files):
    groups = defaultdict(list)
    for m in mutations:
        if m["status"] in SURVIVOR_STATUSES:
            groups[(m["path"], m["line"])].append(m)
    on_changed, pre_existing = [], []
    for (path, line), items in sorted(groups.items()):
        group = {
            "path": path,
            "line": line,
            "status": "NO_COVERAGE"
            if all(i["status"] == "NO_COVERAGE" for i in items)
            else "SURVIVED",
            "mutants": [
                {"status": i["status"], "mutator": i["mutator"], "description": i["description"]}
                for i in items
            ],
        }
        ranges = changed_files.get(path, [])
        (on_changed if line_is_changed(ranges, line) else pre_existing).append(group)
    return on_changed, pre_existing


def print_groups(title, groups):
    print(f"{title}: {len(groups)} line group(s)")
    for g in groups:
        print(f"  {g['path']}:{g['line']}  [{g['status']}]")
        for m in g["mutants"]:
            print(f"      {m['status']:<11} {m['mutator']}: {m['description']}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("report")
    parser.add_argument("--changed")
    parser.add_argument("--json", action="store_true")
    parser.add_argument("--show", metavar="PATH:LINE")
    args = parser.parse_args()

    mutations = parse_mutations(args.report)
    if args.show:
        path, _, line = args.show.rpartition(":")
        for m in mutations:
            if m["path"] == path and m["line"] == int(line):
                print(f"{m['status']:<11} {m['class']}#{m['method']}  {m['mutator']}: {m['description']}")
        return 0

    changed = load_json(args.changed or state_dir(repo_root()) / "changed.json")
    on_changed, pre_existing = group_by_line(mutations, changed["files"])
    if args.json:
        print(json.dumps({"on_changed_lines": on_changed, "pre_existing": pre_existing}, indent=2))
    else:
        print_groups("Survivors on changed lines", on_changed)
        print_groups("Pre-existing survivors (leave alone)", pre_existing)
    return 0


if __name__ == "__main__":
    sys.exit(main())
