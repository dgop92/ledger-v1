package com.dgop92.ledger_v1.app.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dgop92.ledger_v1.domain.account.Account;
import com.dgop92.ledger_v1.domain.account.AccountId;
import com.dgop92.ledger_v1.domain.account.AccountType;
import com.dgop92.ledger_v1.domain.exception.IdempotencyConflictException;
import com.dgop92.ledger_v1.domain.exception.UnknownAccountException;
import com.dgop92.ledger_v1.domain.money.Money;
import com.dgop92.ledger_v1.domain.port.AccountRepository;
import com.dgop92.ledger_v1.domain.port.IdGenerator;
import com.dgop92.ledger_v1.domain.port.TransactionRepository;
import com.dgop92.ledger_v1.domain.transaction.Direction;
import com.dgop92.ledger_v1.domain.transaction.IdempotencyKey;
import com.dgop92.ledger_v1.domain.transaction.JournalEntry;
import com.dgop92.ledger_v1.domain.transaction.JournalEntryDraft;
import com.dgop92.ledger_v1.domain.transaction.JournalEntryId;
import com.dgop92.ledger_v1.domain.transaction.Transaction;
import com.dgop92.ledger_v1.domain.transaction.TransactionId;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;

/**
 * Exercises {@code JdbiTransactionRepository} against a real Postgres instance provisioned by
 * Quarkus Dev Services (Testcontainers) -- the repository is never mocked here.
 */
@QuarkusTest
class JdbiTransactionRepositoryTest {

  @Inject TransactionRepository transactionRepository;

  @Inject AccountRepository accountRepository;

  @Inject IdGenerator idGenerator;

  @Inject Jdbi jdbi;

  private static final Currency USD = Currency.getInstance("USD");

  private AccountId createAccount(AccountType type) {
    AccountId id = idGenerator.newAccountId();
    accountRepository.save(new Account(id, "TX Repo Test Account " + id, type));
    return id;
  }

  private Transaction buildTransaction(
      AccountId debitAccount, AccountId creditAccount, long amount) {
    TransactionId transactionId = idGenerator.newTransactionId();
    Money money = new Money(amount, USD);
    return Transaction.balanced(
        transactionId,
        Instant.now(),
        List.of(debitAccount, creditAccount),
        List.of(
            new JournalEntryDraft(
                idGenerator.newJournalEntryId(), debitAccount, Direction.DEBIT, money),
            new JournalEntryDraft(
                idGenerator.newJournalEntryId(), creditAccount, Direction.CREDIT, money)));
  }

  @Test
  void appendsTransactionAtomically() {
    AccountId debitAccount = createAccount(AccountType.ASSET);
    AccountId creditAccount = createAccount(AccountType.LIABILITY);
    Transaction transaction = buildTransaction(debitAccount, creditAccount, 1000);
    IdempotencyKey idempotencyKey =
        new IdempotencyKey(UUID.randomUUID().toString(), "POST", "hash-" + UUID.randomUUID());

    Transaction result = transactionRepository.append(transaction, idempotencyKey);

    assertEquals(transaction.id(), result.id());
    assertEquals(2, result.entries().size());

    long journalEntryCount =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery("select count(*) from journal_entries where transaction_id = :id")
                    .bind("id", transaction.id().value())
                    .mapTo(Long.class)
                    .one());
    assertEquals(2L, journalEntryCount);

    long idempotencyKeyCount =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        "select count(*) from idempotency_keys where key = :key and operation = 'POST'")
                    .bind("key", idempotencyKey.key())
                    .mapTo(Long.class)
                    .one());
    assertEquals(1L, idempotencyKeyCount);
  }

  @Test
  void unknownAccountCausesRollbackAndThrowsUnknownAccountException() {
    AccountId knownAccount = createAccount(AccountType.ASSET);
    AccountId unknownAccount = new AccountId(UUID.randomUUID());
    Transaction transaction = buildTransaction(knownAccount, unknownAccount, 500);
    IdempotencyKey idempotencyKey =
        new IdempotencyKey(UUID.randomUUID().toString(), "POST", "hash-" + UUID.randomUUID());

    assertThrows(
        UnknownAccountException.class,
        () -> transactionRepository.append(transaction, idempotencyKey));

    long transactionCount =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery("select count(*) from transactions where id = :id")
                    .bind("id", transaction.id().value())
                    .mapTo(Long.class)
                    .one());
    assertEquals(0L, transactionCount);
  }

  @Test
  void samePayloadReplayReturnsOriginalPersistedJournalEntryIds() {
    AccountId debitAccount = createAccount(AccountType.ASSET);
    AccountId creditAccount = createAccount(AccountType.LIABILITY);
    String key = UUID.randomUUID().toString();
    String payloadHash = "hash-" + UUID.randomUUID();

    Transaction firstAttempt = buildTransaction(debitAccount, creditAccount, 750);
    Transaction firstResult =
        transactionRepository.append(firstAttempt, new IdempotencyKey(key, "POST", payloadHash));

    // Simulate a client retry: same idempotency key + same payload hash, but a FRESH Transaction
    // built with newly-generated (never persisted) IDs -- mirrors PostTransactionUseCase
    // re-running end to end on a retried request.
    Transaction secondAttempt = buildTransaction(debitAccount, creditAccount, 750);
    Transaction replayResult =
        transactionRepository.append(secondAttempt, new IdempotencyKey(key, "POST", payloadHash));

    assertEquals(firstResult.id(), replayResult.id());
    assertNotEquals(secondAttempt.id(), replayResult.id());

    Set<JournalEntryId> originalEntryIds =
        firstResult.entries().stream().map(JournalEntry::id).collect(Collectors.toSet());
    Set<JournalEntryId> replayEntryIds =
        replayResult.entries().stream().map(JournalEntry::id).collect(Collectors.toSet());
    Set<JournalEntryId> unpersistedRetryEntryIds =
        secondAttempt.entries().stream().map(JournalEntry::id).collect(Collectors.toSet());

    assertEquals(originalEntryIds, replayEntryIds);
    assertFalse(replayEntryIds.stream().anyMatch(unpersistedRetryEntryIds::contains));

    long transactionCount =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery("select count(*) from transactions where id = :id")
                    .bind("id", firstResult.id().value())
                    .mapTo(Long.class)
                    .one());
    assertEquals(1L, transactionCount);
  }

  @Test
  void differentPayloadWithSameKeyThrowsIdempotencyConflictException() {
    AccountId debitAccount = createAccount(AccountType.ASSET);
    AccountId creditAccount = createAccount(AccountType.LIABILITY);
    String key = UUID.randomUUID().toString();

    Transaction first = buildTransaction(debitAccount, creditAccount, 200);
    transactionRepository.append(first, new IdempotencyKey(key, "POST", "hash-a"));

    Transaction second = buildTransaction(debitAccount, creditAccount, 200);

    assertThrows(
        IdempotencyConflictException.class,
        () -> transactionRepository.append(second, new IdempotencyKey(key, "POST", "hash-b")));
  }
}
