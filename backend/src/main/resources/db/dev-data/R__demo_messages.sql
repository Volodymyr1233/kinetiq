-- local profile only; times are relative to when this first runs, so recreate the database for fresh ones
-- the demo trainer has two unread messages from the demo client, and the waiting client has two from the trainer;
-- the waiting client's conversation is long enough for a second page; the second demo trainer has none
insert into conversations (id, client_id, trainer_id, created_at, updated_at, version)
values ('00000000-0000-4000-8000-000000000501', '00000000-0000-4000-8000-000000000003',
        '00000000-0000-4000-8000-000000000001', now() - interval '2 days', now(), 0),
       ('00000000-0000-4000-8000-000000000502', '00000000-0000-4000-8000-000000000005',
        '00000000-0000-4000-8000-000000000001', now() - interval '2 days', now(), 0)
on conflict do nothing;

insert into messages (id, conversation_id, sender_id, text, received_at, created_at, updated_at, version)
values ('00000000-0000-4000-8000-000000000601', '00000000-0000-4000-8000-000000000501',
        '00000000-0000-4000-8000-000000000001', 'Welcome. Write here whenever a session raises a question.',
        now() - interval '2 days', now(), now(), 0),
       ('00000000-0000-4000-8000-000000000602', '00000000-0000-4000-8000-000000000501',
        '00000000-0000-4000-8000-000000000003', 'Thanks. Should I warm up before the first set of squats?',
        now() - interval '2 days' + interval '20 minutes', now(), now(), 0),
       ('00000000-0000-4000-8000-000000000603', '00000000-0000-4000-8000-000000000501',
        '00000000-0000-4000-8000-000000000001', 'Yes, two light sets with the empty bar are enough.',
        now() - interval '1 day' - interval '2 hours', now(), now(), 0),
       ('00000000-0000-4000-8000-000000000604', '00000000-0000-4000-8000-000000000501',
        '00000000-0000-4000-8000-000000000003', 'That helped, the last set felt much easier.',
        now() - interval '2 hours', now(), now(), 0),
       ('00000000-0000-4000-8000-000000000605', '00000000-0000-4000-8000-000000000501',
        '00000000-0000-4000-8000-000000000003', 'Can we move Thursday to Friday this week?',
        now() - interval '1 hour', now(), now(), 0)
on conflict do nothing;

insert into messages (id, conversation_id, sender_id, text, received_at, created_at, updated_at, version)
select ('00000000-0000-4000-8000-' || lpad((1000 + n)::text, 12, '0'))::uuid,
       '00000000-0000-4000-8000-000000000502',
       case when n % 3 = 0 or n > 58 then '00000000-0000-4000-8000-000000000001'::uuid
            else '00000000-0000-4000-8000-000000000005'::uuid end,
       'Note ' || n || ' about this week''s sessions.',
       now() - interval '2 days' + n * interval '45 minutes', now(), now(), 0
from generate_series(1, 60) as n
on conflict do nothing;

insert into conversation_reads (conversation_id, account_id, last_read_at)
values ('00000000-0000-4000-8000-000000000501', '00000000-0000-4000-8000-000000000001', now() - interval '1 day'),
       ('00000000-0000-4000-8000-000000000501', '00000000-0000-4000-8000-000000000003', now()),
       ('00000000-0000-4000-8000-000000000502', '00000000-0000-4000-8000-000000000001', now()),
       ('00000000-0000-4000-8000-000000000502', '00000000-0000-4000-8000-000000000005',
        now() - interval '2 days' + 58 * interval '45 minutes' + interval '1 minute')
on conflict do nothing;
