package com.dgop92.ledger_v1.domain.balance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dgop92.ledger_v1.domain.account.AccountId;
import com.dgop92.ledger_v1.domain.account.AccountType;
import com.dgop92.ledger_v1.domain.money.Money;
import com.dgop92.ledger_v1.domain.transaction.Direction;
import com.dgop92.ledger_v1.domain.transaction.JournalEntry;
import com.dgop92.ledger_v1.domain.transaction.JournalEntryId;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BalanceTest {

  private static final Currency USD = Currency.getInstance("USD");

  @Test
  void zeroEntriesReturnsZeroBalanceInFallbackCurrency() {
    AccountId accountId = new AccountId(UUID.randomUUID());

    Balance balance = Balance.computeFrom(accountId, List.of(), AccountType.ASSET, USD);

    assertEquals(accountId, balance.accountId());
    assertEquals(new Money(0L, USD), balance.amount());
  }

  @Test
  void debitNormalAccountIncreasesOnDebitAndDecreasesOnCredit() {
    AccountId accountId = new AccountId(UUID.randomUUID());
    List<JournalEntry> entries =
        List.of(
            entry(accountId, Direction.DEBIT, 1000),
            entry(accountId, Direction.DEBIT, 500),
            entry(accountId, Direction.CREDIT, 300));

    Balance balance = Balance.computeFrom(accountId, entries, AccountType.ASSET, USD);

    assertEquals(new Money(1200L, USD), balance.amount());
  }

  @Test
  void creditNormalAccountIncreasesOnCreditAndDecreasesOnDebit() {
    AccountId accountId = new AccountId(UUID.randomUUID());
    List<JournalEntry> entries =
        List.of(
            entry(accountId, Direction.CREDIT, 2000),
            entry(accountId, Direction.CREDIT, 100),
            entry(accountId, Direction.DEBIT, 400));

    Balance balance = Balance.computeFrom(accountId, entries, AccountType.LIABILITY, USD);

    assertEquals(new Money(1700L, USD), balance.amount());
  }

  @Test
  void allowsNegativeResultingBalance() {
    AccountId accountId = new AccountId(UUID.randomUUID());
    List<JournalEntry> entries =
        List.of(entry(accountId, Direction.DEBIT, 100), entry(accountId, Direction.CREDIT, 500));

    Balance balance = Balance.computeFrom(accountId, entries, AccountType.ASSET, USD);

    assertEquals(new Money(-400L, USD), balance.amount());
  }

  @Test
  void creditNormalEquityAccountIncreasesOnCreditAndDecreasesOnDebit() {
    AccountId accountId = new AccountId(UUID.randomUUID());
    List<JournalEntry> entries =
        List.of(entry(accountId, Direction.CREDIT, 900), entry(accountId, Direction.DEBIT, 200));

    Balance balance = Balance.computeFrom(accountId, entries, AccountType.EQUITY, USD);

    assertEquals(new Money(700L, USD), balance.amount());
  }

  @Test
  void creditNormalRevenueAccountIncreasesOnCreditAndDecreasesOnDebit() {
    AccountId accountId = new AccountId(UUID.randomUUID());
    List<JournalEntry> entries =
        List.of(entry(accountId, Direction.CREDIT, 1500), entry(accountId, Direction.DEBIT, 100));

    Balance balance = Balance.computeFrom(accountId, entries, AccountType.REVENUE, USD);

    assertEquals(new Money(1400L, USD), balance.amount());
  }

  @Test
  void debitNormalExpenseAccountIncreasesOnDebitAndDecreasesOnCredit() {
    AccountId accountId = new AccountId(UUID.randomUUID());
    List<JournalEntry> entries =
        List.of(entry(accountId, Direction.DEBIT, 600), entry(accountId, Direction.CREDIT, 50));

    Balance balance = Balance.computeFrom(accountId, entries, AccountType.EXPENSE, USD);

    assertEquals(new Money(550L, USD), balance.amount());
  }

  @Test
  void resultCurrencyComesFromEntriesWhenPresent() {
    AccountId accountId = new AccountId(UUID.randomUUID());
    Currency eur = Currency.getInstance("EUR");
    List<JournalEntry> entries =
        List.of(
            new JournalEntry(
                new JournalEntryId(UUID.randomUUID()),
                accountId,
                Direction.DEBIT,
                new Money(100L, eur)));

    Balance balance = Balance.computeFrom(accountId, entries, AccountType.ASSET, USD);

    assertEquals(eur, balance.amount().currency());
  }

  private static JournalEntry entry(AccountId accountId, Direction direction, long amount) {
    return new JournalEntry(
        new JournalEntryId(UUID.randomUUID()), accountId, direction, new Money(amount, USD));
  }
}
