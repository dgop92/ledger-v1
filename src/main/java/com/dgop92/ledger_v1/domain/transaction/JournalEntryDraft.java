package com.dgop92.ledger_v1.domain.transaction;

import com.dgop92.ledger_v1.domain.account.AccountId;
import com.dgop92.ledger_v1.domain.money.Money;
import java.util.Objects;

/**
 * A single Journal Entry line, submitted to {@link Transaction#balanced} to build a {@link
 * Transaction}. Carries a pre-generated {@link JournalEntryId} supplied by the caller (the use
 * case) -- this type never generates its own ID.
 */
public record JournalEntryDraft(
    JournalEntryId id, AccountId accountId, Direction direction, Money amount) {

  public JournalEntryDraft {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(accountId, "accountId must not be null");
    Objects.requireNonNull(direction, "direction must not be null");
    Objects.requireNonNull(amount, "amount must not be null");
  }
}
