---
description: "Tasks for story #75, Client sees their plan (tasks #76, #77, #78)"
---

# Tasks: Client sees their plan

**Input**: `specs/75-client-plan/` (spec.md, plan.md, research.md, data-model.md, contracts/client-plan-api.md, quickstart.md)

**Tests**: Required by the Definition of Done and principle V; each test sits with the code it covers.

**Organization**: Phases follow the GitHub tasks, in order: #76 (backend), #77, #78 (frontend). One pull request for the story, one commit per task, each ending in `(#<task>)`.

**Paths**: `B` = `backend/src/main/java/com/kinetiq`, `BT` = `backend/src/test/java/com/kinetiq`, `BR` = `backend/src/main/resources`, `F` = `frontend/src`.

## Phase 1: Setup

- [X] T001 Commit `specs/75-client-plan/` with the code of task #76, so the spec is reviewed in that pull request. `CONTEXT.md` needs no new term

## Phase 2: task #76, client plan API with week view (US1, US4)

**Goal**: `GET /api/v1/plan?week=N` and `POST /api/v1/plan/acknowledgement` for a signed-in client (contract: `contracts/client-plan-api.md`).

- [X] T002 [US4] Migration `BR/db/migration/V15__add_plan_seen.sql` as in `data-model.md`: `client_seen_at timestamptz` (nullable) on `plans` and `ck_plans_seen_published check (client_seen_at is null or first_week is not null)`. Before writing it, run `git fetch` and `gh pr list --json number,files` and renumber if `V15` is taken
- [X] T003 [US1] Domain: `ViewOwnPlanUseCase` (`view(client, week)`, `acknowledge(client)`) and `OwnPlanView` (plan, week number and start date or none, currentWeek, finished, seen, library) in `B/plan/domain/input/`; `PlanWeekOutOfRangeException` (`plan.week-out-of-range`, kind `INVALID`) in `domain/exception/`; `PlanRepository.isSeenByClient(client)` and `markSeenByClient(client, at)` in `domain/output/`; `PlanViewService` in `domain/service/`: a missing plan and a draft both throw `PlanNotFoundException` before anything is read; the client's time zone from `CurrentAccountUseCase.find` and the trainer's library from their `getTrainerId()`; week defaults to the current plan week; a `week` below 1, above `weeks`, or given for a finished plan is refused; a finished plan with no `week` answers `finished` and no week; the week's start is `firstWeek + (number - 1) * 7` days
- [X] T004 [US1] The view says `seen` from `PlanRepository.isSeenByClient`, so the `Plan` aggregate is left as it is; saving and publishing never change the column (data-model rules)
- [X] T005 [US4] Adapter in `B/plan/infrastructure/output/`: `PlanEntity.clientSeenAt`, `JpaPlanRepository` update `update PlanEntity p set p.clientSeenAt = :at where p.clientId = :client and p.firstWeek is not null and p.clientSeenAt is null` (`@Modifying`), `PlanRepositoryImpl.isSeenByClient` and `markSeenByClient`; it must not change `version` or `updated_at` and `replace` must keep the column
- [X] T006 [US4] Adapter test `BT/plan/infrastructure/output/PlanRepositoryImplIT.java` additions: the mark is set once and a second call changes nothing; a draft is never marked; a save by the trainer keeps the mark and the version is the one saved, not raised by the mark
- [X] T007 [US1] Domain test `BT/plan/domain/service/PlanViewServiceTest.java` with a fake repository, fake directory and fixed clock: a draft and a client with no plan answer the same; current week and the Monday date of week N; Sunday evening in the client's zone that is already Monday in UTC (and the reverse) gives the right week; week 0, week above `weeks` and a week for a finished plan are refused; a finished plan answers `finished` and no week, with its sessions; names come from the client's trainer's library; `acknowledge` on a draft or no plan is `plan.not-found`
- [X] T008 [US1] Web in `B/plan/infrastructure/input/`: `ClientPlanController` (`GET /api/v1/plan`, `POST /api/v1/plan/acknowledgement` returning `204`), `ClientPlanResponse` (required fields marked `@NotNull`, `week` optional, reusing `PlanResponse.SessionResponse`), mapper method in `PlanWebMapper`, bean in `PlanConfig`; the client id comes from the sign-in only
- [X] T009 [US4] `SecurityConfig` (`B/common/infrastructure/config/`): `/api/v1/plan/**` for role `CLIENT` only, before `/api/**`
- [X] T010 [US4] Controller test `BT/plan/infrastructure/input/ClientPlanControllerTest.java`: `200` with the response shape and the signed-in client's id passed to the use case; the `week` query passed through; `404 plan.not-found`; `400 plan.week-out-of-range`; `204` for the acknowledgement; `403 request.not-allowed` for a trainer and `401` for a visitor on both operations; a non-numeric `week` is `400 request.invalid`
- [X] T011 [US1] Demo data in `BR/db/dev-data/R__demo_accounts.sql`, `R__demo_quiz.sql`, `R__demo_plans.sql` (idempotent): add `Demo Client Seen` (published plan, `client_seen_at` set) and `Demo Client Finished` (published plan whose first week is more than `weeks` weeks back) for `demo.trainer@example.com`, with trainer, consent and a quiz each; `Demo Client Published` stays the unseen one. Update the README demo table
- [X] T012 Regenerate `frontend/src/lib/api/schema.d.ts` (`npm run gen:api`, backend running, `SERVER_PORT` free) and run `npm run check`
- [X] T013 `./gradlew check` passes (Docker running), including `HexagonalArchitectureTest`; follow `quickstart.md` step 4 by hand; commit `feat(backend): add client plan api (#76)`

