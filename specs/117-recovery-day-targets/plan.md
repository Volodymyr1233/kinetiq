# Implementation Plan: Today's targets are lightened on a recovery day

**Branch**: `feat/117-recovery-day-targets` | **Date**: 2026-11-09 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/117-recovery-day-targets/spec.md`

## Summary

On a Recovery day check-in, today's targets are derived lighter by one pure rule in `readiness`:
- **Entries with a load**: load −10%, rounded in the client's unit with a half going lighter.
- **Entries without a load**: one repetition fewer, or 10% fewer seconds.
- **Every entry**: RPE −1, never below 5.

Nothing about the plan is written. The only new stored value is the client's choice, `LIGHTER` or `ORIGINAL`, as a column on `check_ins`. Saving different answers resets it, and it is fixed with the check-in once a set is logged.
- **Today**: `GET /api/v1/check-ins/today` carries the adjustment, and `PUT /api/v1/check-ins/today/targets` switches it.
- **Logger**: `sessionlog` asks readiness for the adjustment of a session's date, shows old → new, and copies the targets in use into each set log.

On the frontend, `app/` passes readiness's adjustment into the plan's Today page. A shared component draws old → new, and "Use original targets" switches at once with a `sonner` Undo message.

One migration (`V22`), no new dependency.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1) backend; TypeScript, React 19 frontend

**Primary Dependencies**: Existing only (Spring MVC, Spring Data JPA, Flyway; React Router, TanStack Query, react-i18next, shadcn `Badge`, `Button`, `Card`, `sonner`)

**Storage**: PostgreSQL; `V22__add_check_in_target_choice.sql` adds `check_ins.target_choice`; demo rows in `db/dev-data/R__demo_check_ins.sql` and `R__demo_session_logs.sql`

**Testing**:
- Backend: JUnit 5 + AssertJ for the domain (`AdjustmentTest` with every Edge Cases example in kg and lb, `CheckInTest`, `CheckInServiceTest` and `SessionLogServiceTest` with fakes and a fixed clock), `@DataJpaTest` Testcontainers `CheckInRepositoryImplIT` for the new column, `@WebMvcTest` for `CheckInControllerTest` and the session-log mapping.
- Frontend: Vitest + Testing Library for Today adjusted, the switch with Undo, the readiness lines, the logger's old → new, and the shared `TargetChange`.
- Docker is needed for the backend `check`.

**Target Platform**: Browser, client screens at 375 px; same-origin `/api/v1`

**Project Type**: Web application (backend + frontend in one repository)

**Performance Goals**: The adjustment arrives with the existing check-in read; the switch shows at once (optimistic) and is confirmed by one `PUT`.

**Constraints**:
- The plan is never written.
- The client and the day never come from the request.
- Nothing is red.
- Every string exists in EN and PL.
- Touch targets are 48 px, with no sideways scroll at 375 and 320 px.
- Struck-through values carry an accessible text.

**Scale/Scope**: One column; at most one adjustment per client per day; sessions of up to a few dozen entries.

## Constitution Check

| Principle | Check | Result |
|---|---|---|
| I. Hexagonal boundaries | The rule, the choice and the use case stay in `readiness.domain`; readiness now also reads `account.domain.model.WeightUnit`, `exercise.domain.model.Measure` and `CurrentAccountUseCase`, none of which depend on readiness. `sessionlog → readiness` already exists; `plan` gains no dependency (research R1). The frontend crosses features only in `app/` (R8). `HexagonalArchitectureTest` runs in `check`. | Pass |
| II. One contract | One new endpoint and two changed answers, generated into `schema.d.ts` in the same pull request; new code `check-in.no-adjustment` translated by the frontend. | Pass |
| III. Secure by default | It sits under the existing CLIENT rules. Nothing is addressable by id but the client's own session logs, which already answer `404` for another client, and a test covers adjusted ones. Client B never sees or changes A's choice (test). Nothing goes to an LLM. | Pass |
| IV. Explainable, approved plan changes | The adjustment is a deterministic rule (FR-006). Every change shows old → new and its reason (FR-016, FR-007), and is reversible by one tap (FR-020, FR-021). The plan itself is not changed, and a test proves it (FR-013). | Pass |
| V. Tests verify behaviour | Rule edges in both units, floors, no-load measures and "nothing changes"; reset on changed answers; refusals; copies on set logs; plan unchanged; ownership; UI switch, Undo, rollback, fixed state, accessible text. No getter or snapshot tests. | Pass |
| VI. Simplicity | One column, derived targets, no new table, feature or dependency. The race between a switch and the first set is named with `ponytail:` (R7). The `adjusted` prop is the existing slot pattern. | Pass |
| VII. One design system | Design-system tokens `adjusted`/`adjusted-muted` and a `Badge` variant for them; `num` for values; `Button` and `sonner`. Checked in the browser in light and dark at 375 px, EN and PL. | Pass |

No violations, so no Complexity Tracking. Re-checked after Phase 1: unchanged.

## Project Structure

### Documentation (this feature)

```text
specs/117-recovery-day-targets/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/targets-api.md
├── checklists/requirements.md
└── tasks.md             # /speckit-tasks
```

### Source Code (repository root)

```text
backend/src/main/
├── java/com/kinetiq/
│   ├── readiness/
│   │   ├── domain/model/{Adjustment, Target, Planned, Cause, TargetChoice}.java   # new
│   │   ├── domain/model/CheckIn.java                                         # + targetChoice, choose, reset on change
│   │   ├── domain/input/{CheckInUseCase, TodayCheckIn}.java                  # + choose, adjustmentOn, adjustment
│   │   ├── domain/input/ChooseTargetsCommand.java                            # new
│   │   ├── domain/exception/NoAdjustmentException.java                       # new, check-in.no-adjustment
│   │   ├── domain/service/CheckInService.java                                # + CurrentAccountUseCase
│   │   └── infrastructure/
│   │       ├── config/ReadinessConfig.java
│   │       ├── input/{CheckInController, CheckInWebMapper, TodayCheckInResponse}.java
│   │       ├── input/ChooseTargetsRequest.java                               # new
│   │       └── output/{CheckInEntity, CheckInMapper}.java                    # + target_choice
│   ├── sessionlog/
│   │   ├── domain/model/SessionLog.java                                      # logSet copies the targets in use
│   │   ├── domain/input/StartedSession.java                                  # + adjustment
│   │   ├── domain/service/SessionLogService.java                             # asks CheckInUseCase.adjustmentOn
│   │   ├── domain/input/SessionStates.java                                   # + adjusted (#275)
│   │   ├── domain/service/SessionStateService.java                           # adjusted per session (#275)
│   │   ├── infrastructure/config/SessionLogConfig.java
│   │   ├── infrastructure/input/SessionStatesResponse.java                   # + adjusted (#275)
│   │   └── infrastructure/input/{SessionLogResponse, SessionLogWebMapper}.java   # + planned; set copies
├── resources/db/migration/V22__add_check_in_target_choice.sql
└── resources/db/dev-data/{R__demo_check_ins, R__demo_session_logs}.sql
backend/src/test/java/com/kinetiq/{readiness,sessionlog}/...

