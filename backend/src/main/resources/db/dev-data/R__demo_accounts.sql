-- local profile only; both accounts use the demo password Kinetiq-demo-1 (see README)
insert into accounts (id, name, email, password_hash, role, time_zone, email_confirmed, created_at, updated_at, version)
values ('00000000-0000-4000-8000-000000000001', 'Demo Trainer', 'demo.trainer@example.com',
        '{pbkdf2}0da7be77a2095e2d1256884a362ecc07cbeb133048ff08db4960f690fd3d192a9ea91fd5dfd7811a78a1a47964050a7d',
        'TRAINER', 'Europe/Warsaw', true, now(), now(), 0),
       ('00000000-0000-4000-8000-000000000002', 'Demo Unconfirmed', 'demo.unconfirmed@example.com',
        '{pbkdf2}0da7be77a2095e2d1256884a362ecc07cbeb133048ff08db4960f690fd3d192a9ea91fd5dfd7811a78a1a47964050a7d',
        'TRAINER', 'Europe/Warsaw', false, now(), now(), 0)
on conflict do nothing;
