package com.dgop92.ledger_v1.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dgop92.ledger_v1.domain.account.AccountId;
import com.dgop92.ledger_v1.domain.exception.TransactionNotFoundException;
import com.dgop92.ledger_v1.domain.money.Money;
import com.dgop92.ledger_v1.domain.transaction.Direction;
import com.dgop92.ledger_v1.domain.transaction.JournalEntryDraft;
import com.dgop92.ledger_v1.domain.transaction.JournalEntryId;
import com.dgop92.ledger_v1.domain.transaction.Transaction;
import com.dgop92.ledger_v1.domain.transaction.TransactionId;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetTransactionUseCaseTest {

  private static final Currency USD = Currency.getInstance("USD");

  private final FakeTransactionRepository transactionRepository = new FakeTransactionRepository();
  private final GetTransactionUseCase useCase = new GetTransactionUseCase(transactionRepository);

  private static Transaction buildTransaction() {
    TransactionId transactionId = new TransactionId(UUID.randomUUID());
    AccountId debitAccount = new AccountId(UUID.randomUUID());
    AccountId creditAccount = new AccountId(UUID.randomUUID());
    Money money = new Money(1000, USD);
    return Transaction.balanced(
        transactionId,
        Instant.now(),
        List.of(debitAccount, creditAccount),
        List.of(
            new JournalEntryDraft(
                new JournalEntryId(UUID.randomUUID()), debitAccount, Direction.DEBIT, money),
            new JournalEntryDraft(
                new JournalEntryId(UUID.randomUUID()), creditAccount, Direction.CREDIT, money)));
  }

  @Test
  void returnsTransactionWhenFound() {
    Transaction transaction = buildTransaction();
    transactionRepository.appended.add(transaction);

    Transaction found = useCase.execute(transaction.id());

    assertEquals(transaction, found);
  }

  @Test
  void throwsWhenTransactionNotFound() {
    TransactionId id = new TransactionId(UUID.randomUUID());

    assertThrows(TransactionNotFoundException.class, () -> useCase.execute(id));
  }
}
