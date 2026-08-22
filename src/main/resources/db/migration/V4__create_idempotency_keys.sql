create table idempotency_keys (
    id bigserial primary key,
    key varchar(255) not null,
    operation varchar(20) not null,
    payload_hash varchar(64) not null,
    transaction_id uuid not null references transactions (id),
    constraint uq_idempotency_keys_key_operation unique (key, operation)
);
