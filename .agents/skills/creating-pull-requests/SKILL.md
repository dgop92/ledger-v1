---
name: creating-pull-requests
description: Use when the user asks to create, open, submit, or make a pull request/PR for this repo, or says things like 'ship this branch', 'push this for review', or 'PR this'.
---

# Creating Pull Requests

Basic workflow for opening a pull request on this repo (`dgop92/ledger-v1`) with the GitHub CLI.

## Quick Start

1. Confirm the current branch isn't `main` and has commits ahead of `main`:
   ```bash
   git status
   git log main..HEAD --oneline
   ```
   If there are no commits ahead of `main`, stop and tell the user there's nothing to open a PR for.

2. Push the branch, setting upstream if needed:
   ```bash
   git push -u origin HEAD
   ```

3. Review the full set of changes going into the PR (not just the latest commit):
   ```bash
   git diff main...HEAD
   ```

4. Open the PR with `gh`, filling in the sections from `.github/pull_request_template.md`:
   ```bash
   gh pr create --title "<short imperative title>" --body "$(cat <<'EOF'
   ## Summary
   - <what changed and why>

   ## Test plan
   - [ ] <how this was verified>
   EOF
   )"
   ```
   `gh pr create` picks up `.github/pull_request_template.md` automatically when `--body`/`--body-file` is omitted; pass an explicit `--body` (as above) when filling the template in on the agent's behalf.

5. Report the PR URL back to the user.

## Notes

- Never push to `main` or force-push without explicit user confirmation.
- Only commit/push changes the user asked to include — don't sweep in unrelated local edits.
- If `gh` is not authenticated (`gh auth status` fails), tell the user instead of attempting to work around it.
