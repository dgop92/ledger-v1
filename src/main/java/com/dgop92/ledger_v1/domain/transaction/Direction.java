package com.dgop92.ledger_v1.domain.transaction;

/**
 * The side of a Journal Entry. Distinct from {@link
 * com.dgop92.ledger_v1.domain.account.AccountType.NormalBalance} even though it currently shares
 * the same two values -- an entry's direction is a separate concept from an account type's
 * normal-balance side.
 */
public enum Direction {
  DEBIT,
  CREDIT
}
