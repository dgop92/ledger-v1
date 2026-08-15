package com.dgop92.ledger_v1.domain.exception;

/**
 * Raised when the same Idempotency-Key is resubmitted with a payload whose hash does not match the
 * originally persisted payload.
 */
public final class IdempotencyConflictException extends DomainException {

  public IdempotencyConflictException(String key) {
    super("IDEMPOTENCY_CONFLICT", "Idempotency key conflict: " + key);
  }
}
