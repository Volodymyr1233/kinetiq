# Implementation Plan: Client asks about a logged set

**Branch**: `feat/172-ask-about-set` | **Date**: 2026-11-10 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/172-ask-about-set/spec.md`

## Summary

A message may carry an About reference to one of its sender's own set logs. #173 stores it as one nullable column on `messages`, checks in `MessagingService.send` that the set log belongs to a session log of the sender (`404 message.about-not-found` otherwise, the same for another client's set, an unknown id and a trainer), and returns the set's session, exercise, place and RPE with each message, read from the set as it is now. #174 adds an icon button "Ask your trainer" to every logged row of the logger, locked or not; it opens the client's conversation in a sheet over the logger with the About chip in the composer, and the outbox carries the reference through Sending, Not sent and Retry. #175 shows the chip above the message in both conversation views as a link: the client's to the logger on that session, the trainer's to the client's Logs tab, both with the set in the address, marked, in view and focused. No dependency is added.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1) backend; TypeScript, React 19 frontend

**Primary Dependencies**: Existing only: Spring MVC, Spring Data JPA, `JdbcTemplate`, MapStruct, Lombok; React Router 8 (`useSearchParams`, `Link`), TanStack Query, react-i18next, shadcn (`sheet`, `button`, `table`, all present), Lucide

**Storage**: PostgreSQL; one migration, `V26__add_message_about.sql` (a nullable column and a foreign key)

**Testing**: JUnit 5 + AssertJ (service with fake ports), `@WebMvcTest` (controller), `@DataJpaTest` with Testcontainers (repository and `JdbcAboutSets`), Vitest + Testing Library (frontend). Docker must be running for the backend gate.

**Target Platform**: Browser; client screens at 375 px, trainer screens at 1280 px; same-origin `/api/v1`

**Project Type**: Web application (backend + frontend in one repository)

**Performance Goals**: A page of messages costs one more query when any of its messages has a reference, none otherwise. The logger makes no extra request until the action is used.

**Constraints**: Only the sender's own set can be referenced, and a refusal tells nothing; the logger keeps its state while the conversation is open; a feature does not import another feature; every string in English and Polish; no sideways scroll at 375 px and 320 px; the chip and the mark do not rely on colour alone.

**Scale/Scope**: At most one reference per message; 50 messages a page.

## Constitution Check

*GATE: passes before research; re-checked after design.*

| Principle | Check | Result |
|---|---|---|
| I. Hexagonal boundaries | The rule is in `MessagingService` through a new output port `AboutSets`; its JDBC adapter reads the session-log tables and uses no class of `sessionlog.infrastructure`. The domain takes typed ids and `LocalizedText` from other features' domain models; no feature imports messaging, so no cycle. `HexagonalArchitectureTest` runs in `check`. | Pass |
| II. One contract | The request and the response each gain one optional field; `schema.d.ts` is regenerated in the commit of #173. The new code `message.about-not-found` has `errors.message.about-not-found` in both languages. | Pass |
| III. Secure by default | No new endpoint and none opened. Ownership is one query, so another client's set, an unknown set and a trainer's attempt answer the same `404`; tests prove the first and the last. Nothing reaches an LLM or a log. | Pass |
| IV. Explainable plan changes | Not touched. | n/a |
| V. Tests verify behaviour | Service tests for each case of the rule and for a repeat; repository tests that a reference is stored and read back with the set's current values; controller tests for the field and the code; frontend tests for when the action shows, the composer chip, Retry with a reference, the chip's link, and both addresses. No getter tests. | Pass |
| VI. Simplicity | One column, one port, one chip component, the existing sheet; no copy of the set on the message, no new endpoint, no dependency (research R1, R2, R7). | Pass |
| VII. One design system | `Sheet`, `Button`, `Table` from shadcn and Kinetiq tokens; the chip follows the design system (6.7); checked in the browser, light and dark, at 375 px (client) and 1280 px (trainer), in English and Polish. | Pass |

No violations, so no Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/172-ask-about-set/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── api.md
├── checklists/
│   └── requirements.md
└── tasks.md             # /speckit-tasks
```

