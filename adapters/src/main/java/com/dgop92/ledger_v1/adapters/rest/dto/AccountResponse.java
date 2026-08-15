package com.dgop92.ledger_v1.adapters.rest.dto;

import com.dgop92.ledger_v1.domain.account.Account;
import java.util.UUID;

/** Response payload representing a persisted Account. */
public record AccountResponse(UUID id, String name, String accountType) {

  public static AccountResponse fromDomain(Account account) {
    return new AccountResponse(account.id().value(), account.name(), account.accountType().name());
  }
}
