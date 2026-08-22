package com.dgop92.ledger_v1.adapters.persistence;

import com.dgop92.ledger_v1.domain.account.AccountId;
import com.dgop92.ledger_v1.domain.money.Money;
import com.dgop92.ledger_v1.domain.port.JournalRepository;
import com.dgop92.ledger_v1.domain.transaction.Direction;
import com.dgop92.ledger_v1.domain.transaction.JournalEntry;
import com.dgop92.ledger_v1.domain.transaction.JournalEntryId;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;

/** JDBI3-backed implementation of {@link JournalRepository}. */
public final class JdbiJournalRepository implements JournalRepository {

  private static final String SELECT_BY_ACCOUNT_ID_SQL =
      "select id, account_id, direction, amount_minor_units, currency from journal_entries "
          + "where account_id = :accountId";

  private static final RowMapper<JournalEntry> JOURNAL_ENTRY_ROW_MAPPER =
      (rs, ctx) ->
          new JournalEntry(
              new JournalEntryId(rs.getObject("id", UUID.class)),
              new AccountId(rs.getObject("account_id", UUID.class)),
              Direction.valueOf(rs.getString("direction")),
              new Money(
                  rs.getLong("amount_minor_units"),
                  Currency.getInstance(rs.getString("currency"))));

  private final Jdbi jdbi;

  public JdbiJournalRepository(Jdbi jdbi) {
    this.jdbi = Objects.requireNonNull(jdbi, "jdbi must not be null");
  }

  @Override
  public List<JournalEntry> findByAccountId(AccountId id) {
    Objects.requireNonNull(id, "id must not be null");
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(SELECT_BY_ACCOUNT_ID_SQL)
                .bind("accountId", id.value())
                .map(JOURNAL_ENTRY_ROW_MAPPER)
                .list());
  }
}
