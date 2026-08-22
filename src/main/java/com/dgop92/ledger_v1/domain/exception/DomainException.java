package com.dgop92.ledger_v1.domain.exception;

/** Base type for the fixed set of unchecked domain exceptions (AD-9), one per errorCode. */
public abstract class DomainException extends RuntimeException {

  private final String errorCode;

  protected DomainException(String errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public String errorCode() {
    return errorCode;
  }
}
