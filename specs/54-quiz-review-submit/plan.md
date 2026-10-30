# Implementation Plan: Client reviews and submits the quiz

**Branch**: `feat/56-quiz-review-step` (first task; later tasks use their own branches) | **Date**: 2026-10-25 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/54-quiz-review-submit/spec.md`, building on `specs/50-client-quiz`

## Summary

A review step that lists the seven answers from the draft in the tab, with Change links back to the steps (task #56). A backend `quiz` feature that stores a client's answers once and answers their status, enforced by a unique constraint (task #55). A submit action and two waiting states that persist until a plan is published (task #57). One pull request per task, squash merged; the first one also carries the Spec Kit files. Tasks #55 and #57 are planned in outline and refined when their blockers are merged.

## Technical Context

**Language/Version**: TypeScript 6; Java 25 (task #55)

**Primary Dependencies**: React 19, React Router 8, TanStack Query 5 (task #57), react-i18next, shadcn/ui; Spring Boot 4.1 (MVC, Security 7, Data JPA, Flyway, Validation), MapStruct, Lombok (task #55). No new dependency.

**Storage**: Task #56 reads the draft in `sessionStorage` (spec 50). Task #55 adds tables `quiz_submissions` and `quiz_submission_likes` (data-model.md).

**Testing**: Vitest + Testing Library; JUnit 5 + AssertJ (unit with a fake repository, `@WebMvcTest`, `@DataJpaTest` + Testcontainers)

**Target Platform**: Browser SPA at 375 px; REST API under `/api/v1`, same origin

**Project Type**: Web application; task #56 touches `frontend/` only

**Performance Goals**: SC-001: review to waiting state in under 30 s

**Constraints**: The quiz API needs a signed-in client (R7); no answer reaches the address bar or logs; one submission per client, enforced by the database (R9)

**Scale/Scope**: 2 routes (review, waiting), 1 resource with 2 operations, 2 tables

## Constitution Check

| Principle | Status |
|---|---|
| I. Hexagonal boundaries | Pass. Task #55 adds a feature `quiz` with the usual layers; its domain uses the `account` feature's `AccountId`, as `exercise` does, and `account` does not know `quiz`. |
| II. One contract | Pass. Task #55 regenerates `schema.d.ts` in its own pull request. Errors are problem details with stable codes (`quiz.already-submitted`, `validation.failed`, R8). Task #56 calls no API. |
| III. Secure by default | Pass. No public endpoint; `/api/v1/quiz/**` requires the role `CLIENT` (R7). The client is taken from the session, never from the request. Answers are health data: never logged, kept until task #168 decides retention and encryption (spec, Clarifications). |
| IV. Plan changes | Not affected; drafting the first plan is a later story, and its prompts carry no names or ids. |
| V. Tests verify behaviour | Pass. The test list below covers rules, the one-submission guarantee, error mapping, role checks and screen behaviour. |
| VI. Simplicity | Pass. No form on the review (R1), one status enum (R6), no change to `common` (R8), no polling (R10). |
| VII. One design system | Pass. shadcn components and tokens; EN and PL; browser check at 375 px, light and dark. |

Re-checked after design: unchanged.

### Definition of Done items that do not apply

- *Another person's resource answers 404*: no operation addresses a quiz by id, so no other client's quiz can be asked for (R6). Another role is refused by role (R7), and a test proves it.
- *New entities get demo data*: applies to task #55 (data-model.md, "Demo data").

## Project Structure

### Documentation (this feature)

```text
specs/54-quiz-review-submit/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/api.md
├── checklists/requirements.md
└── tasks.md             (/speckit-tasks)
```

### Source Code

```text
frontend/src/
├── app/router.ts                                  /app/quiz/review (#56), /app/quiz/waiting (#57)
├── features/quiz/
│   ├── quiz-schema.ts (+ .test.ts)                isComplete(draft) (#56)
│   ├── quiz-draft.ts                              clearDraft() (#57)
│   ├── api/quiz.ts                                useQuizStatus, useSubmitQuiz (#57)
│   ├── components/quiz-status-check.tsx           the quiz pages follow the status (#57, research R12)
│   └── pages/
│       ├── review-page.tsx (+ .test.tsx)          the review (#56)
│       ├── quiz-page.tsx (+ .test.tsx)            Continue and Back after Change (#56)
│       └── waiting-page.tsx (+ .test.tsx)         building and reviewing states (#57)
└── locales/en.json, pl.json                       quiz.review.* (#56), quiz.waiting.* (#57)

specs/50-client-quiz/research.md, contracts/routes.md   the review route now exists (#56)

backend/src/main/java/com/kinetiq/quiz/            (#55)
├── domain/model/        QuizSubmission, QuizSubmissionId, Goal, Experience, Level, Like, QuizStatus
├── domain/input/        SubmitQuizUseCase (submit and status) + SubmitQuizCommand
├── domain/output/       QuizSubmissionRepository
├── domain/service/      QuizSubmissionService
├── domain/exception/    QuizAlreadySubmittedException
└── infrastructure/      config, input (QuizController, SubmitQuizRequest, QuizStatusResponse, QuizWebMapper), output (entity, JPA repository, mapper, Impl)
backend/src/main/resources/db/migration/V12__create_quiz_submissions.sql   (#55, number per "Open items")
backend/src/main/resources/db/dev-data/R__demo_quiz.sql, R__demo_accounts.sql   (#55, a second demo client with a submission)
README.md                                                                   (#55, that client in the demo accounts)
backend/src/main/java/com/kinetiq/common/infrastructure/config/SecurityConfig.java   /api/v1/quiz/** for CLIENT (#55)
```

**Structure Decision**: One feature, `quiz`, on both sides, as `CONTEXT.md` names it and as spec 50 started it.

## Work per task (one pull request each, squash merged)

| Task | Pull request title | Contents |
|---|---|---|
| #56 | `feat(frontend): add quiz review step` | Spec Kit files of story #54; `isComplete`; the review page, its route and tests; Continue and Back after Change in the step page; `quiz.review.*` in EN and PL; "Build my plan" disabled until #57 (R5); spec 50's research R12 and routes contract updated; browser check per quickstart.md |
| #55 | `feat(backend): add quiz answers model and submit api` | Domain model with every rule and its tests, service with the one-submission rule, migration, persistence with the race test, controller with validation and role tests, `SecurityConfig`, demo data, `schema.d.ts` |
| #57 | `feat(frontend): add quiz submit and waiting states` | Status and submit hooks, "Build my plan" action, building and reviewing states, waiting route, the quiz pages following the status (R12), draft removal, loading and error states, tests |

Task order: #56, then #55 once task #31 is merged, then #57.

## Tests

| Test | Covers |
|---|---|
| `quiz-schema.test.ts` (#56) | `isComplete`: true only with `reached` 7 and steps 1–6 valid |
| `review-page.test.tsx` (#56) | Seven rows in step order with labels and values; "About you" in English and Polish number format; likes as a list in screen order; "No notes" for empty notes; each "Change" link named for its row and leading to its step; Back to step 7; an incomplete draft redirects to the step that may open; "Build my plan" disabled; focus on the heading |
| `quiz-page.test.tsx` (#56) | A step opened from the review returns to the review with Continue and with Back; Continue stays disabled while that step is invalid |
| `QuizSubmissionTest` (#55) | Each rule of spec 50 refused at its bound and accepted inside; notes trimmed and empty notes absent; likes 1–5 |
| `QuizSubmissionServiceTest` (#55, fake repository) | First submit stored with the time; second refused with `quiz.already-submitted`; status for no submission and for a submission |
| `QuizSubmissionRepositoryImplIT` (#55) | Stored row with and without notes; the database refuses a broken check; a second submission for the same client, as in a race, turns into `quiz.already-submitted` |
| `QuizControllerTest` (#55) | `201` and body; each field's `validation.failed` code; an unknown code `request.invalid`; `409` `quiz.already-submitted`; `401` without a session; `403` for a trainer |
| `KinetiqBackendApplicationIT` (#55) | `/api/v1/quiz` refused without a session |
| `waiting-page.test.tsx` and submit tests (#57) | Building state while sending; reviewing state after success and after a reload; `quiz.already-submitted` shows the waiting state; failure shows the message and "Try again"; draft removed; loading and error of the status read; a step and the review send a client who submitted to the waiting state, the waiting page sends one who did not to the quiz |

## Open items

| Item | Task | How it is settled |
|---|---|---|
| Migration number `V12`: the highest on `master` is `V11` (task #32), and no open pull request adds one | #55 | Checked again before opening and before merging the pull request; renumbered if taken (`backend/CLAUDE.md`) |
| Whether the waiting state is guarded by #57 alone or by the entry check of task #53 | #57 | Settled when #57 started: #57 checks the quiz pages by status, #53 the client area and the landing after sign-in (research R12; spec 50, T026) |

## Complexity Tracking

None.
