-- local profile only; every account uses the demo password Kinetiq-demo-1 (see README)
insert into accounts (id, name, email, password_hash, role, trainer_id, time_zone, email_confirmed,
                      health_data_consent_at, created_at, updated_at, version)
values ('00000000-0000-4000-8000-000000000001', 'Demo Trainer', 'demo.trainer@example.com',
        '{pbkdf2}0da7be77a2095e2d1256884a362ecc07cbeb133048ff08db4960f690fd3d192a9ea91fd5dfd7811a78a1a47964050a7d',
        'TRAINER', null, 'Europe/Warsaw', true, null, now(), now(), 0),
       ('00000000-0000-4000-8000-000000000002', 'Demo Unconfirmed', 'demo.unconfirmed@example.com',
        '{pbkdf2}0da7be77a2095e2d1256884a362ecc07cbeb133048ff08db4960f690fd3d192a9ea91fd5dfd7811a78a1a47964050a7d',
        'TRAINER', null, 'Europe/Warsaw', false, null, now(), now(), 0),
       ('00000000-0000-4000-8000-000000000003', 'Demo Client', 'demo.client@example.com',
        '{pbkdf2}0da7be77a2095e2d1256884a362ecc07cbeb133048ff08db4960f690fd3d192a9ea91fd5dfd7811a78a1a47964050a7d',
        'CLIENT', '00000000-0000-4000-8000-000000000001', 'Europe/Warsaw', true, now(), now(), now(), 0),
       ('00000000-0000-4000-8000-000000000004', 'Demo Trainer Two', 'demo.trainer2@example.com',
        '{pbkdf2}0da7be77a2095e2d1256884a362ecc07cbeb133048ff08db4960f690fd3d192a9ea91fd5dfd7811a78a1a47964050a7d',
        'TRAINER', null, 'Europe/Warsaw', true, null, now(), now(), 0),
       -- has submitted the quiz (R__demo_quiz.sql), while the first demo client can still answer it
       ('00000000-0000-4000-8000-000000000005', 'Demo Client Waiting', 'demo.client.waiting@example.com',
        '{pbkdf2}0da7be77a2095e2d1256884a362ecc07cbeb133048ff08db4960f690fd3d192a9ea91fd5dfd7811a78a1a47964050a7d',
        'CLIENT', '00000000-0000-4000-8000-000000000001', 'Europe/Warsaw', true, now(), now(), now(), 0),
       -- their plans are in R__demo_plans.sql: a draft, and a published plan past week 4
       ('00000000-0000-4000-8000-000000000006', 'Demo Client Draft', 'demo.client.draft@example.com',
        '{pbkdf2}0da7be77a2095e2d1256884a362ecc07cbeb133048ff08db4960f690fd3d192a9ea91fd5dfd7811a78a1a47964050a7d',
        'CLIENT', '00000000-0000-4000-8000-000000000001', 'Europe/Warsaw', true, now(), now(), now(), 0),
       ('00000000-0000-4000-8000-000000000007', 'Demo Client Published', 'demo.client.published@example.com',
        '{pbkdf2}0da7be77a2095e2d1256884a362ecc07cbeb133048ff08db4960f690fd3d192a9ea91fd5dfd7811a78a1a47964050a7d',
        'CLIENT', '00000000-0000-4000-8000-000000000001', 'Europe/Warsaw', true, now(), now(), now(), 0),
       -- published plans in R__demo_plans.sql: one the client has seen, one that has finished
       ('00000000-0000-4000-8000-000000000008', 'Demo Client Seen', 'demo.client.seen@example.com',
        '{pbkdf2}0da7be77a2095e2d1256884a362ecc07cbeb133048ff08db4960f690fd3d192a9ea91fd5dfd7811a78a1a47964050a7d',
        'CLIENT', '00000000-0000-4000-8000-000000000001', 'Europe/Warsaw', true, now(), now(), now(), 0),
       ('00000000-0000-4000-8000-000000000009', 'Demo Client Finished', 'demo.client.finished@example.com',
        '{pbkdf2}0da7be77a2095e2d1256884a362ecc07cbeb133048ff08db4960f690fd3d192a9ea91fd5dfd7811a78a1a47964050a7d',
        'CLIENT', '00000000-0000-4000-8000-000000000001', 'Europe/Warsaw', true, now(), now(), now(), 0),
       -- their drafts are in R__demo_plans.sql: one built automatically, one that failed to be
       ('00000000-0000-4000-8000-000000000010', 'Demo Client Built', 'demo.client.built@example.com',
        '{pbkdf2}0da7be77a2095e2d1256884a362ecc07cbeb133048ff08db4960f690fd3d192a9ea91fd5dfd7811a78a1a47964050a7d',
        'CLIENT', '00000000-0000-4000-8000-000000000001', 'Europe/Warsaw', true, now(), now(), now(), 0),
       ('00000000-0000-4000-8000-000000000011', 'Demo Client Failed', 'demo.client.failed@example.com',
        '{pbkdf2}0da7be77a2095e2d1256884a362ecc07cbeb133048ff08db4960f690fd3d192a9ea91fd5dfd7811a78a1a47964050a7d',
        'CLIENT', '00000000-0000-4000-8000-000000000001', 'Europe/Warsaw', true, now(), now(), now(), 0),
       -- the second trainer's client, with a built draft, so isolation of the approvals list can be tried
       ('00000000-0000-4000-8000-000000000020', 'Demo Client Other', 'demo.client.other@example.com',
        '{pbkdf2}0da7be77a2095e2d1256884a362ecc07cbeb133048ff08db4960f690fd3d192a9ea91fd5dfd7811a78a1a47964050a7d',
        'CLIENT', '00000000-0000-4000-8000-000000000004', 'Europe/Warsaw', true, now(), now(), now(), 0),
       -- a built draft that is being built again (R__demo_plans.sql)
       ('00000000-0000-4000-8000-000000000021', 'Demo Client Rebuilding', 'demo.client.rebuilding@example.com',
        '{pbkdf2}0da7be77a2095e2d1256884a362ecc07cbeb133048ff08db4960f690fd3d192a9ea91fd5dfd7811a78a1a47964050a7d',
        'CLIENT', '00000000-0000-4000-8000-000000000001', 'Europe/Warsaw', true, now(), now(), now(), 0)
