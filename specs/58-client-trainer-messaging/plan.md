# Implementation Plan: Client and trainer exchange messages

**Branch**: `feat/58-client-trainer-messaging` (one pull request for the whole story, one commit per task) | **Date**: 2026-11-02 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/58-client-trainer-messaging/spec.md`

## Summary

A new backend feature `messaging` (tasks #59, #60) and a new frontend feature `messaging` (tasks #61, #62). A conversation exists per client–trainer pair, created when the first message is sent. Messages are stored once, with an id the sender chose, so a retry can never duplicate. Each person's last-read time lives in its own row, so two people marking read at the same moment never collide. The unread count is a query over messages and that time. The screens poll every few seconds while visible (TanStack Query `refetchInterval`, which pauses in a hidden tab, exactly what the clarified hidden-tab rule needs). A message being sent, or failed, is kept in the page's own memory until the server confirms it.

## Technical Context

**Language/Version**: Java 25 (backend), TypeScript 6 (frontend)

**Primary Dependencies**: Spring Boot 4.1 (MVC, Security, Data JPA, JDBC), Flyway, MapStruct, Lombok; React 19, React Router 8, TanStack Query 5 (`useInfiniteQuery` for earlier pages), react-i18next, shadcn/ui. **No new dependency.** New shadcn components: none; `textarea`, `badge`, `empty`, `avatar`, `skeleton`, `input-group` and `alert` are already installed (`npx shadcn@latest docs <name>` is read before use).

**Storage**: PostgreSQL, migration `V14__create_messaging.sql` (V13 is the latest on `master`; no open pull request adds a migration)

**Testing**: JUnit 5 + AssertJ with hand-written fakes (domain), `@WebMvcTest` (controller), `@DataJpaTest` + Testcontainers (persistence, the read model); Vitest + Testing Library + `userEvent` (frontend)

**Target Platform**: Browser SPA: client at 375 px, trainer at 1280 px; API under `/api/v1`

**Project Type**: Web application; this story touches `backend/` and `frontend/`

**Performance Goals**: a message shows on the other person's open screen within 15 s (SC-003): open conversation and list poll every 5 s, the navigation count every 10 s

**Constraints**: message text is never logged and never sent to an LLM; every operation is by the signed-in person; no new dependency; no push connection

**Scale/Scope**: one trainer has tens of clients; a conversation holds hundreds to a few thousand short messages; 5 endpoints, 3 tables, 2 pages, about 45 strings per language

## Constitution Check

| Principle | Status |
|---|---|
| I. Hexagonal boundaries | Pass. New feature `messaging` with `domain` (model, input, output, service, exception) and `infrastructure` (config, input, output). It reads `accounts`, `quiz_submissions` and `plans` through one JDBC read model and imports no class of another feature, as `JdbcClientDirectory` does. `HexagonalArchitectureTest` must stay green; no new rule needed. |
| II. One contract | Pass. Five endpoints in the generated OpenAPI document; `schema.d.ts` regenerated in the commit of task #60. Errors carry a stable `code` (contracts/api.md); screens translate by code and show no server text. |
| III. Secure by default | Pass. No public endpoint. Every call is by the signed-in person (`/api/**` is authenticated), CSRF applies to the two writes, a person who is not one of the two gets `404` (R1). Text is not logged and not sent to an LLM (R9). |
| IV. Plan changes | Not affected. |
| V. Tests verify behaviour | Pass. See Tests: unread rule, access rules, idempotent send, ordering and paging, read marks never going back, message states and retry, polling and hidden-tab behaviour. No test of getters or markup. |
| VI. Simplicity | Pass. No new library, no push, no cache, no outbox table. The one read model is a single query. Deliberate simplifications are listed under R10. |
| VII. One design system | Pass. Kinetiq tokens and installed shadcn components; EN and PL; browser check at 375 px (client) and 1280 px (trainer), light and dark. |

Re-checked after design: unchanged.

### Definition of Done items

- *Another person's resource answers 404*: applies, tests in `MessagingServiceTest` and `ConversationControllerTest` (a third person, the other trainer's client, an unknown id).
- *New entities get demo data*: applies, `R__demo_messages.sql` (data-model.md, Demo data).
- *API change comes with regenerated `schema.d.ts`*: in the commit of task #60.
- *UI checked in the browser, light and dark, 375 px / 1280 px, keyboard and labels*: in the commits of tasks #61 and #62, per quickstart.md. The story's final review repeats it on the whole, and checks the backend (quickstart.md, Security checks).

## Project Structure

### Documentation (this feature)

```text
specs/58-client-trainer-messaging/
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
backend/src/main/
├── resources/db/migration/V14__create_messaging.sql              tables, constraints, indexes (#59)
├── resources/db/dev-data/R__demo_messages.sql                    demo conversations, local profile only (#59)
└── java/com/kinetiq/messaging/
    ├── domain/
    │   ├── model/            ConversationId, MessageId, MessageText, ConversationPair, Conversation, Message,
    │   │                     MessagePage, ConversationOverview, LastMessage, PlanWeek            (#59)
    │   ├── exception/        MessageEmptyException, MessageTooLongException,
    │   │                     MessageIdInUseException                                   (#59)
    │   │                     ConversationNotFoundException, MessageNotFoundException   (#60)
    │   │                     MessageIdInUseException                                   (#59 text rules, #60 the rest)
    │   ├── output/           ConversationRepository, MessageRepository, ConversationParticipants,
    │   │                     ConversationDirectory                                     (#59)
    │   ├── input/            ExchangeMessagesUseCase, SendMessageCommand               (#60)
    │   └── service/          MessagingService                                          (#60)
    └── infrastructure/
        ├── config/           MessagingConfig                                           (#60)
        ├── input/            ConversationController, request/response records, MessagingWebMapper (#60)
        └── output/           ConversationEntity, MessageEntity,
                              Jpa* repositories (with the native read-mark upsert), *RepositoryImpl, MessagingMapper,
                              JdbcConversationParticipants, JdbcConversationDirectory   (#59)

backend/src/test/java/com/kinetiq/messaging/
├── domain/                   MessageTextTest, MessagingServiceTest, fakes
└── infrastructure/           ConversationControllerTest, MessageRepositoryImplIT, ConversationDirectoryIT

frontend/src/
├── lib/api/schema.d.ts                                           regenerated (#60)
├── app/router.ts                                                 messages routes (#61, #62)
├── app/client-area.tsx                                           unread count on the tab (#61)
├── app/trainer-area.tsx                                          unread count in the sidebar (#62)
├── components/account-avatar.tsx                                 moved from the account feature, now used by two (#61)
├── features/messaging/
│   ├── api/messaging.ts                                          queries and mutations (#61)
│   ├── message-time.ts (+ .test.ts)                              times, day separators (#61)
│   ├── conversation-filter.ts (+ .test.ts)                       search by name, ignoring case and diacritics (#62)
│   ├── use-outbox.ts (+ .test.tsx)                               Sending / Not sent / Retry state (#61)
│   ├── use-page-visible.ts                                       visible tab (#61)
│   ├── first-unread.ts (+ .test.ts)                              the first unread message of those loaded
│   ├── use-first-unread.ts                                       where an open conversation starts, then marks it read
│   ├── components/
│   │   ├── unread-count.tsx (+ .test.tsx)                        "99+", announced name (#61)
│   │   ├── message-list.tsx (+ .test.tsx)                        bubbles, days, states, load earlier (#61)
│   │   ├── composer.tsx (+ .test.tsx)                            Enter / Shift+Enter, limit, Send (#61)
│   │   ├── conversation.tsx                                      messages and composer, shared by both pages (#61)
│   │   ├── load-error.tsx                                        translated message with "Try again" (#61)
│   │   └── conversation-list.tsx (+ .test.tsx)                   rows, search, empty (#62)
│   └── pages/
│       ├── client-messages-page.tsx (+ .test.tsx)                (#61)
│       └── trainer-messages-page.tsx (+ .test.tsx)               (#62)
└── locales/en.json, pl.json                                      messaging.* (#61, #62)
```

The last-read rows have no entity: they are written by one native upsert and read by counting queries (R5).

**Structure Decision**: One feature `messaging` on each side, as `CONTEXT.md` names Conversation and Message. The message list and the composer are used by both screens, so they live in the feature, not in `components/`.

## Work per task (one commit each, in one pull request)

| Task | Commit subject | Contents |
|---|---|---|
| #59 | `feat(backend): add conversation and message model (#59)` | Spec Kit files of story #58 and `CONTEXT.md` terms; migration V14; domain model with `MessageText`; ports; JPA adapters and the read model; demo data; unit and `*IT` tests of the unread rule, ordering, read marks and the pair rule |
| #60 | `feat(backend): add send, list and mark-read api (#60)` | Use case and service; controller and records; error codes; `@WebMvcTest` and service tests including every `404`; regenerated `schema.d.ts` |
| #61 | `feat(frontend): add client conversation screen (#61)` | API hooks, outbox, message list, composer, client page and route, tab count, strings EN and PL, tests, browser check at 375 px |
| #62 | `feat(frontend): add trainer messages screen (#62)` | Conversation list with search, trainer page and routes, sidebar count, strings, tests, browser check at 1280 px |

Order: #59, #60, #61, #62 (each needs the one before). After #62: a whole-story review (quickstart.md) on the running app and API, with the fixes it finds as further commits of the same pull request.

## Tests

| Test | Covers |
|---|---|
| `MessageTextTest` | Trimmed, Unicode edge spaces removed; empty and whitespace-only refused (`message.empty`); 2000 units accepted, 2001 refused (`message.too-long`); inner line breaks kept |
| `MessagingServiceTest` | A client reaches only their own pair; a trainer only their client; a third person, another trainer, an unknown client and an unlinked client all get `ConversationNotFoundException`; send creates the conversation once; sending the same id and text again returns the stored message; the same id with other text or from another conversation gives `MessageIdInUseException`; mark read of a pair without a conversation does nothing; `before` of a message of another conversation gives `MessageNotFoundException`; page has at most 50 and `hasMore` |
| `ConversationControllerTest` | Status codes (200/201/204), response shape carries no email, no sender id, only `mine`; `400` empty, too long and missing fields with codes; `404` codes; unauthenticated gives `401`; a write without the CSRF token gives `403` |
| `MessageRepositoryImplIT` | Read mark never moves back and does not touch the other person's; two people marking read in parallel both succeed; paging by `(received_at, id)` gives a stable order with equal times; a repeated id stores nothing and two parallel attempts store one row; two parallel first sends make one conversation; constraint on text length |
| `ConversationDirectoryIT` | Unread = other's messages after my last-read time, never mine, all when never read; trainer: every linked client and no row for the trainer's own account, ordered by last message then name, none from another trainer; client: one row with the trainer's name, without goal or week; unread totals; goal and plan week; last message preview cut to 140 units; re-linked client does not show the old trainer's history |
| `message-time.test.ts` | Today / Yesterday / date separators and list times in EN and PL, in the viewer's time zone |
| `use-outbox.test.tsx` | Sending → confirmed removes the pending entry; failure keeps text as Not sent; Retry sends the same id and text |
| `composer.test.tsx` | Send disabled when empty or spaces; Enter sends; Shift+Enter adds a line; Enter during text composition does not send; typing and pasting stop at 2000; cleared after send |
| `message-list.test.tsx` | Messages in order with the newest last; own messages marked Sent with the time on the last of a run; Sending…; Not sent with Retry; no read ticks; "Load earlier" only when more exist; plain text (markup shown as typed); a message whose first attempt did arrive shows once |
| `unread-count.test.tsx` | Hidden at 0; "99+" above 99; announced as part of the name |
| `client-messages-page.test.tsx` | Opens in the conversation with the trainer's name; empty invitation; send, fail, retry; marks read when visible and unread; stays unread in a hidden tab and is marked when shown; load error with "Try again" |
| `conversation-filter.test.ts` | "zielinski" finds "Zieliński", "lukasz" finds "Łukasz"; first name or surname; case ignored; nothing matches message text |
| `conversation-list.test.tsx` | Rows with name, preview, "You: ", time and count; "No messages yet"; search and no-match message; no clients state with invite link |
| `trainer-messages-page.test.tsx` | Nothing selected and nothing marked read on open; choosing a conversation opens it by its address and marks it read; reload keeps it; unknown id shows the not-found state; search hiding the open row keeps it open; Enter sends; a narrow window shows the list or the conversation, with a way back |
| `client-area.test.tsx`, `trainer-area.test.tsx` (extended) | Navigation shows the total and drops it after reading; a failing unread-count request hides the count and leaves the page working |

## Open items

| Item | Settled by |
|---|---|
| The "Messages" tab of the trainer's client page (`/trainer/clients/:id/messages`, currently an empty tab) | Not in this story; it may later open the same conversation |

## Complexity Tracking

None.
