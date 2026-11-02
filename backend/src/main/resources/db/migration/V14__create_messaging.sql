create table conversations (
    id         uuid        not null,
    client_id  uuid        not null,
    trainer_id uuid        not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    version    bigint      not null,
    constraint pk_conversations primary key (id),
    constraint fk_conversations_client foreign key (client_id) references accounts (id),
    constraint fk_conversations_trainer foreign key (trainer_id) references accounts (id),
    constraint uq_conversations_client_trainer unique (client_id, trainer_id)
);

create table messages (
    id              uuid          not null,
    conversation_id uuid          not null,
    sender_id       uuid          not null,
    text            varchar(2000) not null,
    received_at     timestamptz   not null,
    created_at      timestamptz   not null,
    updated_at      timestamptz   not null,
    version         bigint        not null,
    constraint pk_messages primary key (id),
    constraint fk_messages_conversation foreign key (conversation_id) references conversations (id),
    constraint fk_messages_sender foreign key (sender_id) references accounts (id),
    constraint ck_messages_text_not_blank check (char_length(btrim(text)) > 0)
);

create index ix_messages_conversation_received on messages (conversation_id, received_at desc, id desc);

create table conversation_reads (
    conversation_id uuid        not null,
    account_id      uuid        not null,
    last_read_at    timestamptz not null,
    constraint pk_conversation_reads primary key (conversation_id, account_id),
    constraint fk_conversation_reads_conversation foreign key (conversation_id) references conversations (id),
    constraint fk_conversation_reads_account foreign key (account_id) references accounts (id)
);
