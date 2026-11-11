# Research: Messages arrive without reloading

Decisions behind [plan.md](plan.md). Library behaviour was checked against the current Spring Framework 7 and Spring Security 7 documentation (context7).

## R1. Transport: Spring MVC `SseEmitter`

- **Decision**: One `GET /api/v1/conversations/events` endpoint returning an `SseEmitter` (`text/event-stream`). The browser reads it with the native `EventSource`.
- **Rationale**: The story asks for server-sent events. Spring MVC supports them with no new dependency, and virtual threads make a held request cheap. `EventSource` sends the session cookie on the same origin by itself, so authentication is the existing cookie and nothing new is stored in the browser (constitution III).
- **Alternatives considered**: WebSocket/STOMP (two-way, a new dependency and its own security setup, for a one-way need); long polling (more requests and more code than SSE); keeping spec 58's fixed refreshes (what the story replaces).

## R2. Events are notices, not copies

- **Decision**: An event has a name and the conversation's client id as its data, nothing else: `event: message` (a message was stored in that conversation) or `event: read` (the person on this stream marked it read). The app then uses the existing operations (newest page of messages, list, unread count).
- **Rationale**: Every rule of spec 58 stays in one place: who may see what, `mine`, About chips in the reader's language, names. The stream cannot leak more than those operations, because it carries no message data (FR-004). A conversation is already named by its client id in every messaging path.
- **Alternatives considered**: sending the whole `MessageResponse` (per-recipient mapping and locale on the stream, and text kept in a second code path); sending the new unread count (still needs the list and messages fetched, and a second source of truth for the count).

## R3. Getting from a use case to the streams without breaking the hexagon

- **Decision**:
  - `domain.model.ConversationChange(AccountId person, AccountId client, Kind kind)`, `Kind` = `MESSAGE`, `READ`: one notice for one person.
  - `domain.output.ConversationEvents` port with `publish(ConversationChange)`. `MessagingService` decides who is told: on a stored new message, one `MESSAGE` change for the client and one for the trainer; on mark read, one `READ` change for the reader only. A repeated send (same id, already stored) publishes nothing.
  - `infrastructure.output.SpringConversationEvents` implements the port with Spring's `ApplicationEventPublisher`, publishing the `ConversationChange` itself.
  - `infrastructure.input.ConversationStreams` (component) holds the open streams and receives the change with `@TransactionalEventListener` (default phase `AFTER_COMMIT`, `fallbackExecution = true`).
  - `infrastructure.input.ConversationStreamController` opens a stream and registers it.
- **Rationale**: Input and output adapters stay apart (`HexagonalArchitectureTest`): they share only a domain record, through Spring's event bus. `AFTER_COMMIT` matters: the browser asks for the message as soon as it hears of it, so the notice must not leave before the message is committed. A plan-change decline (`PlanChangeDecider`) sends its message through the same use case inside its own transaction and is announced after that transaction commits, for free. Who is told is a business rule (no read receipts), so it is in the domain service and unit-tested with a fake port.
- **Alternatives considered**: the registry as the output adapter (the controller would then depend on an output adapter, which the architecture test forbids); a registry in `domain` (holds framework types); publishing inside the transaction (the reader could fetch before commit and see nothing).

## R4. Sending without slowing the sender

- **Decision**: `ConversationStreams` sends each notice on a virtual thread (`Executors.newVirtualThreadPerTaskExecutor()`), so the request that stored the message returns at once (FR-009). A send that fails (`IOException`, `IllegalStateException` on a completed emitter) removes that stream from the registry and completes it; it never reaches the caller.
- **Rationale**: a stuck client socket must not block a person's send. `SseEmitter.send` is safe to call from several threads.
- **Alternatives considered**: `@Async` on the listener (needs `@EnableAsync` and an executor bean for one call site).

## R5. Lifetime, heartbeat and the end of a sign-in

- **Decision**:
  - Each emitter has a 30-minute timeout; `onTimeout` completes it normally. `EventSource` then reconnects by itself, which counts as use of the sign-in, as spec 58's refreshes did.
  - A `@Scheduled` heartbeat every 25 seconds (scheduling is already enabled by `PlanConfig`, and sweeps already live in `infrastructure.input`) does two things per open stream: it checks that the stream's session still exists in the Spring Session JDBC store (`SessionRepository.findById`, which does not touch the session's last-access time outside a request), completing the stream if not; and it sends a `ping` event, completing the stream if the write fails. The ping is a named event rather than an SSE comment, because `EventSource` hides comments and the app needs to see that the stream is alive.
  - The emitter's `onCompletion`, `onTimeout` and `onError` remove it from the registry.
