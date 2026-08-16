package com.dgop92.ledger_v1.adapters.persistence;

import com.dgop92.ledger_v1.domain.account.AccountId;
import com.dgop92.ledger_v1.domain.exception.IdempotencyConflictException;
import com.dgop92.ledger_v1.domain.exception.UnknownAccountException;
import com.dgop92.ledger_v1.domain.money.Money;
import com.dgop92.ledger_v1.domain.port.TransactionRepository;
import com.dgop92.ledger_v1.domain.transaction.Direction;
import com.dgop92.ledger_v1.domain.transaction.IdempotencyKey;
import com.dgop92.ledger_v1.domain.transaction.JournalEntry;
import com.dgop92.ledger_v1.domain.transaction.JournalEntryDraft;
import com.dgop92.ledger_v1.domain.transaction.JournalEntryId;
import com.dgop92.ledger_v1.domain.transaction.Transaction;
import com.dgop92.ledger_v1.domain.transaction.TransactionId;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.PreparedBatch;

/** JDBI3-backed implementation of {@link TransactionRepository}. */
public final class JdbiTransactionRepository implements TransactionRepository {

  private static final String UNIQUE_VIOLATION_SQL_STATE = "23505";
  private static final String FOREIGN_KEY_VIOLATION_SQL_STATE = "23503";
  private static final String IDEMPOTENCY_KEY_UNIQUE_CONSTRAINT =
      "uq_idempotency_keys_key_operation";

  private static final String INSERT_TRANSACTION_SQL =
      "insert into transactions (id, posted_at) values (:id, :postedAt)";

  private static final String INSERT_JOURNAL_ENTRY_SQL =
      "insert into journal_entries (id, transaction_id, account_id, direction, amount_minor_units, currency) "
          + "values (:id, :transactionId, :accountId, :direction, :amountMinorUnits, :currency)";

  private static final String INSERT_IDEMPOTENCY_KEY_SQL =
      "insert into idempotency_keys (key, operation, payload_hash, transaction_id) "
          + "values (:key, :operation, :payloadHash, :transactionId)";

  private static final String SELECT_TRANSACTION_SQL =
      "select posted_at from transactions where id = :id";

  private static final String SELECT_JOURNAL_ENTRIES_SQL =
      "select id, account_id, direction, amount_minor_units, currency from journal_entries "
          + "where transaction_id = :transactionId order by id";

  private static final String SELECT_IDEMPOTENCY_KEY_SQL =
      "select payload_hash, transaction_id from idempotency_keys where key = :key and operation = :operation";

  // DISTINCT is required: a Transaction may have multiple Journal Entry legs against the same
  // Account, which would otherwise duplicate that transaction's id once per matching leg.
  private static final String SELECT_TRANSACTION_IDS_BY_ACCOUNT_ID_SQL =
      "select distinct t.id from transactions t "
          + "join journal_entries je on je.transaction_id = t.id "
          + "where je.account_id = :accountId order by t.id";

  private static final RowMapper<JournalEntryDraft> JOURNAL_ENTRY_DRAFT_ROW_MAPPER =
      (rs, ctx) ->
          new JournalEntryDraft(
              new JournalEntryId(rs.getObject("id", UUID.class)),
              new AccountId(rs.getObject("account_id", UUID.class)),
              Direction.valueOf(rs.getString("direction")),
              new Money(
                  rs.getLong("amount_minor_units"),
                  Currency.getInstance(rs.getString("currency"))));

  private final Jdbi jdbi;

  public JdbiTransactionRepository(Jdbi jdbi) {
    this.jdbi = Objects.requireNonNull(jdbi, "jdbi must not be null");
  }

  @Override
  public Transaction append(Transaction transaction, IdempotencyKey idempotencyKey) {
    Objects.requireNonNull(transaction, "transaction must not be null");
    Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");

    try {
      return jdbi.inTransaction(
          handle -> {
            insertTransaction(handle, transaction);
            insertJournalEntries(handle, transaction);
            insertIdempotencyKey(handle, transaction, idempotencyKey);
            return transaction;
          });
    } catch (RuntimeException e) {
      Optional<SQLException> sqlException = findSqlException(e);
      if (sqlException.isPresent()
          && UNIQUE_VIOLATION_SQL_STATE.equals(sqlException.get().getSQLState())
          && isIdempotencyKeyConstraintViolation(sqlException.get())) {
        return resolveIdempotencyConflict(idempotencyKey);
      }
      if (sqlException.isPresent()
          && FOREIGN_KEY_VIOLATION_SQL_STATE.equals(sqlException.get().getSQLState())) {
        throw new UnknownAccountException(sqlException.get().getMessage());
      }
      throw e;
    }
  }

  @Override
  public Optional<Transaction> findById(TransactionId id) {
    Objects.requireNonNull(id, "id must not be null");
    return jdbi.withHandle(
        handle ->
            findPostedAt(handle, id.value())
                .map(postedAt -> fetchTransaction(handle, id.value(), postedAt)));
  }

