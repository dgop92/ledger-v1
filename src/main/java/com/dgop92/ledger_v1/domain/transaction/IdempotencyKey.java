package com.dgop92.ledger_v1.domain.transaction;

import java.util.Objects;

/**
 * Input to {@link com.dgop92.ledger_v1.domain.port.TransactionRepository#append}, carrying the
 * client-submitted idempotency key, the operation it applies to (this story always {@code "POST"}),
 * and the payload hash to persist/compare on conflict.
 */
public record IdempotencyKey(String key, String operation, String payloadHash) {

  public IdempotencyKey {
    Objects.requireNonNull(key, "key must not be null");
    Objects.requireNonNull(operation, "operation must not be null");
    Objects.requireNonNull(payloadHash, "payloadHash must not be null");
  }

  public static IdempotencyKey forPost(String key, String payloadHash) {
    return new IdempotencyKey(key, "POST", payloadHash);
  }

  public static IdempotencyKey forReverse(String key, String payloadHash) {
    return new IdempotencyKey(key, "REVERSE", payloadHash);
  }
}
