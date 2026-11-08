---
name: creating-pull-requests
compatibility: Requires git and an authenticated GitHub CLI (gh)
description: Drafts and opens a GitHub pull request for the current branch, using .github/pull_request_template.md as the source of truth for the PR body shape (honest test-plan checklist, Claude Code attribution footer). Use when the user explicitly asks to create a PR, open a pull request, or ship a branch ('PR this', 'push this for review'). Do NOT use for running build/test verification (assumed already done) or for deciding how to integrate a branch (merge vs PR vs keep) — that decision belongs to the user.
---

# Creating Pull Requests

Opens a GitHub PR for the current branch using this repo's house style. This
skill only drafts and opens the PR — it assumes verification (`./gradlew build`)
already happened this session per AGENTS.md, and does not re-run it.

## When NOT to use this

- The user hasn't decided how to integrate the branch yet (merge locally vs.
  PR vs. leave as-is) — that's their call, not this skill's.
- Verification hasn't been run this session — run it first, then come back.
- The current branch is `main`, or has no commits ahead of `main`
  (`git log main..HEAD --oneline` is empty) — tell the user there's nothing
  to open a PR for.

## Steps

1. **Push the branch, no confirmation needed:**

   ```bash
   git push -u origin <current-branch>
   ```

   This skill is explicitly authorized to push without asking first — that's
   a deliberate exception to the general "confirm before push" rule, scoped
   to this skill only. It never covers pushing to `main` or force-pushing.

2. **Read `.github/pull_request_template.md`** — it is the source of truth
   for the PR body. Review the full change set going into the PR
   (`git log main..HEAD`, `git diff main...HEAD`), not just the latest commit.

3. **Draft the PR body from the template:**

   - Keep every heading from the template, in the same order and with the
     same wording. Do not add, rename, or drop sections, and do not rely on
     a shape remembered from elsewhere — if the template changes, the PR
     body changes with it.
   - Replace each placeholder (including `<!-- ... -->` comments) with real
     content; leave no HTML comments or placeholder text behind.
   - Section-filling rules:
     - **Summary-style sections**: bullet points, focused on why, matching
       the tone of past PRs (`gh pr list --state merged` for reference).
     - **Issue-link lines** (e.g. `Closes #`), if the template has one:
       fill in only when an issue number is obviously inferable (branch
       name, session context, explicit user mention). Otherwise remove the
       line — do not ask the user for one and do not guess.
     - **Test-plan checklists**: list only checks that were *actually run
       this session*, checked or unchecked honestly based on their real
       outcome. Never mark a box `[x]` for a check that wasn't run. Template
       items that weren't run may be left unchecked or removed; it's fine
       for the list to differ from the template's example items.
   - **Attribution footer**: append it per the current system instructions
     for pull request descriptions (checked at the time this skill runs —
     do not hardcode a footer here, as the required text can change).

4. **Open the PR ready for review** (not draft):

   ```bash
   gh pr create --title "<short imperative title>" --body "$(cat <<'EOF'
   <drafted body>
   EOF
   )"
   ```

   Base branch defaults to `main` unless the user says otherwise.

5. **Report the PR URL** `gh pr create` prints back to the user.

## Example

Template (`.github/pull_request_template.md`):

```markdown
## Summary
- <!-- What changed and why -->

## Test plan
- [ ] <!-- How this was verified (tests run, manual checks, etc.) -->
```

Filled body, when `./gradlew build` passed and nothing was checked manually:

```markdown
## Summary
- Reject transfers between accounts with different currencies so ledger balances can't silently mix units.

## Test plan
- [x] `./gradlew build`
- [ ] Manual check against the local Postgres (not run)

<attribution footer per system instructions>
```

## Notes

- This skill passes an explicit `--body`, so GitHub's auto-applied template
  is bypassed; step 3 is what keeps the body aligned with the template.
