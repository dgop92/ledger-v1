package com.dgop92.ledger_v1.adapters.rest.dto;

import java.math.BigDecimal;

/** Request payload for a single Journal Entry line within {@link PostTransactionRequest}. */
public record JournalEntryRequest(
    String accountId, String direction, BigDecimal amountMinorUnits) {}
