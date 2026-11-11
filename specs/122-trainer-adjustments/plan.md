# Implementation Plan: Trainer sees a client's adjustments

**Branch**: `feat/122-trainer-adjustments` | **Date**: 2026-11-10 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/122-trainer-adjustments/spec.md`

## Summary

The trainer's client overview gains the client's last 14 days of readiness and their recovery days. The trainer's session log gains the lighter targets of a recovery day. Nothing is stored. One pull request for story #122, with one commit per task:

- **#123, backend**: `ClientTrainingViewService.overview` (in `sessionlog`) reads the 14 days of check-ins through a new `CheckInUseCase.between`. It builds one `ReadinessDay` per date. For each check-in in the Recovery day band it builds a `RecoveryDay`: the score, `Cause.of(answers)`, the outcome `Optional<TargetChoice>` from the existing `adjustmentOn` over the session log's copy or the plan's session, and the day's `ClientSession`. `GET /api/v1/clients/{id}/overview` returns `readiness` and `recoveryDays`.
- **#124, frontend**: A card "Readiness, last 14 days" becomes the first card of the overview grid. It holds a strip of 14 plain flex bars in the readiness scale, each with a written score and a text alternative, and under it the recovery-day entries with reason, outcome and session link. The Logs tab keeps its chosen session in `?session=`, so the link opens that log.
- **#290, both sides**: The trainer's session log passes the day's adjustment instead of `null`, so `planned` and the unlogged sets' targets follow it. It adds `adjustment { causes, inUse }`. The log table shows `TargetChange`, and the detail shows the reason line.

No migration and no new dependency. The demo data gains one `ORIGINAL` choice.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1) backend; TypeScript, React 19 frontend

**Primary Dependencies**: Existing only. Backend: Spring MVC, Spring Data JPA. Frontend: React Router (`useSearchParams`), TanStack Query, react-i18next, and the shadcn `Card`, `Badge`, `Button` and `Table`, plus the shared `TargetChange` and `SessionStateBadge`.

**Storage**: PostgreSQL, read only: `check_ins`, `session_logs` and the plan tables. One derived query on the existing unique `(client_id, check_in_date)` index. Demo change in `db/dev-data/R__demo_check_ins.sql`.

**Testing**:
- **Backend domain**: `ClientTrainingViewServiceTest`, plain with fakes and a fixed clock in a non-UTC time zone. It covers 14 dates, empty days, a score of 0, every outcome (lighter, original, none), the session from a log, from the plan or none, a day before the publish day, and the session log with an adjustment.
- **Backend domain, readiness**: `CauseTest` for the moved rule, if the existing `AdjustmentTest` does not already pin it.
- **Backend persistence**: `CheckInRepositoryImplIT` for `between`, covering both ends included, oldest first and another client excluded.
- **Backend web**: `ClientTrainingControllerTest` with `@WebMvcTest` covers the new fields, fields left out, `404` for another trainer's client, `403` for a client and `401` for a visitor.
- **Frontend**: Vitest and Testing Library cover the strip (14 items, empty versus 0, text alternatives, empty messages), the entries (order, reason, outcome, link or no link), the Logs tab opened by `?session=`, and `SessionLogTable` and the detail line with an adjustment.
- Docker is needed for the backend `check`.

**Target Platform**: Browser, trainer screens at 1280 px; same-origin `/api/v1`

**Project Type**: Web application (backend and frontend in one repository)

**Performance Goals**: No new request; the overview adds one indexed query for at most 14 rows and one `adjustmentOn` per recovery day (a few at most).

**Constraints**:
- The 14 days are in the client's time zone.
- A missing check-in is never shown as 0.
- Nothing is red.
- The band is never shown by colour alone.
- Every string exists in EN and PL.
- Trainer ownership is checked in the use case, and another trainer's client gets `404`.
- No personal data in logs.

**Scale/Scope**: 14 days per client; one card, one table change, one URL parameter.

## Constitution Check

| Principle | Check | Result |
|---|---|---|
| I. Hexagonal boundaries | `sessionlog.domain` uses `readiness.domain.input.CheckInUseCase` and `readiness.domain.model` types (`CheckIn`, `Readiness`, `Cause`, `TargetChoice`, `Adjustment`). That dependency already exists, and readiness gains no dependency on `sessionlog`. The repository method stays behind `CheckInRepository`. On the frontend, `sessionlog` imports no other feature: the band map moves to `lib/readiness.ts` and the cause strings are i18n keys. `HexagonalArchitectureTest` runs in `check`. | Pass |
| II. One contract | Two changed responses, generated into `schema.d.ts` in the same pull request; no new error code. | Pass |
| III. Secure by default | It uses the existing `TRAINER` routes, and `clients.getClient(trainer, client)` runs first in both use cases. Another trainer's client gets `404` and a client gets `403`, with tests. No new endpoint, and nothing goes to an LLM. Answers are not logged. | Pass |
| IV. Explainable, approved plan changes | The trainer now sees the old value, the new value and the reason of each automatic adjustment (#290). The adjustment is still the existing rule, and nothing changes the plan. | Pass |
| V. Tests verify behaviour | Tests cover window edges, empty versus 0, each outcome, session sources, ownership, rendering and link behaviour. There are no getter, mapping-only or snapshot tests. | Pass |
| VI. Simplicity | Nothing is stored, and there is no new table, port, enum or dependency. `Optional<TargetChoice>` stands for the three outcomes, plain divs replace a chart, and the Logs tab gets a URL parameter rather than a new route. For days not started the plan's targets are used, as `SessionStateService` already does with its `ponytail:` note. | Pass |
| VII. One design system | `ready-*` tokens, the `num` class, `Card`, `Badge`, `SessionStateBadge` and `TargetChange` are used, with no palette classes. It is checked in the browser at 1280 px, in light and dark, EN and PL. | Pass |

No violations, so the plan has no Complexity Tracking section. Re-checked after Phase 1: unchanged.

## Project Structure

### Documentation (this feature)

```text
specs/122-trainer-adjustments/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/overview-api.md
├── checklists/requirements.md
└── tasks.md             # /speckit-tasks
```

### Source Code (repository root)

```text
backend/src/main/java/com/kinetiq/
├── readiness/
│   ├── domain/model/Cause.java                       # + of(Answers), moved from Adjustment
│   ├── domain/model/Adjustment.java                  # uses Cause.of
│   ├── domain/input/CheckInUseCase.java              # + between(client, from, to)
│   ├── domain/output/CheckInRepository.java          # + findAllByClientAndDateBetween
│   ├── domain/service/CheckInService.java            # implements between
│   └── infrastructure/output/
│       ├── JpaCheckInRepository.java                 # + derived query
│       └── CheckInRepositoryImpl.java
└── sessionlog/
    ├── domain/input/ClientOverview.java              # + readiness, recoveryDays, ReadinessDay, RecoveryDay
    ├── domain/input/ClientSessionLog.java            # + Optional<Adjustment> (#290)
    ├── domain/service/ClientTrainingViewService.java # builds both; takes CheckInUseCase
    └── infrastructure/
        ├── config/SessionLogConfig.java              # passes CheckInUseCase
        └── input/
            ├── ClientOverviewResponse.java           # + readiness, recoveryDays
            ├── ClientSessionLogResponse.java         # + adjustment (#290)
            └── SessionLogWebMapper.java              # maps them; passes the adjustment to entries
