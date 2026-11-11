---
description: "Tasks for story #147, Messages arrive without reloading (tasks #148, #149)"
---

# Tasks: Messages arrive without reloading

**Input**: `specs/147-live-messages/` (spec.md, plan.md, research.md, data-model.md, contracts/conversation-events.md, quickstart.md)

**Tests**: Required by the Definition of Done and principle V; each test sits with the code it covers.

**Organization**: Phases follow the GitHub tasks, in order: #148 (backend and generated types; serves US1, US2 and US4) and #149 (frontend; serves US1, US2 and US3). One pull request for the story, one commit per task, merged with rebase or a merge commit.

**Paths**: `B` = `backend/src/main/java/com/kinetiq`, `BT` = `backend/src/test/java/com/kinetiq`, `F` = `frontend/src`.

## Phase 1: Setup

- [X] T001 Branch `feat/147-live-messages` exists in its worktree, on top of `master` (`CLAUDE.md`, Workflow). No migration, no demo data and no new environment variable in this story (data-model.md), so nothing to check in `gh pr list`. `specs/147-live-messages/` goes into the commit of #148, so the spec is reviewed with the story's first commit

## Phase 2: task #148, Event stream per signed-in person (US4, US1, US2)

**Blocked by**: #60 (closed).

**Goal**: a signed-in person opens `GET /api/v1/conversations/events`; after a message is committed, every stream of both people of its conversation gets `event: message`; after a mark read, every stream of the reader (only) gets `event: read`; streams end within 1 minute of their sign-in, hold at most 10 per sign-in, and never slow a send (spec FR-001 to FR-010; contract: `contracts/conversation-events.md`; research R1 to R9).

**Independent test**: `quickstart.md` Automated (backend) and Manual step 1.

### Domain

- [X] T002 [P] [US1] `B/messaging/domain/model/ConversationChange.java`: record `(AccountId person, AccountId client, Kind kind)` with nested `enum Kind { MESSAGE, READ }`; the compact constructor requires all three ("Required", data-model.md). Javadoc: one notice for one person that one of their conversations changed; the conversation is named by its client
- [X] T003 [P] [US1] `B/messaging/domain/output/ConversationEvents.java`: port `void publish(ConversationChange change)`. Javadoc: tells the person's open streams, after the current transaction commits; never fails the caller
- [X] T004 [US1] `B/messaging/domain/service/MessagingService.java`: takes `ConversationEvents`. `send`: when `messages.saveIfNew(message)` is true, publish `MESSAGE` for `pair.client()` and for `pair.trainer()`; the repeated-send branch publishes nothing. `markRead`: inside the `ifPresent`, publish `READ` for `person` only, with a one-line comment that the other person is never told (no read receipts). `B/messaging/infrastructure/config/MessagingConfig.java` passes the new port
- [X] T005 [US1] `BT/messaging/domain/service/MessagingFakes.java`: a recording fake `ConversationEvents`. `BT/messaging/domain/service/MessagingServiceTest.java`, a `@Nested` class for the notices: a new message from the client notifies client and trainer with `MESSAGE` and the client's id; the same from the trainer; the same id and text sent again notifies nobody; an empty or too long text, an unknown About set and someone else's conversation notify nobody; mark read notifies only the reader with `READ`; mark read with no stored conversation notifies nobody. Update the service's construction in existing tests

### Output adapter

- [X] T006 [US1] `B/messaging/infrastructure/output/SpringConversationEvents.java`: `@Component` implementing `ConversationEvents` with `ApplicationEventPublisher.publishEvent(change)` (research R3). A comment says the input adapter receives it after commit, so the two adapters share only the domain record

### Input adapter

- [X] T007 [US4] `B/messaging/infrastructure/input/ConversationStreams.java`: `@Component` holding open streams (`person`, `signIn` session id, `SseEmitter`) in one list in opening order (`ponytail:` comment: linear scan, index by person if streams reach the thousands). Methods:
  - `SseEmitter open(AccountId person, String signIn)`: creates an emitter with a 30-minute timeout; if `signIn` already holds 10 streams, sends the oldest of that sign-in `retry: 30000` (`SseEmitter.event().reconnectTime(30_000)`) and completes it first ("at most 10 streams per `signIn`", data-model.md), with a comment that the delay stops evicted tabs from evicting each other in a loop; registers `onCompletion`, `onTimeout` (completes normally) and `onError` to remove it; sends one `ping` event at once (research R9; a named event, as `EventSource` hides comments)
  - `@TransactionalEventListener(fallbackExecution = true) void on(ConversationChange change)`: for every stream whose `person` equals `change.person()`, sends `event: message` or `event: read` with `data` = the client id, each on a virtual thread from `Executors.newVirtualThreadPerTaskExecutor()` (research R4); a send that fails removes and completes that stream and is logged at debug without the data
  - `@Scheduled(fixedDelay = 25, timeUnit = SECONDS) void heartbeat()`: for each stream, completes it when `SessionRepository.findById(signIn)` is `null`, else sends a `ping` event and drops it on failure (research R5; `ponytail:` comment: one lookup per stream per tick, batch if thousands)
  - `@EventListener(ContextClosedEvent.class) void closeAll()`: completes every stream (research R8)
  - Logging: only that a stream opened or closed, never the person's data or any text (FR-010)