frontend/src/
├── components/target-change.tsx (+ test)                 # old struck → new, accessible text
├── components/value-list.tsx                             # values apart by hidden middle dots (#274)
├── components/ui/badge.tsx                               # + variant adjusted (design system)
├── features/readiness/
│   ├── api/check-in.ts                                   # + useChooseTargets (optimistic, rollback)
│   ├── components/adjustment.tsx (+ test)                # mark (badge + switch + Undo), reason, note
│   └── components/readiness-card.tsx                     # lighter line, fixed-with-targets line
├── features/plan/
│   ├── pages/today-page.tsx (+ test)                     # + adjusted prop: mark, reason, targets
│   ├── components/entry-row.tsx                          # optional today target → TargetChange
│   └── components/{session-card, week-strip}.tsx         # "Adjusted" chip in the Plan week (#275)
├── features/sessionlog/pages/logger-page.tsx (+ test)    # entry.planned → old → new line under the name
├── app/today-route.tsx                                   # passes readiness's adjustment to TodayPage
├── lib/query-keys.ts                                     # SESSION_LOGS_KEY, shared by sessionlog and readiness
├── lib/api/schema.d.ts                                   # regenerated
└── locales/{en,pl}.json                                  # readiness.adjustment.*, errors.check-in.no-adjustment
```

**Structure Decision**: The adjustment belongs to `readiness` (research R1), and `sessionlog` reads it through readiness's input port. On the frontend, `app/` composes readiness into the plan's Today page, and the one piece both `plan` and `sessionlog` draw moves to `components/` (research R8).

## Phases

- **#118 (backend)**: `Target`, `Planned`, `Cause`, `Adjustment` (`lighter`, `of`, `inUse`) with `AdjustmentTest`, covering:
  - every example of the spec's Edge Cases in kg and lb, halves included;
  - the RPE floor;
  - entries without a load, in repetitions and in seconds;
  - the floors of FR-002 and FR-004;
  - "nothing changes" giving no adjustment;
  - the order of the causes.
- **#119 (backend)**:
  - migration `V22`;
  - `TargetChoice` on `CheckIn`, with the reset on changed answers;
  - `CheckInUseCase.today`, `choose` and `adjustmentOn`, with `NoAdjustmentException` and its handler mapping;
  - controller endpoint and response;
  - `sessionlog`: start and find carry the adjustment, `logSet` copies the targets in use, the mapper adds `planned` and uses set copies;
  - tests: refusals, idempotence, plan unchanged, ownership, set copies;
  - regenerated `schema.d.ts`, demo data.
- **#120 (frontend)**:
  - `TargetChange` and the `adjusted` badge variant;
  - the Today page's `adjusted` prop, with `AdjustmentMark` (badge only), `AdjustmentReason` and `today-route` wiring;
  - the readiness card's lighter and fixed lines;
  - the logger's old → new;
  - strings in EN and PL; tests.
- **#121 (frontend)**:
  - `useChooseTargets` (optimistic, rollback, invalidate the session log);
  - "Use original targets" and "Use lighter targets" in `AdjustmentMark`, with focus kept on the switch, an `sr-only` status and the `sonner` Undo message;
  - the fixed state;
  - strings; tests; browser check at 375 px in light and dark, EN and PL.

- **#275 (both sides, added after review)**: `adjusted` per session in `GET /api/v1/session-states` (research R12), with tests; regenerated `schema.d.ts`; the "Adjusted" chip in the Plan week.
- **#274 (bug, frontend)**: `ValueList` for target values on Today, the Plan week and the logger; no "Today" chip on the Today tab; "Continue session" while in progress; the rest timer kept across tabs; optional band lines.

Commit order in the story's pull request: #118, #119, #120, #121, #275, #274. Each is one commit ending with `(#<task>)`.
