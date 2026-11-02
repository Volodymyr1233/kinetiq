# Research: Client and trainer exchange messages

No `NEEDS CLARIFICATION` was left in the spec. These are the design decisions the plan rests on, each with what was rejected. The libraries are the ones already in the project; the documentation tool was not available while planning, so the first task that uses a library API it has not used before (`useInfiniteQuery`, `refetchInterval` in the background-pause behaviour) checks the current documentation before relying on it.

## R1 Addressing and the access rule

**Decision**: A conversation is addressed by the **client's id**: `/api/v1/conversations/{clientId}/...`. Access is one rule in one place: look up the client's *current* trainer; the signed-in person must be that client or that trainer; otherwise `404 conversation.not-found`. An unknown id, another trainer's client, a third person and an unlinked client all give the same answer.

**Rationale**: The pair is (client, current trainer), so the client id names it. It needs no conversation id to exist before the first message (R2), and the trainer's list is already keyed by client. A conversation id would leak nothing but would need a lookup before the access check.

**Alternatives**: `/conversations/{conversationId}` (needs a row before the first message, or a second route); `/clients/{id}/messages` (those routes are trainer-only in `SecurityConfig`, and the client would need their own route anyway).

## R2 When a conversation is stored

**Decision**: The row is created by the first **send**, with the pair at that moment. Reading and marking read never create it: a pair without a row reads as an empty conversation and marking it read does nothing. The trainer's list comes from the accounts, so it shows every linked client without needing rows.

**Rationale**: A conversation without messages carries no information, and creating rows on a read would make `GET` write. The race of two first sends is settled by the unique key on the pair: the insert is `on conflict do nothing`, and the loser reads the winner's row.

**Alternatives**: Create one per client when the client joins (touches the join flow of another story); create on first read (a read with a side effect).

## R3 Sending safely twice

**Decision**: The sender chooses the message's id (a UUID made in the browser) and sends it with the text. The id is the primary key, and the row is written with `on conflict (id) do nothing`: a failed insert would abort the use case's PostgreSQL transaction, so nothing could be read after it. A repeated send of the same id with the same conversation, sender and text returns the stored message with the same status as the first time. The same id with anything different gives `409 message.id-in-use`. A lost response and a Retry therefore never duplicate (SC-004).

**Rationale**: It is the smallest design that makes Retry safe without an idempotency table or header store, and the key constraint of the database settles the race of two simultaneous attempts. The `409` stops one person from using another person's message id to probe or collide.

**Alternatives**: An `Idempotency-Key` header with a separate table (a second store for the same guarantee); server-made ids (a retry cannot be told from a new message).

**Note**: `crypto.randomUUID()` needs a secure context. Production is HTTPS and development is `localhost`, so both qualify.

## R4 Order, time and paging

