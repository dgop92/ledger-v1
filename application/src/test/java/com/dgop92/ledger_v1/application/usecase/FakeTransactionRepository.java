package com.dgop92.ledger_v1.application.usecase;

import com.dgop92.ledger_v1.domain.port.TransactionRepository;
import com.dgop92.ledger_v1.domain.transaction.IdempotencyKey;
import com.dgop92.ledger_v1.domain.transaction.Transaction;
import java.util.ArrayList;
import java.util.List;

/** In-memory {@link TransactionRepository} test double, shared across use case unit tests. */
final class FakeTransactionRepository implements TransactionRepository {

  final List<Transaction> appended = new ArrayList<>();
  final List<IdempotencyKey> idempotencyKeys = new ArrayList<>();

  @Override
  public Transaction append(Transaction transaction, IdempotencyKey idempotencyKey) {
    appended.add(transaction);
    idempotencyKeys.add(idempotencyKey);
    return transaction;
  }
}
