package com.dgop92.ledger_v1.domain.transaction;

import com.dgop92.ledger_v1.domain.account.AccountId;
import com.dgop92.ledger_v1.domain.money.Money;
import java.util.Objects;

/** A persisted line of a {@link Transaction}. */
public record JournalEntry(
    JournalEntryId id, AccountId accountId, Direction direction, Money amount) {

  public JournalEntry {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(accountId, "accountId must not be null");
    Objects.requireNonNull(direction, "direction must not be null");
    Objects.requireNonNull(amount, "amount must not be null");
  }
}
