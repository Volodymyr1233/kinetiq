# Implementation Plan: Messages arrive without reloading

**Branch**: `feat/147-live-messages` | **Date**: 2026-11-11 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/147-live-messages/spec.md`

## Summary

Each tab of a signed-in person opens one server-sent event stream. When a message is stored or a conversation is marked read, the messaging use case publishes a notice for the people concerned (both people for a message, only the reader for a read). After the transaction commits, the notice goes to every open stream of those people, carrying only the event name and the conversation's client id. The app reacts by refreshing the list and count and merging the conversation's newest page, reconnects with backoff, catches up after every connect, and drops spec 58's fixed refreshes. Details and alternatives: [research.md](research.md).

## Technical Context

**Language/Version**: Java 25 (backend), TypeScript 5 / React 19 (frontend)

**Primary Dependencies**: Spring Boot 4.1 (Spring MVC `SseEmitter`, Spring Security 7, Spring Session JDBC, scheduling), TanStack Query, native `EventSource`. No new dependency.

**Storage**: none new. Open streams live in the server's memory (R5, R6).

**Testing**: JUnit 5 + AssertJ (unit, hand-written fakes), `@SpringBootTest` with Testcontainers PostgreSQL and `MockMvcTester` for the real stream; Vitest + Testing Library with a fake `EventSource`.

**Target Platform**: one server process serving the SPA and the API (docs/adr/0001); evergreen browsers on phone and desktop.

**Project Type**: web application (`backend/`, `frontend/`).

**Performance Goals**: notice on screen in under 2 s (SC-001, SC-002); live again within 30 s of the server being reachable (SC-003); no timed requests while idle (SC-004).

**Constraints**: a notice only after commit; sending never slowed by a stream (FR-009); a stream ends within 1 minute of its sign-in (FR-006); at most 10 streams per sign-in (FR-001).

**Scale/Scope**: a trainer with tens of clients, a few tabs per person. One list of streams, scanned linearly (R6 ceiling).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Check | Result |
|---|---|---|
| I. Hexagonal boundaries | Domain gets a plain record and a port; the output adapter publishes a Spring event, the input adapter listens. Input and output adapters never reference each other; `HexagonalArchitectureTest` unchanged and passing (R3). | Pass |
| II. One contract | The stream's path is in the generated OpenAPI document and the regenerated `schema.d.ts`, and the frontend types its URL against `paths`. Event data is a bare id, documented in [contracts/conversation-events.md](contracts/conversation-events.md). | Pass |
| III. Secure by default | Covered by the existing `/api/**` authenticated rule; no public endpoint. Cookie session only. Events carry no text or names; only the people of the conversation are told, reads only to the reader. Stream closes when its sign-in ends. Permitting the `ASYNC` dispatch only lets an already-authorized request finish (R7). No LLM. | Pass |
| IV. Explainable plan changes | Not touched. | N/A |
| V. Tests verify behaviour | Who is told, the registry rules, a real stream receiving an event after commit, refusal when signed out, closing after the session ends, and the app's reactions and reconnects (R14). | Pass |
| VI. Simplicity | No new dependency, no new table, notices instead of copies, no replay, no fallback refresh, no connection indicator. Two marked ceilings (linear scan, one session lookup per stream per tick). | Pass |
| VII. One design system | No visual change; no new strings. Still checked in the browser at 375 px and 1280 px, light and dark, EN and PL (quickstart step 8). | Pass |

Post-design re-check: unchanged, all pass. No entry in Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/147-live-messages/
├── spec.md
├── plan.md                       # this file
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/conversation-events.md
├── checklists/requirements.md
└── tasks.md                      # /speckit-tasks
```

### Source Code

```text
backend/src/main/java/com/kinetiq/
├── common/infrastructure/config/SecurityConfig.java          # permit DispatcherType.ASYNC (R7)
└── messaging/
    ├── domain/
    │   ├── model/ConversationChange.java                     # new (data-model)
    │   ├── output/ConversationEvents.java                    # new port
    │   └── service/MessagingService.java                     # publishes on new message and on mark read
    └── infrastructure/
        ├── config/MessagingConfig.java                       # wires the port
        ├── output/SpringConversationEvents.java              # new: ApplicationEventPublisher adapter
        └── input/
            ├── ConversationStreams.java                      # new: registry, after-commit listener, heartbeat, shutdown
            └── ConversationStreamController.java             # new: GET /api/v1/conversations/events

backend/src/test/java/com/kinetiq/
├── messaging/infrastructure/input/ConversationStreamIT.java  # real stream: 401, event after commit, isolation, session end
└── messaging/
    ├── domain/service/MessagingServiceTest.java              # who is told (+ fake in MessagingFakes)
    └── infrastructure/input/ConversationStreamsTest.java     # registry rules

frontend/src/
├── app/app.tsx                                               # mounts ConversationEventsSync
├── app/conversation-events-sync.tsx                          # new: one stream per cached account
├── features/messaging/
│   ├── api/messaging.ts                                      # refresh intervals removed; newest-page merge
│   ├── use-conversation-events.ts                            # new: EventSource, events → cache, reconnect
│   └── use-conversation-events.test.tsx                      # new
└── lib/api/schema.d.ts                                       # regenerated
```

**Structure Decision**: the existing web-application layout. Backend code stays in the `messaging` feature; the only cross-cutting change is one line in `SecurityConfig`. Frontend logic is in `features/messaging`; `app/` only mounts it above the routes, as `AccountSettingsSync` does.

## Task outline (for `/speckit-tasks`)

1. **#148 Event stream per signed-in person** (backend, one commit): `ConversationChange`, `ConversationEvents` port, publishing in `MessagingService` with its unit tests; `SpringConversationEvents`; `ConversationStreams` and `ConversationStreamController` with their tests; `ASYNC` dispatch permitted; IT over the real session cookie; regenerated `schema.d.ts`; quickstart steps 1 and 6 checked through the Vite proxy.
2. **#149 Frontend subscription** (frontend, one commit, after #148): `useConversationEvents` and `ConversationEventsSync`, newest-page merge, reconnect with probe and backoff, refresh intervals removed, tests updated; browser checks of quickstart steps 2 to 8.

Each commit ends with `(#148)` / `(#149)`; one pull request for story #147 that closes #147, #148 and #149.

## Complexity Tracking

None.
