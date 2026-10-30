---
description: "Tasks for story #35, Trainer sees their clients (task #36 first)"
---

# Tasks: Trainer sees their clients

**Input**: `specs/35-trainer-clients/` (spec.md, plan.md, research.md, data-model.md, contracts/clients-api.md, quickstart.md)

**Tests**: Required by the Definition of Done and principle V; each test sits with the code it covers.

**Organization**: Phases follow the GitHub tasks. This file lists task #36 in full; #37 and #38 are added when they start.

**Paths**: `B` = `backend/src/main/java/com/kinetiq`, `BT` = `backend/src/test/java/com/kinetiq`.

## Phase 1: Setup

- [X] T001 Commit `specs/35-trainer-clients/` with the code of task #36, so the spec is reviewed in that pull request. `CONTEXT.md` needs no new term

## Phase 2: task #36, client list API (US1)

**Goal**: `GET /api/v1/clients` lists the signed-in trainer's clients with goal and plan state (contract: `contracts/clients-api.md`).

- [X] T002 [US1] Domain: `ClientSummary`, `PlanState` in `B/client/domain/model/`; `ViewClientsUseCase` in `domain/input/`; `ClientDirectory` in `domain/output/`; `ClientViewService` in `domain/service/`
- [X] T003 [US1] Adapter `B/client/infrastructure/output/JdbcClientDirectory.java`: the query of `data-model.md`, mapping the goal and the plan state
- [X] T004 [US1] Adapter test `BT/client/infrastructure/output/JdbcClientDirectoryIT.java`: only the trainer's own clients (a second trainer's client and a trainer account never appear); order by name without regard to case, then id; goal and `PLAN_TO_REVIEW` with a quiz, no goal and `NO_QUIZ` without; empty list for a trainer with no clients
- [X] T005 [US1] Web: `ClientController` (`GET /api/v1/clients`), `ClientListResponse`, `ClientResponse` (required fields marked, goal optional), `ClientWebMapper`, `ClientConfig` wiring the service
- [X] T006 [US1] `SecurityConfig`: `/api/v1/clients/**` for role `TRAINER` only
- [X] T007 [US1] Controller test `BT/client/infrastructure/input/ClientControllerTest.java`: `200` with the response shape and the signed-in trainer's id passed to the use case; `403` for a client; `401` for a visitor
- [X] T008 Regenerate `frontend/src/lib/api/schema.d.ts` (`npm run gen:api`, backend running) and run `npm run check`
- [X] T009 `./gradlew check` passes (Docker running), including `HexagonalArchitectureTest`; commit `feat(backend): add client list api (#36)`; follow `quickstart.md` step 2 by hand

## Phase 3: task #37, all-clients table with search (US2)

**Goal**: `/trainer/clients` lists the clients from `GET /api/v1/clients` with a search by name and an empty state with the invite action (spec FR-005, FR-007).

**Paths**: `F` = `frontend/src`.

- [X] T010 [US2] `F/features/client/api/clients.ts`: `useClients` (the response type comes from the generated schema)
- [X] T011 [US2] `F/features/client/components/client-table.tsx` and `pages/clients-page.tsx`: columns Client, Goal, Plan, Last log; plan state as a badge with text; "None yet" for a goal and a last log that are not there; search over the list already read; empty state with an invite link; state for no match; loading and error with retry
- [X] T012 [US2] Route `/trainer/clients` renders `ClientsPage`; `ClientsPlaceholder` and its link are removed from `F/app/trainer-area.tsx`
- [X] T013 [US2] Strings `clients.*` in `F/locales/en.json` and `pl.json` (the goal names reuse `quiz.goal.options`)
- [X] T014 [US2] Tests `F/features/client/pages/clients-page.test.tsx`: rows in the order received with goal and plan state; "None yet"; search by part of a name in any letter case, with spaces around; no match and clearing; empty state with the invite link and no search box; error and retry
- [X] T015 `npm run check` passes; commit `feat(frontend): add all-clients table with search (#37)`; look at the screen at 1280 px, light and dark, by keyboard (the Definition of Done), following `quickstart.md` step 4
## Phase 4: task #38, client page frame with tabs (US3)

**Goal**: `/trainer/clients/<id>` shows the client's name, goal and plan state with the tabs Overview, Plan, Logs and Messages, empty until their features land; an unknown or another trainer's client shows "not found" (spec FR-006).

- [X] T016 [US3] Backend: `GET /api/v1/clients/{id}` (`contracts/clients-api.md`); `ViewClientsUseCase.getClient`, `ClientDirectory.findClient`, `ClientNotFoundException` (`client.not-found`, `404`); the use case is renamed from `ListClientsUseCase` now that it also reads one client
- [X] T017 [US3] Tests: `ClientViewServiceTest` (own client, another trainer's and unknown both refused), `JdbcClientDirectoryIT` (own client with goal and state; another trainer's client, a trainer account and an unknown id all absent), `ClientControllerTest` (`200`, `404` with the code, `403` for a client, `401` for a visitor)
- [X] T018 [US3] Regenerate `schema.d.ts`; add the shadcn `tabs` component through the CLI
- [X] T019 [US3] Frontend: `F/features/client/pages/client-page.tsx` (header, tabs bound to the address, not-found state, loading, error with retry), shared `PlanStateBadge`, the client's name in the table links to the page, route `:id` with the four tab routes, strings `clients.page.*` in English and Polish
- [X] T020 [US3] Tests `client-page.test.tsx`: header and tabs; "None yet" without a quiz; each tab has its own address and a reload opens it; arrow keys move between tabs; not found for an unknown id and for text that is no id; error and retry; opened from the table
- [X] T021 Both gates pass; commit `feat: add client page frame with tabs (#38)`; look at the page at 1280 px, light and dark, by keyboard, following `quickstart.md` step 5

## Dependencies

T002 → T003 → T004; T002 → T005 → T006 → T007; T008 after T005; T009 last.
