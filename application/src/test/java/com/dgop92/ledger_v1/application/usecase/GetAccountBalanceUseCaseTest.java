package com.dgop92.ledger_v1.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dgop92.ledger_v1.domain.account.Account;
import com.dgop92.ledger_v1.domain.account.AccountId;
import com.dgop92.ledger_v1.domain.account.AccountType;
import com.dgop92.ledger_v1.domain.balance.Balance;
import com.dgop92.ledger_v1.domain.exception.AccountNotFoundException;
import com.dgop92.ledger_v1.domain.money.Money;
import com.dgop92.ledger_v1.domain.transaction.Direction;
import com.dgop92.ledger_v1.domain.transaction.JournalEntry;
import com.dgop92.ledger_v1.domain.transaction.JournalEntryId;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetAccountBalanceUseCaseTest {

  private static final Currency USD = Currency.getInstance("USD");

  private final FakeAccountRepository accountRepository = new FakeAccountRepository();
  private final FakeJournalRepository journalRepository = new FakeJournalRepository();
  private final GetAccountBalanceUseCase useCase =
      new GetAccountBalanceUseCase(accountRepository, journalRepository);

  @Test
  void computesBalanceFromPostedEntries() {
    AccountId id = new AccountId(UUID.randomUUID());
    accountRepository.save(new Account(id, "Cash", AccountType.ASSET));
    journalRepository.entries.add(
        new JournalEntry(
            new JournalEntryId(UUID.randomUUID()), id, Direction.DEBIT, new Money(1000L, USD)));
    journalRepository.entries.add(
        new JournalEntry(
            new JournalEntryId(UUID.randomUUID()), id, Direction.CREDIT, new Money(200L, USD)));

    Balance balance = useCase.execute(id);

    assertEquals(new Money(800L, USD), balance.amount());
  }

  @Test
  void returnsZeroBalanceWhenAccountHasNoEntries() {
    AccountId id = new AccountId(UUID.randomUUID());
    accountRepository.save(new Account(id, "Cash", AccountType.ASSET));

    Balance balance = useCase.execute(id);

    assertEquals(new Money(0L, USD), balance.amount());
  }

  @Test
  void throwsWhenAccountNotFound() {
    AccountId id = new AccountId(UUID.randomUUID());

    assertThrows(AccountNotFoundException.class, () -> useCase.execute(id));
  }
}
