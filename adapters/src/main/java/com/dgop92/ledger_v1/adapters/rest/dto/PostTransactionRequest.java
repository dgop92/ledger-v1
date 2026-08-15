package com.dgop92.ledger_v1.adapters.rest.dto;

import java.util.List;

/** Request payload for {@code POST /transactions}. */
public record PostTransactionRequest(List<JournalEntryRequest> entries) {}
