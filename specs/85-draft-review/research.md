# Research: Trainer reviews and publishes a drafted plan

Decisions that the plan rests on. Spec 79's research (providers, prompt, retry) still holds and is not repeated.

## R1. A rebuild is a drafting with a flag

**Decision**: `plan_draftings` gains `rebuild boolean not null default false`. Build again inserts a row with it set; the existing job runs it, the sweep ends it.

**Rationale**: The job, the 6 minute limit, the restart safety and the client state (`PLAN_DRAFTING`) all hang on that row. Reusing it gives them for free. The only difference is what happens to the stored plan when the row is removed.

**Alternatives**: a second table for rebuilds (duplicates the sweep and the "whoever removes the row stores" guard); a separate job type (more code, same behaviour).

## R2. A finished rebuild replaces the plan in place

**Decision**: On success the stored draft keeps its row and id; goal, length, sessions and mark are replaced and the version is raised by the ordinary save. On failure nothing changes. A rebuild result is dropped when the plan has been published, the answers or the trainer have gone, or the row was already removed.

**Rationale**: A raised version is exactly what makes a stale tab's save fail with `plan.changed`. Deleting and inserting would restart the version and could let a stale tab save over the new draft. Keeping the row also keeps the plan's age (spec assumption).

**Alternatives**: delete and insert (version collision); store the new draft elsewhere and swap (second copy of the plan, nothing gained).

## R3. Save and publish are refused while a drafting runs

**Decision**: `PlanManagementService.save` and `publish` look at the client's drafting before changing an existing plan and throw `PlanDraftingInProgressException` (`plan.drafting`, 409). Today only a first save checks it.

**Rationale**: Between Build again and the result the trainer could otherwise save, bump the version, and have the result replace those edits silently. The refusal is cheap and explicit; the read-only editor (R5) means a normal trainer never hits it.

**Alternatives**: let the result lose to a saved version (the trainer's work would win over a rebuild they asked for, and the "replaced" promise breaks); lock the row (does not help across a long drafting).

## R4. The limit: a table, a rolling window and a lock

**Decision**: Table `plan_build_requests (trainer_id, requested_at)`. The domain counts the trainer's rows from the last 24 hours; at 10 it refuses and reports the moment the oldest counted row leaves the window. Count and insert run under `pg_advisory_xact_lock` keyed on the trainer id. Rows older than the window are deleted when a new request is recorded. The constants live in the domain service, not in configuration (constitution VI).

**Rationale**: A persisted count survives restarts, which matters because the limit protects a paid budget. A rolling window gives "can be used again at …" a precise answer without a calendar or time zone. The lock stops two simultaneous requests passing at 9. No library: it is one query and one insert.

**Alternatives**: in-memory counter (lost on restart, wrong with two instances); calendar day (needs a trainer time zone, resets at midnight in a burst); a rate-limit library (a dependency for 20 lines).

## R5. The editor reads the stored draft while a rebuild runs

**Decision**: `GET /clients/{id}/plan` keeps answering with the stored draft. The Plan tab already knows a drafting is running from the client's `planState`; it now also asks for the plan during drafting, and shows the drafting state above it when a plan exists, or alone when the plan query answers `plan.not-found` (first drafting). Editing controls, Save and Publish are disabled while drafting. The tab polls the client every 5 seconds only while the state is on screen, as today.

**Rationale**: No API change is needed to know "plan exists and a drafting runs". The tab is the only consumer.

**Alternatives**: a `drafting` flag on the plan response (a second source of truth for what the client state already says).

## R6. The list is a read model

**Decision**: One SQL query over `plans` joined to `accounts` on the trainer, unpublished plans only, with the client's name, mark, weeks, the count of sessions and the creation time, ordered by creation time and then plan id. The trainer is part of the join condition, so no other trainer's data can come back. Built in the `plan` feature and, as `JdbcClientDirectory` does, it reads another feature's table by SQL without importing its classes.

**Rationale**: Same pattern as the client list; one query, no N+1; a stable order.

**Alternatives**: compose from the client use case plus a plan lookup per client (N+1, and the client state cannot tell "no plan" from "draft" without a plan lookup anyway).

## R7. Quiz answers for the trainer

**Decision**: `ReviewClientAnswersUseCase.answers(trainer, client)` in the `client` feature. It checks ownership with the client feature's own directory (so a foreign or unknown client throws `ClientNotFoundException`) and reads the answers through `quiz`'s existing `ViewQuizAnswersUseCase`. Endpoint `GET /api/v1/clients/{id}/quiz-answers`; `404` with `client.not-found` for another trainer's or unknown client, `404` with `quiz.not-found` when an own client has no answers (the panel then says they are unavailable). The answers are served whatever the plan's state; the panel simply is not shown for a published plan. Terminology: the product says "Build again"; no new CONTEXT term is added.

**Rationale**: `client` already imports `quiz` (`ClientSummary`, `ClientResponse` and `JdbcClientDirectory` use `Goal`), so a call in that direction adds no dependency. The reverse, `quiz` calling `client`, would make a cycle, which constitution I forbids.

**Alternatives**: put the read in `quiz` (cycle with `client`); put it in `plan` (it already depends on both, but answers would leak into a feature that does not own them); embed the answers in the plan response (couples two stories' contracts).

## R8. Detecting a failed rebuild

**Decision**: The editor remembers the plan's version when it showed Build again. When the client state leaves `PLAN_DRAFTING` and the refetched plan has the same version, the rebuild failed: it shows the message of FR-015a. A higher version means it succeeded. Nothing is stored for this.

**Rationale**: The version already means "something changed". A failure is exactly "nothing changed".

**Alternatives**: a persisted result flag (a column and a clearing rule for a message that only matters to a trainer watching).

## R9. The 429

**Decision**: `PlanRebuildLimitReachedException` has kind `TOO_MANY_ATTEMPTS` and code `plan.rebuild-limit-reached`, and carries `availableAt` (ISO instant) through `properties()`, as the handler already supports. The frontend formats it in the trainer's locale.

**Rationale**: Same mechanism as `invite.limit-reached` and sign-in locking; no new handler.

## R10. The client's status ignores a rebuild

**Decision**: `QuizSubmissionService` asks `QuizSubmissionRepository.isPlanDrafting`, whose native query reads `plan_draftings` by client. It becomes `... where client_id = :clientId and not rebuild`.

**Rationale**: The client's waiting screen turns into "building your plan" on any row. A rebuild would otherwise take a client who is waiting for review back to building, and show that their draft is being redone, against spec 79 (FR-003, FR-029). The trainer-side state (`JdbcClientDirectory`) keeps counting rebuild rows, which is intended.

**Alternatives**: leave it (the client sees a flip with no meaning to them); a second status for the client (a new state for nothing).
