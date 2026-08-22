package com.dgop92.ledger_v1.application.usecase;

import com.dgop92.ledger_v1.domain.account.AccountId;
import com.dgop92.ledger_v1.domain.exception.AlreadyReversedException;
import com.dgop92.ledger_v1.domain.exception.IdempotencyConflictException;
import com.dgop92.ledger_v1.domain.port.TransactionRepository;
import com.dgop92.ledger_v1.domain.transaction.IdempotencyKey;
import com.dgop92.ledger_v1.domain.transaction.Transaction;
import com.dgop92.ledger_v1.domain.transaction.TransactionId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * In-memory {@link TransactionRepository} test double, shared across use case unit tests.
 * Approximates {@code JdbiTransactionRepository}'s unique-constraint semantics closely enough to
 * exercise idempotent replay, idempotency conflict, and already-reversed detection without a real
 * database: an {@code original_transaction_id} already present among {@link #appended} is treated
 * like the DB's {@code uq_transactions_original_transaction_id} violation (checked first, mirroring
 * the insert order in the real repository), and a reused (key, operation) pair is treated like the
 * {@code uq_idempotency_keys_key_operation} violation.
 */
final class FakeTransactionRepository implements TransactionRepository {

  final List<Transaction> appended = new ArrayList<>();
  final List<IdempotencyKey> idempotencyKeys = new ArrayList<>();

  @Override
  public Transaction append(Transaction transaction, IdempotencyKey idempotencyKey) {
    if (transaction.originalTransactionId().isPresent()) {
      boolean alreadyReversed =
          appended.stream()
              .anyMatch(
                  existing ->
                      existing.originalTransactionId().equals(transaction.originalTransactionId()));
      if (alreadyReversed) {
        return resolveConflict(idempotencyKey, transaction);
      }
    }

    int existingIndex = indexOfIdempotencyKey(idempotencyKey.key(), idempotencyKey.operation());
    if (existingIndex >= 0) {
      IdempotencyKey existing = idempotencyKeys.get(existingIndex);
      if (existing.payloadHash().equals(idempotencyKey.payloadHash())) {
        return appended.get(existingIndex);
      }
      throw new IdempotencyConflictException(idempotencyKey.key());
    }

    appended.add(transaction);
    idempotencyKeys.add(idempotencyKey);
    return transaction;
  }

  private Transaction resolveConflict(IdempotencyKey idempotencyKey, Transaction attempted) {
    int existingIndex = indexOfIdempotencyKey(idempotencyKey.key(), idempotencyKey.operation());
    if (existingIndex >= 0) {
      IdempotencyKey existing = idempotencyKeys.get(existingIndex);
      if (existing.payloadHash().equals(idempotencyKey.payloadHash())) {
        return appended.get(existingIndex);
      }
      throw new IdempotencyConflictException(idempotencyKey.key());
    }
    String originalTransactionId =
        attempted.originalTransactionId().map(TransactionId::toString).orElse("unknown");
    throw new AlreadyReversedException(originalTransactionId);
  }

  private int indexOfIdempotencyKey(String key, String operation) {
    for (int i = 0; i < idempotencyKeys.size(); i++) {
      IdempotencyKey candidate = idempotencyKeys.get(i);
      if (candidate.key().equals(key) && candidate.operation().equals(operation)) {
        return i;
      }
    }
    return -1;
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
