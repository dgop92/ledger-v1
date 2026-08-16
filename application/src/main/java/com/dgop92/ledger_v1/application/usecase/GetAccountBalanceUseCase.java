package com.dgop92.ledger_v1.application.usecase;

import com.dgop92.ledger_v1.domain.account.Account;
import com.dgop92.ledger_v1.domain.account.AccountId;
import com.dgop92.ledger_v1.domain.balance.Balance;
import com.dgop92.ledger_v1.domain.exception.AccountNotFoundException;
import com.dgop92.ledger_v1.domain.port.AccountRepository;
import com.dgop92.ledger_v1.domain.port.JournalRepository;
import com.dgop92.ledger_v1.domain.transaction.JournalEntry;
import java.util.Currency;
import java.util.List;
import java.util.Objects;

/**
 * Resolves an {@link Account} by ID (throwing {@link AccountNotFoundException} if absent), fetches
 * its raw {@link JournalEntry} list, and computes its current {@link Balance} at query time.
 */
public final class GetAccountBalanceUseCase {

  /**
   * This story does not yet model per-account currency; an Account with no Journal Entries returns
   * a zero Balance minted in this fixed default currency.
   */
  private static final Currency DEFAULT_CURRENCY = Currency.getInstance("USD");

  private final AccountRepository accountRepository;
  private final JournalRepository journalRepository;

  public GetAccountBalanceUseCase(
      AccountRepository accountRepository, JournalRepository journalRepository) {
    this.accountRepository =
        Objects.requireNonNull(accountRepository, "accountRepository must not be null");
    this.journalRepository =
        Objects.requireNonNull(journalRepository, "journalRepository must not be null");
  }

  public Balance execute(AccountId id) {
    Objects.requireNonNull(id, "id must not be null");
    Account account =
        accountRepository
            .findById(id)
            .orElseThrow(() -> new AccountNotFoundException(id.toString()));
    List<JournalEntry> entries = journalRepository.findByAccountId(id);
    return Balance.computeFrom(id, entries, account.accountType(), DEFAULT_CURRENCY);
  }
}
