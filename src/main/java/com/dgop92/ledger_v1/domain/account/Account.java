package com.dgop92.ledger_v1.domain.account;

import java.util.Objects;

/** An immutable Chart of Accounts entry. */
public final class Account {

  private final AccountId id;
  private final String name;
  private final AccountType accountType;

  public Account(AccountId id, String name, AccountType accountType) {
    this.id = Objects.requireNonNull(id, "id must not be null");
    this.name = Objects.requireNonNull(name, "name must not be null");
    this.accountType = Objects.requireNonNull(accountType, "accountType must not be null");
  }

  public AccountId id() {
    return id;
  }

  public String name() {
    return name;
  }

  public AccountType accountType() {
    return accountType;
  }
}
