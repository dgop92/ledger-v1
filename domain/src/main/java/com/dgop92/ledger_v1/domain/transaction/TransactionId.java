package com.dgop92.ledger_v1.domain.transaction;

import java.util.Objects;
import java.util.UUID;

/** Typed identifier for a {@link Transaction}. */
public final class TransactionId {

  private final UUID value;

  public TransactionId(UUID value) {
    this.value = Objects.requireNonNull(value, "value must not be null");
  }

  public UUID value() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TransactionId other)) {
      return false;
    }
    return value.equals(other.value);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
