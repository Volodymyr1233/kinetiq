# Data model: Trainer and client decide on a plan change

Extends `plan_changes` (migration `V24`). New migration `V25__add_plan_change_decisions.sql` (re-check `master` and open pull requests for a taken number before the pull request).

## `plan_changes` — new columns

| Column | Type | Notes |
|---|---|---|
| `trainer_accepted_at` | timestamptz, null | set when the trainer accepts; cleared by their undo |
| `client_accepted_at` | timestamptz, null | same for the client |
| `declined_by` | varchar(7), null | `TRAINER` or `CLIENT` |
| `decline_reason` | varchar(9), null | `TOO_EASY`, `TOO_HARD`, `SCHEDULE`, `INJURY`, `OTHER` |
| `decline_note` | varchar(500), null | plain text; the same text is posted as a message |

Constraints (named):

- `ck_plan_changes_declined_by`, `ck_plan_changes_decline_reason`: value lists above.
- `ck_plan_changes_decision_matches_state`:
  - `PENDING`: no acceptance, no decline columns;
  - `ACCEPTED_BY_ONE`: exactly one `*_accepted_at` set;
  - `APPLIED`: both set;
  - `DECLINED`: `declined_by` and `decline_reason` set (acceptances may remain as history);
  - `EXPIRED`: decline columns empty.
- `ck_plan_changes_decline_note_with_decline`: `decline_note` only with a decline.
- Existing: unique open index per client (`PENDING`, `ACCEPTED_BY_ONE`), `ended_at` index.
- `ended_at` is set on `APPLIED` and `DECLINED` (decision time) and, by #143, on `EXPIRED`.

Existing rows are all `PENDING` (or ended without decision data for `EXPIRED`), so the checks hold; no backfill. Demo data (`R__demo_plan_changes.sql`) gains one accepted-by-trainer, one applied and one declined plan change for demo clients, idempotent like the existing inserts.

## Domain model (backend, `planchange.domain.model`)

- **`Side`** (new enum): `TRAINER`, `CLIENT`.
- **`DeclineReason`** (new enum): `TOO_EASY`, `TOO_HARD`, `SCHEDULE`, `INJURY`, `OTHER`.
- **`Decision`** (new record, held by a declined plan change): `Side by`, `DeclineReason reason`, `String note` (nullable, ≤ 500 after trimming).
- **`PlanChange`** (changed): adds `Instant trainerAcceptedAt`, `Instant clientAcceptedAt`, `Decision decision`.
  - `accept(Side, Instant now, LocalDate today)` — refused when ended (`PlanChangeEndedException`) or `today >= effectiveWeek` (`PlanChangeWeekBegunException`); the second accept by the same side is a no-op.
  - `undo(Side)` — only while `ACCEPTED_BY_ONE` and the side is the acceptor; otherwise `PlanChangeNothingToUndoException` (ended: `PlanChangeEndedException`).
  - `decline(Side, DeclineReason, String note, Instant now, LocalDate today)` — from `PENDING` or `ACCEPTED_BY_ONE` to `DECLINED`, sets `endedAt`.
  - `acceptedBy(Side)` — read helper for views.
- Transition table:

| From | accept (side) | undo (side) | decline |
|---|---|---|---|
| PENDING | → ACCEPTED_BY_ONE | refused (nothing to undo) | → DECLINED |
| ACCEPTED_BY_ONE (A) | A again: no-op; other side: → APPLIED | A: → PENDING; other: refused | → DECLINED |
| APPLIED, DECLINED, EXPIRED | refused (ended) | refused (ended) | refused (ended) |

## Output ports (`planchange.domain.output`)

- `PlanChangeRepository` gains `findForUpdate(PlanChangeId)`, `update(PlanChange)` (saves decision columns and state), `listFor(trainer-owned clients, ...)` is a read model, not a repository method: see below.
- New read port **`PlanChangeApprovals`**: `List<ApprovalRow> forTrainer(AccountId trainer, Instant endedSince)` — one JDBC query over `plan_changes` joined to the trainer's clients (`accounts.trainer_id = :trainer`), open ones plus those with `ended_at >= endedSince`, newest first. Follows `JdbcPlanSource`/`JdbcTrainingHistory`.
- `PlanChangeRepository.findEnded(AccountId client, int limit)` for the client page history (newest first).

## Input ports (`planchange.domain.input`)

- `DecidePlanChangeUseCase`: `accept`, `undo`, `keepCurrent` for a trainer and for a client — each takes the signed-in `AccountId`, the `PlanChangeId` and (keep) `DeclineReason` + note, and returns the updated `PlanChangeView`/`ClientPlanChangeView`.
- `ViewPlanChangeUseCase` (trainer, existing) gains `ended(trainer, client)` (last 5) and `approvals(trainer)`.
- `ViewOwnPlanChangeUseCase` (client, new): `current(client)` and `get(client, id)`.

## Views (what the responses carry, beyond #131)

- `acceptedByTrainer`, `acceptedByClient` (booleans) and `declinedBy`, `declineReason`, `declineNote` (null unless declined), `endedAt`.
- `counterpartName` for the client's view (the trainer's name) and `clientName` for the trainer's (existing).
- Approval row: `id`, `clientId`, `clientName`, `title`, `reason`, `state`, `acceptedByTrainer`, `acceptedByClient`, `declinedBy`, `declineReason`, `effectiveWeek`, `createdAt`, `endedAt`.

## Frontend state

- Server state only (TanStack Query): `['plan-changes', id]`, `['plan-changes', 'approvals']`, `['clients', id, 'plan-changes', 'ended']`, client `['plan', 'change', 'current']`. A mutation sets the returned plan change as the cached detail and invalidates the lists, nav count, and for decline the conversation queries (a message was added).
- Local state: the keep-current form (reason, note) and its open flag.
