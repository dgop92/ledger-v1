package com.dgop92.ledger_v1.domain.transaction;

import com.dgop92.ledger_v1.domain.account.AccountId;
import com.dgop92.ledger_v1.domain.exception.InvalidTransactionException;
import com.dgop92.ledger_v1.domain.exception.UnbalancedTransactionException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** An immutable, balanced Transaction, constructible only via {@link #balanced}. */
public final class Transaction {

  private static final int MIN_ENTRY_COUNT = 2;

  private final TransactionId id;
  private final Instant postedAt;
  private final List<JournalEntry> entries;

  private Transaction(TransactionId id, Instant postedAt, List<JournalEntry> entries) {
    this.id = id;
    this.postedAt = postedAt;
    this.entries = entries;
  }

  /**
   * Builds a Transaction from pre-generated IDs and drafts, validating that debits equal credits.
   * Never generates any ID itself -- {@code id} and each {@link JournalEntryDraft}'s {@link
   * JournalEntryId} must arrive already assigned by the caller.
   */
  public static Transaction balanced(
      TransactionId id,
      Instant postedAt,
      List<AccountId> accountIds,
      List<JournalEntryDraft> lines) {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(postedAt, "postedAt must not be null");
    Objects.requireNonNull(accountIds, "accountIds must not be null");
    Objects.requireNonNull(lines, "lines must not be null");

    if (lines.size() < MIN_ENTRY_COUNT) {
      throw new InvalidTransactionException(
          "a transaction must contain at least 2 journal entries");
    }

    List<JournalEntry> entries = new ArrayList<>(lines.size());
    long debitTotal = 0L;
    long creditTotal = 0L;
    for (JournalEntryDraft line : lines) {
      Objects.requireNonNull(line, "line must not be null");
      if (line.direction() == Direction.DEBIT) {
        debitTotal = Math.addExact(debitTotal, line.amount().amountMinorUnits());
      } else {
        creditTotal = Math.addExact(creditTotal, line.amount().amountMinorUnits());
      }
      entries.add(new JournalEntry(line.id(), line.accountId(), line.direction(), line.amount()));
    }

    if (debitTotal != creditTotal) {
      throw new UnbalancedTransactionException(debitTotal, creditTotal);
    }

    return new Transaction(id, postedAt, List.copyOf(entries));
  }

  public TransactionId id() {
    return id;
  }

  public Instant postedAt() {
    return postedAt;
  }

  public List<JournalEntry> entries() {
    return entries;
  }
}
