alter table transactions
    add column original_transaction_id uuid references transactions (id);

alter table transactions
    add constraint uq_transactions_original_transaction_id unique (original_transaction_id);
