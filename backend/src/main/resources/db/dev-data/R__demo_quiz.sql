-- local profile only; demo.client.waiting@example.com has submitted the quiz, so the waiting state shows for it
insert into quiz_submissions (id, client_id, goal, experience, level, age, height_cm, weight_kg, days_per_week, notes,
                              submitted_at, created_at, updated_at, version)
values ('00000000-0000-4000-8000-000000000301', '00000000-0000-4000-8000-000000000005', 'BUILD_MUSCLE',
        'YEARS_1_TO_2', 'INTERMEDIATE', 29, 168, 62.5, 3, 'Left knee gets stiff on deep squats', now(), now(), now(), 0)
on conflict do nothing;

insert into quiz_submission_likes (submission_id, like_code)
values ('00000000-0000-4000-8000-000000000301', 'WEIGHTS'),
       ('00000000-0000-4000-8000-000000000301', 'YOGA_PILATES')
on conflict do nothing;
