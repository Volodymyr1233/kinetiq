# Implementation Plan: Client sees what comes next on a rest day

**Branch**: `feat/264-rest-day-next` | **Date**: 2026-11-08 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/264-rest-day-next/spec.md`

## Summary

On a rest day the Today answer gains the next session of the plan: the first session after today in this or a later week, with the date it falls on and its entries. The frontend shows it on the rest-day card with a "Rest" badge, the fixed line "No session today. No check-in either.", the estimated minutes (the plan editor's formula, shared) and a "See this week" link to the Plan tab, and a count of the week's done sessions under the card from the session states the page already reads. No migration, no new endpoint, no new dependency.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1) backend; TypeScript, React 19 frontend

**Primary Dependencies**: Existing only (Spring MVC, MapStruct; React Router, TanStack Query, react-i18next, shadcn `badge`, `button`)

**Storage**: None. The next session is derived from the plan and the clock.

**Testing**: JUnit 5 + AssertJ (`PlanViewService` with a fake repository and a fixed clock), `@WebMvcTest` (`TodayController`), Vitest + Testing Library (page, estimate). Docker only for the backend `check`.

**Target Platform**: Browser, client screens at 375 px; same-origin `/api/v1`

**Project Type**: Web application (backend + frontend in one repository)

**Performance Goals**: Still one request for Today, plus the session-states read the page already makes.

**Constraints**: The client comes from the sign-in only; draft and missing plan answer alike; every string in English and Polish; no sideways scroll at 375 px and 320 px.

**Scale/Scope**: One next session; a few exercises.

## Constitution Check

| Principle | Check | Result |
|---|---|---|
| I. Hexagonal boundaries | The change stays inside `plan`: `TodayView` and `PlanViewService.today`; no new port or feature dependency. `HexagonalArchitectureTest` runs in `check`. | Pass |
| II. One contract | `TodayResponse` gains an optional `next`; `schema.d.ts` is regenerated in the same pull request. | Pass |
| III. Secure by default | `/api/v1/today/**` already needs the role CLIENT; the client is taken from the sign-in; nothing new is exposed. | Pass |
| IV. Explainable plan changes | Not touched. | n/a |
| V. Tests verify behaviour | Service tests with a fixed clock (later this week, next week, none left, last day, session day and finished unchanged), controller test (shape, `next` only on a rest day), page tests, estimate test. No getter tests. | Pass |
| VI. Simplicity | No new endpoint; the minutes formula is extracted from `sessionTotals` and shared, not duplicated on the server. | Pass |
| VII. One design system | Kinetiq tokens, shadcn `badge` and `button`; checked in the browser, light and dark, 375 px; EN and PL strings. | Pass |

No violations, so no Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/264-rest-day-next/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/today-api.md
├── checklists/requirements.md
└── tasks.md             # /speckit-tasks
```

### Source Code (repository root)

```text
backend/src/main/java/com/kinetiq/plan/
├── domain/input/TodayView.java                    # + next (date and session) for REST
├── domain/service/PlanViewService.java            # today() finds the next session
└── infrastructure/input/{TodayResponse, PlanWebMapper}.java   # + next
backend/src/main/resources/db/dev-data/            # demo clients for the three rest days
backend/src/test/java/com/kinetiq/plan/...         # PlanViewServiceTest, TodayControllerTest

frontend/src/
├── features/plan/
│   ├── plan-draft.ts                              # estimatedMinutes extracted from sessionTotals
│   ├── pages/today-page.tsx (+ test)              # rest-day card, count line
│   └── components/                                # small rest card if the page grows
├── lib/api/schema.d.ts                            # regenerated
└── locales/{en,pl}.json                           # plan.today.*
```

**Structure Decision**: Stays inside the `plan` feature, as specs `75` and `89` do.

## Phases

- **Backend**: the next session in `TodayView`, the response and tests, demo data, regenerated `schema.d.ts`.
- **Frontend**: shared estimate, the rest-day card and the count line, strings, tests, browser check.

One task #264, so one commit.
