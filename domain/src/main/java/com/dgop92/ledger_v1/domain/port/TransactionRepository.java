package com.dgop92.ledger_v1.domain.port;

import com.dgop92.ledger_v1.domain.transaction.IdempotencyKey;
import com.dgop92.ledger_v1.domain.transaction.Transaction;

/** Persistence port for the atomic append of a {@link Transaction}. */
public interface TransactionRepository {

  /**
   * Atomically persists {@code transaction} and {@code idempotencyKey} in a single database
   * transaction. On a same-key/same-payload replay, returns the original persisted Transaction
   * (with its true persisted IDs) instead of inserting a duplicate.
   */
  Transaction append(Transaction transaction, IdempotencyKey idempotencyKey);
}
