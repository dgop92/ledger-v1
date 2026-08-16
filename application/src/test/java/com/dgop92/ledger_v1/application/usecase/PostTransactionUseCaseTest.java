package com.dgop92.ledger_v1.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dgop92.ledger_v1.domain.account.AccountId;
import com.dgop92.ledger_v1.domain.exception.InvalidTransactionException;
import com.dgop92.ledger_v1.domain.exception.UnbalancedTransactionException;
import com.dgop92.ledger_v1.domain.port.IdGenerator;
import com.dgop92.ledger_v1.domain.transaction.JournalEntryId;
import com.dgop92.ledger_v1.domain.transaction.Transaction;
import com.dgop92.ledger_v1.domain.transaction.TransactionId;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PostTransactionUseCaseTest {

  private final FakeTransactionRepository transactionRepository = new FakeTransactionRepository();
  private final IdGenerator idGenerator =
      new IdGenerator() {
        @Override
        public AccountId newAccountId() {
          throw new UnsupportedOperationException("not used by PostTransactionUseCase");
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
  private final PostTransactionUseCase useCase =
      new PostTransactionUseCase(transactionRepository, idGenerator);

  private static PostTransactionCommand.EntryInput entry(
      String accountId, String direction, long amount) {
    return new PostTransactionCommand.EntryInput(accountId, direction, BigDecimal.valueOf(amount));
  }

  @Test
  void postsBalancedTransaction() {
    String debitAccount = UUID.randomUUID().toString();
    String creditAccount = UUID.randomUUID().toString();

    Transaction transaction =
        useCase.execute(
            new PostTransactionCommand(
                List.of(entry(debitAccount, "DEBIT", 1000), entry(creditAccount, "CREDIT", 1000)),
                "key-1"));

    assertEquals(2, transaction.entries().size());
    assertEquals(1, transactionRepository.appended.size());
    assertEquals(transaction, transactionRepository.appended.get(0));
  }

  @Test
  void generatesDistinctJournalEntryIdsPerLine() {
    String debitAccount = UUID.randomUUID().toString();
    String creditAccount = UUID.randomUUID().toString();

    Transaction transaction =
        useCase.execute(
            new PostTransactionCommand(
                List.of(entry(debitAccount, "DEBIT", 1000), entry(creditAccount, "CREDIT", 1000)),
                "key-2"));

    assertNotEquals(transaction.entries().get(0).id(), transaction.entries().get(1).id());
  }

  @Test
  void rejectsZeroEntries() {
    assertThrows(
        InvalidTransactionException.class,
        () -> useCase.execute(new PostTransactionCommand(List.of(), "key-3")));
    assertEquals(0, transactionRepository.appended.size());
  }

  @Test
  void rejectsSingleEntry() {
    String accountId = UUID.randomUUID().toString();
    assertThrows(
        InvalidTransactionException.class,
        () ->
            useCase.execute(
                new PostTransactionCommand(List.of(entry(accountId, "DEBIT", 1000)), "key-4")));
    assertEquals(0, transactionRepository.appended.size());
  }

  @Test
  void rejectsUnbalancedEntries() {
    String debitAccount = UUID.randomUUID().toString();
    String creditAccount = UUID.randomUUID().toString();

    assertThrows(
        UnbalancedTransactionException.class,
        () ->
            useCase.execute(
                new PostTransactionCommand(
                    List.of(
                        entry(debitAccount, "DEBIT", 1000), entry(creditAccount, "CREDIT", 900)),
                    "key-5")));
    assertEquals(0, transactionRepository.appended.size());
  }

  @Test
  void rejectsNonPositiveAmount() {
    String debitAccount = UUID.randomUUID().toString();
    String creditAccount = UUID.randomUUID().toString();

    assertThrows(
        InvalidTransactionException.class,
        () ->
            useCase.execute(
                new PostTransactionCommand(
                    List.of(entry(debitAccount, "DEBIT", 0), entry(creditAccount, "CREDIT", 0)),
                    "key-6")));
    assertEquals(0, transactionRepository.appended.size());
  }

  @Test
  void rejectsNegativeAmount() {
    String debitAccount = UUID.randomUUID().toString();
    String creditAccount = UUID.randomUUID().toString();

    assertThrows(
        InvalidTransactionException.class,
        () ->
            useCase.execute(
                new PostTransactionCommand(
                    List.of(
                        entry(debitAccount, "DEBIT", -100), entry(creditAccount, "CREDIT", -100)),
                    "key-7")));
  }

  @Test
  void rejectsNonIntegerAmount() {
    String debitAccount = UUID.randomUUID().toString();
    String creditAccount = UUID.randomUUID().toString();
    PostTransactionCommand.EntryInput fractional =
        new PostTransactionCommand.EntryInput(debitAccount, "DEBIT", new BigDecimal("100.5"));

    assertThrows(
        InvalidTransactionException.class,
        () ->
            useCase.execute(
                new PostTransactionCommand(
                    List.of(fractional, entry(creditAccount, "CREDIT", 100)), "key-8")));
  }

  @Test
  void rejectsMalformedAccountId() {
    String creditAccount = UUID.randomUUID().toString();
    assertThrows(
        InvalidTransactionException.class,
        () ->
            useCase.execute(
                new PostTransactionCommand(
                    List.of(entry("not-a-uuid", "DEBIT", 100), entry(creditAccount, "CREDIT", 100)),
                    "key-9")));
  }

  @Test
  void rejectsUnknownDirection() {
    String debitAccount = UUID.randomUUID().toString();
    String creditAccount = UUID.randomUUID().toString();
    assertThrows(
        InvalidTransactionException.class,
        () ->
            useCase.execute(
                new PostTransactionCommand(
                    List.of(
                        entry(debitAccount, "SIDEWAYS", 100), entry(creditAccount, "CREDIT", 100)),
                    "key-10")));
  }

  @Test
  void directionValuesAreCaseSensitive() {
    String debitAccount = UUID.randomUUID().toString();
    String creditAccount = UUID.randomUUID().toString();
    assertThrows(
        InvalidTransactionException.class,
        () ->
            useCase.execute(
                new PostTransactionCommand(
                    List.of(entry(debitAccount, "debit", 100), entry(creditAccount, "CREDIT", 100)),
                    "key-11")));
  }
}
