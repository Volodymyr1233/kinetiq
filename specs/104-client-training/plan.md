# Implementation Plan: Trainer sees a client's training

**Branch**: `feat/105-client-overview-api` | **Date**: 2026-11-08 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/104-client-training/spec.md`

## Summary

A trainer reads one of their own clients' training: this week, the recent sessions and the quiz answers on the Overview tab, and every logged set on the Logs tab. Everything is derived on read; nothing is stored. #105 adds `ViewClientTrainingUseCase` to the `sessionlog` feature and `GET /api/v1/clients/{id}/overview`: the ownership check of the client page (`ViewClientsUseCase.getClient`, `404 client.not-found`), the client's week with the session states of #99, the four most recent sessions (session logs plus Missed sessions never started), each with `locked`, sets logged out of planned and average RPE, and the quiz answers. `SessionLog` gains `setsPlanned()` and `averageRpe()`, which the session summary of #99 then reuses, and the repository reads every session log of a client in three queries. #106 builds the Overview tab, #107 the logs API and tab, #261 the client's note on it, and #108 the progress section on top of story #156. No dependency and no migration are added.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1) backend; TypeScript, React 19 frontend

**Primary Dependencies**: Existing only: Spring MVC, Spring Data JPA (Hibernate 7, `@Fetch(FetchMode.SUBSELECT)`), MapStruct, Lombok; React Router, TanStack Query, react-i18next, shadcn (`card`, `badge`, `table`, `skeleton`, `alert`, present); `chart` with #108 (#159)

**Storage**: PostgreSQL, unchanged; no migration

**Testing**: JUnit 5 + AssertJ (service with hand-written fakes and a fixed clock), `@WebMvcTest` (controller), `@DataJpaTest` with Testcontainers (repository), Vitest + Testing Library (tabs). Docker must be running for the backend gate.

**Target Platform**: Browser, trainer screens at 1280 px; same-origin `/api/v1`

**Project Type**: Web application (backend + frontend in one repository)

**Performance Goals**: An overview in one request; the client's session logs in three queries whatever their number, and a client has at most 96 sessions (research R5, R6).

**Constraints**: Another trainer's client answers 404 like an unknown one; week and today are the client's; loads in the trainer's unit; reading changes nothing; every string in English and Polish; a feature does not import another feature; no state by colour alone.

**Scale/Scope**: One client per request; at most 96 sessions per plan, a few hundred set logs.

## Constitution Check

*GATE: passes before research; re-checked after design.*

| Principle | Check | Result |
|---|---|---|
| I. Hexagonal boundaries | A use case in `sessionlog.domain.input`, a plain service wired in `SessionLogConfig`. `sessionlog` gains dependencies on `client` and `quiz` input ports only; no cycle (research R1). The controller calls one use case; entities stay in `infrastructure.output`. `HexagonalArchitectureTest` runs in `check`. | Pass |
| II. One contract | Each API task regenerates `schema.d.ts` in its pull request; errors are existing codes (`client.not-found`, `session-log.not-found`) that the frontend translates. | Pass |
| III. Secure by default | `/api/v1/clients/**` already needs role TRAINER; ownership is checked in the use case first, and a session log is read by id and client. Nothing reaches an LLM; the client's note and quiz notes are shown only to their trainer. | Pass |
| IV. Explainable plan changes | Not touched. | n/a |
| V. Tests verify behaviour | Service tests for each label and boundary with a fixed clock, the recent list, the plan states and 404; a controller test for shape and errors; a repository test for reading every log with its sets; tab tests for what the trainer sees. No getter tests. | Pass |
| VI. Simplicity | No stored figures, no new state value, one list rule for both tabs, the aggregate's own figures reused by the summary (research R4 to R7). One response record repeats the quiz answers' fields because a feature's web types are private (research R3). | Pass |
| VII. One design system | Tabs from Kinetiq tokens and shadcn components at trainer density, the existing badge and quiz answers moved up and reused, checked in the browser in light and dark at 1280 px; EN and PL strings. | Pass |

No violations, so no Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/104-client-training/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── client-training-api.md
├── checklists/
│   └── requirements.md
└── tasks.md             # /speckit-tasks
```

### Source Code (repository root)

```text
backend/src/main/java/com/kinetiq/sessionlog/
├── domain/
│   ├── input/ViewClientTrainingUseCase.java             # #105: overview; #107: sessions, session log
│   ├── input/ClientOverview.java                        # #105: today, week, recent sessions, quiz answers (class, Optional getters)
│   ├── input/ClientSession.java                         # #105: one session as the trainer reads it (class, Optional getters)
│   ├── model/SessionLog.java                            # #105: + setsPlanned(), averageRpe()
│   ├── model/SessionSummary.java                        # #105: uses them
│   ├── output/SessionLogRepository.java                 # #105: + findAllByClient
│   └── service/ClientTrainingViewService.java           # #105
└── infrastructure/
    ├── config/SessionLogConfig.java                     # #105: + bean
    ├── input/ClientTrainingController.java              # #105: GET /api/v1/clients/{id}/overview; #107: session logs
    ├── input/ClientOverviewResponse.java                # #105
    ├── input/SessionLogWebMapper.java                   # #105: + overview mapping
    └── output/{SessionLogEntity,JpaSessionLogRepository,SessionLogRepositoryImpl}.java   # #105: findAllByClient, subselect fetching
backend/src/test/java/com/kinetiq/sessionlog/...         # ClientTrainingViewServiceTest, ClientTrainingControllerTest,
                                                         # SessionLogRepositoryImplIT, the two repository fakes

frontend/src/
├── features/sessionlog/
│   ├── api/client-training.ts                           # #106: overview; #107: sessions, session log
│   ├── components/week-days.tsx                         # #106: this week's day strip
│   ├── pages/client-overview-tab.tsx                    # #106
│   └── pages/client-logs-tab.tsx                        # #107; #261: the note
├── features/plan/components/draft-panel.tsx             # #106: uses the moved quiz answers
├── features/plan/components/session-card.tsx            # #106: imports the moved badge
├── features/plan/pages/today-page.tsx                   # #106: imports the moved badge
├── components/quiz-answers.tsx                          # #106: moved up from the draft panel
├── components/session-state-badge.tsx                   # #106: moved up from features/plan, + locked
├── app/router.ts                                        # #106: overview index; #107: logs
├── lib/api/schema.d.ts                                  # #105, #107, #261: regenerated
└── locales/{en,pl}.json                                 # #106, #107, #261
```

**Structure Decision**: The trainer's view lives in the `sessionlog` feature on both sides, because it reads session logs and `sessionlog` may depend on `client`, `quiz` and `plan` without a cycle (research R1). What the Plan tab and the draft panel already show moves up to `components/` so both features use it (research R9). `features/plan` keeps its own week strip; the trainer's day strip is new, because the board shows a row of days, not the client's list.

## Phases

- **#105 (backend)**: the use case, its records and service, `setsPlanned()` and `averageRpe()` with the summary reusing them, `findAllByClient` with subselect fetching, the controller and response, tests, regenerated `schema.d.ts`. The spec and this plan go in its pull request.
- **#106 (frontend)**: the Overview tab at the client page's index, the day strip, the moved quiz answers and badge, strings, tests. Blocked by #105.
- **#107 (backend and frontend)**: the sessions and session log endpoints, the Logs tab, strings, tests, regenerated `schema.d.ts`. Its API needs only #99 and the list rule of #105 (research R5); its tab reuses the badge moved in #106, so it is built after #106.
- **#261 (backend and frontend)**: the note in the session log answer and on the Logs tab. Blocked by #107.
- **#108 (frontend, Iteration 4)**: the progress section from story #156's statistics for trainers and its chart. Blocked by #106, #158 and #159.

Each task is its own pull request, squash merged.
