package com.dgop92.ledger_v1.domain.account;

import java.util.Objects;
import java.util.UUID;

/** Typed identifier for an {@link Account}. */
public final class AccountId {

  private final UUID value;

  public AccountId(UUID value) {
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
    if (!(o instanceof AccountId other)) {
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