on conflict do nothing;

-- databases created before V10 and V11 hold the demo client without a trainer and consent
update accounts
set trainer_id = coalesce(trainer_id, '00000000-0000-4000-8000-000000000001'),
    health_data_consent_at = coalesce(health_data_consent_at, now())
where id = '00000000-0000-4000-8000-000000000003' and (trainer_id is null or health_data_consent_at is null);

-- a plan with a session on Monday to Saturday, so the Today screen shows a session on almost every day
insert into accounts (id, name, email, password_hash, role, trainer_id, time_zone, email_confirmed,
                      health_data_consent_at, created_at, updated_at, version)
values ('00000000-0000-4000-8000-000000000012', 'Demo Client Daily', 'demo.client.daily@example.com',
        '{pbkdf2}0da7be77a2095e2d1256884a362ecc07cbeb133048ff08db4960f690fd3d192a9ea91fd5dfd7811a78a1a47964050a7d',
        'CLIENT', '00000000-0000-4000-8000-000000000001', 'Europe/Warsaw', true, now(), now(), now(), 0)
on conflict do nothing;

-- a plan in its last week with a session on Monday only, so from Tuesday on Today shows a rest day with nothing next
insert into accounts (id, name, email, password_hash, role, trainer_id, time_zone, email_confirmed,
                      health_data_consent_at, created_at, updated_at, version)
values ('00000000-0000-4000-8000-000000000013', 'Demo Client Closing', 'demo.client.closing@example.com',
        '{pbkdf2}0da7be77a2095e2d1256884a362ecc07cbeb133048ff08db4960f690fd3d192a9ea91fd5dfd7811a78a1a47964050a7d',
        'CLIENT', '00000000-0000-4000-8000-000000000001', 'Europe/Warsaw', true, now(), now(), now(), 0)
on conflict do nothing;
