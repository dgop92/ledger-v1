package com.dgop92.ledger_v1.application.usecase;

import com.dgop92.ledger_v1.domain.account.AccountId;
import com.dgop92.ledger_v1.domain.port.TransactionRepository;
import com.dgop92.ledger_v1.domain.transaction.IdempotencyKey;
import com.dgop92.ledger_v1.domain.transaction.Transaction;
import com.dgop92.ledger_v1.domain.transaction.TransactionId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

  @Override
  public Optional<Transaction> findById(TransactionId id) {
    return appended.stream().filter(transaction -> transaction.id().equals(id)).findFirst();
  }

  @Override
  public List<Transaction> findByAccountId(AccountId id) {
    return appended.stream()
        .filter(
            transaction ->
                transaction.entries().stream().anyMatch(entry -> entry.accountId().equals(id)))
        .toList();
  }
}
