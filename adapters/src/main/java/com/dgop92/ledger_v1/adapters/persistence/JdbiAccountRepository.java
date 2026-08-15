package com.dgop92.ledger_v1.adapters.persistence;

import com.dgop92.ledger_v1.domain.account.Account;
import com.dgop92.ledger_v1.domain.account.AccountId;
import com.dgop92.ledger_v1.domain.account.AccountType;
import com.dgop92.ledger_v1.domain.port.AccountRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;

/** JDBI3-backed implementation of {@link AccountRepository}. */
public final class JdbiAccountRepository implements AccountRepository {

  private static final String INSERT_SQL =
      "insert into accounts (id, name, account_type) values (:id, :name, :accountType)";

  private static final String SELECT_BY_ID_SQL =
      "select id, name, account_type from accounts where id = :id";

  private static final String SELECT_ALL_SQL =
      "select id, name, account_type from accounts order by id";

  private static final RowMapper<Account> ACCOUNT_ROW_MAPPER =
      (rs, ctx) ->
          new Account(
              new AccountId(rs.getObject("id", UUID.class)),
              rs.getString("name"),
              AccountType.valueOf(rs.getString("account_type")));

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

  @Override
  public Optional<Account> findById(AccountId id) {
    Objects.requireNonNull(id, "id must not be null");
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(SELECT_BY_ID_SQL)
                .bind("id", id.value())
                .map(ACCOUNT_ROW_MAPPER)
                .findOne());
  }

  @Override
  public List<Account> findAll() {
    return jdbi.withHandle(
        handle -> handle.createQuery(SELECT_ALL_SQL).map(ACCOUNT_ROW_MAPPER).list());
  }
}
