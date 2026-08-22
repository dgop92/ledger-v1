package com.dgop92.ledger_v1.application.usecase;

import com.dgop92.ledger_v1.domain.account.Account;
import com.dgop92.ledger_v1.domain.account.AccountId;
import com.dgop92.ledger_v1.domain.port.AccountRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** In-memory {@link AccountRepository} test double, shared across use case unit tests. */
final class FakeAccountRepository implements AccountRepository {

  final List<Account> saved = new ArrayList<>();

  @Override
  public void save(Account account) {
    saved.add(account);
  }

  @Override
  public Optional<Account> findById(AccountId id) {
    return saved.stream().filter(account -> account.id().equals(id)).findFirst();
  }

  @Override
  public List<Account> findAll() {
    return List.copyOf(saved);
  }
}