  @Override
  public List<Transaction> findByAccountId(AccountId id) {
    Objects.requireNonNull(id, "id must not be null");
    return jdbi.withHandle(
        handle -> {
          List<UUID> transactionIds =
              handle
                  .createQuery(SELECT_TRANSACTION_IDS_BY_ACCOUNT_ID_SQL)
                  .bind("accountId", id.value())
                  .mapTo(UUID.class)
                  .list();
          return transactionIds.stream().map(txId -> fetchTransaction(handle, txId)).toList();
        });
  }

  private void insertTransaction(Handle handle, Transaction transaction) {
    handle
        .createUpdate(INSERT_TRANSACTION_SQL)
        .bind("id", transaction.id().value())
        .bind("postedAt", transaction.postedAt())
        .execute();
  }

  private void insertJournalEntries(Handle handle, Transaction transaction) {
    PreparedBatch batch = handle.prepareBatch(INSERT_JOURNAL_ENTRY_SQL);
    for (JournalEntry entry : transaction.entries()) {
      batch
          .bind("id", entry.id().value())
          .bind("transactionId", transaction.id().value())
          .bind("accountId", entry.accountId().value())
          .bind("direction", entry.direction().name())
          .bind("amountMinorUnits", entry.amount().amountMinorUnits())
          .bind("currency", entry.amount().currency().getCurrencyCode())
          .add();
    }
    batch.execute();
  }

  private void insertIdempotencyKey(
      Handle handle, Transaction transaction, IdempotencyKey idempotencyKey) {
    handle
        .createUpdate(INSERT_IDEMPOTENCY_KEY_SQL)
        .bind("key", idempotencyKey.key())
        .bind("operation", idempotencyKey.operation())
        .bind("payloadHash", idempotencyKey.payloadHash())
        .bind("transactionId", transaction.id().value())
        .execute();
  }

  /**
   * Resolves a unique-violation on (key, operation): re-fetches the existing row (including the
   * persisted transaction ID) and compares payload hashes. Matching hash -> rehydrates and returns
   * the original persisted Transaction with its true persisted IDs. Mismatched hash -> throws
   * {@link IdempotencyConflictException}.
   */
  private Transaction resolveIdempotencyConflict(IdempotencyKey idempotencyKey) {
    return jdbi.withHandle(
        handle -> {
          Optional<ExistingIdempotencyRow> existing =
              handle
                  .createQuery(SELECT_IDEMPOTENCY_KEY_SQL)
                  .bind("key", idempotencyKey.key())
                  .bind("operation", idempotencyKey.operation())
                  .map(
                      (rs, ctx) ->
                          new ExistingIdempotencyRow(
                              rs.getString("payload_hash"),
                              rs.getObject("transaction_id", UUID.class)))
                  .findOne();

          if (existing.isEmpty()) {
            throw new IdempotencyConflictException(idempotencyKey.key());
          }
          ExistingIdempotencyRow row = existing.get();
          if (!row.payloadHash().equals(idempotencyKey.payloadHash())) {
            throw new IdempotencyConflictException(idempotencyKey.key());
          }
          return fetchTransaction(handle, row.transactionId());
        });
  }

  /**
   * Rehydrates a {@link Transaction} from its persisted rows. The journal-entries SELECT includes
   * the {@code id} column, so replay/reads reconstruct the exact persisted {@link JournalEntryId}s
   * rather than minting new ones.
   */
  private Transaction fetchTransaction(Handle handle, UUID transactionId) {
    Instant postedAt =
        findPostedAt(handle, transactionId)
            .orElseThrow(
                () -> new IllegalStateException("transaction not found: " + transactionId));
    return fetchTransaction(handle, transactionId, postedAt);
  }

  private Transaction fetchTransaction(Handle handle, UUID transactionId, Instant postedAt) {
    List<JournalEntryDraft> drafts =
        handle
            .createQuery(SELECT_JOURNAL_ENTRIES_SQL)
            .bind("transactionId", transactionId)
            .map(JOURNAL_ENTRY_DRAFT_ROW_MAPPER)
            .list();

    List<AccountId> accountIds = drafts.stream().map(JournalEntryDraft::accountId).toList();

    return Transaction.balanced(new TransactionId(transactionId), postedAt, accountIds, drafts);
  }

  private Optional<Instant> findPostedAt(Handle handle, UUID transactionId) {
    return handle
        .createQuery(SELECT_TRANSACTION_SQL)
        .bind("id", transactionId)
        .map((rs, ctx) -> rs.getTimestamp("posted_at").toInstant())
        .findOne();
  }

  /**
   * A 23505 unique-violation can come from any unique constraint (including a near-impossible
   * transactions/journal_entries primary-key collision). Only treat it as an idempotency conflict
   * when it actually names the idempotency-key uniqueness constraint.
   */
  private static boolean isIdempotencyKeyConstraintViolation(SQLException sqlException) {
    String message = sqlException.getMessage();
    return message != null && message.contains(IDEMPOTENCY_KEY_UNIQUE_CONSTRAINT);
  }

  private static Optional<SQLException> findSqlException(Throwable throwable) {
    Throwable current = throwable;
    while (current != null) {
      if (current instanceof SQLException sqlException) {
        return Optional.of(sqlException);
      }
      current = current.getCause();
    }
    return Optional.empty();
  }

  private record ExistingIdempotencyRow(String payloadHash, UUID transactionId) {}
}
