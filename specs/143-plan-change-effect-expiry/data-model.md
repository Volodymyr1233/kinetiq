# Data Model: Plan change takes effect or expires

## Plan change (existing aggregate, extended)

| Field | Type | Notes |
|---|---|---|
| `state` | existing enum | unchanged: `PENDING`, `ACCEPTED_BY_ONE`, `APPLIED`, `DECLINED`, `EXPIRED` |
| `tookEffectAt` | instant, optional | set once, when the plan was revised from this change; only for `APPLIED` |
| `expiryCause` | enum `UNDECIDED`, `PLAN_EDITED`, optional | set only for `EXPIRED` |
| `endedAt` | existing | set when the plan change ends: both acceptances (applied), decline, or expiry |

Derived (never stored): **upcoming** = `APPLIED` and no `tookEffectAt`; **in effect** = `APPLIED` and `tookEffectAt` set.

### Transitions added

| From | To | By | Condition |
|---|---|---|---|
| `PENDING`, `ACCEPTED_BY_ONE` | `EXPIRED` (`UNDECIDED`) | the step | effective Monday has begun in the client's zone |
| `APPLIED`, not in effect | `APPLIED`, in effect | the step | effective Monday has begun, content still valid, plan revised |
| `APPLIED`, not in effect | `EXPIRED` (`PLAN_EDITED`) | the step | effective Monday has begun, content no longer valid or plan gone |

Declined, expired and in-effect plan changes are final. The step does nothing to them.

### Validation (database)

- `ck_plan_changes_took_effect_applied`: `took_effect_at is null or state = 'APPLIED'`.
- `ck_plan_changes_expiry_cause`: `(state = 'EXPIRED') = (expiry_cause is not null)`; values `UNDECIDED`, `PLAN_EDITED`.
- Existing: one open plan change per client; extended so an upcoming change also counts (the unique partial index covers `PENDING`, `ACCEPTED_BY_ONE`, and `APPLIED` with `took_effect_at is null`).
- Index for the step: `(effective_week)` where the plan change is open or upcoming.

## Plan revision (new, in the plan feature)

The sessions of a plan as they were before an applied plan change.

| Table | Columns |
|---|---|
| `plan_revisions` | `id`, `plan_id` (fk), `valid_until` (a Monday: the effective week it ends before), `created_at`; unique `(plan_id, valid_until)` |
| `plan_revision_sessions` | `id`, `revision_id` (fk), `day_of_week`, `name`; unique `(revision_id, day_of_week)` |
| `plan_revision_session_entries` | `session_id` (fk), `position`, `exercise_id` (fk), `sets`, `amount`, `load_kg`, `rpe`, `rest_seconds`; same checks as `plan_session_entries` |

Rows are written once and never updated. Foreign keys cascade on delete, like the live plan tables after `V27__erase_with_account.sql`, so deleting an account removes its revisions.

## Relationships

- A plan has zero or more revisions; the live sessions are the newest state.
- A plan change that took effect corresponds to one revision (same effective Monday).
- No new link from plan change to revision: the pair is found through the client's plan and the Monday.

## Demo data (local-only Flyway location)

`R__demo_plan_changes.sql` gains: an applied plan change not yet in effect, one in effect with its revision, an expired one for each cause.
