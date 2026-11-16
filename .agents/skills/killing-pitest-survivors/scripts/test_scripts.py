"""Unit tests for the helper scripts. Run: uv run --no-project python -m unittest test_scripts (from this folder)."""

import tempfile
import unittest
from pathlib import Path

import guardrail
from common import line_is_changed, parse_hunks, parse_mutations
from survivors import group_by_line

DIFF = """\
+++ b/src/main/java/com/dgop92/ledger_v1/domain/Money.java
@@ -10,0 +11,3 @@ class Money
@@ -20 +23 @@ class Money
@@ -30,2 +33,0 @@ class Money
+++ /dev/null
"""

XML = """<mutations>
<mutation detected='false' status='{status}' numberOfTestsRun='1'>
<sourceFile>Money.java</sourceFile><mutatedClass>com.dgop92.ledger_v1.domain.Money</mutatedClass>
<mutatedMethod>add</mutatedMethod><lineNumber>{line}</lineNumber>
<mutator>org.pitest.mutationtest.engine.gregor.mutators.MathMutator</mutator>
<description>Replaced addition with subtraction</description></mutation>
</mutations>"""


def write_xml(directory, name, status, line=12):
    path = Path(directory) / name
    path.write_text(XML.format(status=status, line=line))
    return str(path)


class DiffParsing(unittest.TestCase):
    def test_hunks_give_added_and_modified_ranges_and_skip_deletions(self):
        changed = parse_hunks(DIFF)
        self.assertEqual(
            {"src/main/java/com/dgop92/ledger_v1/domain/Money.java": [[11, 13], [23, 23]]},
            changed,
        )

    def test_line_is_changed_is_inclusive(self):
        self.assertTrue(line_is_changed([[11, 13]], 13))
        self.assertFalse(line_is_changed([[11, 13]], 14))


class SurvivorGrouping(unittest.TestCase):
    def test_splits_changed_from_pre_existing_and_labels_status(self):
        with tempfile.TemporaryDirectory() as tmp:
            mutations = parse_mutations(write_xml(tmp, "a.xml", "NO_COVERAGE", line=12))
        path = "src/main/java/com/dgop92/ledger_v1/domain/Money.java"
        on_changed, pre_existing = group_by_line(mutations, {path: [[11, 13]]})
        self.assertEqual(["NO_COVERAGE"], [g["status"] for g in on_changed])
        self.assertEqual([], pre_existing)
        on_changed, pre_existing = group_by_line(mutations, {path: [[1, 2]]})
        self.assertEqual([], on_changed)
        self.assertEqual(1, len(pre_existing))


class Guardrail(unittest.TestCase):
    TEST = (
        "class MoneyTest {\n"
        "  @Test\n  void adds() {\n    assertEquals(3, money.add(1, 2));\n  }\n}\n"
    )

    def test_weakened_assertion_and_removed_method_are_reported(self):
        weakened = self.TEST.replace("assertEquals(3, money.add(1, 2))", "assertNotNull(money)")
        reasons = guardrail.check_tests({"t": self.TEST}, {"t": weakened})
        self.assertTrue(any("assertion removed or altered" in r for r in reasons))
        reasons = guardrail.check_tests({"t": self.TEST}, {"t": "class MoneyTest {}"})
        self.assertTrue(any("test method removed" in r for r in reasons))

    def test_added_test_and_assertion_pass(self):
        extended = self.TEST.rstrip("}\n") + "\n  @Test\n  void subtracts() {\n    assertEquals(1, money.sub(3, 2));\n  }\n}\n"
        self.assertEqual([], guardrail.check_tests({"t": self.TEST}, {"t": extended}))

    def test_disabled_is_rejected(self):
        disabled = self.TEST.replace("@Test", "@Test @Disabled")
        self.assertTrue(guardrail.check_tests({"t": self.TEST}, {"t": disabled}))

    def test_production_change_is_reported(self):
        self.assertEqual(1, len(guardrail.check_production({"a": "x"}, {"a": "y"})))
        self.assertEqual([], guardrail.check_production({"a": "x"}, {"a": "x"}))

    def test_verify_reverts_production_edit_and_killed_without_test(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            main = root / "src/main/java/Money.java"
            test = root / "src/test/MoneyTest.java"
            main.parent.mkdir(parents=True)
            test.parent.mkdir(parents=True)
            main.write_text("return a + b;")
            test.write_text(self.TEST)
            snapshot = root / "build" / "pitest-skill" / "snapshot"
            for tree in guardrail.TREES:
                (snapshot / tree).parent.mkdir(parents=True, exist_ok=True)
                __import__("shutil").copytree(root / tree, snapshot / tree)
            before = write_xml(tmp, "before.xml", "SURVIVED")
            after = write_xml(tmp, "after.xml", "KILLED")
            main.write_text("return a - b;")
            reasons, _ = guardrail.verify(root, before, after)
            self.assertTrue(any("production code changed" in r for r in reasons))
            self.assertTrue(any("no test was added or changed" in r for r in reasons))
            self.assertEqual("return a + b;", main.read_text())


if __name__ == "__main__":
    unittest.main()
