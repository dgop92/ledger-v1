package com.dgop92.ledger_v1.domain.exception;

/** Raised when a requested Account ID does not correspond to any persisted Account. */
public final class AccountNotFoundException extends DomainException {

  public AccountNotFoundException(String accountId) {
    super("ACCOUNT_NOT_FOUND", "Account not found: " + accountId);
  }
}
