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
