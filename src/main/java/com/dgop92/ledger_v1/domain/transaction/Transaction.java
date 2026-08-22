package com.dgop92.ledger_v1.domain.transaction;

import com.dgop92.ledger_v1.domain.account.AccountId;
import com.dgop92.ledger_v1.domain.exception.InvalidTransactionException;
import com.dgop92.ledger_v1.domain.exception.UnbalancedTransactionException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** An immutable, balanced Transaction, constructible only via {@link #balanced}. */
public final class Transaction {

  private static final int MIN_ENTRY_COUNT = 2;

  private final TransactionId id;
  private final Instant postedAt;
  private final List<JournalEntry> entries;
  private final Optional<TransactionId> originalTransactionId;

  private Transaction(
      TransactionId id,
      Instant postedAt,
      List<JournalEntry> entries,
      Optional<TransactionId> originalTransactionId) {
    this.id = id;
    this.postedAt = postedAt;
    this.entries = entries;
    this.originalTransactionId = originalTransactionId;
  }

  /**
   * Builds a Transaction from pre-generated IDs and drafts, validating that debits equal credits.
   * Never generates any ID itself -- {@code id} and each {@link JournalEntryDraft}'s {@link
   * JournalEntryId} must arrive already assigned by the caller. Delegates to the 5-arg overload
   * with an empty {@code originalTransactionId} (i.e. this is not a reversal).
   */
  public static Transaction balanced(
      TransactionId id,
      Instant postedAt,
      List<AccountId> accountIds,
      List<JournalEntryDraft> lines) {
    return balanced(id, postedAt, accountIds, lines, Optional.empty());
  }

  /**
   * Builds a Transaction from pre-generated IDs and drafts, validating that debits equal credits.
   * Never generates any ID itself -- {@code id} and each {@link JournalEntryDraft}'s {@link
   * JournalEntryId} must arrive already assigned by the caller. {@code originalTransactionId}, when
   * present, marks this Transaction as a reversal of the referenced original.
   */
  public static Transaction balanced(
      TransactionId id,
      Instant postedAt,
      List<AccountId> accountIds,
      List<JournalEntryDraft> lines,
      Optional<TransactionId> originalTransactionId) {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(postedAt, "postedAt must not be null");
    Objects.requireNonNull(accountIds, "accountIds must not be null");
    Objects.requireNonNull(lines, "lines must not be null");
    Objects.requireNonNull(originalTransactionId, "originalTransactionId must not be null");

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

    return new Transaction(id, postedAt, List.copyOf(entries), originalTransactionId);
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

  public Optional<TransactionId> originalTransactionId() {
    return originalTransactionId;
  }
}
