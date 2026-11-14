package com.dgop92.ledger_v1.adapters.rest.dto;

import com.dgop92.ledger_v1.domain.transaction.Transaction;
import com.dgop92.ledger_v1.domain.transaction.TransactionId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The one canonical response payload representing a persisted Transaction, reused by any endpoint
 * returning a Transaction.
 */
public record TransactionResponse(
    UUID id,
    Instant postedAt,
    List<JournalEntryResponse> journalEntries,
    UUID originalTransactionId) {

  public static TransactionResponse fromDomain(Transaction transaction) {
    List<JournalEntryResponse> journalEntries =
        transaction.entries().stream().map(JournalEntryResponse::fromDomain).toList();
    UUID originalTransactionId =
        transaction.originalTransactionId().map(TransactionId::value).orElse(null);
    return new TransactionResponse(
        transaction.id().value(), transaction.postedAt(), journalEntries, originalTransactionId);
  }
}
