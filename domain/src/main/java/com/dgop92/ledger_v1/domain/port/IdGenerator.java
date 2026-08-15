package com.dgop92.ledger_v1.domain.port;

import com.dgop92.ledger_v1.domain.account.AccountId;

/** Port for generating stable, unique entity identifiers. */
public interface IdGenerator {

  AccountId newAccountId();
}
