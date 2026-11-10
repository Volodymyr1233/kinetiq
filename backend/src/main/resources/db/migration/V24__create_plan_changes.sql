-- a plan change suggested for one client's plan by a rule, with content checked against the rule's limits
create table plan_changes (
    id             uuid        not null,
    client_id      uuid        not null,
    rule           varchar(16) not null,
    state          varchar(16) not null,
    -- the version of the plan the content was made from, so a later story can tell whether the plan changed since
    plan_version   bigint      not null,
    window_start   date        not null,
    window_end     date        not null,
    effective_week date        not null,
    removed_day    varchar(9),
    removed_name   varchar(60),
    created_at     timestamptz not null,
    ended_at       timestamptz,
    constraint pk_plan_changes primary key (id),
    constraint fk_plan_changes_client foreign key (client_id) references accounts (id),
    constraint ck_plan_changes_rule check (rule in ('LOW_COMPLETION', 'HIGH_RPE', 'STALLED_WEIGHT')),
    constraint ck_plan_changes_state check (state in ('PENDING', 'ACCEPTED_BY_ONE', 'APPLIED', 'DECLINED', 'EXPIRED')),
    constraint ck_plan_changes_window check (window_end > window_start),
    -- isodow is 1 for Monday, so the effective week is Monday to Sunday
    constraint ck_plan_changes_effective_week_monday check (extract(isodow from effective_week) = 1),
    constraint ck_plan_changes_removed_day check (removed_day in (
        'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
    constraint ck_plan_changes_removed_together check ((removed_day is null) = (removed_name is null))
);

-- at most one plan change per client that has not ended
create unique index uq_plan_changes_one_open_per_client on plan_changes (client_id)
    where state in ('PENDING', 'ACCEPTED_BY_ONE');

create index ix_plan_changes_client_id_ended_at on plan_changes (client_id, ended_at);

-- one row per changed value of one entry of the plan
create table plan_change_targets (
    plan_change_id uuid         not null,
    session_day    varchar(9)   not null,
    position       integer      not null,
    target         varchar(12)  not null,
    -- exercise_id and session_name depend on the entry, not on the target: copied per row, at most a few per entry,
    -- so the screen shows the plan as the content was made from it even after the trainer edits the plan
    exercise_id    uuid         not null,
    session_name   varchar(60)  not null,
    old_value      numeric(6,1) not null,
    new_value      numeric(6,1) not null,
    constraint pk_plan_change_targets primary key (plan_change_id, session_day, position, target),
    constraint fk_plan_change_targets_plan_change foreign key (plan_change_id) references plan_changes (id),
    constraint fk_plan_change_targets_exercise foreign key (exercise_id) references exercises (id),
    constraint ck_plan_change_targets_session_day check (session_day in (
        'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
    constraint ck_plan_change_targets_position check (position >= 0),
    constraint ck_plan_change_targets_target check (target in ('SETS', 'AMOUNT', 'LOAD_KG', 'RPE', 'REST_SECONDS')),
    constraint ck_plan_change_targets_changed check (new_value <> old_value)
);

create index ix_plan_change_targets_exercise_id on plan_change_targets (exercise_id);

-- an entry of the removed session that moves to the end of another session
create table plan_change_moves (
    plan_change_id uuid       not null,
    from_position  integer    not null,
    to_day         varchar(9) not null,
    constraint pk_plan_change_moves primary key (plan_change_id, from_position),
    constraint fk_plan_change_moves_plan_change foreign key (plan_change_id) references plan_changes (id),
    constraint ck_plan_change_moves_from_position check (from_position >= 0),
    constraint ck_plan_change_moves_to_day check (to_day in (
        'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'))
);

-- the numbers the rule compared, one value per row
create table plan_change_figures (
    id             uuid         not null,
    plan_change_id uuid         not null,
    kind           varchar(17)  not null,
    exercise_id    uuid,
    week_start     date         not null,
    value          numeric(7,3) not null,
    constraint pk_plan_change_figures primary key (id),
    constraint fk_plan_change_figures_plan_change foreign key (plan_change_id) references plan_changes (id),
    constraint fk_plan_change_figures_exercise foreign key (exercise_id) references exercises (id),
    constraint uq_plan_change_figures unique nulls not distinct (plan_change_id, kind, exercise_id, week_start),
    constraint ck_plan_change_figures_kind check (kind in (
        'MEAN_RPE', 'BEST_LOAD_KG', 'SESSIONS_PLANNED', 'SESSIONS_FINISHED')),
    constraint ck_plan_change_figures_exercise check ((exercise_id is not null) = (kind = 'BEST_LOAD_KG')),
    constraint ck_plan_change_figures_week_start_monday check (extract(isodow from week_start) = 1)
);

create index ix_plan_change_figures_exercise_id on plan_change_figures (exercise_id);

-- the exercises the rule concerns: above RPE 8, or stalled
create table plan_change_exercises (
    plan_change_id uuid not null,
    exercise_id    uuid not null,
    constraint pk_plan_change_exercises primary key (plan_change_id, exercise_id),
    constraint fk_plan_change_exercises_plan_change foreign key (plan_change_id) references plan_changes (id),
    constraint fk_plan_change_exercises_exercise foreign key (exercise_id) references exercises (id)
);

create index ix_plan_change_exercises_exercise_id on plan_change_exercises (exercise_id);