**Decision**: Messages are ordered by the time the server received them, then by id as a tie-break, and the time is cut to microseconds before storing (PostgreSQL's precision), so a time read back equals the time written. A page is the newest 50, returned oldest to newest, with `hasMore`. The next page is asked with `before={id of the oldest message the client has}`. An id that is not in this conversation gives `404 message.not-found`.

**Rationale**: Paging by a message id cannot skip or repeat a message when new ones arrive, which an offset would. Cutting the time keeps "after my last-read time" exact.

**Alternatives**: Offset paging (shifts when messages arrive); a timestamp cursor (the same instant can belong to two messages); a database sequence column (one more column to explain the order that the time already gives).

## R5 Last-read time and the unread rule

**Decision**: A table `conversation_reads (conversation_id, account_id, last_read_at)`, one row per person, written by one native upsert: `last_read_at = greatest(existing, now)`. Unread for a person = messages in the conversation whose sender is the other person and whose time is after that person's last-read time (all of them when there is no row). The rule is two counting queries (one conversation; the total over all) and nothing is stored as a counter.

**Rationale**: A single statement makes "never moves back" and "two people marking read at once" correct without a version check, which would turn a harmless race into a `500`. A counter would have to be kept in step with every send.

**Alternatives**: Two columns on the conversation updated through the entity (the optimistic lock fails when both people mark read together); stored unread counters (state to keep consistent).

## R6 The list is a read model

**Decision**: One JDBC query over `accounts`, `conversations`, `messages`, `quiz_submissions` and `plans` gives the rows of the list for either role: for a trainer, every client linked to them; for a client, their own row, with the trainer's name. Each row has the other person's name, the client's goal (the plan's when a plan exists, else the quiz's, null before the quiz), the current plan week of a published plan, the last message (text cut to 140 units, whether it is mine, when) and the unread count. Order: last message newest first, then clients without messages by name. No feature class is imported (the pattern of `JdbcClientDirectory`).

**Rationale**: It is a read across four tables with a lateral join; mapping it through entities of other features would break the hexagonal rules or load far more than needed. One round trip per refresh keeps the 5-second polling cheap.

**Alternatives**: Call the client and account use cases and combine in code (an N+1, and a dependency from `messaging` on both features).

## R7 Finding out quickly: refresh instead of push

**Decision**: While a messages screen is open the list and the open conversation are refreshed every 5 seconds and the navigation count every 10 seconds, with the query library's own interval. Its refresh stops in a hidden tab, which is what the clarified rule needs: a message that arrives in a hidden tab is neither fetched nor marked read until the tab is shown. When the tab becomes visible the data is refreshed at once, and the open conversation is marked read if it has unread messages.

**Rationale**: Meets "within 15 seconds" (SC-003) with no new moving part. A hidden tab costs the server nothing.

**Alternatives**: Server-sent events or a web socket (a second channel to secure, scale and test; can replace the refresh later without changing what the screens show).

**Ceiling**: Every refresh asks for all pages the person has loaded. For a conversation of hundreds of messages that is a few kilobytes every 5 seconds; revisit only if conversations grow to thousands loaded at once.

## R8 Message states on the sender's screen

**Decision**: A message the person sends is held in the page's memory (a small hook, `use-outbox`), not in the server cache: `Sending` while the request is open, removed on success (the confirmed message is put in the conversation's data at once), `Not sent` with its text on failure. Retry sends the same id and text. After a page reload only what the server stored is shown.

**Rationale**: It is local interface state that does not exist on the server, and the spec says a Not sent message lives only while the page stays open. A failed text is never dropped, because it stays in the list until Retry succeeds.

**Alternatives**: Persist the outbox in browser storage (health details on a shared device; the spec rules it out); use only the mutation's own state (loses the message when the component remounts).

## R9 Privacy of the text

**Decision**: Message text is passed as a value to the domain and to the database and nowhere else. It is not logged, not in an exception message and not in a URL. The check constraints are a backstop; the domain refuses an empty or long text first, so a database error that would carry the failing row (and with it the text) does not happen in normal use. No LLM is involved.

**Rationale**: Messages can hold injuries and sleep problems (spec, User Story 4). PostgreSQL's constraint errors quote the failing row, which the generic handler logs for a `500`.

## R10 Deliberate simplifications

- No limit on the size of the request body beyond the framework's own: the text rule runs in the domain, and only signed-in people can send. Add a body limit if abuse shows (spec: no rate limit).
- The search on the trainer's list runs in the browser over the list already loaded. Names are compared after removing diacritics, with the letter `ł` mapped by hand because it has no decomposition.
- The text length counts UTF-16 units, like the quiz notes and the browser's `maxLength`. The database check counts code points, which are never more than units, so the database never refuses what the domain accepts.
- A page holds 50 messages, a constant in the domain. It is not configurable.

## R11 Where the screens sit

**Decision**: The client's `messages` route replaces `ClientPlaceholder`. The message list fills the area between the header and the tab bar, and the composer sits above the tab bar, which stays visible. The conversation does not scroll the whole page: the message list scrolls and the composer keeps its place, also when the on-screen keyboard is open. The trainer's route is `/trainer/messages/:clientId?`; the list is a column beside the conversation. A narrow window shows one of the two.

**Rationale**: The client area already has the header, the settings button and the tab bar; keeping them makes Messages one tab among four and avoids a special full-screen mode. The address of the open conversation makes reload and sharing work (spec, User Story 2).

**Alternatives**: A full-screen message list without the tab bar, as the board draws it (needs the one-row inbox and its back arrow, which the spec drops).
