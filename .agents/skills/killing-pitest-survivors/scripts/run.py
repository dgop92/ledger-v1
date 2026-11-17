"""Run the repo's `pitest` Gradle task on the changed classes and snapshot the XML report.

Usage: uv run --no-project run.py LABEL [--changed FILE]

LABEL (for example `before` or `after`) names the copy kept at build/pitest-skill/LABEL.xml.
"""

import argparse
import shutil
import subprocess
import sys

from common import load_json, repo_root, state_dir


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("label")
    parser.add_argument("--changed", help="changed.json from changed.py")
    args = parser.parse_args()

    root = repo_root()
    state = state_dir(root)
    changed = load_json(args.changed or state / "changed.json")
    targets = []
    for fqcn in changed["classes"]:
        targets += [fqcn, fqcn + "$*"]  # the class and its nested classes

    command = ["./gradlew", "pitest", "-PpitestTargetClasses=" + ",".join(targets)]
    result = subprocess.run(command, cwd=root)
    report = root / "build" / "reports" / "pitest" / "mutations.xml"
    if result.returncode != 0 or not report.exists():
        print("pitest run failed or produced no mutations.xml", file=sys.stderr)
        return result.returncode or 1
    destination = state / f"{args.label}.xml"
    shutil.copyfile(report, destination)
    print(f"report saved to {destination}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
