-- local profile only; times are relative to when this first runs
-- the demo trainer has unread notifications about the demo client, and the demo client has two of their own
insert into notifications (id, recipient_id, kind, client_id, read_at, created_at, updated_at, version)
values ('00000000-0000-4000-9800-000000000001', '00000000-0000-4000-8000-000000000001', 'NEW_MESSAGE',
        '00000000-0000-4000-8000-000000000003', null, now() - interval '1 hour', now(), 0),
       ('00000000-0000-4000-9800-000000000002', '00000000-0000-4000-8000-000000000001', 'RECOVERY_DAY',
        '00000000-0000-4000-8000-000000000003', null, now() - interval '3 hours', now(), 0),
       ('00000000-0000-4000-9800-000000000003', '00000000-0000-4000-8000-000000000001', 'PLAN_CHANGE_DECISION',
        '00000000-0000-4000-8000-000000000003', now() - interval '1 day', now() - interval '5 days', now(), 0),
       ('00000000-0000-4000-9800-000000000004', '00000000-0000-4000-8000-000000000003', 'PLAN_CHANGE_WAITING',
        null, null, now() - interval '2 hours', now(), 0),
       ('00000000-0000-4000-9800-000000000005', '00000000-0000-4000-8000-000000000003', 'PLAN_CHANGED',
        null, now() - interval '1 day', now() - interval '3 days', now(), 0)
on conflict do nothing;