- **Rationale**: Spring Session JDBC publishes no event when a session is deleted (sign-out, password reset's `endAll`, expiry), so the stream has to look. 25 seconds keeps idle connections open through proxies (FR-007) and ends a stream within 1 minute of its sign-in (FR-006, SC-006). Spring's docs recommend periodic data because the Servlet API does not report a gone client. In the other direction, the browser may not notice a dead connection either: when the backend stopped behind the Vite dev proxy, the proxy left the browser's stream open with nothing coming through, so the app watches for the ping (R12).
- **Alternatives considered**: no timeout (a stream would never re-authenticate); a 1-minute timeout instead of the session check (every tab would reconnect and catch up every minute); `HttpSessionEventPublisher` (Spring Session JDBC does not raise destroy events for sessions deleted by id).
- **Ceiling**: one `findById` query per open stream per tick. Fine for a coaching app's traffic; batch the ids into one query if streams reach the thousands.

## R6. Cap of 10 streams per sign-in

- **Decision**: The registry keeps streams in the order opened. Registering a stream whose sign-in (session id) already holds 10 sends the oldest of that sign-in `retry: 30000` and completes it first, so an evicted tab waits 30 s before reconnecting and tabs cannot evict each other in a tight loop.
- **Rationale**: Clarification 1. Bounds the memory one sign-in can take; the closed tab, if it still exists, reconnects by itself.
- **Ceiling**: the registry is one list scanned on each notice and each tick; `ponytail:` comment in code, index by person if streams reach the thousands.

## R7. Security of the stream

- **Decision**:
  - The endpoint falls under the existing `/api/**` `authenticated()` rule: no change, no new public endpoint. GET only, so CSRF does not apply.
  - `SecurityConfig` permits `DispatcherType.ASYNC` beside `ERROR`. Spring Security authorizes every dispatch; when an emitter completes after its session was deleted, the async dispatch would otherwise be refused and Spring Security would try to write a 401 into a response that is already streaming. An ASYNC dispatch only finishes a request that passed authorization on its REQUEST dispatch; a client cannot start one.
  - The stream's person is the authenticated account id; its sign-in is the session id. Neither comes from the request's parameters.
  - Nothing from the stream is logged except that a stream was opened or closed, without message text (FR-010).
- **Alternatives considered**: a separate rule for the path (the generic rule already covers it).

## R8. Shutdown and restart

- **Decision**: `ConversationStreams` completes every open stream on `ContextClosedEvent`.
- **Rationale**: graceful shutdown waits for open requests; held streams would delay every restart by the full shutdown timeout. `ContextClosedEvent` is published before lifecycle beans (the web server) stop. Browsers reconnect after the restart and catch up (Story 3).

## R9. Proxy and buffering

- **Decision**: No extra configuration. The Vite dev proxy (`http-proxy`) streams responses as they arrive, response compression is off in Spring Boot by default and the project does not turn it on, and production serves the API from the same process without a proxy in front (docs/adr/0001). The heartbeat sends a first `ping` at once when the stream opens, so the browser's `open` fires immediately and buffering would be visible in the quickstart check.
- **Alternatives considered**: `X-Accel-Buffering: no` (only for nginx, which is not in the setup).

## R10. The subscription in the app

- **Decision**:
  - `features/messaging/use-conversation-events.ts`: a hook that, for one account, opens one `EventSource('/api/v1/conversations/events')`, closes it on unmount, and reacts to events (R11).
  - Mounted once in `app/app.tsx` by a small `ConversationEventsSync` component beside `AccountSettingsSync`, keyed by `useCachedAccount()`'s account id. That sits above every route, so both areas, the quiz waiting state and the logger are covered (comment on #149), and signing out (cache cleared) or a different account closes the old stream.
  - The URL is typed: `const EVENTS: keyof paths = '/api/v1/conversations/events'`, so removing or renaming the endpoint fails the type check.
- **Rationale**: the app has no route that is the common parent of every client route (the quiz, the logger and the client area are siblings), and `useCachedAccount` already exists for exactly this kind of app-wide concern without fetching on public pages.
- **Alternatives considered**: mounting in `ClientArea` and `TrainerArea` (misses the waiting state, as the comment on #149 says); a `SharedWorker` for one connection per browser (not supported everywhere, much more code).

## R11. What each event does to the cache

- **Decision**:
  - `message` with client id `c`: invalidate the list and the unread count (`CONVERSATIONS`); if messages of `c` are in the cache, fetch the newest page only and merge its messages by id into the newest cached page (the existing `withMessage` generalised to several). If none of the fetched messages is already known and the page has more behind it (a gap of over 50), invalidate that conversation instead.
  - `read`: invalidate `CONVERSATIONS`.
  - `open` (first connect and every reconnect): invalidate `CONVERSATIONS` and `MESSAGES`, which refetches every active query as after a reload (FR-015). The first connect is included, because a message sent between a page's first load and the stream opening would otherwise be missed.
- **Rationale**: FR-012 asks not to reload earlier pages on every message; merging by id makes an event that crosses a page load or this tab's own send harmless (SC-007, FR-014): `MessageList` already hides an outbox entry whose id is in the cache, and `withMessage` already ignores a known id. The existing `useFirstUnread` marks the conversation read when the list's `dataUpdatedAt` changes and the tab is visible, so a live message in an open conversation is marked read without new code (FR-018).

## R12. Reconnecting

- **Decision**: Let `EventSource` retry by itself while its `readyState` is `CONNECTING` (network drops, a stream that ended). When it gives up (`readyState === CLOSED`: a non-200 answer, such as the dev proxy's 502 while the backend restarts, or a 401), the hook closes it and schedules a new attempt after 1, 2, 4, 8, 16, then 30 seconds, resetting after a successful `open`. Before reopening, it probes `GET /api/v1/conversations/unread-count` through `api`: a 401 there makes the existing `onResponse` middleware send the person to sign-in and the hook stops; a network error or 5xx keeps backing off; a 200 reopens the stream. One timer per hook, so at most one attempt per tab. A watchdog closes a stream that has delivered no event (not even `ping`) for 60 seconds and goes the same way, because a connection a proxy or a sleeping network left half open never tells the browser it is gone.
- **Rationale**: `EventSource` does not expose the status of a failed connect, so the probe is how a 401 is told apart from a server that is down, and it reuses the app's one signed-out path (FR-016). Clarification 2: no fallback refresh while down.
- **Alternatives considered**: `fetch` with a stream reader instead of `EventSource` (status visible, but parsing and retry written by hand).

## R13. Removing the fixed refreshes

- **Decision**: Drop `refetchInterval` from `useConversations`, `useMessages` and `useUnreadCount`, and the constants, and give the three `staleTime: Infinity`: they are fetched when first shown and again only when an event or the catch-up invalidates them. `stream-health.ts` records when the stream was last heard from; within 35 seconds it is current. While it is current, `useFirstUnread` reads the cache when an open conversation comes back on screen, and a send or a mark read leaves the refresh to its own event. Otherwise they ask, and `useFirstUnread` joins a load already on its way (`cancelRefetch: false`) instead of cancelling it and starting another.
- **Rationale**: FR-017 and SC-004. The stream stays open in a hidden tab and catches up after any reconnect, so refetching on every return to the tab only repeated it, and with the server down every return made several failing requests, each retried. Opening a conversation also sent the list three times: a new observer's mount fetch, `useFirstUnread`'s `refetch()` cancelling it to start another, and React's development double effect; mark read then refreshed the list and count, and its `read` event did again. When the stream has gone quiet, `useFirstUnread` still asks, because a phone may have frozen the tab and dropped its stream.

## R14. Tests

- **Backend**:
  - `MessagingServiceTest` (unit, fake `ConversationEvents`): a new message tells both people; a repeated send tells nobody; a refused send (empty, too long, wrong set, someone else's conversation) tells nobody; mark read tells only the reader; mark read of a conversation with no stored conversation tells nobody.
  - `ConversationStreamsTest` (unit, no Spring): a change reaches only that person's streams, on every sign-in of theirs; the 11th stream of a sign-in closes the oldest and the other 10 keep receiving; a stream whose session is gone is closed on the next tick; a failing send removes the stream and does not throw.
  - `ConversationStreamIT` in `messaging.infrastructure.input`, beside the package-private registry so it can call `heartbeat()` (real app, real session cookie, real database): signed out, the stream is 401; signed in as a client, opening the stream and having the trainer send a message makes `event:message` with the client's id appear on the client's stream after commit (Awaitility on the async response); another client's stream receives nothing; deleting the client's session and running the heartbeat closes the stream.
- **Frontend**:
  - `use-conversation-events.test.tsx` with a fake `EventSource` on `globalThis`: one stream per account, closed on unmount and on account change; a `message` event updates the list, count and a loaded conversation, merges without duplicates; a `read` event updates the count; `open` refreshes active queries; on `CLOSED` it probes and reopens with growing delays (fake timers), and a 401 probe stops it.
  - Existing tests that relied on the fixed refreshes are changed to drive updates through the fake stream.
