package com.dgop92.ledger_v1.app.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dgop92.ledger_v1.domain.account.Account;
import com.dgop92.ledger_v1.domain.account.AccountId;
import com.dgop92.ledger_v1.domain.account.AccountType;
import com.dgop92.ledger_v1.domain.money.Money;
import com.dgop92.ledger_v1.domain.port.AccountRepository;
import com.dgop92.ledger_v1.domain.port.IdGenerator;
import com.dgop92.ledger_v1.domain.port.JournalRepository;
import com.dgop92.ledger_v1.domain.port.TransactionRepository;
import com.dgop92.ledger_v1.domain.transaction.Direction;
import com.dgop92.ledger_v1.domain.transaction.IdempotencyKey;
import com.dgop92.ledger_v1.domain.transaction.JournalEntry;
import com.dgop92.ledger_v1.domain.transaction.JournalEntryDraft;
import com.dgop92.ledger_v1.domain.transaction.Transaction;
import com.dgop92.ledger_v1.domain.transaction.TransactionId;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Exercises {@code JdbiJournalRepository} against a real Postgres instance provisioned by Quarkus
 * Dev Services (Testcontainers) -- the repository is never mocked here.
 */
@QuarkusTest
class JdbiJournalRepositoryTest {

  private static final Currency USD = Currency.getInstance("USD");

  @Inject JournalRepository journalRepository;

  @Inject AccountRepository accountRepository;

  @Inject TransactionRepository transactionRepository;

  @Inject IdGenerator idGenerator;

  private AccountId createAccount(AccountType type) {
    AccountId id = idGenerator.newAccountId();
    accountRepository.save(new Account(id, "Journal Repo Test Account " + id, type));
    return id;
  }

  @Test
  void findsRawUnfilteredUnsummedEntriesForKnownAccount() {
    AccountId debitAccount = createAccount(AccountType.ASSET);
    AccountId creditAccount = createAccount(AccountType.LIABILITY);

    postTransaction(debitAccount, creditAccount, 1000);
    postTransaction(debitAccount, creditAccount, 250);

    List<JournalEntry> entries = journalRepository.findByAccountId(debitAccount);

    assertEquals(2, entries.size());
    assertTrue(entries.stream().allMatch(entry -> entry.accountId().equals(debitAccount)));
    assertTrue(entries.stream().allMatch(entry -> entry.direction() == Direction.DEBIT));
    assertTrue(entries.stream().anyMatch(entry -> entry.amount().amountMinorUnits() == 1000L));
    assertTrue(entries.stream().anyMatch(entry -> entry.amount().amountMinorUnits() == 250L));
  }

  @Test
  void returnsEmptyListForAccountWithNoEntries() {
    AccountId id = createAccount(AccountType.ASSET);

    List<JournalEntry> entries = journalRepository.findByAccountId(id);

    assertTrue(entries.isEmpty());
  }

  @Test
  void returnsEmptyListForUnknownAccount() {
    AccountId id = new AccountId(UUID.randomUUID());

    List<JournalEntry> entries = journalRepository.findByAccountId(id);

    assertTrue(entries.isEmpty());
  }

  private void postTransaction(AccountId debitAccount, AccountId creditAccount, long amount) {
    TransactionId transactionId = idGenerator.newTransactionId();
    Money money = new Money(amount, USD);
    Transaction transaction =
        Transaction.balanced(
            transactionId,
            Instant.now(),
            List.of(debitAccount, creditAccount),
            List.of(
                new JournalEntryDraft(
                    idGenerator.newJournalEntryId(), debitAccount, Direction.DEBIT, money),
                new JournalEntryDraft(
                    idGenerator.newJournalEntryId(), creditAccount, Direction.CREDIT, money)));
    IdempotencyKey idempotencyKey =
        new IdempotencyKey(UUID.randomUUID().toString(), "POST", "hash-" + UUID.randomUUID());
    transactionRepository.append(transaction, idempotencyKey);
  }
}
