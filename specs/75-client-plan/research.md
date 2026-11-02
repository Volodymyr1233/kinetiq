# Research: Client sees their plan

Decisions taken while planning. Each: what, why, what else was considered.

## R1. Where the client's read lives

**Decision**: In the existing `plan` feature, as a second use case `ViewOwnPlanUseCase` beside `ManagePlanUseCase`, with its own controller and response.

**Why**: It reads the same aggregate and reuses the session and entry response records. A separate feature would need the plan's types across a feature boundary and would risk a dependency cycle.

**Rejected**: Adding the client case to `ManagePlanUseCase` (it is the trainer's, takes a trainer id and checks ownership; mixing roles in one port is confusing); a new `clientplan` feature (a cycle with `plan`).

## R2. Address of the endpoints

**Decision**: `GET /api/v1/plan?week=N` and `POST /api/v1/plan/acknowledgement` (204). The role CLIENT rule covers `/api/v1/plan/**`.

**Why**: The plan is the client's own, so no id is in the path (FR-002); there is no way to name another client's plan. The trainer's plan stays under `/api/v1/clients/{id}/plan`. The acknowledgement follows the repository's noun style for actions (`/publication`, `/invite-checks`).

**Rejected**: `/api/v1/me/plan` (the repository has no `me` namespace); `PUT /plan/seen` (a verb-like state path).

## R3. Draft and no plan answer alike

**Decision**: A plan without a first week and a missing plan both throw `PlanNotFoundException` (`plan.not-found`, 404), before anything of the plan is read.

**Why**: FR-003: a draft must not reveal its existence to the client. The code already exists and the frontend already translates it.

## R4. Week parameter

**Decision**: Optional `week`, a plan week number. Absent means the current plan week. Outside 1..weeks, or any week while the plan has finished, is refused with `plan.week-out-of-range` (400, kind INVALID). A finished plan asked without a week is not an error: it answers `finished = true` and no week.

**Why**: The finished state is a state of the plan, not a week; a client asking for week 30 of 12 has a stale page, not a clamp to week 12. A clamp would hide the bug.

**Rejected**: Clamping; 404 for a week out of range (404 is for resources of other people; the plan exists).

## R5. Dates and time zone

**Decision**: The service takes the client's time zone from their own `Account` (`CurrentAccountUseCase.find`), computes today there and the current plan week with `Plan.currentWeek`. The response gives the week's first date (`startsOn`, a Monday) as a plain date; the frontend adds the day offset of each session's day.

**Why**: Weeks are Monday to Sunday in the client's time zone (`CONTEXT.md`). Plain dates cannot shift with the browser's zone. The fixed-clock test proves the Sunday-to-Monday boundary (SC-004).

## R6. Exercise names

**Decision**: The client's trainer's library gives names and measures (`ListExercisesUseCase.library(trainerId)`, the trainer id from the client's account), as the trainer's read does, by the request's language.

**Why**: A client's plan can hold the trainer's own exercises, which only that library lists. Reuses the existing port; no new port.

## R7. Where "seen" is kept

**Decision**: A nullable `client_seen_at timestamptz` column on `plans`, set by one statement, `update plans set client_seen_at = now where client_id = ? and first_week is not null and client_seen_at is null`. It does not go through the entity, so it raises neither `version` nor `updated_at`, and no sessions are loaded.

**Why**:
- Per plan and server-side, so it holds across devices and new browsers, and the project forbids using browser storage for state about a client's data (FR-015).
- The trainer's editor saves with a version. A seen mark that raised the version would make the trainer's next save fail as "changed elsewhere" merely because the client looked at the plan. A bulk update does not raise it.
- A later save by the trainer replaces goal, weeks and sessions but not this column, so it is never reset (FR-016). `PlanRepositoryImpl.replace` copies only the named values, so it leaves the column alone; a repository test proves it.
- Idempotent and race-free: a second call matches no row.

**Rejected**: A table `plan_acknowledgements` (a second table for one nullable value); the mark inside the `Plan` aggregate and saved with it (raises the version, and the aggregate is replaced on every save); browser storage (not across devices, forbidden for client data); `localStorage` flag plus server (two truths).

## R8. Who sets the mark, and where the gate lives

**Decision**: The Plan tab acknowledges the plan when it loads and the answer says `seen = false`; "View my plan" is a link to the Plan tab, so there is one way to set it. A gate in `ClientArea` redirects to `/app/plan-ready` when a published plan is unseen, except on the Plan tab itself and the plan-ready page.

**Why**: One mechanism covers both "choose View my plan" and "open the Plan tab by its address" (spec User Story 2, scenario 4). Failing to acknowledge leaves the mark unset, so the screen shows again, which is the safe side.

**Rejected**: An acknowledge call on the button as well (two paths to the same state); redirecting from the quiz status (`QuizStatus` stays about the quiz; it would need a fourth value touching task #53's routing in pull request #229).

## R9. The gate for clients with no published plan

**Decision**: The gate treats `plan.not-found` as "no plan" and renders the client area unchanged. Where such a client belongs (quiz or waiting) is task #53's routing, not this story's.

**Why**: Keeps this story independent of #229; once #229 merges, a client without a published plan never reaches the tabs.

## R10. Plan-ready content

**Decision**: The plan-ready page reads the same query as the Plan tab (cached) and shows goal, number of weeks and sessions per week from the response; no extra endpoint.

**Why**: The response already holds them; one query keeps the gate, the page and the tab consistent.

## R11. Frontend components

**Decision**: Session cards use the shadcn `collapsible` (or `accordion`) if present, added through the CLI otherwise; the week strip is a list of seven day cells in a grid that wraps at 375 px; navigation uses `Button` with icons. Checked in `npx shadcn@latest search` before writing markup.

## R12. Demo data

**Decision**: Extend `R__demo_plans.sql` (idempotent) so the demo trainer's clients cover: published and unseen, published and seen, published and finished (first week more than `weeks` weeks back).

**Why**: Definition of Done requires demo data for new entities; this story adds a column, and each state must be reachable in the browser.
