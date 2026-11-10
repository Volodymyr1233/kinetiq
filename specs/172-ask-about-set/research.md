# Research: Client asks about a logged set

Decisions taken while planning. Each: what, why, what else was considered.

## R1. What is stored

**Decision**: One nullable column, `messages.about_set_log_id`, with a foreign key to `set_logs (id)`. Migration `V26__add_message_about.sql`. No index on it.

**Why**: The chip shows the set as it is now (spec, Assumptions), so everything it shows is read from the set log, its session log, its entry and its exercise; a copy on the message would depend on the set, not on the message's key (`backend/CLAUDE.md`, 3NF). A set log is never deleted, so the key never dangles and nothing scans `messages` by this column.

**Rejected**: A copy of session name, exercise, set number and RPE on the message (a second truth that disagrees with the set after a change within its 24 hours); a separate `message_abouts` table (one optional value per message is a column).

## R2. How messaging reads a set

**Decision**: A new output port in messaging, `AboutSets`, with one method: the set log of an id when it belongs to a session log of the given sender. Its adapter `JdbcAboutSets` reads `set_logs`, `session_logs`, `session_log_entries` and `exercises` with one JDBC query and uses no class of the `sessionlog` infrastructure. The same adapter gives `MessageRepositoryImpl` the sets of a page (`where id = any(?)`), so a page costs one more query when at least one of its messages has a reference and none otherwise.

**Why**: Messaging already reads the accounts, the quiz and the plans this way (`JdbcConversationParticipants`, `JdbcConversationDirectory`: "one query, and no class of those features"). The repository returns complete messages, so the service, the use case and the fakes of the tests deal with one shape: a `Message` that may hold an `AboutSet`.

**Rejected**: A new use case in `sessionlog` called by messaging (a port and a service for one read that the directory pattern already covers); joining in the JPA query of the page (the entity is read-only and plain; a five-table join in JPQL for an optional value on a few rows); the service asking for the sets after the page (two shapes of a message, with and without its set).

## R3. The value in the domain

**Decision**: `AboutSet` record in `messaging.domain.model`: `SetLogId`, `SessionLogId`, session name, session date, exercise name as `LocalizedText`, entry position, set number, RPE. `Message` gets an optional `AboutSet`; `isSameSendAs` also compares the set log id.

**Why**: Typed ids come from `sessionlog.domain.model` and the exercise's two names from `exercise.domain.model`, as messaging already takes `AccountId` and `Goal` from other features' models. No feature imports messaging, so no cycle appears (`HexagonalArchitectureTest`). A repeat with another reference is not the same send, so it answers `message.id-in-use` as a repeat with another text does.

## R4. The rule and its order

**Decision**: `MessagingService.send` keeps its order and adds one step: the pair (`404 conversation.not-found`), the text (`400`), then the reference: `AboutSets` answers the set only when its session log's client is the sender, otherwise `AboutSetNotFoundException`, code `message.about-not-found`, kind `NOT_FOUND`. Only then is the conversation found or created and the message stored.

**Why**: One query decides "exists and is the sender's", so another client's set, an unknown id and a trainer's attempt cannot be told apart (FR-002): a trainer is never the client of a session log. The check comes before `findOrCreate`, so a refused message leaves no conversation behind, as the text check already does.

