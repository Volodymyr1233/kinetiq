# Implementation Plan: Client checks in and sees readiness

**Branch**: `feat/109-readiness-check-in` | **Date**: 2026-11-08 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/109-readiness-check-in/spec.md`

## Summary

On a session day, before the first set is logged, Today asks the client three 1–5 questions (sleep, soreness, stress). The third answer saves a **check-in**, one per client per date; the server derives **readiness** (0–100) and its **band** with one pure rule and the Today tab shows the readiness card. The answers can be changed ("Change answers", saved with "Done") until a set of today's session is logged; a skip is kept on the device for the day only. The backend adds a feature `readiness` with `GET` and `PUT /api/v1/check-ins/today` for role CLIENT; it reads today's state from `plan` and asks `sessionlog` whether a set is logged through a port it owns, so the dependency points `sessionlog → readiness`, the way story #117 needs. The frontend adds a feature `readiness` whose section `app/` places on the plan's Today page. One migration (`V21`), no new dependency.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1) backend; TypeScript, React 19 frontend

**Primary Dependencies**: Existing only (Spring MVC, Spring Data JPA, Flyway, MapStruct; React Router, TanStack Query, react-i18next, shadcn `ToggleGroup`, `Card`, `Alert`, `Button`, `Skeleton`)

**Storage**: PostgreSQL; migration `V21__create_check_ins.sql`: `check_ins`; demo rows in `db/dev-data/R__demo_check_ins.sql`

**Testing**: JUnit 5 + AssertJ (`Readiness` over all 125 combinations; `CheckInService` with fake ports and a fixed clock), `@DataJpaTest` Testcontainers `*IT` (repository adapter, `SessionLogTrainingProgress`), `@WebMvcTest` (controller), Vitest + Testing Library + `userEvent` (check-in, card, section). Docker is needed for the backend `check`.

**Target Platform**: Browser, client screens at 375 px; same-origin `/api/v1`

**Project Type**: Web application (backend + frontend in one repository)

**Performance Goals**: Readiness visible within 2 s of the third tap (SC-001): one `PUT`, one row.

**Constraints**: No id or date from the request; a refused save changes nothing; never red for a band; every string in English and Polish; 48 px touch targets; no sideways scroll at 375 px and 320 px; arrow keys never choose.

**Scale/Scope**: One row per client per training day.

## Constitution Check

| Principle | Check | Result |
|---|---|---|
| I. Hexagonal boundaries | New feature `readiness` with `domain.{model,input,output,service,exception}` and `infrastructure.{config,input,output}`. It depends on `plan` (`ViewOwnPlanUseCase.today`) and `account` (`AccountId`). Its output port `TrainingProgress` is implemented in `sessionlog.infrastructure.output`, so `sessionlog → readiness`; no cycle now or with #117 (research R2). `HexagonalArchitectureTest` runs in `check`. | Pass |
| II. One contract | Two endpoints with generated OpenAPI types; `schema.d.ts` regenerated in the same pull request; codes `check-in.training-started`, `check-in.no-session-today`, `validation.failed`, translated by the frontend. | Pass |
| III. Secure by default | `/api/v1/check-ins/**` added to the role CLIENT rule; client and day from the sign-in only; no path id, so no other person's check-in is addressable, and a test proves client B never sees A's; no answers in logs; nothing goes to an LLM. Health data covered by the join consent (spec 30). | Pass |
| IV. Explainable, approved plan changes | Readiness is a deterministic rule in the domain; no plan change and no adjustment here (#117). | Pass |
| V. Tests verify behaviour | Rule over every combination and both examples; refusals; idempotent save; ownership; UI behaviour (third answer saves, arrows move only, skip, retry, change and cancel, card after training started). No getter or snapshot tests. | Pass |
| VI. Simplicity | Score not stored; one save operation; no new dependency; skip in browser storage, not a table; two `// ponytail:` races named (research R4, R7). The port in R2 is the architecture's way to name a dependency, not an abstraction for later. | Pass |
| VII. One design system | `ToggleGroup`, `Card`, `Alert`, `Button` with Kinetiq tokens; `ready-high/mid/low` and their `-muted` for bands; `num` for the score; checked in the browser in light and dark at 375 px, EN and PL. | Pass |

No violations, so no Complexity Tracking. Re-checked after Phase 1: unchanged.

## Project Structure

### Documentation (this feature)

```text
specs/109-readiness-check-in/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/check-in-api.md
├── checklists/requirements.md
└── tasks.md             # /speckit-tasks
```

### Source Code (repository root)

```text
backend/src/main/
├── java/com/kinetiq/
│   ├── readiness/
│   │   ├── domain/model/{CheckIn, CheckInId, Answers, Readiness}.java      # Readiness holds the Band enum
│   │   ├── domain/input/{CheckInUseCase, SaveCheckInCommand, TodayCheckIn}.java
│   │   ├── domain/output/{CheckInRepository, TrainingProgress}.java
│   │   ├── domain/service/CheckInService.java
│   │   ├── domain/exception/{NoSessionTodayException, TrainingStartedException}.java
│   │   └── infrastructure/
│   │       ├── config/ReadinessConfig.java
│   │       ├── input/{CheckInController, SaveCheckInRequest, TodayCheckInResponse, CheckInWebMapper}.java
│   │       └── output/{CheckInEntity, JpaCheckInRepository, CheckInMapper, CheckInRepositoryImpl}.java
│   ├── sessionlog/infrastructure/output/SessionLogTrainingProgress.java  # implements readiness's TrainingProgress
│   └── common/infrastructure/config/SecurityConfig.java                 # + /api/v1/check-ins/** hasRole CLIENT
├── resources/db/migration/V21__create_check_ins.sql
└── resources/db/dev-data/R__demo_check_ins.sql
backend/src/test/java/com/kinetiq/readiness/...
backend/src/test/java/com/kinetiq/sessionlog/infrastructure/output/SessionLogTrainingProgressIT.java

frontend/src/
├── features/readiness/
│   ├── api/check-in.ts                                  # useCheckIn, useSaveCheckIn
│   ├── components/{check-in-card, answer-group, readiness-card}.tsx (+ tests)
│   ├── components/readiness-section.tsx (+ test)        # picks check-in, skipped line, card or nothing
│   └── skip.ts                                          # skipped-on-date in localStorage, guarded
├── features/plan/pages/today-page.tsx                   # + beforeSession slot above the session card
├── app/today-route.tsx                                  # TodayPage with ReadinessSection
├── app/router.tsx                                       # /app/today → app/today-route
├── lib/api/schema.d.ts                                  # regenerated
└── locales/{en,pl}.json                                 # readiness.*, errors.check-in.*
```

**Structure Decision**: A feature of its own on both sides (research R1), because readiness grows with #117 and #122. It reads the plan only through `ViewOwnPlanUseCase.today`, and learns about set logs only through its own port. On the frontend, `app/` composes the two features on one tab (research R8).

## Phases

- **#111 (backend)**, first because it depends on nothing and #110's response carries its result: `Answers`, `Readiness` and `Band` with `ReadinessTest` (all 125 combinations, 2/4/3 → 33, 4/2/1 → 83, edges 42/50/67/75).
- **#110 (backend)**: migration `V21`, `CheckIn` and `CheckInId`, repository port and JPA adapter with `IT`; `CheckInUseCase` and `CheckInService` (today, save, refusals), `TrainingProgress` and its `sessionlog` adapter, controller, request and response, security rule, tests, regenerated `schema.d.ts`, demo data.
- **#112 (frontend)**: `check-in.ts` hooks, `AnswerGroup` (ToggleGroup as a radio group), `CheckInCard` (saves on the third answer, Skip, saving and error states), skip storage, `ReadinessSection`, the slot on Today and `app/today-route.tsx`, strings.
- **#113 (frontend)**: `ReadinessCard` (score, band, line, answers, ready-* colours, Change answers with Done and Cancel, fixed state), focus and live announcement, strings.

Commit order in the story's pull request: #111, #110, #112, #113. Each is one commit ending with `(#<task>)`.
