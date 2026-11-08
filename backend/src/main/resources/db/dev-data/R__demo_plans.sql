-- Every published plan here was published on the Monday of its first week, except Demo Client Daily's (see below).
-- local profile only; Demo Client Draft has a draft, and Demo Client Published a plan of 16 weeks whose first week is 5
-- weeks before the first run, so it starts in week 6 and shortening it below that can be tried. The client has not
-- opened it yet, so the plan-ready screen shows. Demo Client Seen has opened a plan of 8 weeks, in week 3, and Demo
-- Client Finished has a plan of 4 weeks that ended last week. Demo Client Built has a draft built automatically, and
-- Demo Client Failed an empty one that could not be. The three drafts wait for different times, so the approvals list
-- has an order to show, and Demo Client Other is the second trainer's, so it never shows for the first. Demo Client
-- Rebuilding has a built draft being built again; the sweep ends that drafting a few minutes after start, keeping it
insert into plans (id, client_id, goal, weeks, first_week, published_on, client_seen_at, automatic_draft, created_at,
                   updated_at, version)
values ('00000000-0000-4000-9300-000000000001', '00000000-0000-4000-8000-000000000006', 'GET_STRONGER', 12, null, null,
        null, null, now() - interval '3 days', now(), 0),
       ('00000000-0000-4000-9300-000000000002', '00000000-0000-4000-8000-000000000007', 'LOSE_FAT', 16,
        (date_trunc('week', current_date) - interval '5 weeks')::date,
        (date_trunc('week', current_date) - interval '5 weeks')::date, null, null, now(), now(), 0),
       ('00000000-0000-4000-9300-000000000003', '00000000-0000-4000-8000-000000000008', 'GENERAL_FITNESS', 8,
        (date_trunc('week', current_date) - interval '2 weeks')::date,
        (date_trunc('week', current_date) - interval '2 weeks')::date, now(), null, now(), now(), 0),
       ('00000000-0000-4000-9300-000000000004', '00000000-0000-4000-8000-000000000009', 'MOVE_BETTER', 4,
        (date_trunc('week', current_date) - interval '4 weeks')::date,
        (date_trunc('week', current_date) - interval '4 weeks')::date, now(), null, now(), now(), 0),
       ('00000000-0000-4000-9300-000000000005', '00000000-0000-4000-8000-000000000010', 'BUILD_MUSCLE', 12, null,
        null, null, 'BUILT', now() - interval '3 hours', now(), 0),
       ('00000000-0000-4000-9300-000000000006', '00000000-0000-4000-8000-000000000011', 'GENERAL_FITNESS', 12, null,
        null, null, 'FAILED', now() - interval '26 hours', now(), 0),
       ('00000000-0000-4000-9300-000000000020', '00000000-0000-4000-8000-000000000020', 'LOSE_FAT', 12, null,
        null, null, 'BUILT', now() - interval '5 hours', now(), 0),
       ('00000000-0000-4000-9300-000000000021', '00000000-0000-4000-8000-000000000021', 'GET_STRONGER', 12, null,
        null, null, 'BUILT', now() - interval '8 hours', now(), 0)
on conflict do nothing;

insert into plan_sessions (id, plan_id, name, day_of_week)
values ('00000000-0000-4000-9400-000000000001', '00000000-0000-4000-9300-000000000001', 'Heavy lower', 'MONDAY'),
       ('00000000-0000-4000-9400-000000000002', '00000000-0000-4000-9300-000000000002', 'Upper body', 'MONDAY'),
       ('00000000-0000-4000-9400-000000000003', '00000000-0000-4000-9300-000000000002', 'Lower body', 'THURSDAY'),
       ('00000000-0000-4000-9400-000000000004', '00000000-0000-4000-9300-000000000003', 'Full body A', 'TUESDAY'),
       ('00000000-0000-4000-9400-000000000005', '00000000-0000-4000-9300-000000000003', 'Full body B', 'FRIDAY'),
       ('00000000-0000-4000-9400-000000000006', '00000000-0000-4000-9300-000000000004', 'Mobility', 'WEDNESDAY'),
       ('00000000-0000-4000-9400-000000000007', '00000000-0000-4000-9300-000000000004', 'Strength', 'SATURDAY'),
       ('00000000-0000-4000-9400-000000000008', '00000000-0000-4000-9300-000000000005', 'Lower body A', 'MONDAY'),
       ('00000000-0000-4000-9400-000000000009', '00000000-0000-4000-9300-000000000005', 'Upper body', 'WEDNESDAY'),
       ('00000000-0000-4000-9400-000000000010', '00000000-0000-4000-9300-000000000005', 'Lower body B', 'FRIDAY'),
       ('00000000-0000-4000-9400-000000000020', '00000000-0000-4000-9300-000000000020', 'Full body A', 'TUESDAY'),
       ('00000000-0000-4000-9400-000000000021', '00000000-0000-4000-9300-000000000020', 'Full body B', 'FRIDAY'),
       ('00000000-0000-4000-9400-000000000022', '00000000-0000-4000-9300-000000000021', 'Upper body', 'MONDAY'),
       ('00000000-0000-4000-9400-000000000023', '00000000-0000-4000-9300-000000000021', 'Lower body', 'THURSDAY')
