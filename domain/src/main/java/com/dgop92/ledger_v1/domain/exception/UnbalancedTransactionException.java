package com.dgop92.ledger_v1.domain.exception;

/** Raised when a Transaction's journal entries do not have equal debit and credit totals. */
public final class UnbalancedTransactionException extends DomainException {

  public UnbalancedTransactionException(long debitTotal, long creditTotal) {
    super(
        "UNBALANCED_TRANSACTION",
        "Debits (" + debitTotal + ") do not equal credits (" + creditTotal + ")");
  }
}
