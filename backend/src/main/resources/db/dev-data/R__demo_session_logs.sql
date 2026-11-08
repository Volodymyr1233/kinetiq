-- local profile only; finished sessions for Demo Client Daily (plan 00000000-0000-4000-9300-000000000007), so Plan and
-- the done screen have something to show. All start more than 24 hours ago, so they are locked. Day 0 is the Monday of
-- plan week 1: Wednesday and Thursday of week 1 and Monday, Tuesday, Thursday and Friday of week 2 are Done; Friday and
-- Saturday of week 1 and Wednesday and Saturday of week 2 have no log, so they are Missed. Week 3 is left to be
-- trained live. Each log copies its planned session, with one logged set for every planned set.
insert into session_logs (id, client_id, log_date, session_name, started_at, finished_at, created_at, updated_at,
                          version)
select ('00000000-0000-4000-9600-0000000000' || lpad(day.number::text, 2, '0'))::uuid,
       plan.client_id,
       (plan.first_week + day.offset_days)::date,
       session.name,
       ((plan.first_week + day.offset_days) + time '18:00') at time zone 'Europe/Warsaw',
       ((plan.first_week + day.offset_days) + time '18:50') at time zone 'Europe/Warsaw',
       now(),
       now(),
       0
from (values (1, 2, '00000000-0000-4000-9400-000000000013'),
             (2, 3, '00000000-0000-4000-9400-000000000014'),
             (3, 7, '00000000-0000-4000-9400-000000000011'),
             (4, 8, '00000000-0000-4000-9400-000000000012'),
             (5, 10, '00000000-0000-4000-9400-000000000014'),
             (6, 11, '00000000-0000-4000-9400-000000000015')) as day (number, offset_days, session_id)
join plan_sessions session on session.id = day.session_id::uuid
join plans plan on plan.id = session.plan_id
on conflict do nothing;

insert into session_log_entries (session_log_id, position, exercise_id, sets, amount, load_kg, rpe, rest_seconds)
select log.id, entry.position, entry.exercise_id, entry.sets, entry.amount, entry.load_kg, entry.rpe,
       entry.rest_seconds
from session_logs log
join plan_sessions session on session.plan_id = '00000000-0000-4000-9300-000000000007'::uuid
                          and session.name = log.session_name
join plan_session_entries entry on entry.session_id = session.id
where log.id::text like '00000000-0000-4000-9600-%'
on conflict do nothing;

insert into set_logs (id, session_log_id, entry_position, set_number, load_kg, amount, rpe, target_load_kg,
                      target_amount, target_rpe, logged_at, created_at, updated_at, version)
select md5(entry.session_log_id::text || entry.position || '-' || set_number)::uuid,
       entry.session_log_id,
       entry.position,
       set_number,
       entry.load_kg,
       entry.amount,
       least(10, entry.rpe + (set_number - 1) % 2),
       entry.load_kg,
       entry.amount,
       entry.rpe,
       log.started_at + make_interval(mins => entry.position * 12 + set_number * 3),
       now(),
       now(),
       0
from session_log_entries entry
join session_logs log on log.id = entry.session_log_id
cross join lateral generate_series(1, entry.sets) as set_number
where log.id::text like '00000000-0000-4000-9600-%'
on conflict do nothing;