on conflict do nothing;

-- a save through the editor replaces a plan's sessions, so a session here may be gone when this file runs again
insert into plan_session_entries (session_id, position, exercise_id, sets, amount, load_kg, rpe, rest_seconds)
select entry.session_id::uuid, entry.position, entry.exercise_id::uuid, entry.sets, entry.amount,
       entry.load_kg::numeric, entry.rpe, entry.rest_seconds
from (values ('00000000-0000-4000-9400-000000000001', 0, '00000000-0000-4000-9100-000000000011', 4, 5, 100.0, 8, 150),
       ('00000000-0000-4000-9400-000000000002', 0, '00000000-0000-4000-9100-000000000001', 4, 8, 50.0, 8, 120),
       ('00000000-0000-4000-9400-000000000002', 1, '00000000-0000-4000-9100-000000000017', 3, 8, 30.0, 8, 90),
       ('00000000-0000-4000-9400-000000000003', 0, '00000000-0000-4000-9100-000000000011', 3, 5, 80.0, 8, 120),
       ('00000000-0000-4000-9400-000000000003', 1, '00000000-0000-4000-9100-000000000026', 3, 45, null, 7, 60),
       ('00000000-0000-4000-9400-000000000004', 0, '00000000-0000-4000-9100-000000000011', 3, 8, 40.0, 7, 90),
       ('00000000-0000-4000-9400-000000000004', 1, '00000000-0000-4000-9100-000000000026', 3, 30, null, 7, 45),
       ('00000000-0000-4000-9400-000000000005', 0, '00000000-0000-4000-9100-000000000001', 3, 10, 30.0, 7, 90),
       ('00000000-0000-4000-9400-000000000006', 0, '00000000-0000-4000-9100-000000000026', 2, 40, null, 6, 30),
       ('00000000-0000-4000-9400-000000000007', 0, '00000000-0000-4000-9100-000000000011', 3, 6, 50.0, 8, 120),
       ('00000000-0000-4000-9400-000000000008', 0, '00000000-0000-4000-9100-000000000011', 3, 10, 30.0, 7, 90),
       ('00000000-0000-4000-9400-000000000008', 1, '00000000-0000-4000-9100-000000000012', 3, 10, 30.0, 7, 90),
       ('00000000-0000-4000-9400-000000000008', 2, '00000000-0000-4000-9100-000000000015', 3, 12, 20.0, 7, 60),
       ('00000000-0000-4000-9400-000000000008', 3, '00000000-0000-4000-9100-000000000026', 2, 30, null, 6, 45),
       ('00000000-0000-4000-9400-000000000009', 0, '00000000-0000-4000-9100-000000000001', 3, 10, 30.0, 7, 90),
       ('00000000-0000-4000-9400-000000000009', 1, '00000000-0000-4000-9100-000000000006', 3, 10, 35.0, 7, 90),
       ('00000000-0000-4000-9400-000000000009', 2, '00000000-0000-4000-9100-000000000018', 3, 12, 4.0, 6, 60),
       ('00000000-0000-4000-9400-000000000009', 3, '00000000-0000-4000-9100-000000000028', 2, 10, null, 6, 45),
       ('00000000-0000-4000-9400-000000000010', 0, '00000000-0000-4000-9100-000000000014', 3, 12, 60.0, 7, 90),
       ('00000000-0000-4000-9400-000000000010', 1, '00000000-0000-4000-9100-000000000007', 3, 10, 30.0, 7, 90),
       ('00000000-0000-4000-9400-000000000010', 2, '00000000-0000-4000-9100-000000000002', 3, 10, 10.0, 7, 60),
       ('00000000-0000-4000-9400-000000000010', 3, '00000000-0000-4000-9100-000000000021', 2, 12, 6.0, 6, 60),
       ('00000000-0000-4000-9400-000000000020', 0, '00000000-0000-4000-9100-000000000011', 3, 10, 30.0, 7, 90),
       ('00000000-0000-4000-9400-000000000021', 0, '00000000-0000-4000-9100-000000000026', 3, 30, null, 7, 45),
       ('00000000-0000-4000-9400-000000000022', 0, '00000000-0000-4000-9100-000000000001', 4, 6, 60.0, 8, 120),
       ('00000000-0000-4000-9400-000000000023', 0, '00000000-0000-4000-9100-000000000011', 4, 5, 90.0, 8, 150)
) as entry (session_id, position, exercise_id, sets, amount, load_kg, rpe, rest_seconds)
join plan_sessions session on session.id = entry.session_id::uuid
on conflict do nothing;

