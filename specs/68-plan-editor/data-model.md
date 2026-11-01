# Data model: Trainer builds, edits and publishes a plan

Migration `V13__create_plans.sql`. No open pull request takes `V13` (checked 2026-11-01); check again before opening the pull request.

## `plans`

| Column | Type | Rules |
|---|---|---|
| `id` | uuid | primary key |
| `client_id` | uuid | not null, FK `accounts`, unique `uq_plans_client` |
| `goal` | varchar(32) | not null, one of the quiz's goals |
| `weeks` | integer | not null, 4–16 |
| `first_week` | date | null for a draft; a Monday |
| `created_at`, `updated_at`, `version` | | from `BaseEntity` |

The trainer is `accounts.trainer_id` of the client, so it is not stored here.

## `plan_sessions`

| Column | Type | Rules |
|---|---|---|
| `id` | uuid | primary key |
| `plan_id` | uuid | not null, FK `plans` |
| `name` | varchar(60) | not null, not blank |
| `day_of_week` | varchar(9) | not null, `MONDAY`…`SUNDAY` |

Unique `uq_plan_sessions_plan_day` on `(plan_id, day_of_week)`. These rows are replaced with their plan on each save, so they carry no timestamps or version of their own; the plan's version covers them.

## `plan_session_entries`

| Column | Type | Rules |
|---|---|---|
| `session_id` | uuid | FK `plan_sessions`, part of the primary key |
| `position` | integer | part of the primary key, from 0 |
| `exercise_id` | uuid | not null, FK `exercises`, indexed |
| `sets` | integer | 1–20 |
| `amount` | integer | 1–3600; repetitions or seconds, as the exercise measures |
| `load_kg` | numeric(4,1) | null means no added weight; above 0, at most 999.9 |
| `rpe` | integer | 5–10 |
| `rest_seconds` | integer | 0–600 |

## Domain

`plan.domain.model`:

- `Plan`: `id`, `client`, `goal`, `weeks`, `firstWeek` (`Optional`), `sessions`, `version`.
  - `Plan.draft(...)` starts one; `edit(goal, weeks, sessions, today)` and `publish(today)` return the changed plan.
  - `isPublished()`, `currentWeek(today)`, `requireComplete()`.
- `Session`: record of `name`, `day` (`java.time.DayOfWeek`), `entries`. Trims the name.
- `ExerciseEntry`: record of `exercise` (`ExerciseId`), `sets`, `amount`, `loadKg` (null when empty), `rpe`, `restSeconds`.
- `PlanId`: typed id.

## State transitions

```text
(none) --first save, quiz submitted--> draft --publish, complete--> published
draft --save--> draft
published --save, complete, length not shortened below the current week--> published
```

Publishing sets `firstWeek` to the Monday of the current week in the client's time zone. Nothing moves a plan back to draft.

## Rule codes

| Rule | Code | Status |
|---|---|---|
| Foreign or unknown client | `client.not-found` | 404 |
| Own client, no plan | `plan.not-found` | 404 |
| Quiz not submitted | `plan.quiz-not-submitted` | 409 |
| Outdated version, or two first saves | `plan.changed` | 409 |
| Already published | `plan.already-published` | 409 |
| Length shortened below the current week | `plan.weeks-before-current` (property `currentWeek`) | 400 |
| Exercise not in the library | `plan.exercise-not-in-library` | 400 |
| Two sessions on one day | `plan.duplicate-day` | 400 |
| Fewer than 2 sessions when complete | `plan.too-few-sessions` | 400 |
| A session without exercises when complete | `plan.session-without-exercises` | 400 |
| Length, sessions, name, exercises per session, sets, amount, load, RPE, rest out of limits | `plan.weeks-out-of-range`, `plan.too-many-sessions`, `plan.session-name-invalid`, `plan.too-many-exercises`, `plan.sets-out-of-range`, `plan.amount-out-of-range`, `plan.load-out-of-range`, `plan.rpe-out-of-range`, `plan.rest-out-of-range` | 400 |

Through the API, value limits normally answer `validation.failed` with field errors first (research R7); the codes in the last row are for plans built without a request.

## Changes to other features

- `client`: `PlanState` gains `DRAFT` and `PUBLISHED`. `ClientSummary` gains `timeZone`. `JdbcClientDirectory` left-joins `plans`; the goal is `coalesce(plan, quiz)`.
- `quiz`: `QuizSubmissionRepository.hasPublishedPlan`; `status` answers `PLAN_PUBLISHED`.
- `exercise`: `ListExercisesUseCase.library`; `ExerciseRepository.countPlansUsing`; both places that hard-code 0 plans use the real count, except a newly created exercise, which is in no plan.

## Demo data

`R__demo_accounts.sql` gains two clients of Demo Trainer, with quiz answers in `R__demo_quiz.sql`. `R__demo_plans.sql` gives one a draft and the other a published plan of 16 weeks whose first week is 5 weeks before the first run, so it starts in week 6. Demo Client (no quiz) and Demo Client Waiting (quiz, no plan) stay as they are, so all four Plan tab states can be opened.
