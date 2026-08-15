package com.dgop92.ledger_v1.application.usecase;

import com.dgop92.ledger_v1.domain.account.Account;
import com.dgop92.ledger_v1.domain.port.AccountRepository;
import java.util.List;
import java.util.Objects;

/** Returns every persisted {@link Account}, in creation order, unbounded. */
public final class ListAccountsUseCase {

  private final AccountRepository accountRepository;

  public ListAccountsUseCase(AccountRepository accountRepository) {
    this.accountRepository =
        Objects.requireNonNull(accountRepository, "accountRepository must not be null");
  }

  public List<Account> execute() {
    return accountRepository.findAll();
  }
}
