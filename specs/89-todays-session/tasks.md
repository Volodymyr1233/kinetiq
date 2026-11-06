---
description: "Tasks for story #89, Client sees today's session (tasks #90, #91)"
---

# Tasks: Client sees today's session

**Input**: `specs/89-todays-session/` (spec.md, plan.md, research.md, data-model.md, contracts/today-api.md, quickstart.md)

**Tests**: Required by the Definition of Done and principle V; each test sits with the code it covers.

**Organization**: Phases follow the GitHub tasks, in order: #90 (backend), #91 (frontend). One pull request for the story, one commit per task, each ending in `(#<task>)`.

**Paths**: `B` = `backend/src/main/java/com/kinetiq`, `BT` = `backend/src/test/java/com/kinetiq`, `BR` = `backend/src/main/resources`, `F` = `frontend/src`.

## Phase 1: Setup

- [X] T001 Commit `specs/89-todays-session/` with the code of task #90, so the spec is reviewed in that pull request. `CONTEXT.md` needs no new term. Do not commit `backend/bin/` (IDE output)

## Phase 2: task #90, Today API (US1, US2, US4)

**Goal**: `GET /api/v1/today` for a signed-in client (contract: `contracts/today-api.md`). Most of it is drafted in the working tree.

- [X] T002 [US1] Review and finish the drafted domain: `TodayView` (`date`, `state` of `SESSION`, `REST` or `FINISHED`, `session` only for `SESSION`, `library`) in `B/plan/domain/input/`; `ViewOwnPlanUseCase.today(client)`; `PlanViewService.today` in `B/plan/domain/service/` with the shared `localDate(account)`: a draft and no plan are `PlanNotFoundException` before anything is read; `FINISHED` when the current plan week is above the plan's weeks (even on a weekday with a session); the plan's session on today's weekday is `SESSION`, otherwise `REST`
- [X] T003 [US1] Web in `B/plan/infrastructure/input/`: `TodayController` (`GET /api/v1/today`, client from the sign-in only), `TodayResponse` (`date` and `state` required, `session` left out unless `SESSION`, reusing `PlanResponse.SessionResponse`), `PlanWebMapper.toResponse(TodayView, Locale)`
- [X] T004 [US4] `SecurityConfig` (`B/common/infrastructure/config/`): `/api/v1/today/**` for role `CLIENT` only (drafted; check it sits in the same rule as `/api/v1/quiz/**` and `/api/v1/plan/**`)
- [X] T005 [US1] Service tests in `BT/plan/domain/service/PlanViewServiceTest.java` (nested `Today`, fake repository and fixed clock): the session on this weekday with the names of the client's trainer's library; `REST` with no session today; `FINISHED` past the last week, with no session; the last Sunday of the last week is still a normal day; the client's local date when midnight has already passed elsewhere (Los Angeles against Warsaw); a draft and no plan both `plan.not-found`
- [X] T006 [US4] Controller test `BT/plan/infrastructure/input/TodayControllerTest.java`: `200` for each state with the response shape (no `session` for `REST` and `FINISHED`, no `loadKg` when empty); exercise names in the request's language; `404 plan.not-found`; `403 request.not-allowed` for a trainer and `401` for a visitor
- [X] T007 [US1] Demo data in `BR/db/dev-data/R__demo_accounts.sql`, `R__demo_quiz.sql`, `R__demo_plans.sql` (idempotent): `Demo Client Daily` for `demo.trainer@example.com` with trainer, consent, a quiz and a published plan (not finished) with one session on each of Monday to Saturday, each with one exercise; its plan is marked seen so it lands on Today. Add the account to the README demo table
- [X] T008 Regenerate `frontend/src/lib/api/schema.d.ts` (`npm run gen:api` with the backend running, `SERVER_PORT` free) and run `npm run check`
- [X] T009 `./gradlew check` passes (Docker running), including `HexagonalArchitectureTest`; follow `quickstart.md` step 5 by hand; commit `feat(backend): add today api (#90)`

## Phase 3: task #91, Today screen (US1, US2, US3)

**Goal**: the Today tab shows the session, a rest day or the finished-plan message (spec FR-009 to FR-014).

- [X] T010 [US1] `F/features/plan/api/today.ts`: `useToday()` for `GET /api/v1/today`, the language part of the query key (names follow it), a `plan.not-found` failure not retried, typed by the generated schema
- [X] T011 [US1] Extract the entry row (exercise name and the targets line: sets × amount in repetitions or seconds, load in the person's unit or "No load", RPE, rest) from `F/features/plan/components/session-card.tsx` into `F/features/plan/components/entry-row.tsx`, and use it in both; the week view's tests must stay green
- [X] T012 [US1] `F/features/plan/pages/today-page.tsx`: the date in the UI language; for `SESSION` the session's name and every exercise in order, fully shown; for `REST` "Rest day"; for `FINISHED` "Your trainer is preparing your next plan."; skeleton while loading; translated error with "Try again"; `plan.not-found` shows "You don't have a plan yet." without an error; long names wrap, no sideways scroll at 375 px and 320 px
- [X] T013 [US3] "Start session" in the page, only for `SESSION`, once, `Button` size `touch` as the one lime action, `disabled`, with a visible line under it saying starting arrives with logging; it is absent for `REST` and `FINISHED`
- [X] T014 [US1] Route: `/app/today` renders `TodayPage` instead of the placeholder in `F/app/router.ts`
- [X] T015 [US1] Strings `plan.today.*` in `F/locales/en.json` and `pl.json` (title line, rest day, start, why disabled), Polish plural forms where a number appears
- [X] T016 [US1] Page test `F/features/plan/pages/today-page.test.tsx`: a session with its exercises and targets; seconds for an exercise measured in seconds and "No load" without one; a load in pounds for a person whose unit is pounds; a rest day with no "Start session"; a finished plan with no session and no "Start session"; "Start session" present and disabled on a session day; loading, error with retry, and no plan without an error; Polish
- [X] T017 [US2] Update `F/app/client-area.test.tsx` where it expects the Today placeholder, so it expects the page's own answer
- [X] T018 [US1] `npm run check` passes; the tab was checked in a browser as `demo.client.daily`, `demo.client.seen` and `demo.client.finished` at 375 px and 320 px, light and dark, in Polish and pounds, with the keyboard; commit `feat(frontend): add today screen (#91)`

## Phase 4: Close the story

- [X] T019 Both gates pass (`./gradlew check`, `npm run check`), the quickstart was followed end to end. Open the pull request (title `feat: let the client see today's session (#89)`, `Closes #89`, `Closes #90`, `Closes #91`); merge with rebase so each task keeps its commit

## Dependencies

- T001 first; the backend phase before the frontend phase, because the frontend types are generated from it.
- Inside #90: T002 before T003 and T005; T003 before T004 and T006; T007 can run beside T005 and T006; T008 after T003.
- Inside #91: T010 and T011 before T012; T012 before T013, T014, T016; T015 beside T012.

## Parallel opportunities

- T005 (service test), T006 (controller test) and T007 (demo data) touch different files.
- T010 (hook), T011 (entry row) and T015 (strings) are independent of each other.

## Implementation strategy

Task order, one commit per task when its checks pass. The API (#90) is the MVP: it unblocks #110 (check-in API) at once, and #91 unblocks #112 and #95. Nothing is pushed until the story is tested by hand and the user agrees.
