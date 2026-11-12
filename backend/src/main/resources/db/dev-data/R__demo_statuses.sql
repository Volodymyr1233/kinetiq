-- local profile only; dates are relative to when this first runs, so recreate the database for fresh ones. The demo
-- clients of the other files are At risk, Plan to review or No plan; these give the trainer's overview the rest:
-- Demo Client Steady is On track, Demo Client Heavy is Watch for an RPE of 9, and Demo Client Recovery has a recovery
-- day today with the lighter targets. Demo Client Other Risk is the second trainer's, At risk with a recovery day, so
-- the first trainer's overview proves it shows none of it. The file name sorts after the session logs it builds on.
insert into accounts (id, name, email, password_hash, role, trainer_id, time_zone, email_confirmed,
                      health_data_consent_at, created_at, updated_at, version)
values ('00000000-0000-4000-8000-000000000030', 'Demo Client Steady', 'demo.client.steady@example.com',
        '{pbkdf2}0da7be77a2095e2d1256884a362ecc07cbeb133048ff08db4960f690fd3d192a9ea91fd5dfd7811a78a1a47964050a7d',
        'CLIENT', '00000000-0000-4000-8000-000000000001', 'Europe/Warsaw', true, now(), now(), now(), 0),
       ('00000000-0000-4000-8000-000000000031', 'Demo Client Heavy', 'demo.client.heavy@example.com',
        '{pbkdf2}0da7be77a2095e2d1256884a362ecc07cbeb133048ff08db4960f690fd3d192a9ea91fd5dfd7811a78a1a47964050a7d',
        'CLIENT', '00000000-0000-4000-8000-000000000001', 'Europe/Warsaw', true, now(), now(), now(), 0),
       ('00000000-0000-4000-8000-000000000032', 'Demo Client Recovery', 'demo.client.recovery@example.com',
        '{pbkdf2}0da7be77a2095e2d1256884a362ecc07cbeb133048ff08db4960f690fd3d192a9ea91fd5dfd7811a78a1a47964050a7d',
        'CLIENT', '00000000-0000-4000-8000-000000000001', 'Europe/Warsaw', true, now(), now(), now(), 0),
       ('00000000-0000-4000-8000-000000000033', 'Demo Client Other Risk', 'demo.client.other.risk@example.com',
        '{pbkdf2}0da7be77a2095e2d1256884a362ecc07cbeb133048ff08db4960f690fd3d192a9ea91fd5dfd7811a78a1a47964050a7d',
        'CLIENT', '00000000-0000-4000-8000-000000000004', 'Europe/Warsaw', true, now(), now(), now(), 0)
on conflict do nothing;

-- the first three published on the Monday 3 weeks back, so they are in plan week 4; the other trainer's last week
insert into plans (id, client_id, goal, weeks, first_week, published_on, client_seen_at, created_at, updated_at,
                   version)
select plan.id::uuid, plan.client_id::uuid, plan.goal, 8,
       (date_trunc('week', current_date) - make_interval(weeks => plan.weeks_back))::date,
       (date_trunc('week', current_date) - make_interval(weeks => plan.weeks_back))::date,
       now(), now(), now(), 0
from (values ('00000000-0000-4000-9300-000000000030', '00000000-0000-4000-8000-000000000030', 'GET_STRONGER', 3),
             ('00000000-0000-4000-9300-000000000031', '00000000-0000-4000-8000-000000000031', 'BUILD_MUSCLE', 3),
             ('00000000-0000-4000-9300-000000000032', '00000000-0000-4000-8000-000000000032', 'GENERAL_FITNESS', 3),
             ('00000000-0000-4000-9300-000000000033', '00000000-0000-4000-8000-000000000033', 'LOSE_FAT', 1))
         as plan (id, client_id, goal, weeks_back)
on conflict do nothing;

