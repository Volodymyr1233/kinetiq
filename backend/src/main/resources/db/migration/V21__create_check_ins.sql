-- score and band are derived from the answers when read, so they have no column
create table check_ins (
    id            uuid         not null,
    client_id     uuid         not null,
    check_in_date date         not null,
    sleep         smallint     not null,
    soreness      smallint     not null,
    stress        smallint     not null,
    saved_at      timestamptz  not null,
    created_at    timestamptz  not null,
    updated_at    timestamptz  not null,
    version       bigint       not null,
    constraint pk_check_ins primary key (id),
    constraint fk_check_ins_client foreign key (client_id) references accounts (id),
    constraint uq_check_ins_client_date unique (client_id, check_in_date),
    constraint ck_check_ins_sleep check (sleep between 1 and 5),
    constraint ck_check_ins_soreness check (soreness between 1 and 5),
    constraint ck_check_ins_stress check (stress between 1 and 5)
);
