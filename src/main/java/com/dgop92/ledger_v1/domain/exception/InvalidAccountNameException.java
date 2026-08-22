package com.dgop92.ledger_v1.domain.exception;

/** Raised when an Account is created with a null, blank, or over-length name. */
public final class InvalidAccountNameException extends DomainException {

  public InvalidAccountNameException(String reason) {
    super("INVALID_ACCOUNT_NAME", "Invalid account name: " + reason);
  }
}
