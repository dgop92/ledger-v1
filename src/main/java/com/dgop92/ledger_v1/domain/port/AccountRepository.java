package com.dgop92.ledger_v1.domain.port;

import com.dgop92.ledger_v1.domain.account.Account;
import com.dgop92.ledger_v1.domain.account.AccountId;
import java.util.List;
import java.util.Optional;

/** Persistence port for {@link Account}. */
public interface AccountRepository {

  void save(Account account);

  Optional<Account> findById(AccountId id);

  List<Account> findAll();
}
