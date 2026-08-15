package com.dgop92.ledger_v1.application.usecase;

import com.dgop92.ledger_v1.domain.account.AccountId;
import com.dgop92.ledger_v1.domain.exception.InvalidTransactionException;
import com.dgop92.ledger_v1.domain.money.Money;
import com.dgop92.ledger_v1.domain.port.IdGenerator;
import com.dgop92.ledger_v1.domain.port.TransactionRepository;
import com.dgop92.ledger_v1.domain.transaction.Direction;
import com.dgop92.ledger_v1.domain.transaction.IdempotencyKey;
import com.dgop92.ledger_v1.domain.transaction.JournalEntryDraft;
import com.dgop92.ledger_v1.domain.transaction.JournalEntryId;
import com.dgop92.ledger_v1.domain.transaction.PostingPayload;
import com.dgop92.ledger_v1.domain.transaction.Transaction;
import com.dgop92.ledger_v1.domain.transaction.TransactionId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Validates a raw {@link PostTransactionCommand}, computes its canonical payload hash, assigns one
 * {@link JournalEntryId} per line (mirroring the single {@link TransactionId} it assigns), builds
 * the balanced {@link Transaction}, and appends it atomically.
 */
public final class PostTransactionUseCase {

  private static final int MIN_ENTRY_COUNT = 2;
  private static final int MAX_ENTRY_COUNT = 100;

  /**
   * Keeps per-line amounts far below {@link Long#MAX_VALUE} so summing up to {@link
   * #MAX_ENTRY_COUNT} entries can never overflow {@code Math.addExact} inside {@code
   * Transaction.balanced}, turning a would-be {@link ArithmeticException} into a clean validation
   * error instead.
   */
  private static final long MAX_AMOUNT_MINOR_UNITS = Long.MAX_VALUE / MAX_ENTRY_COUNT;

  /**
   * This story does not yet model per-account currency; every Money amount is minted in this fixed
   * default currency.
   */
  private static final Currency DEFAULT_CURRENCY = Currency.getInstance("USD");

  private final TransactionRepository transactionRepository;
  private final IdGenerator idGenerator;

  public PostTransactionUseCase(
      TransactionRepository transactionRepository, IdGenerator idGenerator) {
    this.transactionRepository =
        Objects.requireNonNull(transactionRepository, "transactionRepository must not be null");
    this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
  }

  public Transaction execute(PostTransactionCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    Objects.requireNonNull(command.idempotencyKey(), "idempotencyKey must not be null");

    List<PostTransactionCommand.EntryInput> rawEntries = command.entries();
    if (rawEntries == null || rawEntries.size() < MIN_ENTRY_COUNT) {
      throw new InvalidTransactionException("must contain at least 2 journal entries");
    }
    if (rawEntries.size() > MAX_ENTRY_COUNT) {
      throw new InvalidTransactionException(
          "must not contain more than " + MAX_ENTRY_COUNT + " journal entries");
    }

    List<PostingPayload.Entry> payloadEntries = new ArrayList<>(rawEntries.size());
    List<JournalEntryDraft> drafts = new ArrayList<>(rawEntries.size());
    List<AccountId> accountIds = new ArrayList<>(rawEntries.size());

    for (PostTransactionCommand.EntryInput raw : rawEntries) {
      Objects.requireNonNull(raw, "entry must not be null");
      AccountId accountId = parseAccountId(raw.accountId());
      Direction direction = parseDirection(raw.direction());
      long amountMinorUnits = validateAmount(raw.amountMinorUnits());

      payloadEntries.add(
          new PostingPayload.Entry(accountId.toString(), direction.name(), amountMinorUnits));
      JournalEntryId entryId = idGenerator.newJournalEntryId();
      drafts.add(
          new JournalEntryDraft(
              entryId, accountId, direction, new Money(amountMinorUnits, DEFAULT_CURRENCY)));
      accountIds.add(accountId);
    }

    String payloadHash = new PostingPayload(payloadEntries).canonicalHash();
    TransactionId transactionId = idGenerator.newTransactionId();
    Transaction transaction =
        Transaction.balanced(transactionId, Instant.now(), accountIds, drafts);

    IdempotencyKey idempotencyKey = IdempotencyKey.forPost(command.idempotencyKey(), payloadHash);
    return transactionRepository.append(transaction, idempotencyKey);
  }

  private AccountId parseAccountId(String rawAccountId) {
    if (rawAccountId == null) {
      throw new InvalidTransactionException("accountId must not be null");
    }
    try {
      return new AccountId(UUID.fromString(rawAccountId));
    } catch (IllegalArgumentException e) {
      throw new InvalidTransactionException("accountId must be a valid UUID: " + rawAccountId);
    }
  }

  private Direction parseDirection(String rawDirection) {
    if (rawDirection == null) {
      throw new InvalidTransactionException("direction must not be null");
    }
    try {
      return Direction.valueOf(rawDirection);
    } catch (IllegalArgumentException e) {
      throw new InvalidTransactionException("direction must be DEBIT or CREDIT: " + rawDirection);
    }
  }

  private long validateAmount(BigDecimal rawAmount) {
    if (rawAmount == null) {
      throw new InvalidTransactionException("amountMinorUnits must not be null");
    }
    if (rawAmount.stripTrailingZeros().scale() > 0) {
      throw new InvalidTransactionException("amountMinorUnits must be a whole number");
    }
    long amountMinorUnits;
    try {
      amountMinorUnits = rawAmount.longValueExact();
    } catch (ArithmeticException e) {
      throw new InvalidTransactionException("amountMinorUnits must fit in a long");
    }
    if (amountMinorUnits <= 0) {
      throw new InvalidTransactionException("amountMinorUnits must be positive");
    }
    if (amountMinorUnits > MAX_AMOUNT_MINOR_UNITS) {
      throw new InvalidTransactionException(
          "amountMinorUnits must not exceed " + MAX_AMOUNT_MINOR_UNITS);
    }
    return amountMinorUnits;
  }
}
