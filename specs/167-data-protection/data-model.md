# Data model: Personal and health data are protected

No new table and no new column. One migration changes what happens to an account's data when the account goes.

## `V27__erase_with_account.sql` (#170)

Each foreign key below is dropped and added again with the same name and columns plus `on delete cascade`.

| Table | Constraint | References | Why it cascades |
|---|---|---|---|
| `exercises` | `fk_exercises_owner` | `accounts` | a trainer's own exercises go with the trainer |
| `invites` | `fk_invites_trainer` | `accounts` | a trainer's invites go with the trainer |
| `quiz_submissions` | `fk_quiz_submissions_client` | `accounts` | quiz answers |
| `quiz_submission_likes` | `fk_quiz_submission_likes_submission` | `quiz_submissions` | |
| `plans` | `fk_plans_client` | `accounts` | the plan or draft |
| `plan_sessions` | `fk_plan_sessions_plan` | `plans` | |
| `plan_session_entries` | `fk_plan_session_entries_session` | `plan_sessions` | |
| `plan_draftings` | `fk_plan_draftings_client` | `accounts` | a drafting in progress |
| `plan_build_requests` | `fk_plan_build_requests_trainer` | `accounts` | a trainer's daily-limit records |
| `session_logs` | `fk_session_logs_client` | `accounts` | logs |
| `session_log_entries` | `fk_session_log_entries_session_log` | `session_logs` | |
| `set_logs` | `fk_set_logs_entry` | `session_log_entries` | |
| `check_ins` | `fk_check_ins_client` | `accounts` | check-ins |
| `weekly_checks` | `fk_weekly_checks_client` | `accounts` | |
| `plan_changes` | `fk_plan_changes_client` | `accounts` | plan changes |
| `plan_change_targets` | `fk_plan_change_targets_plan_change` | `plan_changes` | |
| `plan_change_moves` | `fk_plan_change_moves_plan_change` | `plan_changes` | |
| `plan_change_figures` | `fk_plan_change_figures_plan_change` | `plan_changes` | |
| `plan_change_exercises` | `fk_plan_change_exercises_plan_change` | `plan_changes` | |
| `conversations` | `fk_conversations_client` | `accounts` | the client's conversation |
| `messages` | `fk_messages_conversation` | `conversations` | both sides' messages |
| `messages` | `fk_messages_sender` | `accounts` | reached through the conversation as well |
| `messages` | `fk_messages_about_set_log` | `set_logs` | a trainer's or client's message about an erased set |
| `conversation_reads` | `fk_conversation_reads_conversation` | `conversations` | |
| `conversation_reads` | `fk_conversation_reads_account` | `accounts` | |

Kept as they are (no action):

- `fk_accounts_trainer`: a trainer with clients cannot be deleted; this key enforces FR-007 also in a race (research R2).
- `fk_conversations_trainer`: a trainer with a conversation has a client, so it never blocks a permitted deletion.
- Every key to `exercises` from plan, log and plan-change rows: a trainer's exercises are used only by that trainer's clients, who are gone before the trainer can be.

The migration comment says why the keys cascade: erasure is one delete of the account row (spec FR-008).

## What a deletion erases

- **Client** (FR-008): the `accounts` row and, by cascade, every row in the tables above that belongs to it; in the same transaction, before that, the invites of its trainer for its email (`invites` by `trainer_id` and `email`, research R6). Spring Session rows of the account are deleted by principal name (the account id) afterwards (research R5).
- **Trainer** (FR-009): the `accounts` row and, by cascade, its exercises, invites and plan build requests; its sign-ins as above. Refused while any account has `trainer_id` = the trainer.

## Retention (FR-015, research R7)

| Data | Erased when | Selected by |
|---|---|---|
| Unconfirmed trainer account | 7 days after its last confirmation link was sent | `role = 'TRAINER' and not email_confirmed and confirmation_expires_at < now - 6 days` |
| Used or revoked invite | 1 hour after it ended | `status in ('USED','REVOKED') and updated_at < now - 1 hour` |
| Expired invite | 1 hour after it expired | `status = 'OPEN' and expires_at < now - 1 hour` |

## Domain additions (`account` feature)

- `DeletionCheck` (record in `domain.input`): `int clients`, `Optional<String> trainerName`; what `GET /accounts/me/deletion` answers.
- `AccountRepository.delete(AccountId)`; `deleteUnconfirmedTrainers(Instant linkExpiredBefore)` returning the count; `countClientsOf(AccountId trainer)`.
- `InviteRepository.deleteByTrainerAndEmail(AccountId, Email)`; `deleteEnded(Instant before)` returning the count.
- Exceptions: `PasswordIncorrectException` (`account.password-incorrect`, `INVALID`), `TrainerHasClientsException` (`account.has-clients`, `CONFLICT`, property `clients`).

## Demo data

No new entity, so no new demo rows. `demo.unconfirmed@example.com` has no confirmation link, so the sweep, which selects by the link's expiry, keeps it; the demo data needs no change.
