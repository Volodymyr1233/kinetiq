---
description: "Tasks for story #162, Trainer sees the overview of all clients (tasks #163, #164, #165, #166)"
---

# Tasks: Trainer sees the overview of all clients

**Input**: `specs/162-clients-overview/` (spec.md, plan.md)

**Tests**: Required by the Definition of Done and principle V; each test sits with the code it covers.

**Organization**: One pull request for the story, one commit per task, in order: #163, #164, #165, #166.

**Paths**: `B` = `backend/src/main/java/com/kinetiq`, `BT` = `backend/src/test/java/com/kinetiq`, `F` = `frontend/src`.

## Phase 1: task #163, client status (US1)

**Blocked by**: #157 (closed).

- [X] T001 Commit `specs/162-clients-overview/` with the code of task #163
- [X] T002 [US1] `B/overview/domain/model/`: `ClientStatus`, `StatusReason`, `Completion`, `Verdict.of` (spec FR-001, FR-002, FR-005 to FR-007)
- [X] T003 [US1] `B/overview/domain/model/`: `PlanDates`, `RpeTotal`, `ClientTraining` with `completion`, `rpe` and `week` (FR-003, FR-004, FR-009)
- [X] T004 [US1] Tests `BT/overview/domain/model/VerdictTest.java` and `ClientTrainingTest.java`: each threshold on both sides, each window edge (SC-002)
- [X] T005 `CONTEXT.md`: "Active client"; `./gradlew check`; commit `feat(backend): add the client status (#163)`

## Phase 2: task #164, overview API (US2, US4)

**Blocked by**: #163, #110 (closed), #138 (closed).

- [X] T006 [US2] `B/overview/domain/output/TrainingRecords.java` and `B/overview/infrastructure/output/JdbcTrainingRecords.java`: plans with session days, day logs since a date, the last logged set, check-ins since a date, each bounded by the trainer
- [X] T007 [US4] Test `BT/overview/infrastructure/output/JdbcTrainingRecordsIT.java`: each figure from known rows; another trainer's client never read
- [X] T008 [US2] `B/overview/domain/input/ViewOverviewUseCase.java`, `Overview.java`; `B/overview/domain/service/OverviewService.java`: rows, tiles, alerts in the order of FR-010
- [X] T009 [US2] Test `BT/overview/domain/service/OverviewServiceTest.java` with fakes and a fixed clock
- [X] T010 [US2] [US4] Wiring `B/overview/infrastructure/config/OverviewConfig.java`; web `OverviewController`, `OverviewResponse`, `OverviewWebMapper`; test `OverviewControllerTest.java` (fields, absent ones left out, 403 for a client, 401 for a visitor)
- [X] T011 Demo data where a status or alert has nothing to show; regenerate `F/lib/api/schema.d.ts`; `./gradlew check`, `npm run check`; commit `feat(backend): add the trainer's overview api (#164)`

## Phase 3: task #165, overview screen (US2)

**Blocked by**: #164.

- [ ] T012 [US2] `F/features/overview/api/overview.ts`, `components/status-badge.tsx`, `overview-tiles.tsx`, `attention-list.tsx`, `client-table.tsx`; Alert variants `warning` and `low`
- [ ] T013 [US2] `F/features/overview/pages/overview-page.tsx` at `/trainer/clients` with skeletons, error and empty states; the old table at `/trainer/clients/all`
- [ ] T014 [US2] Tests `overview-page.test.tsx`: tiles, every alert kind and its action, loading, empty, error
- [ ] T015 EN and PL texts; browser check at 1280 px, light and dark, EN and PL; `npm run check`; commit `feat(frontend): add the clients overview (#165)`

## Phase 4: task #166, all-clients table (US3)

**Blocked by**: #163, #37 (closed).

- [ ] T016 [US3] `F/features/overview/pages/all-clients-page.tsx` on the overview query: the new columns, sorting, status filters with counts under the search; `F/features/client/pages/clients-page.tsx` and its table removed
- [ ] T017 [US3] Tests `all-clients-page.test.tsx`: filter counts equal rows with and without a search, sorting both ways with empty values last, no match clears both
- [ ] T018 Remove `GET /api/v1/clients` (no caller left) and its tests; regenerate `schema.d.ts`; browser check; `./gradlew check`, `npm run check`; commit `feat: add status columns and filters to all clients (#166)`
