# Glossary — Financial Ledger Backend

- **Account** — A named ledger record belonging to one entry in the flat Chart of Accounts. Has an Account Type. Immutable once created (no update/delete in v1).
- **Chart of Accounts** — The flat (non-hierarchical) set of all Accounts in the system.
- **Account Type** — Classification of an Account: `ASSET`, `LIABILITY`, `EQUITY`, `REVENUE`, or `EXPENSE`. Each has a normal balance direction: `ASSET`/`EXPENSE` are debit-normal (debits increase Balance, credits decrease it); `LIABILITY`/`EQUITY`/`REVENUE` are credit-normal (credits increase Balance, debits decrease it).
- **Journal** — The complete, immutable, append-only sequence of all posted Journal Entries. The Journal IS the audit trail.
- **Transaction** — A single business event posted to the Journal, composed of two or more balanced Journal Entries. Identified by a stable Transaction ID.
- **Journal Entry** — One line within a Transaction: a debit or credit of a given amount against one Account.
- **Debit / Credit** — The two directions a Journal Entry can move value; a Transaction's debits must sum to exactly its credits.
- **Balance** — An Account's current position, computed on demand by summing that Account's Journal Entries — never stored or cached.
- **Minor Unit** — The smallest indivisible unit of the system's currency (e.g., cents). All amounts are integers in minor units; floating point is never used.
- **Minor-Unit Exponent** — The number of minor-unit digits for the system's currency (e.g., 2 for cents), modeled explicitly even though v1 supports exactly one currency.
- **Idempotency Key** — A client-supplied unique token required on every transaction-posting command (Post Transaction, Reverse Transaction), guaranteeing a retried request does not double-post. Resubmitting the same key with the same payload replays the original result; the same key with a different payload is a conflict. A key is scoped per operation, so the same value may be used once for a post and once for a reversal.
- **Reversal / Compensating Entry** — The sole correction mechanism: a new Transaction that references an Original Transaction and reverses its effect. The Original Transaction is never edited or deleted.
- **Original Transaction** — The previously posted Transaction that a Reversal refers to. A Transaction can be reversed at most once, and a Reversal cannot itself be reversed.
- **Normal Balance** — The direction (debit or credit) that increases an Account's Balance, determined by its Account Type.

