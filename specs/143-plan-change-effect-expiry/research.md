# Research: Plan change takes effect or expires

No `NEEDS CLARIFICATION` is left. Decisions come from reading the code on `master` (6636b0a).

## 1. Who runs the step and when

- **Decision**: a scheduled input adapter `PlanChangeEffectSweep` in `planchange.infrastructure.input`, every 5 minutes. It asks `ApplyPlanChangesUseCase` for the due plan changes and settles each in its own transaction, logging only the exception class on a failure.
- **Rationale**: same shape as `PlanChangeSweep` (detection) and `PlanDraftingSweep`. Catching up is free because "due" means "effective Monday is not after the client's local date", not "exactly now".
- **Alternatives**: a cron at Monday 00:00 (misses downtime and time zones); applying lazily when someone reads the plan (reads would write, and nobody may read for days); a database job (domain logic outside the domain).

## 2. What "due" means

- **Decision**: due when the state is `PENDING`, `ACCEPTED_BY_ONE`, or `APPLIED` with no `tookEffectAt`, and the effective week is not after the local date of now in the client's time zone. The query joins the account's time zone.
- **Rationale**: decisions are refused when `today >= effectiveWeek` (`PlanChange.requireDecidable`), so the step and the decision rule share the same clock and zone and there is no window where both succeed. The step takes `findForUpdate` on the plan change, so an acceptance in flight serialises with it.
- **Alternatives**: a UTC date (wrong for other zones); a stored due instant (a client who changes zone keeps the old one).

## 3. How a change reaches the plan

- **Decision**: `planchange` reads the client's `PlanSnapshot`, checks the content is still valid (every touched entry exists with the old value, `PlanSnapshot.apply` works, the plan rules hold; the rule's own limits are not run again because they need the logged weeks of the window and passed at creation against the same old values), then calls `RevisePlanUseCase` with the version it read. The plan feature locks the plan, refuses with `plan.changed` when the version moved (a save raced the check; the step retries on the next tick), stores a revision (the sessions as they were, valid before the effective Monday), replaces the sessions and raises the version. This happens in the same transaction as the plan change update.
- **Rationale**: the content logic (`PlanSnapshot.apply`) already lives in `planchange`; the plan feature stays owner of its tables and plan rules (`Session` and `ExerciseEntry` validate on construction). Raising the version makes a trainer's open editor fail its version check instead of saving over the change.
- **Alternatives**: `planchange` writing plan tables (breaks feature privacy); the plan feature importing `planchange` (a dependency cycle); applying only touched rows (the plan rules cannot be checked on a partial update).

## 4. Past weeks keep the old targets

- **Decision**: `plan_revisions` stores, per applied change, the plan's sessions before it (child tables shaped like the live ones) and the Monday it is valid until. `PlanViewService.view(client, week)` uses the oldest revision whose valid-until Monday is after the asked week's Monday, else the live plan. `SessionStateService` already asks for the plan by week, so past weeks show the right sessions and states with no change there.
- **Rationale**: the story says past weeks show the old targets; today the plan is one template for every week, so a change without history would rewrite the past. A snapshot survives later trainer edits (reverting from stored old values would not).
- **Ceiling**: only applied plan changes create revisions; ordinary trainer edits stay retroactive, as today. Overview, rules and statuses read the live template for their windows (`JdbcTrainingRecords`, `JdbcTrainingHistory`): after a removed session, past weeks count as if that session never existed. Accepted; marked with a `ponytail:` comment where those queries read the plan. Upgrade: read revisions there too.
- **Alternatives**: reverse-applying the content on read (breaks after an edit of the same entry); a full week-by-week plan (a rewrite of the plan model); no history (fails the story).

## 4b. Known ceiling: Today's next session

- The Today "rest day, next session" preview reads the live plan, so before the Monday it can show a session that the change then removes. Accepted; it corrects itself when the plan is revised.

## 5. The trainer editor

- **Decision**: the implemented editor (`plan-page.tsx`) edits the live plan and has no week selector, so it only needs an upcoming banner; the mockup's week pills are not built.
- **Rationale**: nothing in this story needs per-week editing.

## 6. Expiry and its cause

- **Decision**: `EXPIRED` gains a cause, `UNDECIDED` (not accepted by both in time) or `PLAN_EDITED` (accepted by both, but the content no longer fits). A nullable column with a check that it is set only for `EXPIRED`. Responses carry it and the UI picks the sentence.
- **Rationale**: the spec shows two sentences; deriving the cause from the acceptance times would be implicit and brittle. A client who has no plan any more, a finished plan, or a plan the content no longer fits all read as "the plan was edited"; one rule, no extra states.
- **Alternatives**: a new state "not applied" (changes the agreed vocabulary and every consumer of the state).

## 7. "In effect"

- **Decision**: `tookEffectAt` (nullable `timestamptz`) is set when the plan was revised. `APPLIED` with it null is "applied, takes effect on Monday"; with it set it is "in effect" (`CONTEXT.md`). Detection treats `APPLIED` with null `tookEffectAt` as still open (`hasOpen`), so no second plan change is made from a plan that is about to change.
- **Rationale**: a plan change made from the pre-change template would have wrong old values on its Monday.
- **Alternatives**: a state `IN_EFFECT` (a third kind of "applied", invalidates the consumers of story #137).

## 8. Upcoming values on the plan screens

- **Decision**: no plan-feature change for display. The screens read the upcoming plan change (a new GET returning the `APPLIED` change with null `tookEffectAt`, content included) and mark targets for weeks at or after its effective week. After the step the plan holds the values and the endpoint answers `404 plan-change.not-found`, like `current`.
- **Rationale**: avoids a `plan` → `planchange` dependency and a second copy of the numbers.
- **Alternatives**: the plan response embedding the change (cycle); the backend returning a ready week (duplicates `PlanSnapshot.apply` for presentation).

## 9. Testing time

- **Decision**: services take the existing `Clock`; tests pass a fixed one. The sweep holds no logic.
- **Rationale**: spec FR-015; precedent in the account services and `PlanChangeDecider`.

## 10. Security surface

- The step has no HTTP surface. New endpoints are GET only, under the existing client and trainer path groups (role rule first), ownership checked in the use case, `404` for another person's id. Log lines carry no ids of people and no text.
