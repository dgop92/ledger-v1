package com.dgop92.ledger_v1.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dgop92.ledger_v1.domain.account.AccountId;
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

class ListTransactionsUseCaseTest {

  private static final Currency USD = Currency.getInstance("USD");

  private final FakeTransactionRepository transactionRepository = new FakeTransactionRepository();
  private final ListTransactionsUseCase useCase =
      new ListTransactionsUseCase(transactionRepository);

  private static Transaction buildTransaction(AccountId debitAccount, AccountId creditAccount) {
    TransactionId transactionId = new TransactionId(UUID.randomUUID());
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
  void returnsEmptyListWhenAccountHasNoTransactions() {
    AccountId accountId = new AccountId(UUID.randomUUID());

    List<Transaction> transactions = useCase.execute(accountId);

    assertTrue(transactions.isEmpty());
  }

  @Test
  void returnsEmptyListForUnknownAccount() {
    AccountId debitAccount = new AccountId(UUID.randomUUID());
    AccountId creditAccount = new AccountId(UUID.randomUUID());
    transactionRepository.appended.add(buildTransaction(debitAccount, creditAccount));

    AccountId unknownAccount = new AccountId(UUID.randomUUID());
    List<Transaction> transactions = useCase.execute(unknownAccount);

    assertTrue(transactions.isEmpty());
  }

  @Test
  void returnsAllTransactionsReferencingAccount() {
    AccountId debitAccount = new AccountId(UUID.randomUUID());
    AccountId creditAccount = new AccountId(UUID.randomUUID());
    Transaction first = buildTransaction(debitAccount, creditAccount);
    Transaction second = buildTransaction(debitAccount, creditAccount);
    Transaction unrelated =
        buildTransaction(new AccountId(UUID.randomUUID()), new AccountId(UUID.randomUUID()));
    transactionRepository.appended.add(first);
    transactionRepository.appended.add(second);
    transactionRepository.appended.add(unrelated);

    List<Transaction> transactions = useCase.execute(debitAccount);

    assertEquals(List.of(first, second), transactions);
  }
}
