# Race safety comes from database constraints, not check-then-insert

Posting and reversing a transaction are each one atomic repository call, and concurrent-request safety is delegated to Postgres constraints: `UNIQUE (key, operation)` on `idempotency_keys` (a replayed request), `UNIQUE (original_transaction_id)` on `transactions` (a double reversal), and the `journal_entries.account_id` foreign key (an unknown account). The adapter maps each violation to the matching domain exception.

We rejected checking first and inserting after (for example looking up the idempotency key or the original transaction before writing) because two concurrent requests can both pass the check before either commits, producing a duplicate posting or a double reversal.

Consequence: repository tests must run against real Postgres (Testcontainers), because mocks would not exercise the constraints that carry the guarantee.
