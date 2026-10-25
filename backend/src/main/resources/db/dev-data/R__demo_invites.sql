-- local profile only; times are relative to when this first runs, so recreate the database for fresh invites
-- the demo trainer has one invite sent today, one on its last day, one expired and one revoked;
-- the unconfirmed trainer's open invite shows that another trainer's invite answers 404
insert into invites (id, trainer_id, email, status, email_token_hash, link_token_hash, sent_at, expires_at,
                     created_at, updated_at, version)
select id::uuid, trainer_id::uuid, email, status,
       encode(sha256(convert_to('demo-invite-' || n || '-email', 'UTF8')), 'hex'),
       encode(sha256(convert_to('demo-invite-' || n || '-link', 'UTF8')), 'hex'),
       now() - age, now() - age + interval '7 days', now(), now(), 0
from (values (1, '00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000001',
              'julia.nowicka@example.com', 'OPEN', interval '0'),
             (2, '00000000-0000-4000-8000-000000000102', '00000000-0000-4000-8000-000000000001',
              'adam.wrobel@example.com', 'OPEN', interval '6 days 2 hours'),
             (3, '00000000-0000-4000-8000-000000000103', '00000000-0000-4000-8000-000000000001',
              'kasia.lis@example.com', 'OPEN', interval '8 days'),
             (4, '00000000-0000-4000-8000-000000000104', '00000000-0000-4000-8000-000000000001',
              'piotr.zielinski@example.com', 'REVOKED', interval '1 day'),
             (5, '00000000-0000-4000-8000-000000000105', '00000000-0000-4000-8000-000000000002',
              'ewa.mazur@example.com', 'OPEN', interval '1 day'))
         as demo (n, id, trainer_id, email, status, age)
on conflict do nothing;
