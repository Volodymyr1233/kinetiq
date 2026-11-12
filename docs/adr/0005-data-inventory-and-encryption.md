# Data inventory, retention and encryption

Kinetiq protects stored data with TLS on every connection (browser to app, app to the database when they are on different hosts, app to the email and AI services), encrypted disks on the database host and its backups, PBKDF2 hashes for passwords and SHA-256 hashes for link secrets, and no encryption of single columns. Column encryption was rejected: the key would live in the same environment as the application, so the threats left after the measures above (a compromised application or database user) could read the columns anyway, while encrypted logs, check-ins and plans could no longer be queried for progress, plan-change rules and the trainer's overview. Data is kept only as long as the table below says; erasing an account is one delete of its row, which the database cascades to everything that exists only because of it (migration `V27`).

## Data inventory

| Data | Tables | Purpose | Lawful basis | Who receives it | Kept |
|---|---|---|---|---|---|
| Account: name, email, password hash, role, time zone, units, language, theme | `accounts` | signing in, showing the person to their trainer or clients | contract, Art. 6(1)(b) | the client's trainer (client's name and email); the trainer's clients (trainer's name); Resend (email address and name in mails) | until the person deletes the account; an unconfirmed trainer account until 7 days after its last confirmation link was sent |
| Consent to process health data, with its time | `accounts.health_data_consent_at` | proof of the client's explicit consent | legal obligation, Art. 6(1)(c) | nobody | with the account |
| Sign-ins | `spring_session`, `spring_session_attributes` | keeping a browser signed in | contract | nobody | until signed out, after 14 days without use, on a password reset or a deletion |
| Confirmation and password reset links | `accounts` (hashes and expiry only) | proving control of an email | contract | Resend (the link in the mail) | until used, replaced or expired (24 hours and 1 hour) |
| Failed sign-ins | memory of the running app | slowing down password guessing | legitimate interest, Art. 6(1)(f) | nobody | 15 minutes |
| Invites: invited email, trainer, state | `invites` | letting a person join a trainer | legitimate interest of the trainer | Resend (email address, trainer's name) | until 1 hour after it was used, revoked or expired; with the trainer's account |
| Quiz answers: goal, experience, level, age, height, body weight, days, likes, notes | `quiz_submissions`, `quiz_submission_likes` | drafting the first plan | explicit consent, Art. 9(2)(a) (health data) | the trainer; OpenRouter (answers without name or email, notes cleaned of known names, emails and phone numbers) | with the client's account |
| Plan and draft | `plans`, `plan_sessions`, `plan_session_entries`, `plan_draftings` | the training the trainer prescribes | contract | the client and their trainer | with the client's account |
| Session logs and set logs, the session note | `session_logs`, `session_log_entries`, `set_logs` | what the client did | explicit consent, Art. 9(2)(a) | the trainer; OpenRouter (figures only, for plan changes) | with the client's account |
| Check-ins: sleep, soreness, stress, readiness choice | `check_ins` | readiness and recovery days | explicit consent, Art. 9(2)(a) | the trainer | with the client's account |
| Weekly checks | `weekly_checks` | when plan-change rules last ran | contract | nobody | with the client's account |
| Plan changes with their content, figures, decisions and decline notes | `plan_changes`, `plan_change_targets`, `plan_change_moves`, `plan_change_figures`, `plan_change_exercises` | suggesting and approving plan changes | explicit consent, Art. 9(2)(a) | the client and their trainer; OpenRouter (figures and targets without names) | with the client's account |
| Conversation and messages, read markers | `conversations`, `messages`, `conversation_reads` | talking between client and trainer | contract | the other person in the conversation | with the client's account |
| A trainer's own exercises | `exercises` (with an owner) | the trainer's exercise library | contract | the trainer's clients (in their plan) | with the trainer's account |
| Plan build requests | `plan_build_requests` | the trainer's daily limit on new drafts | legitimate interest | nobody | 24 hours; with the trainer's account |
| Database backups | the host's backups | recovery | legitimate interest | the hosting provider | the backup period set at deployment; erased data leaves with the oldest backup |

Application logs hold no names, emails or free text (`backend/CLAUDE.md`). Personal data never goes into a prompt (constitution III).