- [X] T008 [US4] `B/messaging/infrastructure/input/ConversationStreamController.java`: `@RestController`, `GET /api/v1/conversations/events`, `produces = text/event-stream`, returns `streams.open(signedIn(authentication), session.getId())` with the `HttpSession`. Javadoc: the signed-in person's stream of conversation events, its two event names and their data, what it never carries, and that it ends after 30 minutes (EventSource reconnects)
- [X] T009 [US4] `B/common/infrastructure/config/SecurityConfig.java`: `.dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()`, with the comment extended: an async dispatch only finishes a request already authorized on its first dispatch, and a stream ending after its sign-in would otherwise get a 401 written into a running response (research R7). No path rule changes: `/api/**` already requires a sign-in
- [X] T010 [US4] `BT/messaging/infrastructure/input/ConversationStreamsTest.java` (plain unit test, `SessionRepository` faked by a map of live ids, sends observed through an `SseEmitter` subclass recording `send` calls): a change reaches every stream of its person on two sign-ins and no stream of another person; a `READ` change reaches only the reader's streams; the 11th stream of one sign-in sends the first a 30-second reconnect time, completes it, and the other 10 still receive; a stream of a deleted session is completed on `heartbeat()` and receives nothing after; a stream whose send throws is removed and the next change does not throw; `closeAll()` completes every stream
- [X] T011 [US4] New `BT/messaging/infrastructure/input/ConversationStreamIT.java`, in the same package as the package-private `ConversationStreams` so it can call `heartbeat()`: `@Import(TestcontainersConfiguration.class)`, `@SpringBootTest(properties = "kinetiq.plan-drafting.api-key=")`, `@AutoConfigureMockMvc`, `MockMvcTester`; its own small helpers, modelled on `KinetiqBackendApplicationIT`, that store a trainer and a linked client (and a second pair) and sign each in through `POST /api/v1/sign-ins` with the CSRF cookie, keeping the session cookie: `GET /api/v1/conversations/events` signed out answers `401` with `request.unauthenticated`; signed in as a client it starts async with `text/event-stream`; when the client's trainer then sends a message through `POST /api/v1/conversations/{clientId}/messages`, the client's stream content contains `event:message` and `data:<clientId>` (Awaitility); another trainer's client's stream receives nothing; the trainer marking read puts `event:read` on the trainer's stream and nothing on the client's; after `sessions.deleteById(<client session>)` and `heartbeat()`, the client's stream is completed
- [X] T012 [US1] `./gradlew check` passes (Docker running, JDK 25), including `HexagonalArchitectureTest`. With the backend running, `npm run gen:api` regenerates `F/lib/api/schema.d.ts` (the new path), and `npm run check` passes. `quickstart.md` Manual step 1 through the Vite proxy (`event: ping` at once and every 25 s, `401` signed out). Commit `feat(backend): add the conversation event stream (#148)` with `specs/147-live-messages/` and `schema.d.ts`

## Phase 3: task #149, Frontend subscription (US1, US2, US3)

**Blocked by**: #148, #61 (closed), #62 (closed).

**Goal**: every tab of a signed-in person, on every route including the waiting state, holds one stream; events update the list, the count and a loaded conversation; every connect catches up; a stream that gives up is reopened with backoff and a 401 sends the person to sign-in; the fixed refreshes are gone (spec FR-011 to FR-022; research R10 to R13).

**Independent test**: `quickstart.md` Automated (frontend) and Manual steps 2 to 8.

