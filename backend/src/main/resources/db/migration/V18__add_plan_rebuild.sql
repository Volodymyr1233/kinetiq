-- true when the trainer asked for a new draft over a stored one: the result replaces it, a failure leaves it
alter table plan_draftings
    add column rebuild boolean not null default false;

-- one row per accepted Build again, kept 24 hours for the trainer's daily limit; it holds no client and no content
create table plan_build_requests (
    id           uuid        not null,
    trainer_id   uuid        not null,
    requested_at timestamptz not null,
    constraint pk_plan_build_requests primary key (id),
    constraint fk_plan_build_requests_trainer foreign key (trainer_id) references accounts (id)
);

create index ix_plan_build_requests_trainer_requested_at on plan_build_requests (trainer_id, requested_at);
