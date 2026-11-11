-- local profile only: one pending plan change per rule, for clients of Demo Trainer with a published plan
-- (R__demo_plans.sql). Each holds what its rule's fixed transformation gives for that plan, and the numbers that made
-- the rule fire over the weeks before this one. A client who already has an open plan change keeps it.
-- Demo Client Seen: high RPE, the squat of Full body A loses a repetition (8 to 7)
-- Demo Client Published: stalled weight, the squat of Lower body rises 5% (80 to 84 kg)
-- Demo Client Daily: low completion, the Saturday session goes
insert into plan_changes (id, client_id, rule, state, plan_version, window_start, window_end, effective_week,
                          removed_day, removed_name, created_at, ended_at)
values ('00000000-0000-4000-9600-000000000001', '00000000-0000-4000-8000-000000000008', 'HIGH_RPE', 'PENDING', 0,
        (date_trunc('week', current_date) - interval '2 weeks')::date,
        (date_trunc('week', current_date) - interval '1 day')::date,
        (date_trunc('week', current_date) + interval '1 week')::date, null, null, now(), null),
       ('00000000-0000-4000-9600-000000000002', '00000000-0000-4000-8000-000000000007', 'STALLED_WEIGHT', 'PENDING',
        0, (date_trunc('week', current_date) - interval '3 weeks')::date,
        (date_trunc('week', current_date) - interval '1 day')::date,
        (date_trunc('week', current_date) + interval '1 week')::date, null, null, now(), null),
       ('00000000-0000-4000-9600-000000000003', '00000000-0000-4000-8000-000000000012', 'LOW_COMPLETION', 'PENDING',
        0, (date_trunc('week', current_date) - interval '2 weeks')::date,
        (date_trunc('week', current_date) - interval '1 day')::date,
        (date_trunc('week', current_date) + interval '1 week')::date, 'SATURDAY', 'Full body', now(), null)
on conflict do nothing;

insert into plan_change_targets (plan_change_id, session_day, position, target, exercise_id, session_name, old_value,
                                 new_value)
select target.plan_change_id::uuid, target.session_day, target.position, target.target, target.exercise_id::uuid,
       target.session_name, target.old_value, target.new_value
from (values ('00000000-0000-4000-9600-000000000001', 'TUESDAY', 0, 'AMOUNT', '00000000-0000-4000-9100-000000000011',
              'Full body A', 8.0, 7.0),
             ('00000000-0000-4000-9600-000000000002', 'THURSDAY', 0, 'LOAD_KG',
              '00000000-0000-4000-9100-000000000011', 'Lower body', 80.0, 84.0)
) as target (plan_change_id, session_day, position, target, exercise_id, session_name, old_value, new_value)
join plan_changes change on change.id = target.plan_change_id::uuid
on conflict do nothing;

insert into plan_change_exercises (plan_change_id, exercise_id)
select concerned.plan_change_id::uuid, concerned.exercise_id::uuid
from (values ('00000000-0000-4000-9600-000000000001', '00000000-0000-4000-9100-000000000011'),
             ('00000000-0000-4000-9600-000000000002', '00000000-0000-4000-9100-000000000011')
) as concerned (plan_change_id, exercise_id)
join plan_changes change on change.id = concerned.plan_change_id::uuid
on conflict do nothing;

insert into plan_change_figures (id, plan_change_id, kind, exercise_id, week_start, value)
select figure.id::uuid, figure.plan_change_id::uuid, figure.kind, figure.exercise_id::uuid,
       (date_trunc('week', current_date) - make_interval(weeks => figure.weeks_ago))::date, figure.value
