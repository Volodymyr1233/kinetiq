---
description: "Tasks for task #264, Client sees what comes next on a rest day (story #89)"
---

# Tasks: Client sees what comes next on a rest day

**Input**: `specs/264-rest-day-next/` (spec.md, plan.md, research.md, data-model.md, contracts/today-api.md, quickstart.md)

**Tests**: Required by the Definition of Done and principle V; each test sits with the code it covers.

**Organization**: One GitHub task (#264), one pull request, one commit ending in `(#264)`. Backend first, then frontend.

**Paths**: `B` = `backend/src/main/java/com/kinetiq`, `BT` = `backend/src/test/java/com/kinetiq`, `BR` = `backend/src/main/resources`, `F` = `frontend/src`.

## Phase 1: Setup

- [X] T001 Commit `specs/264-rest-day-next/` with the code, so the spec is reviewed in the same pull request. `CONTEXT.md` needs no new term. Do not commit `backend/bin/` (IDE output)

## Phase 2: Today API gives the next session (US1, US4)

**Goal**: `GET /api/v1/today` on a rest day also returns `next` (contract: `contracts/today-api.md`).

- [X] T002 [US1] Domain: `TodayView` in `B/plan/domain/input/` gets `next`, the plan's session and the date it falls on, set only for `REST`; `PlanViewService.today` in `B/plan/domain/service/` finds it as the first session strictly after today, up to the Sunday of the plan's last week, and leaves it absent when none remains; `SESSION` and `FINISHED` carry no `next`; the library of a rest day names the entries of `next`
- [X] T003 [US1] Web in `B/plan/infrastructure/input/`: `TodayResponse` gets optional `next` (`date` and `session` required inside it, reusing `PlanResponse.SessionResponse`), left out when absent; `PlanWebMapper.toResponse(TodayView, Locale)` maps it with exercise names in the request's language
- [X] T004 [US1] Service tests in `BT/plan/domain/service/PlanViewServiceTest.java` (nested `Today`, fixed clock): next later this week; next in the following week when this week's sessions have passed; none left after the last session of the last week; none on the last day of the plan when today has no session; `SESSION` and `FINISHED` carry no `next`; the date follows the client's time zone
- [X] T005 [US4] Controller test in `BT/plan/infrastructure/input/TodayControllerTest.java`: `next` present only for `REST` with date and session and translated names; absent for `SESSION`, `FINISHED` and a rest day with no session left; `404 plan.not-found` for a draft and no plan, `403` for a trainer, `401` for a visitor are still covered
- [X] T006 [US1] Demo data in `BR/db/dev-data/` (idempotent): clients whose today is a rest day with a next session this week, one with it in a later week and one with no session left, kept stable whatever day the app runs (mark the plan seen so they land on Today; add them to the README demo table)
- [X] T007 Regenerate `frontend/src/lib/api/schema.d.ts` (`npx -y openapi-typescript@7.13.0 http://localhost:<port>/v3/api-docs -o src/lib/api/schema.d.ts`, backend on a free `SERVER_PORT`)

## Phase 3: The rest-day card (US1, US2, US3)

**Goal**: The Today rest card of the spec, with the count line under it.

- [X] T008 [US1] Extract the minutes formula from `sessionTotals` in `F/features/plan/plan-draft.ts` into an exported `estimatedMinutes` taking entries with numeric `measure`, `sets`, `amount`, `rest`, used by both `sessionTotals` and the Today card; a test in `plan-draft.test.ts` that both give the same minutes for the same session
- [X] T009 [US1] Rest card in `F/features/plan/pages/today-page.tsx`: "Rest" badge (shadcn `badge`), the line "No session today. No check-in either." and, when `next` is present, "Next: <name> · <date> · <minutes> min" with the date in the UI language (read in UTC as a plain date); no change for `SESSION` and `FINISHED`
- [X] T010 [US2] "See this week" in the rest card: `Button` (`size="touch"`, `asChild`) with a `Link` to `/app/plan`; absent for `SESSION` and `FINISHED`
- [X] T011 [US3] Count line under the card on a rest day: done sessions of this week from `useSessionStates()` (already read by the page) out of the sessions the plan has in a week from `useOwnPlan()`; left out when the states fail; reuse `plan.client.weekDone` or add `plan.today.weekDone` with Polish plural forms
- [X] T012 [US1] Strings under `plan.today.*` in `F/locales/en.json` and `pl.json` (rest badge, no-session line, next, see this week, count if new), minimal edits that keep the files' formatting
- [X] T013 [US1] Page tests in `F/features/plan/pages/today-page.test.tsx`: badge, line, next with name, date and minutes; next in a later week; no `next` line when absent; "See this week" links to `/app/plan`; count line, and left out when the states fail; none of the new elements on a session day or a finished plan; Polish texts and plural forms for 1, 2, 5, 12 and 22

## Phase 4: Polish

- [X] T014 `./gradlew check` in `backend/` (Docker running) and `npm run check` in `frontend/` pass; report real results
- [X] T015 Browser check as the demo clients, light and dark at 375 px, keyboard focus on "See this week", no sideways scroll at 320 px; follow `quickstart.md`
- [X] T016 Commit `feat: show what comes next on a rest day (#264)`; open the pull request with `Closes #264` and `Closes #89` once #262 is merged

## Dependencies

T002 → T003 → T004, T005 → T006 → T007 → T008 → T009 → T010, T011 → T012 → T013 → T014 → T015 → T016. T004 and T005 can be written together; T010 and T011 touch the same page, so they are sequential.
