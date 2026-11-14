---
description: "Tasks for story #150, Trainer and client get in-app notifications (tasks #151, #152, #153, #154, #155, #296)"
---

# Tasks: Trainer and client get in-app notifications

**Input**: `specs/150-notifications/` (spec.md, plan.md)

**Tests**: Required by the Definition of Done and principle V; each test sits with the code it covers.

**Organization**: One pull request for the story, one commit per task, each ending in `(#<task>)`. Order: #151, #153, #154, #155, #152, #296 (the screens can be seen with demo data before every source raises its notice).

**Paths**: `B` = `backend/src/main/java/com/kinetiq`, `BT` = `backend/src/test/java/com/kinetiq`, `BR` = `backend/src/main/resources`, `F` = `frontend/src`.

## Phase 1: #151 Notification model and API

- [ ] T001 Commit `specs/150-notifications/` with this task. Do not commit `backend/bin/`
- [ ] T002 [US2] Shared kernel `B/common/domain/model/NoticeKind.java` (the eleven types) and `Notice.java` (`UUID recipient`, nullable `UUID client`, `NoticeKind kind`, validated in its constructor) and port `B/common/domain/output/NoticeBoard.java` (`raise(Notice)`), plain types only
- [ ] T003 [US2] Migration `V<next>__create_notifications.sql`: `notifications` (id, recipient_id not null fk accounts `on delete cascade`, kind not null with a check over the eleven names, client_id nullable fk accounts `on delete cascade`, read_at nullable, created_at, updated_at, version); index on (recipient_id, created_at desc)
- [ ] T004 [US2] Feature `B/notification`: domain model `Notification`, ids, use case `ViewNotificationsUseCase` (`list(person)` latest 30 with the unread count, `markRead(person, id)`), output ports, `NotificationService`, `NotificationNotFoundException` (404, `notification.not-found`); JPA entity and adapter; `NotificationController` (`GET /api/v1/notifications`, `POST /api/v1/notifications/{id}/read`), responses, mapper, config; client name for notifications about a client through a port filled from the account use case
- [ ] T005 [US2] `SecurityConfig`: `/api/v1/notifications/**` for any signed-in person
- [ ] T006 [US2] Tests: service with a fake repository (own only, newest first, max 30, unread count, mark read twice), controller (shape, 404 for another's, 401), adapter IT; `HexagonalArchitectureTest` passes
- [ ] T007 Demo data `BR/db/dev-data/R__demo_notifications.sql` for a demo client and the demo trainer; regenerate `F/lib/api/schema.d.ts`; both gates; commit `feat(backend): add notification model and api (#151)`

## Phase 2: #153 Notifications on the event stream

- [ ] T008 [US4] After commit of a stored notification, publish a Spring event of a common record (`NoticeStored(UUID recipient)`) from `notification` output; `B/messaging/infrastructure/input/ConversationStreams` sends event `notification` to every stream of that person; document it on the controller
- [ ] T009 [US4] Tests: the recipient's stream receives `notification`, another person's does not, a dead stream is closed without failing the raise; commit `feat(backend): push notifications on the event stream (#153)`

## Phase 3: #154 Bell, side sheet and toasts

- [ ] T010 [US3] `F/features/notifications/api/notifications.ts` (`useNotifications`, `useMarkRead`) and key in `F/lib/query-keys.ts`; stream handler in the existing conversation events hook invalidates it on `notification`
- [ ] T011 [US3] Components: bell button with count and accessible name, side sheet list (latest 30, unread marked, text, time, empty state), toast for a notification that arrives, mounted in the top bars of `client-area.tsx` and `trainer-area.tsx`; choosing an item marks it read and navigates by type (client: plan, messages; trainer: Approvals, the client's page, messages; client deleted goes nowhere)
- [ ] T012 [US3] Strings `notifications.*` in `en.json` and `pl.json` (types, empty, bell label with Polish plurals)
- [ ] T013 [US3] Tests for bell count and 99+, sheet list and empty state, choosing an item, the toast on arrival, Polish; browser check light and dark at 375 and 1280 px; both gates; commit `feat(frontend): add notification bell, sheet and toasts (#154)`

## Phase 4: #155 Approvals count badge

- [ ] T014 [US5] Refresh the Approvals count queries on event `notification` in the trainer area; test that the count updates; commit `feat(frontend): update the approvals count live (#155)`

## Phase 5: #152 Notifications from feature events

- [ ] T015 [US1] `B/common/infrastructure/output/SpringNoticeBoard` (publishes a Spring event) and the recorder in `B/notification/infrastructure/input` that stores the notice in the raising transaction
- [ ] T016 [US1] Raise the notices, each with its service test: plan published and plan saved after publishing (`plan`), draft built (`plan`), plan change created for both and each person's decision for the other (`planchange`), recovery day (`readiness`), message (`messaging`), none for the person who acted
- [ ] T017 [US1] One test per type through the recorder and a rollback test; both gates; commit `feat(backend): raise notifications from feature events (#152)`

## Phase 6: #296 Client deleted notification

- [ ] T018 [US6] In `account` deletion raise `CLIENT_DELETED` for the trainer with no client reference when a client is deleted; extend the erasure test of #170 to notifications addressed to and about the client; commit `feat(backend): notify the trainer when a client deletes their account (#296)`

## Phase 7: Polish

- [ ] T019 `./gradlew check` and `npm run check` pass; browser check both areas, light and dark; PR with `Closes #150` and every task, reviewer, rebase and merge
