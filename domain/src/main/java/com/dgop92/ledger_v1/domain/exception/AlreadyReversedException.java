package com.dgop92.ledger_v1.domain.exception;

/**
 * Raised when a reversal is attempted against a Transaction that has already been reversed, or
 * against a Transaction that is itself a reversal (no re-reversal).
 */
public final class AlreadyReversedException extends DomainException {

  public AlreadyReversedException(String transactionId) {
    super("ALREADY_REVERSED", "Transaction already reversed: " + transactionId);
  }
}
