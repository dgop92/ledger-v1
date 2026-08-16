package com.dgop92.ledger_v1.domain.port;

import com.dgop92.ledger_v1.domain.account.AccountId;
import com.dgop92.ledger_v1.domain.transaction.JournalEntryId;
import com.dgop92.ledger_v1.domain.transaction.TransactionId;

/** Port for generating stable, unique entity identifiers. */
public interface IdGenerator {

  AccountId newAccountId();

  TransactionId newTransactionId();

  JournalEntryId newJournalEntryId();
}
