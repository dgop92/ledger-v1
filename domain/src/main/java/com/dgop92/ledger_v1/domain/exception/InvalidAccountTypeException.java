package com.dgop92.ledger_v1.domain.exception;

/** Raised when an Account is created with an Account Type outside the allowed set. */
public final class InvalidAccountTypeException extends DomainException {

  public InvalidAccountTypeException(String invalidValue) {
    super("INVALID_ACCOUNT_TYPE", "Invalid account type: " + invalidValue);
  }
}
