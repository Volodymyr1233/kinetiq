-- erasing an account is one delete of its row: everything that exists only because of it goes with it (spec 167,
-- FR-008, FR-009); fk_accounts_trainer, fk_conversations_trainer and every key to exercises keep no action, the first
-- one so that a trainer with clients cannot be deleted, even when a client joins at the same moment

alter table exercises
    drop constraint fk_exercises_owner,
    add constraint fk_exercises_owner foreign key (owner_id) references accounts (id) on delete cascade;

alter table invites
    drop constraint fk_invites_trainer,
    add constraint fk_invites_trainer foreign key (trainer_id) references accounts (id) on delete cascade;

alter table quiz_submissions
    drop constraint fk_quiz_submissions_client,
    add constraint fk_quiz_submissions_client foreign key (client_id) references accounts (id) on delete cascade;

alter table quiz_submission_likes
    drop constraint fk_quiz_submission_likes_submission,
    add constraint fk_quiz_submission_likes_submission foreign key (submission_id)
        references quiz_submissions (id) on delete cascade;

alter table plans
    drop constraint fk_plans_client,
    add constraint fk_plans_client foreign key (client_id) references accounts (id) on delete cascade;

alter table plan_sessions
    drop constraint fk_plan_sessions_plan,
    add constraint fk_plan_sessions_plan foreign key (plan_id) references plans (id) on delete cascade;

alter table plan_session_entries
    drop constraint fk_plan_session_entries_session,
    add constraint fk_plan_session_entries_session foreign key (session_id)
        references plan_sessions (id) on delete cascade;

alter table plan_draftings
    drop constraint fk_plan_draftings_client,
    add constraint fk_plan_draftings_client foreign key (client_id) references accounts (id) on delete cascade;

alter table plan_build_requests
    drop constraint fk_plan_build_requests_trainer,
    add constraint fk_plan_build_requests_trainer foreign key (trainer_id) references accounts (id) on delete cascade;

alter table session_logs
    drop constraint fk_session_logs_client,
    add constraint fk_session_logs_client foreign key (client_id) references accounts (id) on delete cascade;

alter table session_log_entries
    drop constraint fk_session_log_entries_session_log,
    add constraint fk_session_log_entries_session_log foreign key (session_log_id)
        references session_logs (id) on delete cascade;

alter table set_logs
    drop constraint fk_set_logs_entry,
    add constraint fk_set_logs_entry foreign key (session_log_id, entry_position)
        references session_log_entries (session_log_id, position) on delete cascade;

alter table check_ins
    drop constraint fk_check_ins_client,
    add constraint fk_check_ins_client foreign key (client_id) references accounts (id) on delete cascade;

alter table weekly_checks
    drop constraint fk_weekly_checks_client,
    add constraint fk_weekly_checks_client foreign key (client_id) references accounts (id) on delete cascade;

alter table plan_changes
    drop constraint fk_plan_changes_client,
    add constraint fk_plan_changes_client foreign key (client_id) references accounts (id) on delete cascade;

alter table plan_change_targets
    drop constraint fk_plan_change_targets_plan_change,
    add constraint fk_plan_change_targets_plan_change foreign key (plan_change_id)
        references plan_changes (id) on delete cascade;

alter table plan_change_moves
    drop constraint fk_plan_change_moves_plan_change,
    add constraint fk_plan_change_moves_plan_change foreign key (plan_change_id)
        references plan_changes (id) on delete cascade;

alter table plan_change_figures
    drop constraint fk_plan_change_figures_plan_change,
    add constraint fk_plan_change_figures_plan_change foreign key (plan_change_id)
        references plan_changes (id) on delete cascade;

alter table plan_change_exercises
    drop constraint fk_plan_change_exercises_plan_change,
    add constraint fk_plan_change_exercises_plan_change foreign key (plan_change_id)
        references plan_changes (id) on delete cascade;

alter table conversations
    drop constraint fk_conversations_client,
    add constraint fk_conversations_client foreign key (client_id) references accounts (id) on delete cascade;

alter table messages
    drop constraint fk_messages_conversation,
    add constraint fk_messages_conversation foreign key (conversation_id)
        references conversations (id) on delete cascade,
    drop constraint fk_messages_sender,
    add constraint fk_messages_sender foreign key (sender_id) references accounts (id) on delete cascade,
    drop constraint fk_messages_about_set_log,
    add constraint fk_messages_about_set_log foreign key (about_set_log_id) references set_logs (id) on delete cascade;

alter table conversation_reads
    drop constraint fk_conversation_reads_conversation,
    add constraint fk_conversation_reads_conversation foreign key (conversation_id)
        references conversations (id) on delete cascade,
    drop constraint fk_conversation_reads_account,
    add constraint fk_conversation_reads_account foreign key (account_id) references accounts (id) on delete cascade;
