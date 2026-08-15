package com.dgop92.ledger_v1.domain.port;

import com.dgop92.ledger_v1.domain.account.Account;

/** Persistence port for {@link Account}. */
public interface AccountRepository {

  void save(Account account);
}
