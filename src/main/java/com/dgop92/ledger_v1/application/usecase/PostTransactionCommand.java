package com.dgop92.ledger_v1.application.usecase;

import java.math.BigDecimal;
import java.util.List;

/** Input to {@link PostTransactionUseCase}. Entry fields are unparsed pending validation. */
public record PostTransactionCommand(List<EntryInput> entries, String idempotencyKey) {

  /** A single unparsed journal-entry line as submitted by the client. */
  public record EntryInput(String accountId, String direction, BigDecimal amountMinorUnits) {}
}
