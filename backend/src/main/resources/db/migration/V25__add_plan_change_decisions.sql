-- who accepted a plan change and when, and how a declined one ended
-- the state stays a column of its own (not derived in queries) so the open-per-client index can use it; the check
-- below keeps it in step with the columns it follows
alter table plan_changes
    add column trainer_accepted_at timestamptz,
    add column client_accepted_at  timestamptz,
    add column declined_by         varchar(7),
    add column decline_reason      varchar(9),
    add column decline_note        varchar(500);

alter table plan_changes
    add constraint ck_plan_changes_declined_by check (declined_by in ('TRAINER', 'CLIENT')),
    add constraint ck_plan_changes_decline_reason check (decline_reason in (
        'TOO_EASY', 'TOO_HARD', 'SCHEDULE', 'INJURY', 'OTHER')),
    add constraint ck_plan_changes_declined_together check ((declined_by is null) = (decline_reason is null)),
    add constraint ck_plan_changes_decline_note_with_decline check (decline_note is null or declined_by is not null),
    -- ended_at is set at decision time on applied and declined; a declined one keeps the acceptances as history
    add constraint ck_plan_changes_decision_matches_state check (case state
        when 'PENDING' then trainer_accepted_at is null and client_accepted_at is null and declined_by is null
        when 'ACCEPTED_BY_ONE' then (trainer_accepted_at is null) <> (client_accepted_at is null)
            and declined_by is null
        when 'APPLIED' then trainer_accepted_at is not null and client_accepted_at is not null
            and declined_by is null
        when 'DECLINED' then declined_by is not null
        when 'EXPIRED' then declined_by is null
        else false
    end);