from (values ('00000000-0000-4000-9610-000000000001', '00000000-0000-4000-9600-000000000001', 'MEAN_RPE', null, 2,
              8.400),
             ('00000000-0000-4000-9610-000000000002', '00000000-0000-4000-9600-000000000001', 'MEAN_RPE', null, 1,
              8.600),
             ('00000000-0000-4000-9610-000000000003', '00000000-0000-4000-9600-000000000002', 'BEST_LOAD_KG',
              '00000000-0000-4000-9100-000000000011', 3, 80.000),
             ('00000000-0000-4000-9610-000000000004', '00000000-0000-4000-9600-000000000002', 'BEST_LOAD_KG',
              '00000000-0000-4000-9100-000000000011', 2, 80.000),
             ('00000000-0000-4000-9610-000000000005', '00000000-0000-4000-9600-000000000002', 'BEST_LOAD_KG',
              '00000000-0000-4000-9100-000000000011', 1, 81.000),
             ('00000000-0000-4000-9610-000000000006', '00000000-0000-4000-9600-000000000003', 'SESSIONS_PLANNED', null,
              2, 4.000),
             ('00000000-0000-4000-9610-000000000007', '00000000-0000-4000-9600-000000000003', 'SESSIONS_FINISHED', null,
              2, 2.000),
             ('00000000-0000-4000-9610-000000000008', '00000000-0000-4000-9600-000000000003', 'SESSIONS_PLANNED', null,
              1, 6.000),
             ('00000000-0000-4000-9610-000000000009', '00000000-0000-4000-9600-000000000003', 'SESSIONS_FINISHED', null,
              1, 3.000)
) as figure (id, plan_change_id, kind, exercise_id, weeks_ago, value)
join plan_changes change on change.id = figure.plan_change_id::uuid
on conflict do nothing;

-- decided plan changes, for clients of Demo Trainer who have no open one:
-- Demo Client Closing: stalled weight, accepted by the trainer three hours ago and waiting for the client (the squat of
-- Full body rises 5%, 40 to 42 kg)
-- Demo Client Finished: high RPE, applied three weeks ago by both (the squat of Strength loses a repetition, 6 to 5)
insert into plan_changes (id, client_id, rule, state, plan_version, window_start, window_end, effective_week,
                          removed_day, removed_name, created_at, ended_at, trainer_accepted_at, client_accepted_at)
values ('00000000-0000-4000-9600-000000000004', '00000000-0000-4000-8000-000000000013', 'STALLED_WEIGHT',
        'ACCEPTED_BY_ONE', 0, (date_trunc('week', current_date) - interval '3 weeks')::date,
        (date_trunc('week', current_date) - interval '1 day')::date,
        (date_trunc('week', current_date) + interval '1 week')::date, null, null, now() - interval '1 day', null,
        now() - interval '3 hours', null),
       ('00000000-0000-4000-9600-000000000005', '00000000-0000-4000-8000-000000000009', 'HIGH_RPE', 'APPLIED', 0,
        (date_trunc('week', current_date) - interval '5 weeks')::date,
        (date_trunc('week', current_date) - interval '3 weeks' - interval '1 day')::date,
        (date_trunc('week', current_date) - interval '2 weeks')::date, null, null,
        date_trunc('week', current_date) - interval '3 weeks' + interval '2 hours',
        date_trunc('week', current_date) - interval '3 weeks' + interval '2 days',
        date_trunc('week', current_date) - interval '3 weeks' + interval '1 day',
        date_trunc('week', current_date) - interval '3 weeks' + interval '2 days')
on conflict do nothing;

insert into plan_change_targets (plan_change_id, session_day, position, target, exercise_id, session_name, old_value,
                                 new_value)
select target.plan_change_id::uuid, target.session_day, target.position, target.target, target.exercise_id::uuid,
       target.session_name, target.old_value, target.new_value
from (values ('00000000-0000-4000-9600-000000000004', 'MONDAY', 0, 'LOAD_KG', '00000000-0000-4000-9100-000000000011',
              'Full body', 40.0, 42.0),
             ('00000000-0000-4000-9600-000000000005', 'SATURDAY', 0, 'AMOUNT', '00000000-0000-4000-9100-000000000011',
              'Strength', 6.0, 5.0)
) as target (plan_change_id, session_day, position, target, exercise_id, session_name, old_value, new_value)
join plan_changes change on change.id = target.plan_change_id::uuid
on conflict do nothing;