### Source Code (repository root)

```text
backend/src/main/resources/db/
├── migration/V26__add_message_about.sql                 # #173
└── dev-data/R__demo_session_logs.sql                    # #173: a demo message with a reference

backend/src/main/java/com/kinetiq/messaging/
├── domain/
│   ├── model/AboutSet.java                              # #173: new
│   ├── model/Message.java                               # #173: optional AboutSet, isSameSendAs
│   ├── input/SendMessageCommand.java                    # #173: + the set log id
│   ├── output/AboutSets.java                            # #173: new port
│   ├── exception/AboutSetNotFoundException.java         # #173: message.about-not-found, NOT_FOUND
│   └── service/MessagingService.java                    # #173: the rule in send
└── infrastructure/
    ├── config/MessagingConfig.java                      # #173: wires AboutSets
    ├── input/{SendMessageRequest,MessageResponse,AboutResponse}.java   # #173
    ├── input/{ConversationController,MessagingWebMapper}.java          # #173: Locale, about
    └── output/{JdbcAboutSets,MessageEntity,JpaMessageRepository,MessageRepositoryImpl,MessagingMapper}.java   # #173
backend/src/test/java/com/kinetiq/messaging/...          # MessagingServiceTest, MessagingFakes, ConversationControllerTest, MessageRepositoryImplIT, JdbcAboutSetsIT

frontend/src/
├── lib/api/schema.d.ts                                  # #173: regenerated
├── features/messaging/
│   ├── api/messaging.ts                                 # #174: aboutSetLogId in the send
│   ├── use-outbox.ts                                    # #174: an entry keeps its about
│   ├── components/about-chip.tsx                        # #174: new (composer use); #175: link use
│   ├── components/{composer,conversation}.tsx           # #174: the chip and its removal
│   ├── components/message-list.tsx                      # #175: the chip above a message, aboutHref
│   └── pages/{client,trainer}-messages-page.tsx         # #174: about in; #175: aboutHref
├── features/sessionlog/
│   ├── components/set-row.tsx                           # #174: the action; #175: the mark
│   ├── components/session-log-table.tsx                 # #175: the mark
│   ├── pages/logger-page.tsx                            # #174: onAsk; #175: ?set
│   └── pages/client-logs-tab.tsx                        # #175: ?session and ?set
├── app/
│   ├── logger-route.tsx                                 # #174: new, the logger with the sheet
│   ├── message-trainer-sheet.tsx                        # #174: its content shared
│   └── router.ts                                        # #174: both logger routes use LoggerRoute
└── locales/{en,pl}.json                                 # #173: the error; #174, #175: the texts

CONTEXT.md                                               # #173: + About reference
```

**Structure Decision**: The reference and the chip belong to `messaging`; the action and the marked row to `sessionlog`. The two meet only in `app/` (the sheet) and through addresses and the generated API type, so neither feature imports the other.

## Phases

One pull request for the story, one commit per task, in this order.

- **#173 (backend, generated types)**: migration, `AboutSet`, `AboutSets` and `JdbcAboutSets`, the rule in `send`, the request and response fields, tests (own set stored; another client's set and a trainer's attempt answer 404 and store nothing; a repeat answers the same; another reference under the same id is `message.id-in-use`), `schema.d.ts`, `errors.message.about-not-found`, the demo message, `CONTEXT.md`.
- **#174 (frontend)**: the action on logged rows, locked included; `LoggerRoute` with the sheet; the chip in the composer with its removal; the outbox and the send hook carry the reference; texts; tests, `locked.test.tsx` included (research R14). Blocked by #173.
- **#175 (frontend)**: the chip as a link above messages in both views and in every delivery state; `?set` in the logger; `?session` and `?set` in the Logs tab; the marked row; texts; tests. Blocked by #174.

After each frontend task: the browser check of `frontend/CLAUDE.md`, light and dark, 375 px and 320 px (client) or 1280 px (trainer), English and Polish, keyboard and labels.
