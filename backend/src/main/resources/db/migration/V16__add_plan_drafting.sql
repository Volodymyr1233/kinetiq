alter table plans
    add column automatic_draft varchar(16),
    add constraint ck_plans_automatic_draft check (automatic_draft in ('BUILT', 'FAILED')),
    -- a published plan is the trainer's, so it carries no mark
    add constraint ck_plans_automatic_draft_unpublished check (first_week is null or automatic_draft is null);

-- a row means a client's first plan is being drafted; it lasts minutes, so started_at needs no index
create table plan_draftings (
    client_id  uuid        not null,
    started_at timestamptz not null,
    constraint pk_plan_draftings primary key (client_id),
    constraint fk_plan_draftings_client foreign key (client_id) references accounts (id)
);