insert into plan_change_exercises (plan_change_id, exercise_id)
select concerned.plan_change_id::uuid, concerned.exercise_id::uuid
from (values ('00000000-0000-4000-9600-000000000004', '00000000-0000-4000-9100-000000000011'),
             ('00000000-0000-4000-9600-000000000005', '00000000-0000-4000-9100-000000000011')
) as concerned (plan_change_id, exercise_id)
join plan_changes change on change.id = concerned.plan_change_id::uuid
on conflict do nothing;

insert into plan_change_figures (id, plan_change_id, kind, exercise_id, week_start, value)
select figure.id::uuid, figure.plan_change_id::uuid, figure.kind, figure.exercise_id::uuid,
       (date_trunc('week', current_date) - make_interval(weeks => figure.weeks_ago))::date, figure.value
from (values ('00000000-0000-4000-9610-000000000010', '00000000-0000-4000-9600-000000000004', 'BEST_LOAD_KG',
              '00000000-0000-4000-9100-000000000011', 3, 40.000),
             ('00000000-0000-4000-9610-000000000011', '00000000-0000-4000-9600-000000000004', 'BEST_LOAD_KG',
              '00000000-0000-4000-9100-000000000011', 2, 40.000),
             ('00000000-0000-4000-9610-000000000012', '00000000-0000-4000-9600-000000000004', 'BEST_LOAD_KG',
              '00000000-0000-4000-9100-000000000011', 1, 41.000),
             ('00000000-0000-4000-9610-000000000013', '00000000-0000-4000-9600-000000000005', 'MEAN_RPE', null, 5,
              8.400),
             ('00000000-0000-4000-9610-000000000014', '00000000-0000-4000-9600-000000000005', 'MEAN_RPE', null, 4,
              8.600)
) as figure (id, plan_change_id, kind, exercise_id, weeks_ago, value)
join plan_changes change on change.id = figure.plan_change_id::uuid
on conflict do nothing;

-- Demo Client declined a plan change five days ago, for the schedule; the message it left is in R__demo_messages.sql
insert into plan_changes (id, client_id, rule, state, plan_version, window_start, window_end, effective_week,
                          removed_day, removed_name, created_at, ended_at, declined_by, decline_reason, decline_note)
values ('00000000-0000-4000-9600-000000000006', '00000000-0000-4000-8000-000000000003', 'HIGH_RPE', 'DECLINED', 0,
        (date_trunc('week', current_date) - interval '3 weeks')::date,
        (date_trunc('week', current_date) - interval '1 week' - interval '1 day')::date,
        (date_trunc('week', current_date) + interval '1 week')::date, null, null, now() - interval '6 days',
        now() - interval '5 days', 'CLIENT', 'SCHEDULE', 'Travelling for work that week.')
on conflict do nothing;

insert into plan_change_targets (plan_change_id, session_day, position, target, exercise_id, session_name, old_value,
                                 new_value)
select '00000000-0000-4000-9600-000000000006'::uuid, 'MONDAY', 0, 'AMOUNT', '00000000-0000-4000-9100-000000000011'::uuid,
       'Lower body', 8.0, 7.0
from plan_changes change
where change.id = '00000000-0000-4000-9600-000000000006'
on conflict do nothing;

insert into plan_change_exercises (plan_change_id, exercise_id)
select change.id, '00000000-0000-4000-9100-000000000011'::uuid
from plan_changes change
where change.id = '00000000-0000-4000-9600-000000000006'
on conflict do nothing;

insert into plan_change_figures (id, plan_change_id, kind, exercise_id, week_start, value)
select figure.id::uuid, '00000000-0000-4000-9600-000000000006'::uuid, 'MEAN_RPE', null,
       (date_trunc('week', current_date) - make_interval(weeks => figure.weeks_ago))::date, figure.value
from (values ('00000000-0000-4000-9610-000000000020', 3, 8.300),
             ('00000000-0000-4000-9610-000000000021', 2, 8.500)
) as figure (id, weeks_ago, value)
join plan_changes change on change.id = '00000000-0000-4000-9600-000000000006'
on conflict do nothing;