**Rejected**: A role check for the trainer (the ownership query already refuses them, with the same answer); `403` (the API answers `404` for another person's resource).

## R5. The API shape

**Decision**: `SendMessageRequest` gains optional `aboutSetLogId`. `MessageResponse` gains optional `about`: `setLogId`, `sessionLogId`, `sessionName`, `sessionDate`, `exerciseName`, `entryPosition`, `setNumber`, `rpe`, left out when the message has no reference. The exercise's name follows the request's language, as in the session log's answers (`LocalizedText.in(locale)`).

**Why**: One generated type serves the sent chip and the chip in the composer: the logger builds the same object from the session it shows, with the `setLogId` that goes into the request. `sessionLogId`, `entryPosition` and `setNumber` are what both links need (R8).

**Rejected**: A separate endpoint that describes a set for a message (a request per chip); the session's date formatted by the server (dates are formatted for the UI language in the browser).

## R6. Exercise names after a language switch

**Decision**: The messages query keeps its key. After the UI language changes, the open conversation shows the exercise name in the new language at its next refresh, within 5 seconds.

**Why**: The conversation already refreshes every 5 seconds (spec 58, R7). A language in the key would refetch every loaded page and move the reader.

## R7. Where the conversation opens

**Decision**: A right `Sheet` over the logger, full width at 375 px, showing the client's conversation exactly as the Messages tab does. A new route component in `app/`, `LoggerRoute`, renders `LoggerPage` with an `onAsk` callback and the sheet; the sheet's content is the one the waiting state's `MessageTrainerSheet` already uses, shared between the two.

**Why**: FR-017: the logger stays mounted under the sheet, so entry panels, the rest timer and pending logs are untouched, and closing the sheet is the way back. A feature may not import another feature (`frontend/CLAUDE.md`), and `app/` already composes them this way (`today-route.tsx`, `summary-route.tsx`, `message-trainer-sheet.tsx`). `Sheet` is installed; no component is added.

**Rejected**: Navigating to `/app/messages` with the set in the router state (the logger unmounts: the open panel is lost, and the way back is the browser's back button); a second copy of the conversation screen inside the logger.

## R8. Addresses of a set

**Decision**:

- Client: `/app/session/{sessionLogId}?set={entryPosition}-{setNumber}`, the address of spec 101 with one search parameter.
- Trainer: `/trainer/clients/{clientId}/logs?session={sessionLogId}&set={entryPosition}-{setNumber}`. The Logs tab's chosen session moves from component state to the `session` parameter; choosing another session sets it and drops `set`.

The row named by `set` gets `aria-current="true"`, a ring around it, is scrolled into view once and takes focus (`tabIndex={-1}`). An unknown or malformed `set` or `session` is ignored.

**Why**: URL state is the next step after local state (`frontend/CLAUDE.md`), and FR-020 asks for an address that survives a reload. The place of a set (entry position and set number) is what both screens key their rows by, so no lookup by set log id is needed. A ring is a line, not only a colour, and `aria-current` says it to assistive technology (FR-021). `useSearchParams` is already used by the Plan tab; React Router's documentation confirms the API is unchanged.

**Rejected**: A hash (`#set-2-3`): the logger's rows render after the session loads, so the browser's own scroll to a hash misses them, and the trainer's tab needs the session as well.

## R9. The action on a row

**Decision**: An icon button at the end of a logged row: `Button` `variant="ghost"` `size-12` with `MessageCircleIcon`, named "Ask your trainer about set {number} of {exercise}", worded as the row's other controls. It shows when the server has the set (`set.log` exists) and no newer change waits on the phone (no pending log, or a pending log that is Synced). The locked row gets the same button after its "Logged" mark. `SetRow` gets an optional `onAsk`; without it the button is not rendered.

**Why**: A row at 375 px holds the set number, three values and the sync badge; a text button does not fit beside them in Polish. The icon is the board's message icon, the one on the Messages tab, and 48 px is the client touch target (`frontend/CLAUDE.md`). The server can only check a set it has stored (FR-011).

**Risk**: With the longest Polish badge the values may wrap to a second line at 320 px. Checked in the browser during #174; the row may wrap, the page may not scroll sideways.

**Rejected**: The action inside the entry panel (a locked row has no panel, and task #174 needs locked sessions); a text link under each logged row (five exercises of three sets grow by fifteen lines).

## R10. The chip

**Decision**: One component, `AboutChip`, in `features/messaging/components`, with the board's look in Kinetiq tokens: a bordered card (`bg-card`, `border`, `rounded-lg`, 80% of the width at most), the label "About" in the small uppercase mono style, and one line "{session} · {date} · {exercise}, set {n} · RPE {rpe}" that wraps. Two uses:

- In the composer, above the field: plain, with a labelled remove button.
- On a message, directly above the bubble on the same side: a `Link` to the address the page gives it (`aboutHref`), with the accessible name of the spec's Texts.

`MessageList` gets `aboutHref` from its page: the client's page builds the logger's address, the trainer's page the Logs tab's (R8). Features link to each other's addresses and import nothing from each other, as the Plan card does.

**Why**: The design system describes one chip (6.7); the composer shows what will be sent with the same component. The session's date is a plain date, formatted in the browser for the UI language, read in UTC as the Logs tab does.

## R11. The outbox

**Decision**: An outbox entry gains an optional `about`; `submit(conversation, text, about)` keeps it, Retry sends the same entry, and the send hook puts `about.setLogId` into the request as `aboutSetLogId`. The composer's chip is state of the conversation that was opened with it and is cleared when a message is handed to the outbox or the chip is removed.

**Why**: FR-015 and US1 scenario 7: the reference travels with the text through Sending, Not sent and Retry, and a line in the list can show its chip before the server answers (FR-022).

## R12. The refusal on the screen

**Decision**: `errors.message.about-not-found` exists in English and Polish. A message refused with it shows "Not sent · Retry" as any failed message.

**Why**: The screens cannot produce the refusal: the action is offered only on the client's own stored sets (spec, Assumptions). A special state for it would be code for a case that only a forged request reaches.

## R13. Demo data

**Decision**: The demo message with a reference is appended to `R__demo_session_logs.sql`: a conversation between Demo Client Daily and their trainer, and one message from the client about a logged set of their latest demo session.

**Why**: Repeatable migrations run in the order of their names, so `R__demo_messages.sql` runs before the set logs exist. Demo Client Daily is the client with session logs.

## R14. What changes in earlier tests

**Decision**: `locked.test.tsx` (spec 101: "no control of a locked row takes focus") renders the logger without a way to ask and stays as it was; the new `ask.test.tsx` proves that with one, a locked session lets focus reach only the back link and the Ask buttons of logged rows. `client-logs-tab.test.tsx` gains the address cases with #175.

**Why**: The spec departs from spec 101, FR-009 and SC-004, and names it.
