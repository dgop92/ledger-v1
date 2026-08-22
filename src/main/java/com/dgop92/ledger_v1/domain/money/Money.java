package com.dgop92.ledger_v1.domain.money;

import java.util.Currency;
import java.util.Objects;

/** An immutable monetary amount in integer minor units. Never floating point. */
public final class Money {

  private final long amountMinorUnits;
  private final Currency currency;

  public Money(long amountMinorUnits, Currency currency) {
    this.amountMinorUnits = amountMinorUnits;
    this.currency = Objects.requireNonNull(currency, "currency must not be null");
  }

  public long amountMinorUnits() {
    return amountMinorUnits;
  }

  public Currency currency() {
    return currency;
  }

  /** Adds {@code other} to this amount, requiring a matching currency and overflow-checked. */
  public Money add(Money other) {
    Objects.requireNonNull(other, "other must not be null");
    if (!currency.equals(other.currency)) {
      throw new IllegalArgumentException(
          "cannot add Money with different currencies: " + currency + " vs " + other.currency);
    }
    return new Money(Math.addExact(amountMinorUnits, other.amountMinorUnits), currency);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Money other)) {
      return false;
    }
    return amountMinorUnits == other.amountMinorUnits && currency.equals(other.currency);
  }

  @Override
  public int hashCode() {
    return Objects.hash(amountMinorUnits, currency);
  }

  @Override
  public String toString() {
    return amountMinorUnits + " " + currency.getCurrencyCode();
  }
}
