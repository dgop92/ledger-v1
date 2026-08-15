package com.dgop92.ledger_v1.adapters.rest.dto;

import com.dgop92.ledger_v1.domain.transaction.JournalEntry;
import java.util.UUID;

/** Response payload representing a persisted Journal Entry. currencyCode is omitted by design. */
public record JournalEntryResponse(
    UUID id, UUID accountId, String direction, long amountMinorUnits) {

  public static JournalEntryResponse fromDomain(JournalEntry entry) {
    return new JournalEntryResponse(
        entry.id().value(),
        entry.accountId().value(),
        entry.direction().name(),
        entry.amount().amountMinorUnits());
  }
}
