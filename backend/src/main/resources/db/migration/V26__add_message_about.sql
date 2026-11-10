-- the logged set a message is about; no index: nothing looks messages up by set, and a set log is never deleted
alter table messages
    add column about_set_log_id uuid,
    add constraint fk_messages_about_set_log foreign key (about_set_log_id) references set_logs (id);
