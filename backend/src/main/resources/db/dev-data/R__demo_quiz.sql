-- local profile only; demo.client.waiting@example.com has submitted the quiz, so the waiting state shows for it
insert into quiz_submissions (id, client_id, goal, experience, level, age, height_cm, weight_kg, days_per_week, notes,
                              submitted_at, created_at, updated_at, version)
values ('00000000-0000-4000-8000-000000000301', '00000000-0000-4000-8000-000000000005', 'BUILD_MUSCLE',
        'YEARS_1_TO_2', 'INTERMEDIATE', 29, 168, 62.5, 3, 'Left knee gets stiff on deep squats', now(), now(), now(), 0),
       ('00000000-0000-4000-8000-000000000302', '00000000-0000-4000-8000-000000000006', 'GET_STRONGER',
        'YEARS_2_TO_5', 'ADVANCED', 34, 181, 84.0, 3, null, now(), now(), now(), 0),
       ('00000000-0000-4000-8000-000000000303', '00000000-0000-4000-8000-000000000007', 'LOSE_FAT',
        'MONTHS_6_TO_12', 'BEGINNER', 41, 172, 78.5, 3, null, now(), now(), now(), 0),
       ('00000000-0000-4000-8000-000000000304', '00000000-0000-4000-8000-000000000008', 'GENERAL_FITNESS',
        'YEARS_1_TO_2', 'INTERMEDIATE', 27, 175, 70.0, 3, null, now(), now(), now(), 0),
       ('00000000-0000-4000-8000-000000000305', '00000000-0000-4000-8000-000000000009', 'MOVE_BETTER',
        'YEARS_2_TO_5', 'INTERMEDIATE', 38, 180, 82.0, 3, null, now(), now(), now(), 0),
       ('00000000-0000-4000-8000-000000000306', '00000000-0000-4000-8000-000000000010', 'BUILD_MUSCLE',
        'MONTHS_6_TO_12', 'BEGINNER', 31, 178, 76.5, 3, null, now(), now(), now(), 0),
       ('00000000-0000-4000-8000-000000000307', '00000000-0000-4000-8000-000000000011', 'GENERAL_FITNESS',
        'YEARS_1_TO_2', 'INTERMEDIATE', 44, 165, 68.0, 3, null, now(), now(), now(), 0),
       ('00000000-0000-4000-8000-000000000320', '00000000-0000-4000-8000-000000000020', 'LOSE_FAT',
        'MONTHS_3_TO_6', 'BEGINNER', 36, 170, 74.0, 2, null, now(), now(), now(), 0),
       ('00000000-0000-4000-8000-000000000321', '00000000-0000-4000-8000-000000000021', 'GET_STRONGER',
        'YEARS_2_TO_5', 'INTERMEDIATE', 33, 176, 79.0, 3, 'Prefers short sessions', now(), now(), now(), 0)
on conflict do nothing;

insert into quiz_submission_likes (submission_id, like_code)
values ('00000000-0000-4000-8000-000000000301', 'WEIGHTS'),
       ('00000000-0000-4000-8000-000000000301', 'YOGA_PILATES'),
       ('00000000-0000-4000-8000-000000000302', 'WEIGHTS'),
       ('00000000-0000-4000-8000-000000000303', 'CARDIO'),
       ('00000000-0000-4000-8000-000000000304', 'BODYWEIGHT'),
       ('00000000-0000-4000-8000-000000000305', 'YOGA_PILATES'),
       ('00000000-0000-4000-8000-000000000306', 'WEIGHTS'),
       ('00000000-0000-4000-8000-000000000307', 'BODYWEIGHT'),
       ('00000000-0000-4000-8000-000000000320', 'CARDIO'),
       ('00000000-0000-4000-8000-000000000321', 'WEIGHTS')
on conflict do nothing;

insert into quiz_submissions (id, client_id, goal, experience, level, age, height_cm, weight_kg, days_per_week, notes,
                              submitted_at, created_at, updated_at, version)
values ('00000000-0000-4000-8000-000000000308', '00000000-0000-4000-8000-000000000012', 'GET_STRONGER',
        'YEARS_2_TO_5', 'ADVANCED', 31, 178, 80.0, 6, null, now(), now(), now(), 0)
on conflict do nothing;

insert into quiz_submission_likes (submission_id, like_code)
values ('00000000-0000-4000-8000-000000000308', 'WEIGHTS')
on conflict do nothing;
