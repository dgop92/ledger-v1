package com.dgop92.ledger_v1.adapters.rest.dto;

/** Request payload for {@code POST /accounts}. */
public record CreateAccountRequest(String name, String accountType) {}