- [X] T013 [US1] `F/features/messaging/api/messaging.ts`: remove `refetchInterval` from `useConversations`, `useMessages` and `useUnreadCount` and the two constants with their comments, and give the three `staleTime: Infinity`; new `F/features/messaging/stream-health.ts` says whether the stream was heard from in the last 35 s, and `useFirstUnread`, `useSendMessage` and `useMarkRead` ask again only when it was not (FR-017). Tests prove that coming back to the tab or the network asks nothing again, that opening a conversation asks for it once, and that a quiet stream still asks on coming back. Generalise `withMessage` to `withMessages(data, messages)` (adds only unknown ids to the newest page, keeps order) and use it in `useSendMessage`. Export `refreshConversation(queryClient, clientId)`: does nothing when no messages of `clientId` are cached; else cancels in-flight fetches of that key, fetches the newest page, and merges it with `withMessages`, or invalidates that key when none of its messages is known and `hasMore` is true (research R11)
- [X] T014 [US1] New `F/features/messaging/use-conversation-events.ts`: `useConversationEvents(accountId: string | undefined)`. With an id, opens one `EventSource` at `const EVENTS: keyof paths = '/api/v1/conversations/events'`; closes it on unmount or id change. `open`: invalidate `CONVERSATIONS` and `MESSAGES`, reset the backoff. `message`: invalidate `CONVERSATIONS` and `refreshConversation(queryClient, event.data)`. `read`: invalidate `CONVERSATIONS`. `error` with `readyState === EventSource.CLOSED`: close it and schedule one attempt after 1, 2, 4, 8, 16, then 30 s; the attempt calls `api.GET('/api/v1/conversations/unread-count')` and reopens on success, stops on 401 (the client middleware has already sent the person to sign-in), and backs off again on any other failure. One timer at a time; cleared on unmount (research R12). A watchdog gives up on a stream that has delivered no event, `ping` included, for 60 s, and goes the same way: the Vite proxy was seen to keep a stream open after the backend stopped Comments say why the probe exists and why the first `open` also catches up
- [X] T015 [US1] New `F/app/conversation-events-sync.tsx`: `ConversationEventsSync` calls `useConversationEvents(useCachedAccount()?.id)` and renders nothing; Javadoc says it sits above the routes so the waiting state and the logger are live too (#149 comment). `F/app/app.tsx` mounts it beside `AccountSettingsSync`
- [X] T016 [US1] New `F/features/messaging/use-conversation-events.test.tsx` with a fake `EventSource` on `globalThis` (records instances, lets the test fire `open`, named events and `error` with a `readyState`) and `fetch` stubbed as in existing tests: one stream per account and none without one; a new account closes the old stream and opens one; a `message` event refetches the list and count and merges the new message into a loaded conversation once, also when the same message is already there; a `message` for a conversation not loaded fetches no messages; a `read` event refetches the count; `open` refetches active messaging queries; after `CLOSED` it probes and reopens after 1 s, then waits 2 s after a failed probe, and makes no attempt after a 401 probe; unmount clears the timer
- [X] T017 [US2] `F/test/event-source.ts`: a fake `EventSource` installed for every test in `F/test/setup.ts`; `renderAppAt` in `F/test/page.tsx` mounts `ConversationEventsSync` as `app.tsx` does. Existing tests that waited on the fixed refreshes (`F/app/quiz-area.test.tsx` and any test in `F/features/messaging/` or `F/app/` that advances timers to see a new message or count) drive the update through the fake `EventSource` or a refetch instead; `F/features/messaging/use-first-unread.ts`'s comment about a second interval is reworded, as the page's observer no longer refreshes on a timer. Two tests that also failed on `master` under a busy full run are made reliable: `offline-logging.test.tsx` no longer awaits `act` around going online (the rest timer kept it busy for up to 15 s), and `logger-route.test.tsx` waits for the earlier session's page instead of reading it at once
- [X] T018 [US3] `npm run check` passes. Browser checks with the backend running (built-in browser; client at 375 px, trainer at 1280 px; light and dark; EN and PL; console clean): `quickstart.md` Manual steps 2 to 8, with a screenshot of a live message on each side. Commit `feat(frontend): subscribe to conversation events (#149)`

## Phase 4: Polish

- [ ] T019 Run `/speckit-converge`; then review the whole diff with the ui-ux-pro-max skill (WCAG 2.2 AA: a live message announced without moving focus or scroll, FR-020) and for security (only the two people told, reads only to the reader, stream closes when the sign-in ends). Open one pull request `feat: deliver messages without reloading` that says `Closes #147`, `Closes #148`, `Closes #149`

## Dependencies

- T002, T003 → T004 → T005; T006 needs T002, T003; T007 needs T002; T008 needs T007; T010 needs T007; T011 needs T004 to T009; T012 last in #148.
- #149 needs #148 committed (schema path, running endpoint). T013 → T014 → T015 → T016; T017 after T013; T018 last.
- Story map: US4 (security) and the publishing for US1/US2 are all in #148; US1, US2 and US3 become visible with #149. No story is usable on its own without both tasks, which is why the story ships as one pull request.

## Parallel opportunities

- #148: T002 and T003 together; T006 alongside T004/T005; T009 alongside T007/T008.
- #149: T017 alongside T014 to T016 once T013 is done.

## Implementation strategy

MVP is the whole story: #148 makes the stream and its rules (testable alone through `ConversationStreamIT` and the proxy check), #149 makes it visible. Each is one commit; neither changes a screen's look.
