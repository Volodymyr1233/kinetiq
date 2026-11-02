# Data Model: Client and trainer exchange messages

Three tables, created by `V14__create_messaging.sql`. Terms are those of `CONTEXT.md`: Message, Conversation, Unread, Delivery state.

## Tables

### `conversations`

One per client and the trainer they train under, created by the first message (research R2).

| Column | Type | Rule |
|---|---|---|
| `id` | uuid, primary key | made in the application |
| `client_id` | uuid, not null | foreign key to `accounts`; the account has role CLIENT |
| `trainer_id` | uuid, not null | foreign key to `accounts`; the trainer the client had when it was created |
| `created_at`, `updated_at`, `version` | as `BaseEntity` | |

Unique on `(client_id, trainer_id)`. A client who moves to another trainer gets a new conversation, so the new trainer never sees the earlier history and the earlier trainer's access ends with the link (spec, User Story 4, scenario 6).

### `messages`

| Column | Type | Rule |
|---|---|---|
| `id` | uuid, primary key | chosen by the sender (research R3) |
| `conversation_id` | uuid, not null | foreign key to `conversations` |
| `sender_id` | uuid, not null | foreign key to `accounts`; the conversation's client or trainer |
| `text` | varchar(2000), not null | check: `char_length(btrim(text)) > 0`; the length check is the backstop of the 2000-unit rule |
| `received_at` | timestamptz, not null | server time, cut to microseconds (research R4) |
| `created_at`, `updated_at`, `version` | as `BaseEntity` | |

Index on `(conversation_id, received_at desc, id desc)`: serves paging and the unread count. A message is never updated or deleted.

### `conversation_reads`

The last-read time of one person in one conversation (FR-004).

| Column | Type | Rule |
|---|---|---|
| `conversation_id` | uuid, not null | foreign key to `conversations` |
| `account_id` | uuid, not null | foreign key to `accounts`; one of the two people |
| `last_read_at` | timestamptz, not null | only ever moves forward |

Primary key `(conversation_id, account_id)`. No row means the person has never opened the conversation. It has no entity: one native upsert writes it (research R5).

## Domain model

| Type | Kind | Holds / rule |
|---|---|---|
| `ConversationId`, `MessageId` | ids | wrap a UUID; `MessageId` is parsed from the sender's value, `ConversationId.random()` |
| `MessageText` | value object | the text after removing spaces and line breaks around it (Unicode spaces too, as the quiz notes); 1 to 2000 UTF-16 units; empty gives `MessageEmptyException`, too long gives `MessageTooLongException`; `MAX_LENGTH` is the one place the limit is written |
| `ConversationPair` | value object | `client`, `trainer`; `includes(person)` |
| `Conversation` | entity | `id`, `pair` |
| `Message` | entity | `id`, `conversation`, `sender`, `text`, `receivedAt`; never changes |
| `MessagePage` | value object | up to 50 messages oldest to newest, and `hasMore` |
| `LastMessage` | value object | `preview` (up to 140 characters), `sentByViewer`, `receivedAt` |
| `PlanWeek` | value object | `current`, `total`; `current` is at least 1, and above `total` once the plan has finished |
| `ConversationOverview` | read model | `clientId`, `name` of the other person, `goal` (optional, a trainer's rows only, the plan's when a plan exists, else the quiz's), `planWeek` (optional, a trainer's rows only, for a published plan), `unread`, `lastMessage` (optional) |

## Rules

- **Unread** for a person in a conversation: the number of its messages whose sender is not that person and whose `received_at` is after their `last_read_at` (all of them when they have no row). Total unread: the sum over all their conversations (clarified: unread messages, not conversations).
- **Marking read** sets `last_read_at` to `greatest(existing, now)`; the other person's row is untouched.
- **Access**: a person can use the conversation of client *C* only when they are *C* or *C*'s current trainer. Anything else, including a missing *C*, is "not found".
- **Sending**: the sender is the signed-in person; the conversation is found or created for the pair; a message with an id that exists is the same send repeated when the conversation, sender and text are all equal, and a conflict otherwise.

## Delivery state (not stored)

| State | Where it exists | Meaning |
|---|---|---|
| Sending | the sender's page | the request is open |
| Sent | the server | the message is stored; every message the server returns is Sent |
| Not sent | the sender's page | the request failed; the text is kept for Retry |

There is no read state; nothing about the other person's reading is returned to the sender.

## Demo data

`R__demo_messages.sql` (local profile only, idempotent): between the demo trainer and the two demo clients, a conversation with a few messages in each direction over two days; one client has unread messages from the trainer and the trainer has unread messages from one client; the other demo trainer has none. One conversation (Demo Client Waiting) holds at least 60 short messages, so a second page of 50 and "Load earlier messages" can be checked in the browser. Fixed message ids, `on conflict do nothing`.
