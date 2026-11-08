-- the day a plan was published, in the client's time zone: first_week is only the Monday of that week, and the
-- sessions of that week before the publishing day are not missed ones
alter table plans
    add column published_on date;

-- the Monday is the earliest day an existing plan can have been published on
update plans
set published_on = first_week
where first_week is not null;

alter table plans
    add constraint ck_plans_published_on_with_first_week check ((published_on is null) = (first_week is null)),
    add constraint ck_plans_published_on_in_first_week check (
        published_on is null or published_on between first_week and first_week + 6);
