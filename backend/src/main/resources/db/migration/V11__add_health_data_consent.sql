alter table accounts
    add column health_data_consent_at timestamptz;

-- ponytail: not valid only because local databases hold the demo client from before; run
-- alter table accounts validate constraint ck_accounts_client_consent once every database has run the updated demo data
alter table accounts
    add constraint ck_accounts_client_consent check ((role = 'CLIENT') = (health_data_consent_at is not null)) not valid;
