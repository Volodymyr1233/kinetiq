# Implementation Plan: Client logs sets

**Branch**: `feat/92-session-logging` | **Date**: 2026-11-07 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/92-session-logging/spec.md`

## Summary

A client opens the logger and today's session starts: a copy of the planned session (name, exercise entries with their targets) is stored as a **session log**, one per client per date. The client logs each set with a tick or with entered values; each **set log** has an id from the client, so a repeated send stores it once, and keeps a copy of its targets. The backend adds a feature `sessionlog` with two endpoints, `POST /api/v1/session-logs` (start or return today's) and `PUT /api/v1/session-logs/{id}/set-logs/{setLogId}` (log or change a set), for role CLIENT. It reads today's session from the `plan` feature's `ViewOwnPlanUseCase.today`. The frontend adds a feature `sessionlog` with the logger page (set rows, entry panel, rest timer) at `/app/session`, outside the tab bar. One migration (`V17`), no new dependency.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1) backend; TypeScript, React 19 frontend

**Primary Dependencies**: Existing only (Spring MVC, Spring Data JPA, Flyway, MapStruct; React Router, TanStack Query, react-i18next, shadcn)

**Storage**: PostgreSQL; migration `V17__create_session_logs.sql`: `session_logs`, `session_log_entries`, `set_logs`

**Testing**: JUnit 5 + AssertJ (domain services with fake ports and a fixed clock), `@DataJpaTest` Testcontainers `*IT` (adapter), `@WebMvcTest` (controller), Vitest + Testing Library. Docker is needed for the backend `check`.

**Target Platform**: Browser, client screens at 375 px; same-origin `/api/v1`

**Project Type**: Web application (backend + frontend in one repository)

**Performance Goals**: One request per logged set; a session has at most 30 exercises of 20 sets.

**Constraints**: Idempotent on the client's id; another client's data answers 404; loads in kilograms; every string in English and Polish; touch targets 48 px; no sideways scroll at 375 px and 320 px.

**Scale/Scope**: One session log a day per client; a few dozen set logs.

## Constitution Check

| Principle | Check | Result |
|---|---|---|
| I. Hexagonal boundaries | New feature `sessionlog` with `domain.{model,input,output,service,exception}` and `infrastructure.{config,input,output}`. It depends on `plan` (today's session), `account` (time zone and trainer) and `exercise` (names) through their input ports; none of them depends on `sessionlog`, so no cycle. `HexagonalArchitectureTest` runs in `check`. | Pass |
| II. One contract | Two endpoints with generated OpenAPI types; `schema.d.ts` regenerated in the same pull request; stable codes, translated by the frontend. | Pass |
| III. Secure by default | `/api/v1/session-logs/**` is added to the role CLIENT rule in `SecurityConfig`; the client comes from the sign-in only; another client's data answers 404; no personal data in logs. | Pass |
| IV. Explainable plan changes | Not touched; the targets copied on each set log are what later rules read. | n/a |
| V. Tests verify behaviour | Domain tests (copy at start, idempotent start, validation, idempotent set, change, foreign session 404, place rules), adapter test (migration, uniqueness, copy), controller test, page tests (tick as planned, entered values, validation, retry keeps input, timer). No getter tests. | Pass |
| VI. Simplicity | Two endpoints, no GET (starting is idempotent and returns the session); no offline queue; no new dependency; the timer is browser-only state. | Pass |
| VII. One design system | Kinetiq tokens and shadcn only; checked in the browser, light and dark, at 375 px; EN and PL strings. | Pass |

No violations, so no Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/92-session-logging/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/session-log-api.md
├── checklists/requirements.md
└── tasks.md             # /speckit-tasks
```

### Source Code (repository root)

```text
backend/src/main/
├── java/com/kinetiq/
│   ├── sessionlog/
│   │   ├── domain/model/{SessionLog, SessionLogId, LoggedEntry, SetLog, SetLogId, SetValues}.java
│   │   ├── domain/input/{LogSessionUseCase, LogSetCommand}.java
│   │   ├── domain/output/SessionLogRepository.java
│   │   ├── domain/service/SessionLogService.java
│   │   ├── domain/exception/{SessionLogNotFoundException, NoSessionTodayException, SetLogOutOfPlanException, SetLogConflictException, SetValuesInvalidException}.java
│   │   └── infrastructure/{config, input, output}/…      # controller, responses, mapper, JPA adapter
│   └── common/infrastructure/config/SecurityConfig.java  # + /api/v1/session-logs/** hasRole CLIENT
└── resources/db/migration/V17__create_session_logs.sql
backend/src/test/java/com/kinetiq/sessionlog/...

frontend/src/
├── features/sessionlog/
│   ├── api/session-log.ts                                # useStartSession, useLogSet
│   ├── components/{set-row, entry-panel, rest-timer}.tsx
│   ├── pages/logger-page.tsx (+ tests)
│   └── set-values.ts                                     # validation and unit steps (+ test)
├── app/router.ts                                         # /app/session beside the client area
├── lib/api/schema.d.ts                                   # regenerated
└── locales/{en,pl}.json                                  # sessionlog.*
```

**Structure Decision**: A feature of its own, because a session log is not a plan: it has its own lifecycle (started, finished by #98, read by #104). It reads the plan only through `ViewOwnPlanUseCase.today`. The frontend feature mirrors it.

## Phases

- **#93 (backend)**: migration `V17`, domain model, repository port and JPA adapter, adapter test.
- **#94 (backend)**: use case and service (start, log or change a set), controller, security rule, tests, regenerated `schema.d.ts`.
- **#95 (frontend)**: route and logger page with set rows, the start call, states, strings.
- **#96 (frontend)**: entry panel and tick-to-log, validation, retry that keeps input.
- **#97 (frontend)**: rest timer.

Each task is one commit in the story's pull request.
