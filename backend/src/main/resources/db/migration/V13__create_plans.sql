create table plans (
    id         uuid        not null,
    client_id  uuid        not null,
    goal       varchar(32) not null,
    weeks      integer     not null,
    first_week date,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    version    bigint      not null,
    constraint pk_plans primary key (id),
    constraint fk_plans_client foreign key (client_id) references accounts (id),
    constraint uq_plans_client unique (client_id),
    constraint ck_plans_goal check (goal in (
        'BUILD_MUSCLE', 'GET_STRONGER', 'LOSE_FAT', 'MOVE_BETTER', 'GENERAL_FITNESS')),
    constraint ck_plans_weeks check (weeks between 4 and 16),
    -- isodow is 1 for Monday, so week numbers count Monday-to-Sunday weeks
    constraint ck_plans_first_week_monday check (first_week is null or extract(isodow from first_week) = 1)
);

create table plan_sessions (
    id          uuid        not null,
    plan_id     uuid        not null,
    name        varchar(60) not null,
    day_of_week varchar(9)  not null,
    constraint pk_plan_sessions primary key (id),
    constraint fk_plan_sessions_plan foreign key (plan_id) references plans (id),
    constraint uq_plan_sessions_plan_day unique (plan_id, day_of_week),
    constraint ck_plan_sessions_name_not_blank check (trim(name) <> ''),
    constraint ck_plan_sessions_day_of_week check (day_of_week in (
        'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'))
);

create table plan_session_entries (
    session_id   uuid         not null,
    position     integer      not null,
    exercise_id  uuid         not null,
    sets         integer      not null,
    amount       integer      not null,
    load_kg      numeric(4,1),
    rpe          integer      not null,
    rest_seconds integer      not null,
    constraint pk_plan_session_entries primary key (session_id, position),
    constraint fk_plan_session_entries_session foreign key (session_id) references plan_sessions (id),
    constraint fk_plan_session_entries_exercise foreign key (exercise_id) references exercises (id),
    constraint ck_plan_session_entries_position check (position >= 0),
    constraint ck_plan_session_entries_sets check (sets between 1 and 20),
    constraint ck_plan_session_entries_amount check (amount between 1 and 3600),
    constraint ck_plan_session_entries_load_kg check (load_kg > 0),
    constraint ck_plan_session_entries_rpe check (rpe between 5 and 10),
    constraint ck_plan_session_entries_rest_seconds check (rest_seconds between 0 and 600)
);

create index ix_plan_session_entries_exercise_id on plan_session_entries (exercise_id);
