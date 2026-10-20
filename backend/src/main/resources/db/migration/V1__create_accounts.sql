create table accounts (
    id              uuid         not null,
    name            varchar(100) not null,
    email           varchar(254) not null,
    password_hash   varchar(255) not null,
    role            varchar(16)  not null,
    time_zone       varchar(64)  not null,
    email_confirmed boolean      not null,
    created_at      timestamptz  not null,
    updated_at      timestamptz  not null,
    version         bigint       not null,
    constraint pk_accounts primary key (id),
    constraint uq_accounts_email unique (email),
    constraint ck_accounts_email_lower_case check (email = lower(email)),
    constraint ck_accounts_role check (role in ('TRAINER', 'CLIENT'))
);
