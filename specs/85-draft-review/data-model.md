# Data Model: Trainer reviews and publishes a drafted plan

## Migration `V18__add_plan_rebuild.sql`

### `plan_draftings` (changed)

| Column | Type | Notes |
|---|---|---|
| `rebuild` | boolean, not null, default false | `true` when the trainer asked for a new draft over an existing plan. The result then replaces the stored draft in place or, on failure, changes nothing. |

Rows written by a quiz submit stay false, so nothing of spec 79 changes.

### `plan_build_requests` (new)

| Column | Type | Notes |
|---|---|---|
| `id` | uuid, primary key | created in the application |
| `trainer_id` | uuid, not null | foreign key to `accounts` (`fk_plan_build_requests_trainer`) |
| `requested_at` | timestamptz, not null | |

- Index `ix_plan_build_requests_trainer_requested_at` on `(trainer_id, requested_at)`: the count reads one trainer's last 24 hours.
- Rows hold no client, no content and no outcome. A row is created when a build is accepted and deleted when it is older than 24 hours at the next request.
- Normal form: every column depends on the key; the trainer is the only entity referred to.

## Domain

| Type | Change |
|---|---|
| `Plan` | New `rebuiltWith(goal, weeks, sessions)`: returns this draft with new content, the mark `BUILT`, the same id and client, complete (`requireComplete`), refused when published. The version is raised by the repository's save. |
| `DraftedPlan` (new record, `plan.domain.model`) | `client` (`AccountId`), `clientName`, `kind` (`BUILT`, `FAILED`, `SAVED`), `weeks`, `sessionsPerWeek`, `createdAt`. A read model, never stored by the domain. |
| `DraftingKind` (new enum) | `FIRST`, `REBUILD`. What `PlanDraftingRepository.remove` reports for the row it removed. |
| `PlanDraftingRepository` | `start(client, at, kind)`; `remove(client)` returns `Optional<DraftingKind>` (empty when there was none); new `exists(client)`. |
| `PlanBuildRequests` (new port) | `lock(trainer)` (transaction-scoped), `since(trainer, instant)` returns the request times oldest first, `record(trainer, at)` also deleting rows before the window. |
| `PlanRebuildLimitReachedException` | Kind `TOO_MANY_ATTEMPTS`, code `plan.rebuild-limit-reached`, property `availableAt`. |

## State changes

```text
first drafting:  (no plan, row FIRST)  --built-->   draft BUILT
                                       --failed-->  draft FAILED (empty)
rebuild:         (draft, row REBUILD)  --built-->   same draft, new content, BUILT, version + 1
                                       --failed-->  same draft, same version, row gone
                                       --overdue--> same as failed
```

## Rules

- Build again is refused, in this order: unknown or foreign client `404`; no plan `404 plan.not-found`; published `409 plan.already-published`; a drafting row exists `409 plan.drafting`; 10 requests in 24 hours `429`. Only a request that passes all of them is recorded.
- The record and the drafting row are written in one transaction; the job starts only after it commits.
- `storeBuilt` for a rebuild is dropped when the plan has meanwhile been published, the answers are gone, or the trainer has changed.
- Age of a draft is the plan's creation time; it does not change on a rebuild or a save.
- The client's own quiz status counts only rows with `rebuild = false`; the trainer's client list and Plan tab count both kinds (research R10).
