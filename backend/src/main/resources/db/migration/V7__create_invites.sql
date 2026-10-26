create table invites (
    id               uuid         not null,
    trainer_id       uuid         not null,
    email            varchar(254) not null,
    status           varchar(16)  not null,
    email_token_hash char(64)     not null,
    link_token_hash  char(64)     not null,
    sent_at          timestamptz  not null,
    expires_at       timestamptz  not null,
    created_at       timestamptz  not null,
    updated_at       timestamptz  not null,
    version          bigint       not null,
    constraint pk_invites primary key (id),
    constraint fk_invites_trainer foreign key (trainer_id) references accounts (id),
    constraint uq_invites_email_token_hash unique (email_token_hash),
    constraint uq_invites_link_token_hash unique (link_token_hash),
    constraint ck_invites_email_lower_case check (email = lower(email)),
    constraint ck_invites_status check (status in ('OPEN', 'USED', 'REVOKED')),
    constraint ck_invites_expiry check (expires_at > sent_at)
);

-- one open invite per trainer and email; also serves the trainer's list of open invites
create unique index uq_invites_open_trainer_email on invites (trainer_id, email) where status = 'OPEN';
