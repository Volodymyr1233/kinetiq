# Implementation Plan: Client checks in and sees readiness

**Branch**: `feat/111-readiness-score` | **Date**: 2026-10-30 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/109-readiness-checkin/spec.md`

## Summary

A client answers sleep, soreness and stress (1 to 5) on Today before the day's first session and sees a readiness score (0 to 100) and band. The story has four tasks. This branch builds **#111 only**: the calculation of the score and the band, as pure domain code in a new `readiness` feature. #110 (check-in model and API), #112 (check-in on Today) and #113 (readiness card) are planned here at the level needed to keep #111's design compatible with them; each is planned in detail when its task starts.

The calculation is a value object `Readiness(score)` with a factory from the three answers and a `band()` method, plus an enum `ReadinessBand`. Nothing is stored: the score follows from the answers (spec Assumptions).

## Technical Context

**Language/Version**: Java 25 (backend); TypeScript 6, React 19 (frontend, later tasks)

**Primary Dependencies**: none beyond the JDK for #111; Lombok and SLF4J are allowed in domain code but not needed. #110 adds Spring Data JPA and Flyway (already in the project); #112 and #113 use the existing shadcn components.

**Storage**: none for #111. #110: one PostgreSQL table `check_ins` (see [data-model.md](data-model.md))

**Testing**: JUnit 5 and AssertJ; plain unit tests for the domain (`*Test`), `@WebMvcTest` and `@DataJpaTest` with Testcontainers for #110; Vitest with Testing Library for #112 and #113

**Target Platform**: the Kinetiq backend (Spring Boot 4) and the client area of the SPA at 375 px

**Project Type**: web application, backend and frontend as separate builds

**Performance Goals**: none specific; the calculation is a handful of integer operations

**Constraints**: domain depends on nothing but the JDK, Lombok and SLF4J (constitution I); no LLM and no non-deterministic input in the calculation (constitution IV)

**Scale/Scope**: 125 possible combinations of answers, 101 possible scores, 3 bands

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Result | Note |
|---|---|---|
| I. Hexagonal Boundaries | Pass | `readiness.domain.model` depends only on `common.domain.exception`; `HexagonalArchitectureTest` runs in `./gradlew check`. #110 adds ports and adapters in the usual packages |
| II. One Contract | Pass | #111 changes no API. #110 changes it and regenerates `schema.d.ts` in its own pull request |
| III. Secure by Default | Pass | #110's endpoints need a signed-in client (spec: no public endpoints); a client reaches only their own check-in |
| IV. Explainable, Approved Plan Changes | Pass | Readiness is rule-based and deterministic, never an LLM; the later lightening of targets (#117) is its own story and shows old value, new value and reason |
| V. Tests Verify Behaviour | Pass | Tests try every combination, the examples, the band edges and the invalid input; no getter tests |
| VI. Simplicity | Pass | One record and one enum; no interface, no strategy, no configuration for the thresholds (named constants) |
| VII. One Design System | Not yet applicable | Applies to #112 and #113; checked then |

No violations; Complexity Tracking is empty.

## Phase 0: Research

See [research.md](research.md). No unknown remains.

## Phase 1: Design

- Entities and the table of #110: [data-model.md](data-model.md)
- Planned API of #110: [contracts/check-in-api.md](contracts/check-in-api.md)
- How to prove the task works: [quickstart.md](quickstart.md)

**Re-check after design**: unchanged. The planned table keeps only the answers; the score is derived, so changing a threshold later leaves no stale data.

## Project Structure

### Documentation (this feature)

```text
specs/109-readiness-checkin/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── check-in-api.md
├── checklists/
│   └── requirements.md
└── tasks.md
```

### Source Code (repository root)

```text
backend/src/main/java/com/kinetiq/readiness/
└── domain/
    └── model/
        ├── Readiness.java          # #111: score and band from three answers
        └── ReadinessBand.java      # #111: Ready, Take it easier, Recovery day
backend/src/test/java/com/kinetiq/readiness/
└── domain/model/
    └── ReadinessTest.java          # #111

# planned, built by later tasks:
#   #110  readiness/domain/{model,input,output,service,exception}, readiness/infrastructure/{config,input,output},
#         backend/src/main/resources/db/migration/V<next>__create_check_ins.sql
#   #112  frontend/src/features/readiness/{api,components}
#   #113  frontend/src/features/readiness/components (card), locales en.json and pl.json
```

**Structure Decision**: a new feature package `readiness`, hexagonal like `account` and `exercise`. Frontend code goes in `frontend/src/features/readiness`, imported by the Today page of the client area.

## Complexity Tracking

No violations to justify.
