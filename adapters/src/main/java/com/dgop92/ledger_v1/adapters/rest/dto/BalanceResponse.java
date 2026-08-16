package com.dgop92.ledger_v1.adapters.rest.dto;

import com.dgop92.ledger_v1.domain.balance.Balance;
import java.util.UUID;

/** Response payload representing an Account's Balance, computed at query time. */
public record BalanceResponse(UUID accountId, long amountMinorUnits, String currencyCode) {

  public static BalanceResponse fromDomain(Balance balance) {
    return new BalanceResponse(
        balance.accountId().value(),
        balance.amount().amountMinorUnits(),
        balance.amount().currency().getCurrencyCode());
  }
}
