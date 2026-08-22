package com.dgop92.ledger_v1.domain.balance;

import com.dgop92.ledger_v1.domain.account.AccountId;
import com.dgop92.ledger_v1.domain.account.AccountType;
import com.dgop92.ledger_v1.domain.money.Money;
import com.dgop92.ledger_v1.domain.transaction.JournalEntry;
import java.util.Currency;
import java.util.List;
import java.util.Objects;

/** An Account's financial position, computed at query time from its raw Journal Entries. */
public record Balance(AccountId accountId, Money amount) {

  public Balance {
    Objects.requireNonNull(accountId, "accountId must not be null");
    Objects.requireNonNull(amount, "amount must not be null");
  }

  /**
   * Sums {@code entries} for {@code accountId}, applying {@code accountType}'s normal-balance
   * direction: an entry increases the balance when its {@link
   * com.dgop92.ledger_v1.domain.transaction.Direction} matches the account type's {@link
   * AccountType.NormalBalance} (compared by name), otherwise it decreases the balance.
   * Overflow-checked long arithmetic throughout ({@code Math.addExact}/{@code Math.subtractExact});
   * never floating point.
   *
   * <p>When {@code entries} is empty, returns a zero Balance in {@code zeroCurrency}. Otherwise the
   * resulting Money is denominated in the entries' (single, shared) currency.
   */
  public static Balance computeFrom(
      AccountId accountId,
      List<JournalEntry> entries,
      AccountType accountType,
      Currency zeroCurrency) {
    Objects.requireNonNull(accountId, "accountId must not be null");
    Objects.requireNonNull(entries, "entries must not be null");
    Objects.requireNonNull(accountType, "accountType must not be null");
    Objects.requireNonNull(zeroCurrency, "zeroCurrency must not be null");

    if (entries.isEmpty()) {
      return new Balance(accountId, new Money(0L, zeroCurrency));
    }

    String normalBalanceName = accountType.normalBalance().name();
    Currency currency = entries.get(0).amount().currency();
    long total = 0L;
    for (JournalEntry entry : entries) {
      long entryAmountMinorUnits = entry.amount().amountMinorUnits();
      if (entry.direction().name().equals(normalBalanceName)) {
        total = Math.addExact(total, entryAmountMinorUnits);
      } else {
        total = Math.subtractExact(total, entryAmountMinorUnits);
      }
    }
    return new Balance(accountId, new Money(total, currency));
  }
}
