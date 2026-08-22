create table journal_entries (
    id uuid primary key,
    transaction_id uuid not null references transactions (id),
    account_id uuid not null references accounts (id),
    direction varchar(10) not null,
    amount_minor_units bigint not null,
    currency varchar(3) not null
);

create index idx_journal_entries_transaction_id on journal_entries (transaction_id);
