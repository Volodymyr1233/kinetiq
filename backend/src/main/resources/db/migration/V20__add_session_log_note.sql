-- what the client wrote for their trainer after finishing; one value per session, so it stays in this table
alter table session_logs
    add column note varchar(500),
    add constraint ck_session_logs_note_not_blank check (note is null or trim(note) <> '');
