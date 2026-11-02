# Balance is computed by domain replay, not SQL aggregation

`JournalRepository` returns an account's raw Journal Entries, and `Balance.computeFrom(entries, accountType)` in the domain sums them using the account type's normal-balance direction. Adapters never pre-aggregate. We rejected a SQL `SUM(...)` so that the balance arithmetic lives in pure domain code, where the invariant and property tests cover it, instead of being duplicated in a query that can silently diverge.

Consequences: a balance read costs O(entries) for that account, and there is no caching or snapshotting. Known costs at the time of writing are no index on `journal_entries.account_id` and an N+1 fan-out in `findByAccountId`. Any future cache must sit outside this contract.
