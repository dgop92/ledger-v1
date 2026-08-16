package com.dgop92.ledger_v1.application.usecase;

import com.dgop92.ledger_v1.domain.account.AccountId;
import com.dgop92.ledger_v1.domain.port.JournalRepository;
import com.dgop92.ledger_v1.domain.transaction.JournalEntry;
import java.util.ArrayList;
import java.util.List;

/** In-memory {@link JournalRepository} test double, shared across use case unit tests. */
final class FakeJournalRepository implements JournalRepository {

  final List<JournalEntry> entries = new ArrayList<>();

  @Override
  public List<JournalEntry> findByAccountId(AccountId id) {
    return entries.stream().filter(entry -> entry.accountId().equals(id)).toList();
  }
}
