package com.dgop92.ledger_v1.application.usecase;

import com.dgop92.ledger_v1.domain.account.AccountId;
import com.dgop92.ledger_v1.domain.exception.AlreadyReversedException;
import com.dgop92.ledger_v1.domain.exception.InvalidTransactionException;
import com.dgop92.ledger_v1.domain.exception.TransactionNotFoundException;
import com.dgop92.ledger_v1.domain.port.IdGenerator;
import com.dgop92.ledger_v1.domain.port.TransactionRepository;
import com.dgop92.ledger_v1.domain.transaction.Direction;
import com.dgop92.ledger_v1.domain.transaction.IdempotencyKey;
import com.dgop92.ledger_v1.domain.transaction.JournalEntry;
import com.dgop92.ledger_v1.domain.transaction.JournalEntryDraft;
import com.dgop92.ledger_v1.domain.transaction.JournalEntryId;
import com.dgop92.ledger_v1.domain.transaction.PostingPayload;
import com.dgop92.ledger_v1.domain.transaction.Transaction;
import com.dgop92.ledger_v1.domain.transaction.TransactionId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Looks up the original {@link Transaction}, inverts every entry's {@link Direction}, computes a
 * canonical payload hash that folds in the original's ID (so two reversals of different originals
 * with shape-identical inverted entries never collide under Idempotency-Key reuse), mints new IDs,
 * builds the reversal {@link Transaction} via {@link Transaction#balanced}, and appends it
 * atomically.
 */
public final class ReverseTransactionUseCase {

  private final TransactionRepository transactionRepository;
  private final IdGenerator idGenerator;

  public ReverseTransactionUseCase(
      TransactionRepository transactionRepository, IdGenerator idGenerator) {
    this.transactionRepository =
        Objects.requireNonNull(transactionRepository, "transactionRepository must not be null");
    this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
  }

  public Transaction execute(ReverseTransactionCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    Objects.requireNonNull(command.idempotencyKey(), "idempotencyKey must not be null");
    if (command.idempotencyKey().isBlank()) {
      throw new InvalidTransactionException("idempotencyKey must not be blank");
    }

    TransactionId originalId = parseTransactionId(command.originalTransactionId());
    Transaction original =
        transactionRepository
            .findById(originalId)
            .orElseThrow(() -> new TransactionNotFoundException(command.originalTransactionId()));

    if (original.originalTransactionId().isPresent()) {
      throw new AlreadyReversedException(command.originalTransactionId());
    }

    List<PostingPayload.Entry> payloadEntries = new ArrayList<>(original.entries().size() + 1);
    // Synthetic marker entry folding the original transaction's ID into the hashed payload, so
    // the persisted payload_hash is unique per original even when two originals' inverted
    // entries happen to be shape-identical.
    payloadEntries.add(
        new PostingPayload.Entry("original:" + originalId.value(), "REVERSAL_OF", 0L));

    List<JournalEntryDraft> drafts = new ArrayList<>(original.entries().size());
    List<AccountId> accountIds = new ArrayList<>(original.entries().size());

    for (JournalEntry entry : original.entries()) {
      Direction inverted =
          entry.direction() == Direction.DEBIT ? Direction.CREDIT : Direction.DEBIT;
      payloadEntries.add(
          new PostingPayload.Entry(
              entry.accountId().toString(), inverted.name(), entry.amount().amountMinorUnits()));
      JournalEntryId entryId = idGenerator.newJournalEntryId();
      drafts.add(new JournalEntryDraft(entryId, entry.accountId(), inverted, entry.amount()));
      accountIds.add(entry.accountId());
    }

    String payloadHash = new PostingPayload(payloadEntries).canonicalHash();
    TransactionId reversalId = idGenerator.newTransactionId();
    Transaction reversal =
        Transaction.balanced(
            reversalId, Instant.now(), accountIds, drafts, Optional.of(originalId));

    IdempotencyKey idempotencyKey =
        IdempotencyKey.forReverse(command.idempotencyKey(), payloadHash);
    return transactionRepository.append(reversal, idempotencyKey);
  }

  private TransactionId parseTransactionId(String rawId) {
    if (rawId == null) {
      throw new TransactionNotFoundException("null");
    }
    try {
      return new TransactionId(UUID.fromString(rawId));
    } catch (IllegalArgumentException e) {
      throw new TransactionNotFoundException(rawId);
    }
  }
}
