# Implementation Plan: Client sees their progress

**Branch**: `feat/157-statistics-queries` | **Date**: 2026-11-10 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/156-client-progress/spec.md`

## Summary

A client reads how their last 4, 8 or 12 plan weeks went on the Progress tab, and their trainer reads the same numbers for each of their clients. Everything is derived on read; nothing is stored. A new `progress` feature holds it on both sides. #157 adds a read model over the session log tables (`JdbcTrainingLog`): one query gives, for each exercise in each session log of a client, its top set and the sum and count of its RPE; a second gives the days of finished session logs. The domain turns these and the client's plan into plan weeks: sessions planned and done, mean RPE, and each exercise's top set and mean RPE (`TrainingWeek`). #158 adds `ViewProgressUseCase` and two endpoints, `GET /api/v1/progress` for the client and `GET /api/v1/clients/{id}/progress` for their trainer, which slice the weeks to the period and add the comparisons, the weeks in a row, "PR today" and each exercise's recent sessions. #159 adds the shadcn chart (Recharts) and the series map of the design system, #160 the Progress tab with its tiles and sessions chart, #161 "By exercise". No migration is added.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1) backend; TypeScript, React 19 frontend

**Primary Dependencies**: Existing: Spring MVC, Spring JDBC (`JdbcTemplate`, as the other read models), Lombok; React Router, TanStack Query, react-i18next, shadcn (`card` with its `metric` variant, `badge`, `toggle-group`, `table`, `skeleton`, `alert`, `empty`, present). New: shadcn `chart` with Recharts 3.10 (#159, see research R10), shadcn `progress` (#160, the sessions bar)

**Storage**: PostgreSQL, unchanged; no migration

**Testing**: `@DataJpaTest` with Testcontainers for the read model (#157, known logs); plain unit tests for the weekly figures and the service with hand-written fakes and a fixed clock; `@WebMvcTest` for the controller; Vitest + Testing Library for the tab and its pure helpers. Docker must be running for the backend gate.

**Target Platform**: Browser, client screens at 375 px; same-origin `/api/v1`

**Project Type**: Web application (backend + frontend in one repository)

**Performance Goals**: One request per period; a fixed number of database reads (the account, the plan with the exercise library, the two log queries) whatever the number of sessions; a plan has at most 16 weeks of at most 6 sessions (research R2).

**Constraints**: Another trainer's client answers 404 like an unknown one; weeks and today are the client's; loads in the reader's unit; reading changes nothing; every string in English and Polish; a feature does not import another feature; no state by colour alone; chart series keep their fixed meanings.

**Scale/Scope**: One client per request; at most 96 sessions per plan and a few thousand set logs.

## Constitution Check

*GATE: passes before research; re-checked after design.*

| Principle | Check | Result |
|---|---|---|
| I. Hexagonal boundaries | A new `progress` feature: an output port in `domain.output` with a JDBC adapter over the log tables, as `planchange`'s `JdbcTrainingHistory` reads other features' tables (research R1); a use case in `domain.input`, a plain service wired in `ProgressConfig`. It depends on the input ports of `account`, `client` and `plan` and on `exercise`'s model; nothing depends on it, so no cycle. `HexagonalArchitectureTest` runs in `check`. | Pass |
| II. One contract | #158 regenerates `schema.d.ts` in its pull request; errors are codes the frontend translates (`client.not-found`, `plan.not-found`, the new `progress.period-invalid`). | Pass |
| III. Secure by default | `/api/v1/clients/**` already needs role TRAINER and the use case checks ownership first (`ViewClientsUseCase.getClient`); `/api/v1/progress/**` is added to the CLIENT paths, and a client reads only their own. No public endpoint. Nothing reaches an LLM. | Pass |
| IV. Explainable plan changes | Not touched; the weekly figures follow the same definitions as the rules of spec 125, so the numbers a client sees agree with them. | n/a |
| V. Tests verify behaviour | The read model against PostgreSQL with known logs (top set order, nulls, ties, another client's logs); the weekly figures at the publish day and week boundaries; the service for the period, comparisons, streak, PR and 404; the controller for shape and errors; the tab for what the client sees. No getter tests. | Pass |
| VI. Simplicity | No stored figures. One query per client for all logged exercises instead of one per figure; weeks, period and comparisons computed once in the domain from those rows (research R2). One response for client and trainer. Chart helpers stay in the feature until task #108 needs them (research R11). | Pass |
| VII. One design system | The tab from Kinetiq tokens and shadcn components; charts by section 3.7 (series tokens, finding titles, table toggle, target band, popover tooltips); checked in the browser in light and dark at 375 px; EN and PL strings. | Pass |

No violations, so no Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/156-client-progress/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── progress-api.md
├── checklists/
│   └── requirements.md
└── tasks.md             # /speckit-tasks
```

### Source Code (repository root)

```text
backend/src/main/java/com/kinetiq/
├── progress/
│   ├── domain/
│   │   ├── model/TopSet.java                       # #157: a set's load, amount and RPE, and which of two is better
│   │   ├── model/LoggedExercise.java               # #157: one exercise in one session log: date, top set, RPE sum, sets
│   │   ├── model/TrainingWeek.java                 # #157: one plan week: planned, done, mean RPE, top set per exercise
│   │   ├── output/TrainingLog.java                 # #157: logged exercises and finished days of a client
│   │   ├── model/Period.java                       # #158: 4, 8 or 12 weeks
│   │   ├── exception/PeriodInvalidException.java   # #158: progress.period-invalid, 400
│   │   ├── input/ViewProgressUseCase.java          # #158: own(client, period), ofClient(trainer, client, period)
│   │   ├── input/Progress.java                     # #158: what both read
│   │   └── service/ProgressViewService.java        # #158
│   └── infrastructure/
│       ├── output/JdbcTrainingLog.java             # #157
│       ├── config/ProgressConfig.java              # #158
│       ├── input/ProgressController.java           # #158: GET /api/v1/progress, GET /api/v1/clients/{id}/progress
│       ├── input/ProgressResponse.java             # #158
│       └── input/ProgressWebMapper.java            # #158: exercise names in the request language
└── common/infrastructure/config/SecurityConfig.java   # #158: + /api/v1/progress/** for CLIENT
backend/src/test/java/com/kinetiq/progress/...      # JdbcTrainingLogIT, TrainingWeekTest (#157);
                                                    # ProgressViewServiceTest, ProgressControllerTest (#158)
CONTEXT.md                                          # #157: Top set; #158: Week streak

frontend/
├── package.json, package-lock.json                 # #159: recharts
└── src/
    ├── components/ui/chart.tsx                     # #159: shadcn CLI, then the section 3.7 styles
    ├── components/ui/progress.tsx                  # #160: shadcn CLI, the sessions bar
    ├── lib/chart-series.ts                         # #159: the five series: token, line width, dash
    ├── lib/api/schema.d.ts                         # #158: regenerated
    ├── features/progress/
    │   ├── api/progress.ts                         # #160: useProgress(weeks)
    │   ├── findings.ts                             # #160: chart titles and the RPE target place; #161: exercise trend
    │   ├── components/chart-card.tsx               # #160: title, subtitle, "Show as table"
    │   ├── components/progress-tiles.tsx           # #160
    │   ├── components/sessions-chart.tsx           # #160
    │   ├── components/exercise-progress.tsx        # #161
    │   └── pages/progress-page.tsx                 # #160; #161 adds "By exercise"
    ├── app/router.ts                               # #160: the page replaces the placeholder
    └── locales/{en,pl}.json                        # #158 (error code), #160, #161
```

**Structure Decision**: Statistics are their own feature, `progress`, on both sides, because they read logs and plans and neither `sessionlog` nor `plan` should grow a second purpose (research R1). The read model reads the log tables directly, as `planchange` does, so no class of `sessionlog` is used. The chart component and the series map are shared (`components/ui`, `lib`) from #159, because every chart of the product uses them; the Progress tab's own chart cards stay in `features/progress` until task #108 also needs them and moves them up.

## Phases

- **#157 (backend)**: the read model and its port, `TopSet`, `LoggedExercise` and `TrainingWeek` with the weekly figures, `JdbcTrainingLogIT` with known logs and `TrainingWeekTest`, "Top set" in `CONTEXT.md`. The spec and this plan go in its pull request.
- **#158 (backend)**: the use case, `Period`, the service (period, comparisons, streak, PR, recent sessions, exercise order), the controller with both endpoints, the security path, tests including 404 for another trainer's client, regenerated `schema.d.ts`, the new error code in both locales, "Week streak" in `CONTEXT.md`. Blocked by #157.
- **#159 (frontend)**: the shadcn chart, Recharts, the section 3.7 styles, `lib/chart-series.ts`; checked in light and dark on a sample chart that is not committed. Blocked by nothing.
- **#160 (frontend)**: the Progress tab with the period switch, the four tiles, the sessions chart, empty, loading and error states, strings, tests. Blocked by #158 and #159.
- **#161 (frontend)**: "By exercise" with its picker, chart, table toggle and recent sets, strings, tests. Blocked by #160.

Each task is its own pull request, squash merged.
