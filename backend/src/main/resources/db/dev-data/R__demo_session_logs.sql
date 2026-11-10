-- local profile only; finished sessions for Demo Client Daily (plan 00000000-0000-4000-9300-000000000007), so Plan and
-- the done screen have something to show. All start more than 24 hours ago, so they are locked. Day 0 is the Monday of
-- plan week 1: Wednesday and Thursday of week 1 and Monday, Tuesday, Thursday and Friday of week 2 are Done; Friday and
-- Saturday of week 1 and Wednesday and Saturday of week 2 have no log, so they are Missed. Week 3 is left to be
-- trained live. Each log copies its planned session, with one logged set for every planned set. Thursday of week 2
-- (day 5) was a recovery day (R__demo_check_ins.sql): its sets were logged at, and copy, the lighter targets.
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
       target.load_kg,
       target.amount,
       least(10, target.rpe + (set_number - 1) % 2),
       target.load_kg,
       target.amount,
       target.rpe,
       log.started_at + make_interval(mins => entry.position * 12 + set_number * 3),
       now(),
       now(),
       0
from session_log_entries entry
join session_logs log on log.id = entry.session_log_id
cross join lateral generate_series(1, entry.sets) as set_number
-- the recovery day's lighter targets, by the rule: 10% less load to 0.5 kg with a half going lighter, RPE one lower
cross join lateral (
    select case when recovery then ceil(entry.load_kg * 0.9 * 2 - 0.5) / 2 else entry.load_kg end as load_kg,
           entry.amount,
           case when recovery then greatest(5, entry.rpe - 1) else entry.rpe end as rpe
    from (select log.id = '00000000-0000-4000-9600-000000000005'::uuid and entry.load_kg is not null as recovery) day
) as target
where log.id::text like '00000000-0000-4000-9600-%'
on conflict do nothing;

-- a question from Demo Client Daily about the last set of the first exercise of their latest session, so the About
-- chip shows in both conversation views; it is here and not with the other demo messages because repeatable
-- migrations run in the order of their names, and the set logs above must exist first
insert into conversations (id, client_id, trainer_id, created_at, updated_at, version)
values ('00000000-0000-4000-8000-000000000503', '00000000-0000-4000-8000-000000000012',
        '00000000-0000-4000-8000-000000000001', now(), now(), 0)
on conflict do nothing;

insert into messages (id, conversation_id, sender_id, text, received_at, about_set_log_id, created_at, updated_at,
                      version)
select '00000000-0000-4000-8000-000000000607',
       conversation.id,
       conversation.client_id,
       'The last set felt heavier than it should. Is that expected?',
       now() - interval '30 minutes',
       asked.id,
       now(),
       now(),
       0
from conversations conversation
cross join lateral (
    select set_log.id
    from set_logs set_log
    where set_log.session_log_id = '00000000-0000-4000-9600-000000000006'::uuid
    order by set_log.entry_position, set_log.set_number desc
    limit 1
) as asked
where conversation.client_id = '00000000-0000-4000-8000-000000000012'::uuid
  and conversation.trainer_id = '00000000-0000-4000-8000-000000000001'::uuid
on conflict do nothing;
