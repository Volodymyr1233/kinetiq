-- AccountErasureIT: client Zofia (…0003) has a row in every table that refers to a client; her trainer Marek (…0001)
-- also coaches the control client Piotr (…0004); Ewa (…0002) is a control trainer with data of her own

insert into accounts (id, name, email, password_hash, role, trainer_id, time_zone, email_confirmed,
                      health_data_consent_at, created_at, updated_at, version)
values ('10000000-0000-4000-8000-000000000001', 'Marek Trainer', 'marek.trainer@example.com', 'hash', 'TRAINER',
        null, 'Europe/Warsaw', true, null, now(), now(), 0),
       ('10000000-0000-4000-8000-000000000002', 'Ewa Control', 'ewa.control@example.com', 'hash', 'TRAINER',
        null, 'Europe/Warsaw', true, null, now(), now(), 0),
       ('10000000-0000-4000-8000-000000000003', 'Zofia Erasure', 'zofia.erasure@example.com', 'hash', 'CLIENT',
        '10000000-0000-4000-8000-000000000001', 'Europe/Warsaw', true, now(), now(), now(), 0),
       ('10000000-0000-4000-8000-000000000004', 'Piotr Control', 'piotr.control@example.com', 'hash', 'CLIENT',
        '10000000-0000-4000-8000-000000000001', 'Europe/Warsaw', true, now(), now(), now(), 0);

insert into exercises (id, owner_id, name, category, equipment, measure, default_sets, default_amount, rest_seconds,
                       cues, created_at, updated_at, version)
values ('20000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', 'Marek squat', 'LEGS',
        'Barbell', 'REPS', 3, 5, 120, 'Brace.', now(), now(), 0),
       ('20000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000002', 'Ewa row', 'BACK',
        'Cable', 'REPS', 3, 10, 90, 'Chest up.', now(), now(), 0);

insert into invites (id, trainer_id, email, status, email_token_hash, link_token_hash, sent_at, expires_at,
                     created_at, updated_at, version)
values ('90000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', 'zofia.erasure@example.com',
        'USED', repeat('a', 64), repeat('b', 64), now(), now() + interval '7 days', now(), now(), 0),
       ('90000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000002', 'someone@example.com',
        'OPEN', repeat('c', 64), repeat('d', 64), now(), now() + interval '7 days', now(), now(), 0);

insert into plan_build_requests (id, trainer_id, requested_at)
values ('a0000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', now());

insert into quiz_submissions (id, client_id, goal, experience, level, age, height_cm, weight_kg, days_per_week, notes,
                              submitted_at, created_at, updated_at, version)
values ('30000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000003', 'BUILD_MUSCLE',
        'YEARS_1_TO_2', 'INTERMEDIATE', 30, 170, 70.0, 3, 'Zofia Erasure prefers mornings', now(), now(), now(), 0),
       ('30000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000004', 'GET_STRONGER',
        'YEARS_2_TO_5', 'ADVANCED', 35, 180, 85.0, 4, null, now(), now(), now(), 0);

insert into quiz_submission_likes (submission_id, like_code)
values ('30000000-0000-4000-8000-000000000001', 'WEIGHTS'),
       ('30000000-0000-4000-8000-000000000002', 'CARDIO');

insert into plans (id, client_id, goal, weeks, first_week, published_on, created_at, updated_at, version)
values ('40000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000003', 'BUILD_MUSCLE', 8,
        '2026-11-02', '2026-11-02', now(), now(), 0),
       ('40000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000004', 'GET_STRONGER', 8,
        '2026-11-02', '2026-11-02', now(), now(), 0);

insert into plan_sessions (id, plan_id, name, day_of_week)
values ('41000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000001', 'Lower body', 'MONDAY'),
       ('41000000-0000-4000-8000-000000000002', '40000000-0000-4000-8000-000000000002', 'Lower body', 'MONDAY');

insert into plan_session_entries (session_id, position, exercise_id, sets, amount, load_kg, rpe, rest_seconds)
values ('41000000-0000-4000-8000-000000000001', 0, '20000000-0000-4000-8000-000000000001', 3, 5, 60.0, 8, 120),
       ('41000000-0000-4000-8000-000000000002', 0, '20000000-0000-4000-8000-000000000001', 3, 5, 80.0, 8, 120);

insert into plan_draftings (client_id, started_at)
values ('10000000-0000-4000-8000-000000000003', now());

insert into session_logs (id, client_id, log_date, session_name, started_at, finished_at, note, created_at,
                          updated_at, version)
values ('50000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000003', '2026-11-09', 'Lower body',
        now(), now(), 'Felt strong', now(), now(), 0),
       ('50000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000004', '2026-11-09', 'Lower body',
        now(), now(), null, now(), now(), 0);

