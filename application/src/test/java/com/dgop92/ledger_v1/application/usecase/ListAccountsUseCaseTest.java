package com.dgop92.ledger_v1.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dgop92.ledger_v1.domain.account.Account;
import com.dgop92.ledger_v1.domain.account.AccountId;
import com.dgop92.ledger_v1.domain.account.AccountType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListAccountsUseCaseTest {

  private final FakeAccountRepository accountRepository = new FakeAccountRepository();
  private final ListAccountsUseCase useCase = new ListAccountsUseCase(accountRepository);

  @Test
  void returnsEmptyListWhenNoAccountsExist() {
    List<Account> accounts = useCase.execute();

    assertTrue(accounts.isEmpty());
  }

  @Test
  void returnsAllAccountsInRepositoryOrder() {
    Account first = new Account(new AccountId(UUID.randomUUID()), "Cash", AccountType.ASSET);
    Account second =
        new Account(new AccountId(UUID.randomUUID()), "Payables", AccountType.LIABILITY);
    accountRepository.save(first);
    accountRepository.save(second);

    List<Account> accounts = useCase.execute();

    assertEquals(List.of(first, second), accounts);
  }
}
