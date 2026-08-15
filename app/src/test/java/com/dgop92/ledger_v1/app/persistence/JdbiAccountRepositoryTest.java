package com.dgop92.ledger_v1.app.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dgop92.ledger_v1.domain.account.Account;
import com.dgop92.ledger_v1.domain.account.AccountId;
import com.dgop92.ledger_v1.domain.account.AccountType;
import com.dgop92.ledger_v1.domain.port.AccountRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;

/**
 * Exercises {@code JdbiAccountRepository} against a real Postgres instance provisioned by Quarkus
 * Dev Services (Testcontainers) -- the repository is never mocked here.
 */
@QuarkusTest
class JdbiAccountRepositoryTest {

  @Inject AccountRepository accountRepository;

  @Inject Jdbi jdbi;

  @Test
  void savesAccountToRealPostgres() {
    AccountId id = new AccountId(UUID.randomUUID());
    Account account = new Account(id, "Persistence Test Account", AccountType.EQUITY);

    accountRepository.save(account);

    String storedName =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery("select name from accounts where id = :id")
                    .bind("id", id.value())
                    .mapTo(String.class)
                    .one());

    assertEquals("Persistence Test Account", storedName);
  }
}