insert into session_log_entries (session_log_id, position, exercise_id, sets, amount, load_kg, rpe, rest_seconds)
values ('50000000-0000-4000-8000-000000000001', 0, '20000000-0000-4000-8000-000000000001', 3, 5, 60.0, 8, 120),
       ('50000000-0000-4000-8000-000000000002', 0, '20000000-0000-4000-8000-000000000001', 3, 5, 80.0, 8, 120);

insert into set_logs (id, session_log_id, entry_position, set_number, load_kg, amount, rpe, target_load_kg,
                      target_amount, target_rpe, logged_at, created_at, updated_at, version)
values ('51000000-0000-4000-8000-000000000001', '50000000-0000-4000-8000-000000000001', 0, 1, 60.0, 5, 9, 60.0, 5, 8,
        now(), now(), now(), 0),
       ('51000000-0000-4000-8000-000000000002', '50000000-0000-4000-8000-000000000002', 0, 1, 80.0, 5, 8, 80.0, 5, 8,
        now(), now(), now(), 0);

insert into check_ins (id, client_id, check_in_date, sleep, soreness, stress, saved_at, created_at, updated_at,
                       version)
values ('60000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000003', '2026-11-09', 4, 2, 3, now(),
        now(), now(), 0),
       ('60000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000004', '2026-11-09', 3, 3, 3, now(),
        now(), now(), 0);

insert into weekly_checks (client_id, week_start, checked_at)
values ('10000000-0000-4000-8000-000000000003', '2026-11-09', now()),
       ('10000000-0000-4000-8000-000000000004', '2026-11-09', now());

insert into plan_changes (id, client_id, rule, state, plan_version, window_start, window_end, effective_week,
                          created_at)
values ('70000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000003', 'HIGH_RPE', 'PENDING', 0,
        '2026-10-26', '2026-11-09', '2026-11-16', now());

insert into plan_change_targets (plan_change_id, session_day, position, target, exercise_id, session_name, old_value,
                                 new_value)
values ('70000000-0000-4000-8000-000000000001', 'MONDAY', 0, 'LOAD_KG', '20000000-0000-4000-8000-000000000001',
        'Lower body', 60.0, 57.5);

insert into plan_change_moves (plan_change_id, from_position, exercise_id, to_day)
values ('70000000-0000-4000-8000-000000000001', 0, '20000000-0000-4000-8000-000000000001', 'WEDNESDAY');

insert into plan_change_figures (id, plan_change_id, kind, exercise_id, week_start, value)
values ('71000000-0000-4000-8000-000000000001', '70000000-0000-4000-8000-000000000001', 'MEAN_RPE', null,
        '2026-11-02', 9.0);

insert into plan_change_exercises (plan_change_id, exercise_id)
values ('70000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000001');

insert into conversations (id, client_id, trainer_id, created_at, updated_at, version)
values ('80000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000003',
        '10000000-0000-4000-8000-000000000001', now(), now(), 0),
       ('80000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000004',
        '10000000-0000-4000-8000-000000000001', now(), now(), 0);

insert into messages (id, conversation_id, sender_id, text, received_at, about_set_log_id, created_at, updated_at,
                      version)
values ('81000000-0000-4000-8000-000000000001', '80000000-0000-4000-8000-000000000001',
        '10000000-0000-4000-8000-000000000003', 'Was that set too heavy?', now(),
        '51000000-0000-4000-8000-000000000001', now(), now(), 0),
       ('81000000-0000-4000-8000-000000000002', '80000000-0000-4000-8000-000000000001',
        '10000000-0000-4000-8000-000000000001', 'Good work, Zofia Erasure', now(), null, now(), now(), 0),
       ('81000000-0000-4000-8000-000000000003', '80000000-0000-4000-8000-000000000002',
        '10000000-0000-4000-8000-000000000004', 'See you Monday', now(), null, now(), now(), 0);

insert into conversation_reads (conversation_id, account_id, last_read_at)
values ('80000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000003', now()),
       ('80000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', now()),
       ('80000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000004', now());

insert into notifications (id, recipient_id, kind, client_id, read_at, created_at, updated_at, version)
values ('90000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000003', 'PLAN_PUBLISHED', null, null,
        now(), now(), 0),
       ('90000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000001', 'NEW_MESSAGE',
        '10000000-0000-4000-8000-000000000003', null, now(), now(), 0),
       ('90000000-0000-4000-8000-000000000003', '10000000-0000-4000-8000-000000000004', 'PLAN_PUBLISHED', null, null,
        now(), now(), 0);
