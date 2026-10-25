alter table accounts
    add column units    varchar(2) not null default 'KG',
    add column language varchar(2) not null default 'EN',
    add column theme    varchar(6) not null default 'SYSTEM',
    add constraint ck_accounts_units check (units in ('KG', 'LB')),
    add constraint ck_accounts_language check (language in ('EN', 'PL')),
    add constraint ck_accounts_theme check (theme in ('LIGHT', 'DARK', 'SYSTEM'));
