-- local profile only; dates are relative to when this first runs, so recreate the database for fresh ones
-- Demo Client Daily checked in on the three days before, one day in each readiness band, and not yet today
insert into check_ins (id, client_id, check_in_date, sleep, soreness, stress, saved_at, created_at, updated_at, version)
values ('00000000-0000-4000-9800-000000000001', '00000000-0000-4000-8000-000000000012', current_date - 1,
        4, 2, 1, now() - interval '1 day', now(), now(), 0),
       ('00000000-0000-4000-9800-000000000002', '00000000-0000-4000-8000-000000000012', current_date - 2,
        3, 3, 3, now() - interval '2 days', now(), now(), 0),
       ('00000000-0000-4000-9800-000000000003', '00000000-0000-4000-8000-000000000012', current_date - 3,
        2, 4, 3, now() - interval '3 days', now(), now(), 0)
on conflict do nothing;

-- a recovery day with a logged session: Thursday of plan week 2 (R__demo_session_logs.sql logs it with the lighter
-- targets), which is never one of the three days before today, since today is in plan week 3
insert into check_ins (id, client_id, check_in_date, sleep, soreness, stress, saved_at, created_at, updated_at, version)
values ('00000000-0000-4000-9800-000000000004', '00000000-0000-4000-8000-000000000012',
        (date_trunc('week', current_date) - interval '2 weeks' + interval '10 days')::date,
        2, 4, 3, (date_trunc('week', current_date) - interval '2 weeks' + interval '10 days 7 hours'),
        now(), now(), 0)
on conflict do nothing;
