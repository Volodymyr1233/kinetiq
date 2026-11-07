# Implementation Plan: Client sees their trainer and a draft summary while waiting for a plan

**Branch**: `feat/248-trainer-on-waiting-state` | **Date**: 2026-11-07 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/247-waiting-trainer-draft/spec.md`

## Summary

The waiting screen shows who reviews the plan (#248) and roughly what it holds (#249 API, #250 card). #248 is frontend only: `app/quiz-area.tsx` reads the trainer's name from the conversation list and gives it to the waiting page, for the avatar and the text. The message button stays as it is, without the name. #249 adds `GET /api/v1/plan/draft-summary` to the `plan` feature, for role CLIENT: training days, weeks and goal of a draft with sessions, and `plan.not-found` for everything else. #250 adds the card to `features/plan`, and the app passes it to the waiting page as a slot. Nothing is stored and no dependency is added.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1) backend; TypeScript, React 19 frontend

**Primary Dependencies**: Existing only: Spring MVC, MapStruct, Lombok; React Router, TanStack Query, react-i18next, shadcn (`card`, `badge`, `avatar`, all present)

**Storage**: PostgreSQL, unchanged; no migration

**Testing**: JUnit 5 + AssertJ (domain service with a fake repository), `@WebMvcTest` (controller), Vitest + Testing Library (frontend). Docker must be running for the backend gate.

**Target Platform**: Browser, client screens at 375 px; same-origin `/api/v1`

**Project Type**: Web application (backend + frontend in one repository)

**Performance Goals**: None beyond a normal page: one read of the conversation list and one read of the summary per visit.

**Constraints**: A client reads nothing of a draft but its summary; no state for client data in browser storage; every string in English and Polish; no sideways scroll at 375 px and 320 px; a feature does not import another feature.

**Scale/Scope**: One trainer and one plan per client; a summary holds at most 6 days.

## Constitution Check

*GATE: passes before research; re-checked after design.*

| Principle | Check | Result |
|---|---|---|
| I. Hexagonal boundaries | `ViewOwnPlanUseCase` gains one read and `PlanViewService` implements it; a new record in `domain.input`; no new feature dependency. `HexagonalArchitectureTest` runs in `check`. | Pass |
| II. One contract | #248 changes no API. #249 adds one endpoint with generated OpenAPI types, and `schema.d.ts` is regenerated in its pull request; it reuses the stable code `plan.not-found`. | Pass |
| III. Secure by default | `/api/v1/plan/**` already needs role CLIENT, so nothing is opened; the client comes from the sign-in, never from path or body. Nothing here reaches an LLM. | Pass |
| IV. Explainable plan changes | Not touched: the summary changes no plan, and a stored draft has already passed the domain bounds or the plan rules. | n/a |
| V. Tests verify behaviour | Service tests (what answers alike, what the summary holds), a controller test (roles, the answer's fields, the code), page tests for each state. No getter tests. | Pass |
| VI. Simplicity | The name comes from an existing read and the slots follow `settings` and `message`. The button stays as it is, so nothing declines a name (research R2). `firstName` moves up instead of gaining a third copy. | Pass |
| VII. One design system | `Card`, `Badge` and `AccountAvatar`, Kinetiq tokens only; checked in the browser, light and dark, at 375 px; EN and PL strings. | Pass |

No violations, so no Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/247-waiting-trainer-draft/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── draft-summary-api.md
├── checklists/
│   └── requirements.md
└── tasks.md             # /speckit-tasks
```

### Source Code (repository root)

```text
backend/src/main/java/com/kinetiq/plan/
├── domain/
│   ├── input/ViewOwnPlanUseCase.java                    # + draftSummary(client)
│   ├── input/DraftSummary.java                          # trainingDays, weeks, goal
│   └── service/PlanViewService.java                     # + draftSummary
└── infrastructure/input/
    ├── ClientPlanController.java                        # + GET /api/v1/plan/draft-summary
    ├── DraftSummaryResponse.java
    └── PlanWebMapper.java                               # + summary response
backend/src/test/java/com/kinetiq/plan/...               # PlanViewServiceTest, ClientPlanControllerTest additions
backend/src/test/java/com/kinetiq/sessionlog/...         # SessionLogServiceTest: its stand-in of the port gains the new method

frontend/src/
├── app/
│   ├── quiz-area.tsx                                    # reads the trainer's name; passes the name and the card
│   └── quiz-area.test.tsx
├── features/
│   ├── quiz/pages/waiting-page.tsx                      # avatar, text with the name, card slot; + tests
│   ├── plan/api/client-plan.ts                          # + useDraftSummary
│   ├── plan/components/draft-card.tsx                   # + test
│   ├── messaging/components/conversation.tsx            # firstName from lib
│   └── account/pages/join-page.tsx                      # firstName from lib
├── lib/
│   ├── first-name.ts                                    # firstName
│   └── api/schema.d.ts                                  # regenerated
└── locales/{en,pl}.json                                 # quiz.waiting.reviewing.*, plan.client.draft.*

CONTEXT.md                                               # + Draft summary; Draft changed
```

**Structure Decision**: Extends the existing features. The summary is a read of the plan, so its API and card live in `plan` and `features/plan`, while the screen stays in `features/quiz`. A feature does not import another feature, so `app/quiz-area.tsx` joins them, as it already does for the Settings and message buttons.

## Phases

- **#248 (frontend)**: `lib/first-name.ts` with `firstName` moved up, the name read in `app/quiz-area.tsx`, the avatar and the text on the waiting page, strings, tests.
- **#249 (backend)**: the domain read, the endpoint, tests, regenerated `schema.d.ts`, `CONTEXT.md`.
- **#250 (frontend)**: `useDraftSummary`, the card, the slot, strings, tests. Blocked by #249.

Each task is its own pull request.
