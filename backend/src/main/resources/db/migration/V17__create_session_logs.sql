create table session_logs (
    id           uuid         not null,
    client_id    uuid         not null,
    log_date     date         not null,
    session_name varchar(60)  not null,
    started_at   timestamptz  not null,
    finished_at  timestamptz,
    created_at   timestamptz  not null,
    updated_at   timestamptz  not null,
    version      bigint       not null,
    constraint pk_session_logs primary key (id),
    constraint fk_session_logs_client foreign key (client_id) references accounts (id),
    constraint uq_session_logs_client_date unique (client_id, log_date),
    constraint ck_session_logs_name_not_blank check (trim(session_name) <> ''),
    constraint ck_session_logs_finished_after_started check (finished_at is null or finished_at >= started_at)
);

-- a copy of the planned entries, because a plan save replaces the plan's sessions and entries
create table session_log_entries (
    session_log_id uuid         not null,
    position       integer      not null,
    exercise_id    uuid         not null,
    sets           integer      not null,
    amount         integer      not null,
    load_kg        numeric(4,1),
    rpe            integer      not null,
    rest_seconds   integer      not null,
    constraint pk_session_log_entries primary key (session_log_id, position),
    constraint fk_session_log_entries_session_log foreign key (session_log_id) references session_logs (id),
    constraint fk_session_log_entries_exercise foreign key (exercise_id) references exercises (id),
    constraint ck_session_log_entries_position check (position >= 0),
    constraint ck_session_log_entries_sets check (sets between 1 and 20),
    constraint ck_session_log_entries_amount check (amount between 1 and 3600),
    constraint ck_session_log_entries_load_kg check (load_kg > 0),
    constraint ck_session_log_entries_rpe check (rpe between 5 and 10),
    constraint ck_session_log_entries_rest_seconds check (rest_seconds between 0 and 600)
);

create index ix_session_log_entries_exercise_id on session_log_entries (exercise_id);

-- the id is made by the client, so sending a set twice stores it once
create table set_logs (
    id               uuid         not null,
    session_log_id   uuid         not null,
    entry_position   integer      not null,
    set_number       integer      not null,
    load_kg          numeric(4,1),
    amount           integer      not null,
    rpe              integer      not null,
    target_load_kg   numeric(4,1),
    target_amount    integer      not null,
    target_rpe       integer      not null,
    logged_at        timestamptz  not null,
    created_at       timestamptz  not null,
    updated_at       timestamptz  not null,
    version          bigint       not null,
    constraint pk_set_logs primary key (id),
    constraint fk_set_logs_entry foreign key (session_log_id, entry_position)
        references session_log_entries (session_log_id, position),
    constraint uq_set_logs_place unique (session_log_id, entry_position, set_number),
    constraint ck_set_logs_set_number check (set_number >= 1),
    constraint ck_set_logs_load_kg check (load_kg > 0),
    constraint ck_set_logs_amount check (amount between 1 and 3600),
    constraint ck_set_logs_rpe check (rpe between 5 and 10),
    constraint ck_set_logs_target_load_kg check (target_load_kg > 0),
    constraint ck_set_logs_target_amount check (target_amount between 1 and 3600),
    constraint ck_set_logs_target_rpe check (target_rpe between 5 and 10)
);

create index ix_set_logs_session_log_id on set_logs (session_log_id);
