alter table accounts
    add column confirmation_token_hash char(64),
    add column confirmation_expires_at timestamptz,
    add column mail_sent_at            timestamptz,
    add constraint uq_accounts_confirmation_token_hash unique (confirmation_token_hash),
    add constraint ck_accounts_confirmation check (
        (confirmation_token_hash is null) = (confirmation_expires_at is null));
