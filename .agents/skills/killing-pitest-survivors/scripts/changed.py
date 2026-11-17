"""Print the in-scope classes and changed line ranges since the merge-base with main.

Usage: uv run --no-project changed.py [--base main] [--out FILE]

Writes JSON {"files": {path: [[start, end], ...]}, "classes": [FQCN, ...]}.
Prints "nothing in Pitest scope" and exits 0 when the diff touches no in-scope class.
"""

import argparse
import json
import sys

from common import changed_lines, path_to_class, repo_root, state_dir


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", default="main")
    parser.add_argument("--out", help="write JSON here (default build/pitest-skill/changed.json)")
    args = parser.parse_args()

    files = changed_lines(base_ref=args.base)
    if not files:
        print("nothing in Pitest scope")
        return 0
    result = {"files": files, "classes": sorted(path_to_class(p) for p in files)}
    out = args.out or str(state_dir(repo_root()) / "changed.json")
    with open(out, "w") as handle:
        json.dump(result, handle, indent=2)
    print(json.dumps(result, indent=2))
    print(f"written to {out}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
