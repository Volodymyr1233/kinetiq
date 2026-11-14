# API contract: Plan change takes effect or expires

All paths are under `/api/v1`, session cookie authentication, same origin. Errors are problem details with a stable `code`. Existing paths and decision endpoints (story #137) are unchanged. **There is no endpoint that applies or expires a plan change**: the step is internal.

## New: the upcoming plan change

A plan change both people accepted whose effective Monday has not begun yet.

| Method and path | Who | Answers |
|---|---|---|
| `GET /plan/changes/upcoming` | client | `200` with the plan change (same shape as `GET /plan/changes/{id}`); `404` `plan-change.not-found` when there is none, as `GET /plan/changes/current` does |
| `GET /clients/{id}/plan-changes/upcoming` | trainer | `200` with the plan change (same shape as `GET /plan-changes/{id}`); `404` `plan-change.not-found` when there is none; `404` for a client that is not theirs, as the other client paths do |

A third read, for the note on the client's plan: `GET /plan/changes/applied` (client) answers `200` with the client's latest plan change that is in effect (same shape as `GET /plan/changes/{id}`), or `404` `plan-change.not-found` when none is. The trainer reads the in-effect ones from the existing `GET /clients/{id}/plan-changes/ended`.

All are GET, return the stored content (targets with old and new values, removed session, moves, effective week) and the reason; all are authenticated and role-scoped by the existing path groups.

## Changed: plan change responses

Responses of `GET /plan/changes/current`, `/plan/changes/{id}`, `/plan-changes/{id}`, `/clients/{id}/plan-change`, `/clients/{id}/plan-changes/ended` and `/approvals/plan-changes` gain:

| Field | Type | Meaning |
|---|---|---|
| `tookEffectAt` | instant, nullable | when the plan started using the change; set only for `APPLIED` |
| `expiryCause` | `UNDECIDED` or `PLAN_EDITED`, nullable | why it expired; set only for `EXPIRED` |

`state` keeps its five values. The client derives "upcoming" (`APPLIED`, no `tookEffectAt`) and "in effect" (`APPLIED`, `tookEffectAt` set).

The "ended" list and the Approvals "Decided" tab include applied and expired changes of the last 30 days as before. An upcoming change is listed under "Decided" with its Monday until it is in effect.

## Changed: the client's plan

`GET /plan?week=N` keeps its shape. For a week before the effective Monday of an applied plan change, `sessions` are the sessions as they were (from the stored revision); for later weeks, the live ones. `GET /plan/states` follows because it reads the plan by week.

## Refusals

No new refusal. A decision on a plan change that has ended or whose week has begun keeps the codes of story #137 (`plan-change.ended`, `plan-change.week-begun`, `plan-change.nothing-to-undo`).

## Security

- Every endpoint above requires authentication; the client paths are client-only and the trainer paths trainer-only (role rule before ownership).
- Another person's plan change, client or unknown id: `404`, never `403`.
- Nothing in the responses carries the note text of a decline beyond what story #137 already shows; no endpoint accepts a body.