## Phase 3: task #77, plan screen with week strip (US1)

**Goal**: the client's Plan tab shows the plan week by week (spec FR-008 to FR-013).

- [X] T014 [US1] `F/features/plan/api/client-plan.ts`: `useOwnPlan(week?)` for `GET /api/v1/plan` (query key per week; a `plan.not-found` failure is not retried) and `useAcknowledgePlan` for the `POST`, both typed by the generated schema
- [X] T015 [US1] Components in `F/features/plan/components/`: `week-strip.tsx` (seven days of the week with weekday, date and the session on it or a rest-day mark; dates from the week's start date plus the day's offset, formatted for the active language) and `session-card.tsx` (a collapsible card: exercises in order with sets, amount in repetitions or seconds, load converted with `fromKg` and the person's unit or "no load", RPE with the `num` class, rest). Check `npx shadcn@latest search -q collapsible` first and add through the CLI
- [X] T016 [US1] `F/features/plan/pages/client-plan-page.tsx`: "Week N of M" and the week's dates, previous and next week buttons (disabled at the ends), "Current week" away from it, a live region announcing the week, skeleton while loading, translated error with "Try again"; the week asked for is kept in the URL (`?week=`); no sideways scroll at 375 px and 320 px
- [X] T017 [US1] Route: `/app/plan` renders `ClientPlanPage` instead of the placeholder in `F/app/router.ts`
- [X] T018 [US1] Strings `plan.client.*` in `F/locales/en.json` and `pl.json` (Polish plural forms for weeks and exercises)
- [X] T019 [US1] Page test `F/features/plan/pages/client-plan-page.test.tsx`: the current week with its dates and sessions on their days; next and previous move the week and the dates; previous is unavailable in week 1 and next in the last week; opening a session shows its exercises and closing hides them; seconds for an exercise measured in seconds; a load in pounds for a person whose unit is pounds, and "no load" without one; loading and error with retry
- [ ] T020 [US1] `npm run check` passes (done); the browser check is still to do; check the tab in the browser as `demo.client.published@example.com` at 375 px, light and dark, with the keyboard; commit `feat(frontend): add client plan screen with week strip (#77)`

## Phase 4: task #78, plan-ready screen and plan-finished state (US2, US3)

**Goal**: a client sees "your plan is ready" once and a finished plan says the next one is being prepared (spec FR-014 to FR-018).

- [X] T021 [US2] `F/features/plan/pages/plan-ready-page.tsx`: goal, number of weeks and sessions per week from `useOwnPlan`, and "View my plan" as a link to `/app/plan`; a client without a published plan is sent to `/app` (task #53's routing decides the rest); no tab bar, 48 px touch targets
- [X] T022 [US2] The Plan tab acknowledges the plan once when the answer says `seen` is false (`useAcknowledgePlan`), and refreshes the cached plan on success
- [X] T023 [US2] Gate in `F/app/client-area.tsx`: when the client's plan is published and unseen, any tab except Plan redirects to `/app/plan-ready`; `plan.not-found` renders the area unchanged; the gate shows nothing while the plan loads. Route `/app/plan-ready` beside the client area in `F/app/router.ts`, behind `requireRole('CLIENT')`
- [X] T024 [US3] `ClientPlanPage` shows "Your trainer is preparing your next plan." and the plan's number of weeks in place of the strip when `finished`
- [X] T025 [US2] Strings for the plan-ready page and the finished state in `en.json` and `pl.json`
- [X] T026 [US2] Tests: `plan-ready-page.test.tsx` (the summary and the link; redirect without a published plan), the finished state in `client-plan-page.test.tsx`, and a gate test in `F/app/client-area.test.tsx` (unseen redirects Today to plan-ready; seen does not; the Plan tab itself is not redirected; no plan renders the tab as before)
- [ ] T027 [US2] `npm run check` passes (done); the browser check is still to do: unseen client sees plan-ready once and then never again, also after sign-out and in a private window; finished client sees the finished state; 375 px, light and dark; commit `feat(frontend): add plan-ready screen and finished state (#78)`

## Phase 5: Close the story

- [ ] T028 Both gates pass (`./gradlew check`, `npm run check`), the quickstart was followed end to end, `README.md` demo table is current. Open the pull request (title `feat: let the client see their plan (#75)`, `Closes #75`, `Closes #76`, `Closes #77`, `Closes #78`); merge with rebase so each task keeps its commit

## Dependencies

- T001 first; the backend phase (T002 to T013) before the frontend phases, because the frontend types are generated from it.
- Inside #76: T002 before T005 and T006; T003 and T004 before T005, T007 and T008; T008 before T009 and T010; T011 can run beside T007 to T010; T012 after T008.
- #77 before #78: T022 to T024 build on `ClientPlanPage`.

## Parallel opportunities

- T006 (adapter test), T007 (service test) and T011 (demo data) touch different files and can be written in parallel once T003 to T005 exist.
- T015 (components) and T018 (strings) can run beside T014.

## Implementation strategy

Build in task order and commit each task when its checks pass: the API is the MVP of the story (it unblocks #90 Today API), then the Plan tab, then the plan-ready screen and the finished state. Nothing is pushed until the story is tested by hand and the user agrees.
