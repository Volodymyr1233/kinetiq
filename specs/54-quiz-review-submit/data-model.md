# Data Model: Client reviews and submits the quiz

The answer codes and rules are spec 50's (`specs/50-client-quiz/data-model.md`, "Option codes" and "Submitted answers"). This file adds what is stored when a client submits. Task #55 refines the migration's number and columns when it starts (plan, "Open items").

## Quiz submission (domain)

| Field | Type | Rule |
|---|---|---|
| `id` | `QuizSubmissionId` (UUID) | created in the application |
| `client` | `AccountId` | the signed-in client; at most one submission per client |
| `goal` | `Goal` | one of the goal codes |
| `experience` | `Experience` | one of the experience codes |
| `level` | `Level` | one of the level codes |
| `age` | `int` | 16–99 |
| `heightCm` | `int` | 120–230 |
| `weightKg` | `BigDecimal`, scale 1 | 30.0–250.0, at most one decimal |
| `likes` | `Set<Like>` | 1–5 distinct codes |
| `daysPerWeek` | `int` | 2–6 |
| `notes` | `Optional<String>` | trimmed; absent when empty after trimming; at most 500 UTF-16 units |
| `submittedAt` | `Instant` | time of the submit |

Every rule is checked in the compact constructor or factory and throws `InvalidValueException` (domain tests per rule, task #55). The aggregate has no methods that change it: a submission never changes (spec FR-009).

## Quiz status (domain)

`QuizStatus`: `NOT_SUBMITTED`, `WAITING_FOR_PLAN`, `PLAN_PUBLISHED`.

| Situation | Status |
|---|---|
| No submission for the client | `NOT_SUBMITTED` |
| A submission, no published plan | `WAITING_FOR_PLAN` |
| A submission and a published plan | `PLAN_PUBLISHED` (never before the plan stories) |

## Table `quiz_submissions` (task #55)

| Column | Type | Constraint |
|---|---|---|
| `id` | `uuid` | `pk_quiz_submissions` |
| `client_id` | `uuid not null` | `fk_quiz_submissions_client` → `accounts(id)`; `uq_quiz_submissions_client` |
| `goal`, `experience`, `level` | `varchar(32) not null` | check constraints with the codes |
| `age` | `smallint not null` | check 16–99 |
| `height_cm` | `smallint not null` | check 120–230 |
| `weight_kg` | `numeric(4,1) not null` | check 30.0–250.0 |
| `days_per_week` | `smallint not null` | check 2–6 |
| `notes` | `varchar(500)` | null when empty |
| `submitted_at` | `timestamptz not null` | |
| `created_at`, `updated_at`, `version` | as `BaseEntity` | |

## Table `quiz_submission_likes` (task #55)

| Column | Type | Constraint |
|---|---|---|
| `submission_id` | `uuid not null` | `fk_quiz_submission_likes_submission` → `quiz_submissions(id)` |
| `like_code` | `varchar(32) not null` | check with the like codes |
| | | `pk_quiz_submission_likes` (`submission_id`, `like_code`) |

## Demo data (task #55)

`db/dev-data/R__demo_quiz.sql`: one submission for the demo client account of story #18, so the waiting state can be seen locally. Idempotent insert.

## Draft in the tab (frontend)

Unchanged from spec 50. Task #57 adds `clearDraft()` after a successful submit (research R11).
