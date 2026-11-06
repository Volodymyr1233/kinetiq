# Data model: First plan is drafted from the quiz

## Storage

### `plan_draftings` (new)

A row means a client's first plan is being drafted. It is written in the submit's transaction and removed in the transaction that stores the result.

| Column | Type | Rules |
|---|---|---|
| `client_id` | uuid | Primary key, foreign key to `accounts` |
| `started_at` | timestamptz | Not null; the sweep compares it with the limit |

An index on `started_at` is not needed: the table holds only drafts in progress.

### `plans` (changed)

| Column | Type | Rules |
|---|---|---|
| `automatic_draft` | varchar(16), null | `BUILT` or `FAILED`; null for a plan the trainer has saved |

Constraints:

- `ck_plans_automatic_draft`: the value is `BUILT`, `FAILED` or null.
- `ck_plans_automatic_draft_unpublished`: `first_week is null or automatic_draft is null`, because a published plan has no mark.

Existing rows get null, so nothing changes for them.

`client_seen_at` (spec 75) is unrelated: it is set only on a published plan and is left as it is by a save.

The migration is `V16__add_plan_drafting.sql`, after `V15__add_plan_seen.sql` on `master`. Check again before opening the pull request.

## Domain

### `AutomaticDraft` (new enum, `plan.domain.model`)

`BUILT`, `FAILED`.

### `Plan` (changed)

Gains an optional `automaticDraft`.

| Operation | Result |
|---|---|
| `Plan.built(client, goal, weeks, sessions)` | A draft marked `BUILT`; complete by construction, checked with `requireComplete()` |
| `Plan.failed(client, goal, weeks)` | An empty draft marked `FAILED` |
| `edit(...)` | Clears the mark, because the plan is now the trainer's |
| `publish(...)` | Clears the mark |

### `GeneratedPlan` (new record, `plan.domain.model`)

What the model answered, not yet trusted: `sessions[]` of `name` and `entries[]`, each with `exerciseKey`, `sets`, `amount`, `loadKg` (null when absent), `rpe`, `restSeconds`. Values are boxed so a missing one is a rejection and not a zero.

### `DraftInput` (new record, `plan.domain.input`)

What a prompt and its checks need, read in one short transaction: the `QuizSubmission`, the trainer's `AccountId`, the client's and the trainer's names, the client's `Language` and the trainer's library. The names are used only to clean text; they are never placed in a prompt. The trainer's id tells a result to drop when the client's trainer has changed.

### `QuizStatus` (changed)

```text
NOT_SUBMITTED -> DRAFTING_PLAN -> WAITING_FOR_PLAN -> PLAN_PUBLISHED
```

| Status | Condition, first match wins |
|---|---|
| `NOT_SUBMITTED` | No submission |
| `PLAN_PUBLISHED` | The client's plan is published |
| `DRAFTING_PLAN` | A `plan_draftings` row exists |
| `WAITING_FOR_PLAN` | Otherwise |

`submit` returns `DRAFTING_PLAN`.

### `PlanState` (changed)

| State | Condition, first match wins | Text |
|---|---|---|
| `PLAN_DRAFTING` | A `plan_draftings` row exists | Plan being drafted |
| `PUBLISHED` | A plan with a first week | Published |
| `DRAFT_FAILED` | A draft marked `FAILED` | Draft failed |
| `PLAN_TO_REVIEW` | A draft marked `BUILT` | Plan to review |
| `DRAFT` | Any other draft | Draft |
| `PLAN_TO_REVIEW` | Answers and no plan, from before this story | Plan to review |
| `NO_QUIZ` | No answers | No quiz yet |

## Drafting lifecycle

```text
submit ──> row written ──> job runs ──┬─ valid answer ───────> Plan.built, row removed
                                      ├─ 2 failed attempts ──> Plan.failed, row removed
                                      └─ no key ─────────────> Plan.failed, row removed
restart ──> row older than 5 min 30 s ──> sweep: Plan.failed, row removed
client already has a plan ──> nothing stored, row removed
answers or trainer gone, or trainer changed ──> nothing stored, row removed
row gone when the job finishes ──> result dropped
```

## Rejection codes

Each check of FR-015 to FR-017 gives one code. The second attempt turns codes into sentences ([contracts/prompt.md](contracts/prompt.md)). The codes appear in logs and in the retry, never in an API response.

| Code | Breach |
|---|---|
| `draft.session-count` | Not as many sessions as days per week |
| `draft.session-day` | A session without a day of the week in capitals, or two sessions on one day |
| `draft.entry-count` | Fewer than 4 or more than 10 entries in a session |
| `draft.unknown-exercise` | A key outside the catalogue, or an exercise no longer in the library |
| `draft.repeated-exercise` | The same exercise twice in one session |
| `draft.session-name` | Blank, over 40 characters, not unique, or holding a link or email address |
| `draft.sets` | Outside 1–8 |
| `draft.amount` | Outside 1–50 repetitions or 5–3600 seconds, as the exercise measures |
| `draft.rpe` | Outside 5–9, or outside 5–8 for a client aged 16–17 or 60 and over |
| `draft.rest` | Outside 0–300 seconds |
| `draft.load` | Not empty, and not above 0 with one decimal at most, up to twice the client's body weight |
| `draft.missing-value` | A required value absent |
| `draft.plan-rule` | Any plan rule of spec 68 that the above did not catch |

## Demo data

Local-only, in `db/dev-data`:

- `demo.client.built@example.com`, with a quiz and a draft marked `BUILT`, holding three sessions.
- `demo.client.failed@example.com`, with a quiz and an empty draft marked `FAILED`.

The existing demo clients stay as they are.
