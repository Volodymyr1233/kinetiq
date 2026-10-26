alter table accounts
    add column reset_token_hash char(64),
    add column reset_expires_at timestamptz,
    add constraint uq_accounts_reset_token_hash unique (reset_token_hash),
    add constraint ck_accounts_reset check ((reset_token_hash is null) = (reset_expires_at is null));
