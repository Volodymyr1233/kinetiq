alter table accounts
    add column trainer_id uuid,
    add constraint fk_accounts_trainer foreign key (trainer_id) references accounts (id);

-- ponytail: not valid only because local databases hold the demo client from before; run
-- alter table accounts validate constraint ck_accounts_client_trainer once every database has run the updated demo data
alter table accounts
    add constraint ck_accounts_client_trainer check ((role = 'CLIENT') = (trainer_id is not null)) not valid;

create index ix_accounts_trainer_id on accounts (trainer_id);
