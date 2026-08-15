package com.dgop92.ledger_v1.app.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dgop92.ledger_v1.domain.account.Account;
import com.dgop92.ledger_v1.domain.account.AccountId;
import com.dgop92.ledger_v1.domain.account.AccountType;
import com.dgop92.ledger_v1.domain.port.AccountRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;
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

  @Test
  void findsAccountByIdWhenPresent() {
    AccountId id = new AccountId(UUID.randomUUID());
    Account account = new Account(id, "Find By Id Account", AccountType.LIABILITY);
    accountRepository.save(account);

    Optional<Account> found = accountRepository.findById(id);

    assertTrue(found.isPresent());
    assertEquals(id, found.get().id());
    assertEquals("Find By Id Account", found.get().name());
    assertEquals(AccountType.LIABILITY, found.get().accountType());
  }

  @Test
  void findByIdReturnsEmptyWhenAbsent() {
    AccountId id = new AccountId(UUID.randomUUID());

    Optional<Account> found = accountRepository.findById(id);

    assertTrue(found.isEmpty());
  }

  @Test
  void findAllIncludesSavedAccounts() {
    AccountId firstId = new AccountId(UUID.randomUUID());
    AccountId secondId = new AccountId(UUID.randomUUID());
    Account first = new Account(firstId, "Find All First", AccountType.ASSET);
    Account second = new Account(secondId, "Find All Second", AccountType.EQUITY);
    accountRepository.save(first);
    accountRepository.save(second);

    List<AccountId> ids = accountRepository.findAll().stream().map(Account::id).toList();

    assertTrue(ids.contains(firstId));
    assertTrue(ids.contains(secondId));
  }

  @Test
  void findAllOrdersById() {
    // Fixed, deterministically-ordered ids (unlike random UUIDv4 test ids elsewhere in this
    // class) so this test actually proves the repository's `ORDER BY id` clause, independent of
    // insertion order or any real UuidV7IdGenerator behavior.
    AccountId smallerId = new AccountId(UUID.fromString("00000000-0000-0000-0000-0000000000a1"));
    AccountId largerId = new AccountId(UUID.fromString("00000000-0000-0000-0000-0000000000a2"));
    accountRepository.save(new Account(largerId, "Order Test Larger", AccountType.ASSET));
    accountRepository.save(new Account(smallerId, "Order Test Smaller", AccountType.LIABILITY));

    List<AccountId> ids = accountRepository.findAll().stream().map(Account::id).toList();

    assertTrue(ids.indexOf(smallerId) < ids.indexOf(largerId));
  }
}
