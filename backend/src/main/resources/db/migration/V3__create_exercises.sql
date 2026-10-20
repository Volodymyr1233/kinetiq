create table exercises (
    id             uuid         not null,
    owner_id       uuid,
    name           varchar(100) not null,
    name_pl        varchar(100),
    category       varchar(16)  not null,
    equipment      varchar(50)  not null,
    equipment_pl   varchar(50),
    measure        varchar(16)  not null,
    default_sets   integer      not null,
    default_amount integer      not null,
    rest_seconds   integer      not null,
    cues           varchar(500) not null,
    cues_pl        varchar(500),
    created_at     timestamptz  not null,
    updated_at     timestamptz  not null,
    version        bigint       not null,
    constraint pk_exercises primary key (id),
    constraint fk_exercises_owner foreign key (owner_id) references accounts (id),
    constraint ck_exercises_name_not_blank check (trim(name) <> ''),
    constraint ck_exercises_category check (category in (
        'CHEST', 'BACK', 'LEGS', 'SHOULDERS', 'ARMS', 'CORE', 'FULL_BODY', 'CARDIO', 'MOBILITY')),
    constraint ck_exercises_measure check (measure in ('REPS', 'SECONDS')),
    constraint ck_exercises_default_sets check (default_sets >= 1),
    constraint ck_exercises_default_amount check (default_amount >= 1),
    constraint ck_exercises_rest_seconds check (rest_seconds >= 0)
);

create index ix_exercises_owner_id on exercises (owner_id);
