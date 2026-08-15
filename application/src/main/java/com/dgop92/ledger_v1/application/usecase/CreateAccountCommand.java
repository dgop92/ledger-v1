package com.dgop92.ledger_v1.application.usecase;

/** Input to {@link CreateAccountUseCase}. The account type is unparsed pending validation. */
public record CreateAccountCommand(String name, String accountType) {}
