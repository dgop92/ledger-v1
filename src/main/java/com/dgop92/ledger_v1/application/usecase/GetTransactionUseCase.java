package com.dgop92.ledger_v1.application.usecase;

import com.dgop92.ledger_v1.domain.exception.TransactionNotFoundException;
import com.dgop92.ledger_v1.domain.port.TransactionRepository;
import com.dgop92.ledger_v1.domain.transaction.Transaction;
import com.dgop92.ledger_v1.domain.transaction.TransactionId;
import java.util.Objects;

/**
 * Looks up a single {@link Transaction} by ID, throwing {@link TransactionNotFoundException} if
 * absent.
 */
public final class GetTransactionUseCase {

  private final TransactionRepository transactionRepository;

  public GetTransactionUseCase(TransactionRepository transactionRepository) {
    this.transactionRepository =
        Objects.requireNonNull(transactionRepository, "transactionRepository must not be null");
  }

  public Transaction execute(TransactionId id) {
    Objects.requireNonNull(id, "id must not be null");
    return transactionRepository
        .findById(id)
        .orElseThrow(() -> new TransactionNotFoundException(id.toString()));
  }
}
