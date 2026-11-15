-- when an applied plan change started to be used by the plan, and why an expired one expired
-- an applied plan change with no took_effect_at is upcoming: both people accepted it and its Monday has not begun
alter table plan_changes
    add column took_effect_at timestamptz,
    add column expiry_cause   varchar(11);

alter table plan_changes
    add constraint ck_plan_changes_took_effect_applied check (took_effect_at is null or state = 'APPLIED'),
    add constraint ck_plan_changes_expiry_cause check (expiry_cause in ('UNDECIDED', 'PLAN_EDITED')),
    add constraint ck_plan_changes_expiry_cause_expired check ((state = 'EXPIRED') = (expiry_cause is not null));

-- an upcoming plan change still counts: a second one made from a plan that is about to change would carry wrong
-- old values on its Monday
drop index uq_plan_changes_one_open_per_client;
create unique index uq_plan_changes_one_open_per_client on plan_changes (client_id)
    where state in ('PENDING', 'ACCEPTED_BY_ONE') or (state = 'APPLIED' and took_effect_at is null);

-- the step that applies or expires plan changes looks for the ones that have not settled yet
create index ix_plan_changes_effective_week_unsettled on plan_changes (effective_week)
    where state in ('PENDING', 'ACCEPTED_BY_ONE') or (state = 'APPLIED' and took_effect_at is null);

-- the sessions of a plan as they were before an applied plan change took effect, valid before the Monday it did; rows
-- are written once and never updated
create table plan_revisions (
    id          uuid        not null,
    plan_id     uuid        not null,
    valid_until date        not null,
    created_at  timestamptz not null,
    constraint pk_plan_revisions primary key (id),
    constraint fk_plan_revisions_plan foreign key (plan_id) references plans (id) on delete cascade,
    constraint uq_plan_revisions_plan_valid_until unique (plan_id, valid_until),
    -- isodow is 1 for Monday, so a revision ends at the start of a Monday-to-Sunday week
    constraint ck_plan_revisions_valid_until_monday check (extract(isodow from valid_until) = 1)
);

create table plan_revision_sessions (
    id          uuid        not null,
    revision_id uuid        not null,
    name        varchar(60) not null,
    day_of_week varchar(9)  not null,
    constraint pk_plan_revision_sessions primary key (id),
    constraint fk_plan_revision_sessions_revision foreign key (revision_id)
        references plan_revisions (id) on delete cascade,
    constraint uq_plan_revision_sessions_revision_day unique (revision_id, day_of_week),
    constraint ck_plan_revision_sessions_name_not_blank check (trim(name) <> ''),
    constraint ck_plan_revision_sessions_day_of_week check (day_of_week in (
        'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'))
);

create table plan_revision_session_entries (
    session_id   uuid         not null,
    position     integer      not null,
    exercise_id  uuid         not null,
    sets         integer      not null,
    amount       integer      not null,
    load_kg      numeric(4,1),
    rpe          integer      not null,
    rest_seconds integer      not null,
    constraint pk_plan_revision_session_entries primary key (session_id, position),
    constraint fk_plan_revision_session_entries_session foreign key (session_id)
        references plan_revision_sessions (id) on delete cascade,
    constraint fk_plan_revision_session_entries_exercise foreign key (exercise_id) references exercises (id),
    constraint ck_plan_revision_session_entries_position check (position >= 0),
    constraint ck_plan_revision_session_entries_sets check (sets between 1 and 20),
    constraint ck_plan_revision_session_entries_amount check (amount between 1 and 3600),
    constraint ck_plan_revision_session_entries_load_kg check (load_kg > 0),
    constraint ck_plan_revision_session_entries_rpe check (rpe between 5 and 10),
    constraint ck_plan_revision_session_entries_rest_seconds check (rest_seconds between 0 and 600)
);

create index ix_plan_revision_session_entries_exercise_id on plan_revision_session_entries (exercise_id);
create index ix_plan_revision_sessions_revision_id on plan_revision_sessions (revision_id);