-- Monday, Wednesday and Friday, and Monday to Saturday for the recovery day, so it falls on a session day
insert into plan_sessions (id, plan_id, name, day_of_week)
values ('00000000-0000-4000-9400-000000000040', '00000000-0000-4000-9300-000000000030', 'Lower body', 'MONDAY'),
       ('00000000-0000-4000-9400-000000000041', '00000000-0000-4000-9300-000000000030', 'Upper body', 'WEDNESDAY'),
       ('00000000-0000-4000-9400-000000000042', '00000000-0000-4000-9300-000000000030', 'Full body', 'FRIDAY'),
       ('00000000-0000-4000-9400-000000000043', '00000000-0000-4000-9300-000000000031', 'Lower body', 'MONDAY'),
       ('00000000-0000-4000-9400-000000000044', '00000000-0000-4000-9300-000000000031', 'Upper body', 'WEDNESDAY'),
       ('00000000-0000-4000-9400-000000000045', '00000000-0000-4000-9300-000000000031', 'Full body', 'FRIDAY'),
       ('00000000-0000-4000-9400-000000000046', '00000000-0000-4000-9300-000000000032', 'Lower body', 'MONDAY'),
       ('00000000-0000-4000-9400-000000000047', '00000000-0000-4000-9300-000000000032', 'Upper body', 'TUESDAY'),
       ('00000000-0000-4000-9400-000000000048', '00000000-0000-4000-9300-000000000032', 'Lower body', 'WEDNESDAY'),
       ('00000000-0000-4000-9400-000000000049', '00000000-0000-4000-9300-000000000032', 'Upper body', 'THURSDAY'),
       ('00000000-0000-4000-9400-000000000050', '00000000-0000-4000-9300-000000000032', 'Full body', 'FRIDAY'),
       ('00000000-0000-4000-9400-000000000051', '00000000-0000-4000-9300-000000000032', 'Mobility', 'SATURDAY'),
       ('00000000-0000-4000-9400-000000000052', '00000000-0000-4000-9300-000000000033', 'Full body', 'MONDAY'),
       ('00000000-0000-4000-9400-000000000053', '00000000-0000-4000-9300-000000000033', 'Full body', 'THURSDAY')
on conflict do nothing;

-- one back squat entry per session, 3 sets of 5
insert into plan_session_entries (session_id, position, exercise_id, sets, amount, load_kg, rpe, rest_seconds)
select session.id, 0, '00000000-0000-4000-9100-000000000011'::uuid, 3, 5, 80.0, 8, 120
from plan_sessions session
where session.plan_id in ('00000000-0000-4000-9300-000000000030', '00000000-0000-4000-9300-000000000031',
                          '00000000-0000-4000-9300-000000000032', '00000000-0000-4000-9300-000000000033')
on conflict do nothing;

-- every planned day before today finished by the first three, each set at the client's RPE; the other trainer's
-- client logs nothing
insert into session_logs (id, client_id, log_date, session_name, started_at, finished_at, created_at, updated_at,
                          version)
select md5(plan.client_id::text || day::date)::uuid, plan.client_id, day::date, session.name,
       (day::date + time '18:00') at time zone 'Europe/Warsaw',
       (day::date + time '18:50') at time zone 'Europe/Warsaw',
       now(), now(), 0
from plans plan
join plan_sessions session on session.plan_id = plan.id
cross join lateral generate_series(plan.first_week, current_date - 1, interval '1 day') as day
where plan.id in ('00000000-0000-4000-9300-000000000030', '00000000-0000-4000-9300-000000000031',
                  '00000000-0000-4000-9300-000000000032')
  and trim(to_char(day, 'FMDAY')) = session.day_of_week
on conflict do nothing;

insert into session_log_entries (session_log_id, position, exercise_id, sets, amount, load_kg, rpe, rest_seconds)
select log.id, 0, '00000000-0000-4000-9100-000000000011'::uuid, 3, 5, 80.0, 8, 120
from session_logs log
where log.client_id in ('00000000-0000-4000-8000-000000000030', '00000000-0000-4000-8000-000000000031',
                        '00000000-0000-4000-8000-000000000032')
on conflict do nothing;

insert into set_logs (id, session_log_id, entry_position, set_number, load_kg, amount, rpe, target_load_kg,
                      target_amount, target_rpe, logged_at, created_at, updated_at, version)
select md5(log.id::text || '-' || set_number)::uuid, log.id, 0, set_number, 80.0, 5,
       case log.client_id
           when '00000000-0000-4000-8000-000000000031' then 9
           else 7 + (set_number - 1) % 2
       end,
       80.0, 5, 8, log.started_at + make_interval(mins => set_number * 4), now(), now(), 0
from session_logs log
cross join generate_series(1, 3) as set_number
where log.client_id in ('00000000-0000-4000-8000-000000000030', '00000000-0000-4000-8000-000000000031',
                        '00000000-0000-4000-8000-000000000032')
on conflict do nothing;

-- slept badly, very sore and stressed: readiness 8, a recovery day, on a session day only (Sunday has none)
insert into check_ins (id, client_id, check_in_date, sleep, soreness, stress, saved_at, target_choice, created_at,
                       updated_at, version)
select check_in.id::uuid, check_in.client_id::uuid, current_date, 1, 5, 4, now(), 'LIGHTER', now(), now(), 0
from (values ('00000000-0000-4000-9800-000000000030', '00000000-0000-4000-8000-000000000032'),
             ('00000000-0000-4000-9800-000000000033', '00000000-0000-4000-8000-000000000033'))
         as check_in (id, client_id)
where extract(isodow from current_date) between 1 and 6
on conflict do nothing;
