-- one row per client and week the rules were checked for, so a week is never checked twice
create table weekly_checks (
    client_id  uuid        not null,
    week_start date        not null,
    checked_at timestamptz not null,
    constraint pk_weekly_checks primary key (client_id, week_start),
    constraint fk_weekly_checks_client foreign key (client_id) references accounts (id),
    -- isodow is 1 for Monday, so a week is Monday to Sunday
    constraint ck_weekly_checks_week_start_monday check (extract(isodow from week_start) = 1)
);
