package com.dgop92.ledger_v1.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dgop92.ledger_v1.domain.account.AccountId;
import com.dgop92.ledger_v1.domain.exception.AlreadyReversedException;
import com.dgop92.ledger_v1.domain.exception.IdempotencyConflictException;
import com.dgop92.ledger_v1.domain.exception.TransactionNotFoundException;
import com.dgop92.ledger_v1.domain.money.Money;
import com.dgop92.ledger_v1.domain.port.IdGenerator;
import com.dgop92.ledger_v1.domain.transaction.Direction;
import com.dgop92.ledger_v1.domain.transaction.IdempotencyKey;
import com.dgop92.ledger_v1.domain.transaction.JournalEntry;
import com.dgop92.ledger_v1.domain.transaction.JournalEntryDraft;
import com.dgop92.ledger_v1.domain.transaction.JournalEntryId;
import com.dgop92.ledger_v1.domain.transaction.Transaction;
import com.dgop92.ledger_v1.domain.transaction.TransactionId;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReverseTransactionUseCaseTest {

  private static final Currency USD = Currency.getInstance("USD");

  private final FakeTransactionRepository transactionRepository = new FakeTransactionRepository();
  private final IdGenerator idGenerator =
      new IdGenerator() {
        @Override
        public AccountId newAccountId() {
          throw new UnsupportedOperationException("not used by ReverseTransactionUseCase");
        }

        @Override
        public TransactionId newTransactionId() {
          return new TransactionId(UUID.randomUUID());
        }

        @Override
        public JournalEntryId newJournalEntryId() {
          return new JournalEntryId(UUID.randomUUID());
        }
      };
  private final ReverseTransactionUseCase useCase =
      new ReverseTransactionUseCase(transactionRepository, idGenerator);

  private Transaction postOriginal(AccountId debitAccount, AccountId creditAccount, long amount) {
    JournalEntryDraft debit =
        new JournalEntryDraft(
            new JournalEntryId(UUID.randomUUID()),
            debitAccount,
            Direction.DEBIT,
            new Money(amount, USD));
    JournalEntryDraft credit =
        new JournalEntryDraft(
            new JournalEntryId(UUID.randomUUID()),
            creditAccount,
            Direction.CREDIT,
            new Money(amount, USD));
    Transaction original =
        Transaction.balanced(
            new TransactionId(UUID.randomUUID()),
            Instant.now(),
            List.of(debitAccount, creditAccount),
            List.of(debit, credit));
    return transactionRepository.append(
        original, IdempotencyKey.forPost("post-" + UUID.randomUUID(), "hash-" + UUID.randomUUID()));
  }

  @Test
  void reversesTransactionWithInvertedEntries() {
    AccountId debitAccount = new AccountId(UUID.randomUUID());
    AccountId creditAccount = new AccountId(UUID.randomUUID());
    Transaction original = postOriginal(debitAccount, creditAccount, 1000);

    Transaction reversal =
        useCase.execute(new ReverseTransactionCommand(original.id().toString(), "reverse-key-1"));

    assertEquals(2, reversal.entries().size());
    assertTrue(reversal.originalTransactionId().isPresent());
    assertEquals(original.id(), reversal.originalTransactionId().get());

    assertEquals(Direction.CREDIT, findEntry(reversal, debitAccount).direction());
    assertEquals(Direction.DEBIT, findEntry(reversal, creditAccount).direction());
    assertEquals(1000L, findEntry(reversal, debitAccount).amount().amountMinorUnits());
    assertEquals(1000L, findEntry(reversal, creditAccount).amount().amountMinorUnits());
  }

  @Test
  void throwsNotFoundForUnknownOriginal() {
    assertThrows(
        TransactionNotFoundException.class,
        () ->
            useCase.execute(
                new ReverseTransactionCommand(UUID.randomUUID().toString(), "reverse-key-2")));
  }

  @Test
  void idempotentReplayReturnsSameResultWithoutDuplicateAppend() {
    AccountId debitAccount = new AccountId(UUID.randomUUID());
    AccountId creditAccount = new AccountId(UUID.randomUUID());
    Transaction original = postOriginal(debitAccount, creditAccount, 1000);
    String key = "reverse-key-3";

    Transaction first =
        useCase.execute(new ReverseTransactionCommand(original.id().toString(), key));
    int appendedAfterFirst = transactionRepository.appended.size();

    Transaction second =
        useCase.execute(new ReverseTransactionCommand(original.id().toString(), key));

    assertEquals(first.id(), second.id());
    assertEquals(appendedAfterFirst, transactionRepository.appended.size());
  }

  @Test
  void idempotencyConflictForSameKeyDifferentOriginal() {
    AccountId debitAccount1 = new AccountId(UUID.randomUUID());
    AccountId creditAccount1 = new AccountId(UUID.randomUUID());
    Transaction original1 = postOriginal(debitAccount1, creditAccount1, 1000);

    AccountId debitAccount2 = new AccountId(UUID.randomUUID());
    AccountId creditAccount2 = new AccountId(UUID.randomUUID());
    Transaction original2 = postOriginal(debitAccount2, creditAccount2, 2000);

    String key = "reverse-key-4";
    useCase.execute(new ReverseTransactionCommand(original1.id().toString(), key));

    assertThrows(
        IdempotencyConflictException.class,
        () -> useCase.execute(new ReverseTransactionCommand(original2.id().toString(), key)));
  }

  @Test
  void rejectsReversingAlreadyReversedOriginal() {
    AccountId debitAccount = new AccountId(UUID.randomUUID());
    AccountId creditAccount = new AccountId(UUID.randomUUID());
    Transaction original = postOriginal(debitAccount, creditAccount, 1000);

    useCase.execute(new ReverseTransactionCommand(original.id().toString(), "reverse-key-5a"));

    assertThrows(
        AlreadyReversedException.class,
        () ->
            useCase.execute(
                new ReverseTransactionCommand(original.id().toString(), "reverse-key-5b")));
  }

  @Test
  void rejectsReversingAReversal() {
    AccountId debitAccount = new AccountId(UUID.randomUUID());
    AccountId creditAccount = new AccountId(UUID.randomUUID());
    Transaction original = postOriginal(debitAccount, creditAccount, 1000);

    Transaction reversal =
        useCase.execute(new ReverseTransactionCommand(original.id().toString(), "reverse-key-6a"));

    assertThrows(
        AlreadyReversedException.class,
        () ->
            useCase.execute(
                new ReverseTransactionCommand(reversal.id().toString(), "reverse-key-6b")));
  }

  @Test
  void shapeIdenticalInvertedEntriesFromDifferentOriginalsDoNotCollide() {
    AccountId accountA = new AccountId(UUID.randomUUID());
    AccountId accountB = new AccountId(UUID.randomUUID());

    // Two distinct originals whose entries produce shape-identical inverted entries: same
    // accounts, same amounts, same directions -- differing only by which Transaction they belong
    // to.
    Transaction original1 = postOriginal(accountA, accountB, 500);
    Transaction original2 = postOriginal(accountA, accountB, 500);

    Transaction reversal1 =
        useCase.execute(new ReverseTransactionCommand(original1.id().toString(), "reverse-key-7a"));
    Transaction reversal2 =
        useCase.execute(new ReverseTransactionCommand(original2.id().toString(), "reverse-key-7b"));

    assertNotEquals(reversal1.id(), reversal2.id());
    assertEquals(original1.id(), reversal1.originalTransactionId().orElseThrow());
    assertEquals(original2.id(), reversal2.originalTransactionId().orElseThrow());
  }

  private static JournalEntry findEntry(Transaction transaction, AccountId accountId) {
    return transaction.entries().stream()
        .filter(entry -> entry.accountId().equals(accountId))
        .findFirst()
        .orElseThrow();
  }
}
