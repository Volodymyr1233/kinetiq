---
description: "Tasks for story #247, Client sees their trainer and a draft summary while waiting for a plan (tasks #248, #249, #250)"
---

# Tasks: Client sees their trainer and a draft summary while waiting for a plan

**Input**: `specs/247-waiting-trainer-draft/` (spec.md, plan.md, research.md, data-model.md, contracts/draft-summary-api.md, quickstart.md)

**Tests**: Required by the Definition of Done and principle V; each test sits with the code it covers.

**Organization**: Phases follow the GitHub tasks, in order: #248 (frontend), #249 (backend), #250 (frontend). One pull request per task, squash merged.

**Paths**: `B` = `backend/src/main/java/com/kinetiq`, `BT` = `backend/src/test/java/com/kinetiq`, `F` = `frontend/src`.

## Phase 1: Setup

- [X] T001 Commit `specs/247-waiting-trainer-draft/` with the code of task #248, so the spec is reviewed in that pull request

## Phase 2: task #248, trainer's avatar and name on the waiting state (US1)

**Blocked by**: nothing open.

**Goal**: the reviewing state shows the trainer's avatar and names them in its text; the message button stays as it is (spec FR-001 to FR-006).

- [X] T002 [US1] Move `firstName` to `F/lib/first-name.ts` and import it in `F/features/messaging/components/conversation.tsx` and `F/features/account/pages/join-page.tsx`, removing their local copies (research R3); both screens behave as before
- [X] T003 [US1] `F/features/quiz/pages/waiting-page.tsx`: an optional prop `trainerName`. With a name, the reviewing state shows `AccountAvatar` (`size="xl"`) above the heading and the text `quiz.waiting.reviewing.textNamed` with `firstName(trainerName)`. Without one it reads as before. A long name without spaces breaks inside the word, so the page never scrolls sideways. The building state, the heading, the announcement and the `message` slot do not change. The comment above `Waiting` keeps only the draft summary as not shown
- [X] T004 [US1] `F/app/quiz-area.tsx`: `QuizWaitingRoute` reads `useQuery(conversationsQuery)` and passes `data?.conversations[0]?.name` as `trainerName`; no refresh interval, and a failed read leaves the name out (research R1)
- [X] T005 [US1] String `quiz.waiting.reviewing.textNamed` in `F/locales/en.json` and `pl.json`, as in the spec's Texts; the name stays in the nominative
- [X] T006 [US1] Page test additions in `F/features/quiz/pages/waiting-page.test.tsx`: with a name, the reviewing state shows the initials and the text with the first name, in English and Polish, under the same heading; the building state shows no initials and keeps its text; without a name, the text says "your trainer" and no initials show
- [X] T007 [US1] Route test additions in `F/app/quiz-area.test.tsx`: the name comes from the conversation list; the button stays "Message your trainer" and opens the conversation; when the list fails or holds no conversation, the waiting state shows with "your trainer", without an error; a name that arrives on a later read shows without a reload
- [X] T008 `npm run check` passes; follow `quickstart.md` section 1 at 375 px, light and dark, with the keyboard, and compare the avatar with the board "Quiz · building the plan"; commit `feat(frontend): show the trainer on the waiting state`

## Phase 3: task #249, draft summary API (US3)

**Blocked by**: nothing open.

**Goal**: `GET /api/v1/plan/draft-summary` for a signed-in client (contract: `contracts/draft-summary-api.md`; spec FR-007 to FR-013).

