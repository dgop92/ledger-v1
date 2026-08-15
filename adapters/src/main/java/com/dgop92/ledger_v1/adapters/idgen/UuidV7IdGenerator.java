package com.dgop92.ledger_v1.adapters.idgen;

import com.dgop92.ledger_v1.domain.account.AccountId;
import com.dgop92.ledger_v1.domain.port.IdGenerator;
import com.github.f4b6a3.uuid.UuidCreator;

/** Generates UUIDv7 identifiers, never DB auto-increment. */
public final class UuidV7IdGenerator implements IdGenerator {

  @Override
  public AccountId newAccountId() {
    return new AccountId(UuidCreator.getTimeOrderedEpoch());
  }
}
