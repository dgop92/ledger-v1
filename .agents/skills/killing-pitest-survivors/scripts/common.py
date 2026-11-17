"""Shared helpers for the killing-pitest-survivors scripts (standard library only)."""

import json
import re
import subprocess
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path

MAIN_ROOT = "src/main/java"
TEST_ROOT = "src/test"
SCOPE_PREFIXES = (
    f"{MAIN_ROOT}/com/dgop92/ledger_v1/domain/",
    f"{MAIN_ROOT}/com/dgop92/ledger_v1/application/usecase/",
)
SURVIVOR_STATUSES = ("SURVIVED", "NO_COVERAGE")


def git(*args, cwd=None):
    result = subprocess.run(
        ["git", *args], capture_output=True, text=True, check=True, cwd=cwd
    )
    return result.stdout


def repo_root():
    return Path(git("rev-parse", "--show-toplevel").strip())


def state_dir(root=None):
    path = (root or repo_root()) / "build" / "pitest-skill"
    path.mkdir(parents=True, exist_ok=True)
    return path


def in_scope(path):
    return path.endswith(".java") and path.startswith(SCOPE_PREFIXES)


def path_to_class(path):
    return path[len(MAIN_ROOT) + 1 : -len(".java")].replace("/", ".")


def parse_hunks(diff_text):
    """Map file -> list of [start, end] added or modified line ranges from `git diff -U0`."""
    changed = {}
    current = None
    for line in diff_text.splitlines():
        if line.startswith("+++ "):
            target = line[4:]
            current = target[2:] if target.startswith("b/") else None
        elif line.startswith("@@") and current:
            match = re.match(r"@@ -\S+ \+(\d+)(?:,(\d+))? @@", line)
            if not match:
                continue
            start = int(match.group(1))
            count = int(match.group(2)) if match.group(2) is not None else 1
            if count > 0:
                changed.setdefault(current, []).append([start, start + count - 1])
    return changed


def changed_lines(root=None, base_ref="main"):
    """Changed in-scope lines since the merge-base with main, including uncommitted and untracked work."""
    root = root or repo_root()
    base = git("merge-base", base_ref, "HEAD", cwd=root).strip()
    diff = git("diff", "-U0", "--no-color", base, "--", MAIN_ROOT, cwd=root)
    changed = parse_hunks(diff)
    untracked = git(
        "ls-files", "--others", "--exclude-standard", "--", MAIN_ROOT, cwd=root
    ).splitlines()
    for path in untracked:
        total = len((root / path).read_text().splitlines())
        if total:
            changed[path] = [[1, total]]
    return {path: ranges for path, ranges in changed.items() if in_scope(path)}


def line_is_changed(ranges, line):
    return any(start <= line <= end for start, end in ranges)


def parse_mutations(xml_path):
    """Return every mutation in a Pitest mutations.xml as a dict."""
    mutations = []
    for node in ET.parse(xml_path).getroot().iter("mutation"):
        mutated_class = node.findtext("mutatedClass", "")
        package = mutated_class.rsplit(".", 1)[0] if "." in mutated_class else ""
        source_file = node.findtext("sourceFile", "")
        path = f"{MAIN_ROOT}/{package.replace('.', '/')}/{source_file}"
        mutations.append(
            {
                "path": path,
                "class": mutated_class,
                "method": node.findtext("mutatedMethod", ""),
                "line": int(node.findtext("lineNumber", "0")),
                "mutator": node.findtext("mutator", "").rsplit(".", 1)[-1],
                "description": node.findtext("description", ""),
                "status": node.get("status", ""),
            }
        )
    return mutations


def mutant_key(mutation):
    return (
        mutation["path"],
        mutation["method"],
        mutation["line"],
        mutation["mutator"],
        mutation["description"],
    )


def survivor_counter(mutations):
    return Counter(mutant_key(m) for m in mutations if m["status"] in SURVIVOR_STATUSES)


def load_json(path):
    return json.loads(Path(path).read_text())
