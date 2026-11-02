-- local profile only; Demo Client Draft has a draft, and Demo Client Published a plan of 16 weeks whose first week is 5
-- weeks before the first run, so it starts in week 6 and shortening it below that can be tried. The client has not
-- opened it yet, so the plan-ready screen shows. Demo Client Seen has opened a plan of 8 weeks, in week 3, and Demo
-- Client Finished has a plan of 4 weeks that ended last week
insert into plans (id, client_id, goal, weeks, first_week, client_seen_at, created_at, updated_at, version)
values ('00000000-0000-4000-9300-000000000001', '00000000-0000-4000-8000-000000000006', 'GET_STRONGER', 12, null,
        null, now(), now(), 0),
       ('00000000-0000-4000-9300-000000000002', '00000000-0000-4000-8000-000000000007', 'LOSE_FAT', 16,
        (date_trunc('week', current_date) - interval '5 weeks')::date, null, now(), now(), 0),
       ('00000000-0000-4000-9300-000000000003', '00000000-0000-4000-8000-000000000008', 'GENERAL_FITNESS', 8,
        (date_trunc('week', current_date) - interval '2 weeks')::date, now(), now(), now(), 0),
       ('00000000-0000-4000-9300-000000000004', '00000000-0000-4000-8000-000000000009', 'MOVE_BETTER', 4,
        (date_trunc('week', current_date) - interval '4 weeks')::date, now(), now(), now(), 0)
on conflict do nothing;

insert into plan_sessions (id, plan_id, name, day_of_week)
values ('00000000-0000-4000-9400-000000000001', '00000000-0000-4000-9300-000000000001', 'Heavy lower', 'MONDAY'),
       ('00000000-0000-4000-9400-000000000002', '00000000-0000-4000-9300-000000000002', 'Upper body', 'MONDAY'),
       ('00000000-0000-4000-9400-000000000003', '00000000-0000-4000-9300-000000000002', 'Lower body', 'THURSDAY'),
       ('00000000-0000-4000-9400-000000000004', '00000000-0000-4000-9300-000000000003', 'Full body A', 'TUESDAY'),
       ('00000000-0000-4000-9400-000000000005', '00000000-0000-4000-9300-000000000003', 'Full body B', 'FRIDAY'),
       ('00000000-0000-4000-9400-000000000006', '00000000-0000-4000-9300-000000000004', 'Mobility', 'WEDNESDAY'),
       ('00000000-0000-4000-9400-000000000007', '00000000-0000-4000-9300-000000000004', 'Strength', 'SATURDAY')
on conflict do nothing;

insert into plan_session_entries (session_id, position, exercise_id, sets, amount, load_kg, rpe, rest_seconds)
values ('00000000-0000-4000-9400-000000000001', 0, '00000000-0000-4000-9100-000000000011', 4, 5, 100.0, 8, 150),
       ('00000000-0000-4000-9400-000000000002', 0, '00000000-0000-4000-9100-000000000001', 4, 8, 50.0, 8, 120),
       ('00000000-0000-4000-9400-000000000002', 1, '00000000-0000-4000-9100-000000000017', 3, 8, 30.0, 8, 90),
       ('00000000-0000-4000-9400-000000000003', 0, '00000000-0000-4000-9100-000000000011', 3, 5, 80.0, 8, 120),
       ('00000000-0000-4000-9400-000000000003', 1, '00000000-0000-4000-9100-000000000026', 3, 45, null, 7, 60),
       ('00000000-0000-4000-9400-000000000004', 0, '00000000-0000-4000-9100-000000000011', 3, 8, 40.0, 7, 90),
       ('00000000-0000-4000-9400-000000000004', 1, '00000000-0000-4000-9100-000000000026', 3, 30, null, 7, 45),
       ('00000000-0000-4000-9400-000000000005', 0, '00000000-0000-4000-9100-000000000001', 3, 10, 30.0, 7, 90),
       ('00000000-0000-4000-9400-000000000006', 0, '00000000-0000-4000-9100-000000000026', 2, 40, null, 6, 30),
       ('00000000-0000-4000-9400-000000000007', 0, '00000000-0000-4000-9100-000000000011', 3, 6, 50.0, 8, 120)
on conflict do nothing;
