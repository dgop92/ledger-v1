package com.dgop92.ledger_v1.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dgop92.ledger_v1.domain.account.Account;
import com.dgop92.ledger_v1.domain.account.AccountId;
import com.dgop92.ledger_v1.domain.account.AccountType;
import com.dgop92.ledger_v1.domain.exception.AccountNotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetAccountUseCaseTest {

  private final FakeAccountRepository accountRepository = new FakeAccountRepository();
  private final GetAccountUseCase useCase = new GetAccountUseCase(accountRepository);

  @Test
  void returnsAccountWhenFound() {
    AccountId id = new AccountId(UUID.randomUUID());
    Account account = new Account(id, "Cash", AccountType.ASSET);
    accountRepository.save(account);

    Account found = useCase.execute(id);

    assertEquals(account, found);
  }

  @Test
  void throwsWhenAccountNotFound() {
    AccountId id = new AccountId(UUID.randomUUID());

    assertThrows(AccountNotFoundException.class, () -> useCase.execute(id));
  }
}
