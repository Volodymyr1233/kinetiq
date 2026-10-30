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

- [X] T002 [US1] Domain: `ClientSummary`, `PlanState` in `B/client/domain/model/`; `ListClientsUseCase` in `domain/input/`; `ClientDirectory` in `domain/output/`; `ClientListingService` in `domain/service/`
- [X] T003 [US1] Adapter `B/client/infrastructure/output/JdbcClientDirectory.java`: the query of `data-model.md`, mapping the goal and the plan state
- [X] T004 [US1] Adapter test `BT/client/infrastructure/output/JdbcClientDirectoryIT.java`: only the trainer's own clients (a second trainer's client and a trainer account never appear); order by name without regard to case, then id; goal and `PLAN_TO_REVIEW` with a quiz, no goal and `NO_QUIZ` without; empty list for a trainer with no clients
- [X] T005 [US1] Web: `ClientController` (`GET /api/v1/clients`), `ClientListResponse`, `ClientResponse` (required fields marked, goal optional), `ClientWebMapper`, `ClientConfig` wiring the service
- [X] T006 [US1] `SecurityConfig`: `/api/v1/clients/**` for role `TRAINER` only
- [X] T007 [US1] Controller test `BT/client/infrastructure/input/ClientControllerTest.java`: `200` with the response shape and the signed-in trainer's id passed to the use case; `403` for a client; `401` for a visitor
- [X] T008 Regenerate `frontend/src/lib/api/schema.d.ts` (`npm run gen:api`, backend running) and run `npm run check`
- [X] T009 `./gradlew check` passes (Docker running), including `HexagonalArchitectureTest`; commit `feat(backend): add client list api (#36)`; follow `quickstart.md` step 2 by hand

## Dependencies

T002 → T003 → T004; T002 → T005 → T006 → T007; T008 after T005; T009 last.
