package com.dgop92.ledger_v1.domain.port;

import com.dgop92.ledger_v1.domain.account.AccountId;
import com.dgop92.ledger_v1.domain.transaction.JournalEntry;
import java.util.List;

/** Persistence port for reading raw {@link JournalEntry} rows. */
public interface JournalRepository {

  /** Returns the raw, unfiltered Journal Entries posted against {@code id}. */
  List<JournalEntry> findByAccountId(AccountId id);
}
