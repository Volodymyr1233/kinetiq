alter table plans
    add column client_seen_at timestamptz,
    -- a draft is not visible to the client, so it cannot have been seen
    add constraint ck_plans_seen_published check (client_seen_at is null or first_week is not null);