backend/src/main/resources/db/dev-data/R__demo_check_ins.sql   # one ORIGINAL choice

backend/src/test/java/com/kinetiq/
├── readiness/infrastructure/output/CheckInRepositoryImplIT.java
├── sessionlog/domain/service/ClientTrainingViewServiceTest.java
├── sessionlog/domain/service/{SessionLogServiceTest,SessionStateServiceTest}.java  # fakes gain between
└── sessionlog/infrastructure/input/ClientTrainingControllerTest.java

frontend/src/
├── lib/readiness.ts                                  # BAND_STYLE, moved
├── lib/api/schema.d.ts                               # regenerated
├── features/readiness/components/readiness-card.tsx  # imports BAND_STYLE
├── features/sessionlog/
│   ├── components/readiness-strip.tsx (+ .test.tsx)  # card: strip and entries (#124)
│   ├── components/session-log-table.tsx              # TargetChange for planned (#290)
│   ├── pages/client-overview-tab.tsx (+ test)        # card first in the grid
│   └── pages/client-logs-tab.tsx (+ test)            # ?session=, reason line (#124, #290)
└── locales/{en,pl}.json
```

**Structure Decision**: The existing web application layout. Backend work goes in `sessionlog` (it builds the overview) and `readiness` (it owns check-ins). Frontend work goes in `features/sessionlog`, which owns the overview and Logs tabs, and the one shared map moves to `lib/`.

## Commits (one per task)

1. `feat(backend): add readiness and recovery days to the client overview (#123)`: backend, tests, demo data, regenerated `schema.d.ts`.
2. `feat(frontend): add the readiness strip and recovery days to the overview (#124)`: card, `?session=` in the Logs tab, strings, tests.
3. `feat: show lighter targets in the trainer's session log (#290)`: both sides, with `schema.d.ts` regenerated again.

The pull request closes #123, #124 and #290, and with them #122.
