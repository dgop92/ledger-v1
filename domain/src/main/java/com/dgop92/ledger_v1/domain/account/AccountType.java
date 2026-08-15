package com.dgop92.ledger_v1.domain.account;

/** The set of allowed account types, each with a fixed normal-balance direction. */
public enum AccountType {
  ASSET(NormalBalance.DEBIT),
  LIABILITY(NormalBalance.CREDIT),
  EQUITY(NormalBalance.CREDIT),
  REVENUE(NormalBalance.CREDIT),
  EXPENSE(NormalBalance.DEBIT);

  private final NormalBalance normalBalance;

  AccountType(NormalBalance normalBalance) {
    this.normalBalance = normalBalance;
  }

  public NormalBalance normalBalance() {
    return normalBalance;
  }

  /** The side of the ledger on which an account type's balance normally increases. */
  public enum NormalBalance {
    DEBIT,
    CREDIT
  }
}
