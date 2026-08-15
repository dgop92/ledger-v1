package com.dgop92.ledger_v1.domain.transaction;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The canonical, hashable shape of a Transaction's raw posting fields, computed from raw request
 * fields before any domain object is constructed. Reused unchanged by Epic 3's reversal flow.
 */
public record PostingPayload(List<Entry> entries) {

  public PostingPayload {
    Objects.requireNonNull(entries, "entries must not be null");
    entries = List.copyOf(entries);
  }

  /** SHA-256 over {@code accountId|direction|amountMinorUnits} per entry, hex-lowercase. */
  public String canonicalHash() {
    String canonical =
        entries.stream()
            .map(
                entry ->
                    entry.accountId() + "|" + entry.direction() + "|" + entry.amountMinorUnits())
            .collect(Collectors.joining("\n"));
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder(hashBytes.length * 2);
      for (byte b : hashBytes) {
        hex.append(String.format("%02x", b));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
  }

  /** A single raw entry line, in client-submission order. */
  public record Entry(String accountId, String direction, long amountMinorUnits) {

    public Entry {
      Objects.requireNonNull(accountId, "accountId must not be null");
      Objects.requireNonNull(direction, "direction must not be null");
    }
  }
}
