package com.dgop92.ledger_v1.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dgop92.ledger_v1.domain.account.AccountId;
import com.dgop92.ledger_v1.domain.port.IdGenerator;
import com.dgop92.ledger_v1.domain.transaction.Direction;
import com.dgop92.ledger_v1.domain.transaction.JournalEntryId;
import com.dgop92.ledger_v1.domain.transaction.Transaction;
import com.dgop92.ledger_v1.domain.transaction.TransactionId;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;

/**
 * Hand-rolled property-based test (no jqwik): repeatedly builds randomized-but-valid {@link
 * PostTransactionCommand}s and asserts every {@link Transaction} accepted by {@link
 * PostTransactionUseCase} has sum(debits) == sum(credits).
 */
class TransactionBalancePropertyTest {

  /** Fixed seed, logged below, so a failure is reproducible by re-running with this exact seed. */
  private static final long SEED = 20260815_125959L;

  private static Random random;

  private final FakeTransactionRepository transactionRepository = new FakeTransactionRepository();
  private final IdGenerator idGenerator =
      new IdGenerator() {
        @Override
        public AccountId newAccountId() {
          throw new UnsupportedOperationException("not used by PostTransactionUseCase");
        }

        @Override
        public TransactionId newTransactionId() {
          return new TransactionId(UUID.randomUUID());
        }

        @Override
        public JournalEntryId newJournalEntryId() {
          return new JournalEntryId(UUID.randomUUID());
        }
      };
  private final PostTransactionUseCase useCase =
      new PostTransactionUseCase(transactionRepository, idGenerator);

  @BeforeAll
  static void logSeed() {
    System.out.println("TransactionBalancePropertyTest seed: " + SEED);
    random = new Random(SEED);
  }

  @RepeatedTest(200)
  void sumOfDebitsEqualsSumOfCreditsForEveryAcceptedTransaction() {
    PostTransactionCommand command = randomBalancedCommand(random);

    Transaction transaction = useCase.execute(command);

    long debitTotal =
        transaction.entries().stream()
            .filter(entry -> entry.direction() == Direction.DEBIT)
            .mapToLong(entry -> entry.amount().amountMinorUnits())
            .sum();
    long creditTotal =
        transaction.entries().stream()
            .filter(entry -> entry.direction() == Direction.CREDIT)
            .mapToLong(entry -> entry.amount().amountMinorUnits())
            .sum();

    assertEquals(debitTotal, creditTotal);
  }

  private static PostTransactionCommand randomBalancedCommand(Random random) {
    long total = 1_000 + random.nextInt(1_000_000);
    int debitParts = 1 + random.nextInt(3);
    int creditParts = 1 + random.nextInt(3);

    List<Long> debitAmounts = splitIntoRandomParts(random, total, debitParts);
    List<Long> creditAmounts = splitIntoRandomParts(random, total, creditParts);

    List<PostTransactionCommand.EntryInput> entries = new ArrayList<>();
    for (long amount : debitAmounts) {
      entries.add(
          new PostTransactionCommand.EntryInput(
              UUID.randomUUID().toString(), "DEBIT", BigDecimal.valueOf(amount)));
    }
    for (long amount : creditAmounts) {
      entries.add(
          new PostTransactionCommand.EntryInput(
              UUID.randomUUID().toString(), "CREDIT", BigDecimal.valueOf(amount)));
    }

    return new PostTransactionCommand(entries, UUID.randomUUID().toString());
  }

  /** Splits {@code total} into {@code parts} positive longs that sum exactly to {@code total}. */
  private static List<Long> splitIntoRandomParts(Random random, long total, int parts) {
    List<Long> amounts = new ArrayList<>(parts);
    long remaining = total;
    for (int i = 0; i < parts - 1; i++) {
      long reserveForRest = parts - i - 1;
      long max = remaining - reserveForRest;
      long amount = max > 1 ? 1 + (long) (random.nextDouble() * (max - 1)) : 1;
      amounts.add(amount);
      remaining -= amount;
    }
    amounts.add(remaining);
    return amounts;
  }
}
