package com.dgop92.ledger_v1.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dgop92.ledger_v1.domain.account.Account;
import com.dgop92.ledger_v1.domain.account.AccountId;
import com.dgop92.ledger_v1.domain.account.AccountType;
import com.dgop92.ledger_v1.domain.exception.InvalidAccountNameException;
import com.dgop92.ledger_v1.domain.exception.InvalidAccountTypeException;
import com.dgop92.ledger_v1.domain.port.AccountRepository;
import com.dgop92.ledger_v1.domain.port.IdGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreateAccountUseCaseTest {

  private final List<Account> saved = new ArrayList<>();
  private final AccountRepository accountRepository = saved::add;
  private final IdGenerator idGenerator = () -> new AccountId(UUID.randomUUID());
  private final CreateAccountUseCase useCase =
      new CreateAccountUseCase(accountRepository, idGenerator);

  @Test
  void createsAccountWithValidType() {
    Account account = useCase.execute(new CreateAccountCommand("Cash", "ASSET"));

    assertEquals("Cash", account.name());
    assertEquals(AccountType.ASSET, account.accountType());
    assertEquals(1, saved.size());
    assertEquals(account, saved.get(0));
  }

  @Test
  void rejectsInvalidAccountType() {
    assertThrows(
        InvalidAccountTypeException.class,
        () -> useCase.execute(new CreateAccountCommand("Cash", "BOGUS")));
    assertEquals(0, saved.size());
  }

  @Test
  void rejectsNullName() {
    assertThrows(
        InvalidAccountNameException.class,
        () -> useCase.execute(new CreateAccountCommand(null, "ASSET")));
    assertEquals(0, saved.size());
  }

  @Test
  void rejectsBlankName() {
    assertThrows(
        InvalidAccountNameException.class,
        () -> useCase.execute(new CreateAccountCommand("   ", "ASSET")));
    assertEquals(0, saved.size());
  }

  @Test
  void rejectsOverLengthName() {
    String tooLong = "a".repeat(256);
    assertThrows(
        InvalidAccountNameException.class,
        () -> useCase.execute(new CreateAccountCommand(tooLong, "ASSET")));
    assertEquals(0, saved.size());
  }

  @Test
  void generatesUniqueIdsForEachAccount() {
    Account first = useCase.execute(new CreateAccountCommand("Cash", "ASSET"));
    Account second = useCase.execute(new CreateAccountCommand("Cash", "ASSET"));

    assertNotEquals(first.id(), second.id());
  }
}
