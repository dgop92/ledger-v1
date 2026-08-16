package com.dgop92.ledger_v1.application.usecase;

import com.dgop92.ledger_v1.domain.account.AccountId;
import com.dgop92.ledger_v1.domain.port.TransactionRepository;
import com.dgop92.ledger_v1.domain.transaction.Transaction;
import java.util.List;
import java.util.Objects;

/**
 * Returns every persisted {@link Transaction} referencing a given Account, in posting order,
 * unbounded. The Account's existence is not validated.
 */
public final class ListTransactionsUseCase {

  private final TransactionRepository transactionRepository;

  public ListTransactionsUseCase(TransactionRepository transactionRepository) {
    this.transactionRepository =
        Objects.requireNonNull(transactionRepository, "transactionRepository must not be null");
  }

  public List<Transaction> execute(AccountId accountId) {
    Objects.requireNonNull(accountId, "accountId must not be null");
    return transactionRepository.findByAccountId(accountId);
  }
}
