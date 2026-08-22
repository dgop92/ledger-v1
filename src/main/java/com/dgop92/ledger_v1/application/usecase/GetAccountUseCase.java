package com.dgop92.ledger_v1.application.usecase;

import com.dgop92.ledger_v1.domain.account.Account;
import com.dgop92.ledger_v1.domain.account.AccountId;
import com.dgop92.ledger_v1.domain.exception.AccountNotFoundException;
import com.dgop92.ledger_v1.domain.port.AccountRepository;
import java.util.Objects;

/** Looks up a single {@link Account} by ID, throwing {@link AccountNotFoundException} if absent. */
public final class GetAccountUseCase {

  private final AccountRepository accountRepository;

  public GetAccountUseCase(AccountRepository accountRepository) {
    this.accountRepository =
        Objects.requireNonNull(accountRepository, "accountRepository must not be null");
  }

  public Account execute(AccountId id) {
    Objects.requireNonNull(id, "id must not be null");
    return accountRepository
        .findById(id)
        .orElseThrow(() -> new AccountNotFoundException(id.toString()));
  }
}
