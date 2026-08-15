package com.dgop92.ledger_v1.adapters.persistence;

import com.dgop92.ledger_v1.domain.account.Account;
import com.dgop92.ledger_v1.domain.port.AccountRepository;
import java.util.Objects;
import org.jdbi.v3.core.Jdbi;

/** JDBI3-backed implementation of {@link AccountRepository}. */
public final class JdbiAccountRepository implements AccountRepository {

  private static final String INSERT_SQL =
      "insert into accounts (id, name, account_type) values (:id, :name, :accountType)";

  private final Jdbi jdbi;

  public JdbiAccountRepository(Jdbi jdbi) {
    this.jdbi = Objects.requireNonNull(jdbi, "jdbi must not be null");
  }

  @Override
  public void save(Account account) {
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(INSERT_SQL)
                .bind("id", account.id().value())
                .bind("name", account.name())
                .bind("accountType", account.accountType().name())
                .execute());
  }
}