insert into plan_draftings (client_id, started_at, rebuild)
values ('00000000-0000-4000-8000-000000000021', now(), true)
on conflict do nothing;

-- the second trainer has used 9 of the 10 builds of the last 24 hours, so one more is accepted and the next refused
insert into plan_build_requests (id, trainer_id, requested_at)
select ('00000000-0000-4000-9500-00000000000' || hour)::uuid, '00000000-0000-4000-8000-000000000004',
       now() - make_interval(hours => hour)
from generate_series(1, 9) as hour
on conflict do nothing;

-- Demo Client Daily has opened a plan with a session on Monday to Saturday, in week 3 of 8
-- published on the Wednesday of its first week, so Monday and Tuesday of week 1 show no state
insert into plans (id, client_id, goal, weeks, first_week, published_on, client_seen_at, automatic_draft, created_at,
                   updated_at, version)
values ('00000000-0000-4000-9300-000000000007', '00000000-0000-4000-8000-000000000012', 'GET_STRONGER', 8,
        (date_trunc('week', current_date) - interval '2 weeks')::date,
        (date_trunc('week', current_date) - interval '2 weeks' + interval '2 days')::date, now(), null, now(), now(), 0)
on conflict do nothing;

insert into plan_sessions (id, plan_id, name, day_of_week)
values
       ('00000000-0000-4000-9400-000000000011', '00000000-0000-4000-9300-000000000007', 'Squat day', 'MONDAY'),
       ('00000000-0000-4000-9400-000000000012', '00000000-0000-4000-9300-000000000007', 'Press day', 'TUESDAY'),
       ('00000000-0000-4000-9400-000000000013', '00000000-0000-4000-9300-000000000007', 'Pull day', 'WEDNESDAY'),
       ('00000000-0000-4000-9400-000000000014', '00000000-0000-4000-9300-000000000007', 'Hinge day', 'THURSDAY'),
       ('00000000-0000-4000-9400-000000000015', '00000000-0000-4000-9300-000000000007', 'Push day', 'FRIDAY'),
       ('00000000-0000-4000-9400-000000000016', '00000000-0000-4000-9300-000000000007', 'Full body', 'SATURDAY')
on conflict do nothing;

insert into plan_session_entries (session_id, position, exercise_id, sets, amount, load_kg, rpe, rest_seconds)
select entry.session_id::uuid, entry.position, entry.exercise_id::uuid, entry.sets, entry.amount,
       entry.load_kg::numeric, entry.rpe, entry.rest_seconds
from (values
       ('00000000-0000-4000-9400-000000000011', 0, '00000000-0000-4000-9100-000000000011', 3, 8, 40.0, 7, 90),
       ('00000000-0000-4000-9400-000000000012', 0, '00000000-0000-4000-9100-000000000001', 3, 8, 40.0, 7, 90),
       ('00000000-0000-4000-9400-000000000013', 0, '00000000-0000-4000-9100-000000000006', 3, 8, 40.0, 7, 90),
       ('00000000-0000-4000-9400-000000000014', 0, '00000000-0000-4000-9100-000000000012', 3, 8, 40.0, 7, 90),
       ('00000000-0000-4000-9400-000000000015', 0, '00000000-0000-4000-9100-000000000017', 3, 8, 40.0, 7, 90),
       ('00000000-0000-4000-9400-000000000016', 0, '00000000-0000-4000-9100-000000000026', 3, 45, null, 7, 90)
) as entry (session_id, position, exercise_id, sets, amount, load_kg, rpe, rest_seconds)
join plan_sessions session on session.id = entry.session_id::uuid
on conflict do nothing;
