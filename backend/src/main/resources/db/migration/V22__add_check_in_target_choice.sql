-- the lighter targets of a recovery day are derived from the plan, the answers and the unit, so only the choice is kept
alter table check_ins
    add column target_choice varchar(8) not null default 'LIGHTER',
    add constraint ck_check_ins_target_choice check (target_choice in ('LIGHTER', 'ORIGINAL'));
