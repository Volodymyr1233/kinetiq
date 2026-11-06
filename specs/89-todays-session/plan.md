# Implementation Plan: Client sees today's session

**Branch**: `feat/89-todays-session` | **Date**: 2026-11-06 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/89-todays-session/spec.md`

## Summary

A client opens the Today tab and sees the session their plan has for today's weekday, a rest day, or a finished-plan message. The backend adds one read to the `plan` feature: `GET /api/v1/today`, for role CLIENT, taking the client from the sign-in; it reuses the client's published plan, time zone and trainer's library from the week view of story #75 (`PlanViewService`), and a draft answers like no plan. The frontend replaces the Today placeholder with a page in `features/plan` that shows the date and, by state, the session with its targets, a rest day or the finished message, plus a disabled "Start session" until the logger exists (story #92). No migration, no new dependency.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1) backend; TypeScript, React 19 frontend

**Primary Dependencies**: Existing only (Spring MVC, MapStruct; React Router, TanStack Query, react-i18next, shadcn `button`, `card`, `skeleton`, `alert`)

**Storage**: None added. Today is derived from the plan and the clock; nothing is stored.

**Testing**: JUnit 5 + AssertJ (domain service with a fake repository and a fixed clock), `@WebMvcTest` (controller), Vitest + Testing Library (frontend). Docker is needed only for the backend `check`.

**Target Platform**: Browser, client screens at 375 px; same-origin `/api/v1`

**Project Type**: Web application (backend + frontend in one repository)

**Performance Goals**: One request when the tab opens.

**Constraints**: A draft and a missing plan answer alike; the client comes from the sign-in only; every string in English and Polish; no sideways scroll at 375 px and 320 px.

**Scale/Scope**: One session at most a day; a few exercises.

## Constitution Check

| Principle | Check | Result |
|---|---|---|
| I. Hexagonal boundaries | `today` is a method of the existing `ViewOwnPlanUseCase`; no new port, feature dependency or adapter. `HexagonalArchitectureTest` runs in `check`. | Pass |
| II. One contract | One endpoint, generated OpenAPI types, `schema.d.ts` regenerated in the same pull request; errors carry stable codes (`plan.not-found`) that the frontend translates. | Pass |
| III. Secure by default | `/api/v1/today/**` is added to the role CLIENT rule in `SecurityConfig`; the client is taken from the sign-in, never from path or body. | Pass |
| IV. Explainable plan changes | Not touched. | n/a |
| V. Tests verify behaviour | Service tests with a fixed clock (session, rest, finished, last Sunday, local date across a time-zone boundary, draft), controller tests (shape, roles, codes), page tests. No getter tests. | Pass |
| VI. Simplicity | No new endpoint family or table; the entry row of the week view is reused; no refresh timers. | Pass |
| VII. One design system | Kinetiq tokens and shadcn only; checked in the browser, light and dark, at 375 px; EN and PL strings. | Pass |

No violations, so no Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/89-todays-session/
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
backend/src/main/java/com/kinetiq/
├── plan/
│   ├── domain/input/{ViewOwnPlanUseCase (+today), TodayView}.java
│   ├── domain/service/PlanViewService.java              # + today(); localDate() shared with view()
│   └── infrastructure/input/{TodayController, TodayResponse, PlanWebMapper (+today)}.java
├── common/infrastructure/config/SecurityConfig.java     # + /api/v1/today/** hasRole CLIENT
backend/src/main/resources/db/dev-data/                  # + a client with a session on almost every day
backend/src/test/java/com/kinetiq/plan/...               # PlanViewServiceTest (+Today), TodayControllerTest

frontend/src/
├── features/plan/
│   ├── api/today.ts                                      # useToday
│   ├── components/{entry-row,session-card}.tsx           # entry-row extracted from session-card
│   └── pages/today-page.tsx (+ test)
├── app/router.ts                                         # /app/today renders TodayPage
├── lib/api/schema.d.ts                                   # regenerated
└── locales/{en,pl}.json                                  # plan.today.*
```

**Structure Decision**: Stays inside the `plan` feature, as the week view of #75 does: the same aggregate and the same response records. The entry row (name and targets) is extracted from the session card so the week view and Today draw an entry the same way.

## Phases

- **#90 (backend)**: finish the drafted `today` read (domain, controller, security rule), tests, demo data, regenerated `schema.d.ts`.
- **#91 (frontend)**: Today page with the session, rest and finished states, the disabled "Start session", strings, tests, browser check.

Each task is one commit in the story's pull request.
