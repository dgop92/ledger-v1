package com.dgop92.ledger_v1.application.usecase;

/** Input to {@link ReverseTransactionUseCase}. */
public record ReverseTransactionCommand(String originalTransactionId, String idempotencyKey) {}
