package com.dgop92.ledger_v1.adapters.rest.dto;

/** RFC 7807 Problem Details payload, extended with a project-specific {@code errorCode}. */
public record ProblemDetails(
    String type, String title, int status, String detail, String errorCode) {}
