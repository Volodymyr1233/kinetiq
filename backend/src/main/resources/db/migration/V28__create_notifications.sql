-- a notification belongs to its recipient and may be about a client; both keys cascade, so deleting an account erases
-- what it was told and what was told about it (spec 150, FR-005)
create table notifications
(
    id           uuid        not null,
    recipient_id uuid        not null,
    kind         varchar(40) not null,
    client_id    uuid,
    read_at      timestamptz,
    created_at   timestamptz not null,
    updated_at   timestamptz not null,
    version      bigint      not null,
    constraint pk_notifications primary key (id),
    constraint fk_notifications_recipient foreign key (recipient_id) references accounts (id) on delete cascade,
    constraint fk_notifications_client foreign key (client_id) references accounts (id) on delete cascade,
    constraint ck_notifications_kind check (kind in ('PLAN_PUBLISHED', 'PLAN_CHANGED', 'PLAN_CHANGE_WAITING',
                                                     'PLAN_CHANGE_DECISION', 'NEW_MESSAGE', 'DRAFT_TO_REVIEW',
                                                     'RECOVERY_DAY', 'CLIENT_DELETED'))
);

create index ix_notifications_recipient on notifications (recipient_id, created_at desc);
