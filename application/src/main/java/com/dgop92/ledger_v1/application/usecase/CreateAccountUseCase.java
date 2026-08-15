package com.dgop92.ledger_v1.application.usecase;

import com.dgop92.ledger_v1.domain.account.Account;
import com.dgop92.ledger_v1.domain.account.AccountId;
import com.dgop92.ledger_v1.domain.account.AccountType;
import com.dgop92.ledger_v1.domain.exception.InvalidAccountNameException;
import com.dgop92.ledger_v1.domain.exception.InvalidAccountTypeException;
import com.dgop92.ledger_v1.domain.port.AccountRepository;
import com.dgop92.ledger_v1.domain.port.IdGenerator;
import java.util.Objects;

/**
 * Validates the requested name and account type, assigns an ID, and persists a new {@link Account}.
 */
public final class CreateAccountUseCase {

  private static final int MAX_NAME_LENGTH = 255;

  private final AccountRepository accountRepository;
  private final IdGenerator idGenerator;

  public CreateAccountUseCase(AccountRepository accountRepository, IdGenerator idGenerator) {
    this.accountRepository =
        Objects.requireNonNull(accountRepository, "accountRepository must not be null");
    this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
  }

  public Account execute(CreateAccountCommand command) {
    Objects.requireNonNull(command, "command must not be null");
    String name = validateName(command.name());
    AccountType accountType = parseAccountType(command.accountType());
    AccountId id = idGenerator.newAccountId();
    Account account = new Account(id, name, accountType);
    accountRepository.save(account);
    return account;
  }

  private String validateName(String rawName) {
    if (rawName == null || rawName.isBlank()) {
      throw new InvalidAccountNameException("must not be null or blank");
    }
    if (rawName.length() > MAX_NAME_LENGTH) {
      throw new InvalidAccountNameException("must not exceed " + MAX_NAME_LENGTH + " characters");
    }
    return rawName;
  }

  private AccountType parseAccountType(String rawAccountType) {
    if (rawAccountType == null) {
      throw new InvalidAccountTypeException("null");
    }
    try {
      return AccountType.valueOf(rawAccountType);
    } catch (IllegalArgumentException e) {
      throw new InvalidAccountTypeException(rawAccountType);
    }
  }
}
