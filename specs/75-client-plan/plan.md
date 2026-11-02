# Implementation Plan: Client sees their plan

**Branch**: `feat/75-client-plan` | **Date**: 2026-11-02 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/75-client-plan/spec.md`

## Summary

A client reads their own published plan week by week (#76 API, #77 Plan tab) and sees a "plan ready" screen once per published plan (#78). The backend adds a client-side read of the existing plan aggregate to the `plan` feature: `GET /api/v1/plan?week=N` and `POST /api/v1/plan/acknowledgement`, both for role CLIENT and both taking the client from the sign-in. A draft answers like no plan. The "seen" mark is one nullable column on `plans`, set by a single-statement update that touches neither the version nor the sessions, so the trainer's editor never goes stale. The frontend adds the client Plan tab and the plan-ready page to `features/plan`, and a gate in `app/client-area.tsx` that sends a client with an unseen plan to the plan-ready page. No new dependencies.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1) backend; TypeScript, React 19 frontend

**Primary Dependencies**: Existing only: Spring MVC, Spring Data JPA, Flyway, MapStruct, Lombok; React Router, TanStack Query, react-i18next, shadcn (`tabs`/`collapsible`/`card` if missing, added through the CLI)

**Storage**: PostgreSQL; one migration `V15__add_plan_seen.sql` (`plans.client_seen_at timestamptz`, nullable)

**Testing**: JUnit 5 + AssertJ (domain service with a fake repository and a fixed clock), `@WebMvcTest` (controller), `@DataJpaTest` Testcontainers `*IT` (adapter), Vitest + Testing Library (frontend). Docker must be running for the backend gate.

**Target Platform**: Browser, client screens at 375 px; same-origin `/api/v1`

**Project Type**: Web application (backend + frontend in one repository)

**Performance Goals**: None beyond a normal page: one request per plan week shown; a plan is at most 16 weeks x 6 sessions x 30 entries.

**Constraints**: Another person's data answers 404; a draft is never returned; no state for client data in browser storage; every string in English and Polish; no sideways scroll at 375 px and 320 px.

**Scale/Scope**: One plan per client; a handful of sessions per week.

## Constitution Check

*GATE: passes before research; re-checked after design.*

| Principle | Check | Result |
|---|---|---|
| I. Hexagonal boundaries | New `ViewOwnPlanUseCase` port and `PlanViewService` in `plan`; no new feature dependency (`plan` already uses `account`, `client`, `exercise`); the seen update is a repository method. `HexagonalArchitectureTest` runs in `check`. | Pass |
| II. One contract | Two endpoints with generated OpenAPI types; `schema.d.ts` regenerated in the same pull request; errors carry stable codes (`plan.not-found`, `plan.week-out-of-range`), the frontend translates them. | Pass |
| III. Secure by default | `/api/v1/plan/**` is added to `SecurityConfig` for role CLIENT, not opened; the client is taken from the sign-in, never from path or body; no personal data in logs. | Pass |
| IV. Explainable plan changes | Not touched. | n/a |
| V. Tests verify behaviour | Domain service tests (draft and none answer alike, week bounds, finished state, time zone boundary, mark set once), a controller test (roles, codes), an adapter test (mark leaves version and sessions alone), frontend page tests. No getter tests. | Pass |
| VI. Simplicity | Reuses `PlanResponse` session and entry records; the seen mark is a column, not a table; the week view is derived, not stored; no new dependency. | Pass |
| VII. One design system | Kinetiq tokens and shadcn only; checked in the browser, light and dark, at 375 px; EN and PL strings. | Pass |

No violations, so no Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/75-client-plan/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── client-plan-api.md
├── checklists/
│   └── requirements.md
└── tasks.md             # /speckit-tasks
```

### Source Code (repository root)

```text
backend/src/main/
├── java/com/kinetiq/
│   ├── plan/
│   │   ├── domain/
│   │   │   ├── model/Plan.java                          # + clientSeenAt, markSeen
│   │   │   ├── input/ViewOwnPlanUseCase.java            # view(client, week), acknowledge(client)
│   │   │   ├── input/OwnPlanView.java                   # plan, week, currentWeek, finished, library
│   │   │   ├── output/PlanRepository.java               # + markSeenByClient
│   │   │   ├── service/PlanViewService.java
│   │   │   └── exception/PlanWeekOutOfRangeException.java
│   │   └── infrastructure/
│   │       ├── config/PlanConfig.java                   # + bean
│   │       ├── input/ClientPlanController.java          # GET /api/v1/plan, POST /api/v1/plan/acknowledgement
│   │       ├── input/ClientPlanResponse.java
│   │       ├── input/PlanWebMapper.java                 # + client response
│   │       └── output/{PlanEntity,PlanMapper,PlanRepositoryImpl,JpaPlanRepository}.java
│   └── common/infrastructure/config/SecurityConfig.java # + /api/v1/plan/** hasRole CLIENT
└── resources/db/
    ├── migration/V15__add_plan_seen.sql
    └── dev-data/R__demo_plans.sql                       # + unseen, seen and finished plans for demo clients
backend/src/test/java/com/kinetiq/plan/...               # PlanViewServiceTest, ClientPlanControllerTest, PlanRepositoryIT additions

frontend/src/
├── app/client-area.tsx                                  # gate: unseen plan -> /app/plan-ready; Plan tab route
├── app/router.ts                                        # plan tab, /app/plan-ready
├── features/plan/
│   ├── api/client-plan.ts                               # useOwnPlan, useAcknowledgePlan
│   ├── components/{week-strip,session-card}.tsx
│   └── pages/{client-plan-page,plan-ready-page}.tsx     # + tests
├── lib/api/schema.d.ts                                  # regenerated
└── locales/{en,pl}.json                                 # plan.client.* strings
```

**Structure Decision**: Extends the two existing features, `plan` in the backend and `features/plan` in the frontend, instead of adding a `client-plan` feature: the same aggregate, the same entities and the same response records. The gate lives in `app/` because it joins routing with the plan feature.

## Phases

- **#76 (backend)**: migration V15, domain (view use case and service, week range exception), repository, controller, security rule, demo data, tests, regenerated `schema.d.ts`.
- **#77 (frontend)**: client Plan tab (week strip, session cards, week navigation, loading, error and finished states), strings.
- **#78 (frontend)**: plan-ready page and the gate; the Plan tab marks the plan seen; finished state copy.

Each task is one commit in the story's pull request.
