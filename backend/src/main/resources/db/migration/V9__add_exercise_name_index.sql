create unique index ux_exercises_owner_name on exercises (owner_id, lower(name)) where owner_id is not null;
