package com.dgop92.ledger_v1.domain.exception;

/** Raised when a Journal Entry references an Account ID that does not exist. */
public final class UnknownAccountException extends DomainException {

  public UnknownAccountException(String detail) {
    super("UNKNOWN_ACCOUNT", "Unknown account: " + detail);
  }
}
