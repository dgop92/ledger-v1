package com.dgop92.ledger_v1.domain.exception;

/** Raised when a requested Transaction ID does not correspond to any persisted Transaction. */
public final class TransactionNotFoundException extends DomainException {

  public TransactionNotFoundException(String transactionId) {
    super("TRANSACTION_NOT_FOUND", "Transaction not found: " + transactionId);
  }
}
