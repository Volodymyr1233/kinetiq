-- local profile only; the demo trainer owns three exercises (one timed, one with no equipment or cues) and the second
-- demo trainer one, so each library shows that trainers never see each other's exercises
insert into exercises (id, owner_id, name, category, equipment, measure, default_sets, default_amount, rest_seconds,
                       cues, created_at, updated_at, version)
values ('00000000-0000-4000-9200-000000000001', '00000000-0000-4000-8000-000000000001', 'Copenhagen plank', 'CORE',
        'Bench', 'SECONDS', 3, 20, 60, 'Top leg on the bench. Hips in line with the shoulders.', now(), now(), 0),
       ('00000000-0000-4000-9200-000000000002', '00000000-0000-4000-8000-000000000001', 'Landmine press', 'SHOULDERS',
        'Barbell in a landmine', 'REPS', 3, 10, 90, 'Press up and forward. Ribs down.', now(), now(), 0),
       ('00000000-0000-4000-9200-000000000003', '00000000-0000-4000-8000-000000000001', 'Bear crawl', 'FULL_BODY',
        '', 'REPS', 3, 12, 60, '', now(), now(), 0),
       ('00000000-0000-4000-9200-000000000004', '00000000-0000-4000-8000-000000000004', 'Sled push', 'LEGS',
        'Sled', 'SECONDS', 4, 20, 120, 'Low hips. Drive through the balls of the feet.', now(), now(), 0)
on conflict do nothing;