- [X] T009 [US3] Domain: record `DraftSummary` (`List<DayOfWeek> trainingDays`, `int weeks`, `Goal goal`, with a defensive copy of the list) in `B/plan/domain/input/`; `ViewOwnPlanUseCase.draftSummary(client)`; `PlanViewService.draftSummary`: only a plan with no first week and at least one session gives a summary, in the plan's session order; a failed draft, a draft without sessions, no plan and a published plan all throw `PlanNotFoundException`. The anonymous `ViewOwnPlanUseCase` in `BT/sessionlog/domain/service/SessionLogServiceTest.java` gets the new method, unused there, so the tests compile
- [X] T010 [US3] Domain test additions in `BT/plan/domain/service/PlanViewServiceTest.java`: a draft gives its days in week order, its weeks and its goal, and a draft the trainer saved counts like a built one; a failed draft, a draft without sessions, no plan and a published plan each answer `plan.not-found`
- [X] T011 [US3] Web in `B/plan/infrastructure/input/`: `DraftSummaryResponse` (every field `@NotNull`), `GET /draft-summary` in `ClientPlanController`, a mapper method in `PlanWebMapper`; the client comes from the sign-in only
- [X] T012 [US3] Controller test additions in `BT/plan/infrastructure/input/ClientPlanControllerTest.java`: `200` with `trainingDays`, `weeks` and `goal` and nothing else, so no session names, exercises or loads; the signed-in client's id passed to the use case; `404 plan.not-found`; `403 request.not-allowed` for a trainer; `401` for a visitor
- [X] T013 [US3] `CONTEXT.md`: add "Draft summary" under Planning, and change "Draft", which says the client cannot see a draft
- [X] T014 Regenerate `F/lib/api/schema.d.ts` (`npm run gen:api`, backend running) and run `npm run check`
- [X] T015 `./gradlew check` passes (Docker running), including `HexagonalArchitectureTest`; follow `quickstart.md` section 3; commit `feat(backend): add draft summary api`

## Phase 4: task #250, draft card on the waiting state (US2)

**Blocked by**: #249.

**Goal**: the reviewing state shows the card when a summary exists (spec FR-014 to FR-017).

- [X] T016 [US2] `F/features/plan/api/client-plan.ts`: `useDraftSummary()` for `GET /api/v1/plan/draft-summary`, typed by the generated schema, with no refresh interval (research R6)
- [X] T017 [US2] `F/features/plan/components/draft-card.tsx`: nothing until there is data; then `Card` with `CardHeader` and `CardContent`, the label, the `Badge`, the rows as a description list and the line. Days with `dayName(day, language, 'short')` joined by `Intl.ListFormat`, length with the existing `plan.client.ready.weeks`, focus with `quiz.goal.options.<goal>.title`. Read `npx shadcn@latest docs card` and `npx shadcn@latest docs badge` first, and take the look from the board. The board's badge is blue, so `F/components/ui/badge.tsx` gains the variant `info` from the existing `info` tokens, beside `warning` and `success`
- [X] T018 [US2] `F/features/quiz/pages/waiting-page.tsx`: a slot `draftCard`, shown on the reviewing state only, between the text and the `message` slot, and the comment above `Waiting` removed; `F/app/quiz-area.tsx` passes `<DraftCard />` (research R5)
- [X] T019 [US2] Strings `plan.client.draft.*` in `F/locales/en.json` and `pl.json`, as in the spec's Texts
- [X] T020 [US2] Tests: `F/features/plan/components/draft-card.test.tsx` (the rows and the line for a summary, in English and Polish; nothing for a `plan.not-found` answer or a failed read); in `F/features/quiz/pages/waiting-page.test.tsx` the slot shows on the reviewing state and not on the building state, and arrives by itself at the move from building to reviewing, with focus kept
- [X] T021 `npm run check` passes; follow `quickstart.md` section 2 at 375 px and 320 px, light and dark, with the keyboard, and compare the card with the board; commit `feat(frontend): show the draft card on the waiting state`

## Phase 5: Close the story

- [X] T022 On the branch of task #250, before its pull request is opened, run `/speckit-converge` and turn its findings into new tasks under #247; the pull request of #250 ends with `Closes #250` and `Closes #247`

## Dependencies

- T001 goes with #248. #249 does not need #248; #250 needs #249's generated types.
- Inside #248: T002 before T003; T003 and T005 before T006; T004 before T007.
- Inside #249: T009 before T010 and T011; T011 before T012 and T014.
- Inside #250: T016 before T017; T017 before T018; T019 beside them.

## Parallel opportunities

- T002 and T005 touch different files and can be written together.
- T010 and T012 can be written together once T009 and T011 exist; T013 can be done at any point of #249.
- T017 and T019 can run together after T016.

## Implementation strategy

Build in task order: the trainer on the waiting screen is the MVP and needs no API, then the API, then the card. Each task is one pull request, opened when its checks pass and the user agrees.
