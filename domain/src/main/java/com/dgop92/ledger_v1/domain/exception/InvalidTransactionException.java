package com.dgop92.ledger_v1.domain.exception;

/** Raised when a Transaction request is structurally invalid (entry count, amounts, fields). */
public final class InvalidTransactionException extends DomainException {

  public InvalidTransactionException(String reason) {
    super("INVALID_TRANSACTION", "Invalid transaction: " + reason);
  }
}
