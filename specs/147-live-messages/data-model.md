# Data model: Messages arrive without reloading

Nothing is stored. No table, migration or demo data changes: conversations, messages and last-read times are those of spec 58.

## ConversationChange (domain value, `messaging.domain.model`)

One notice for one person that one of their conversations changed.

| Field | Type | Rule |
|---|---|---|
| `person` | `AccountId` | Who is told. Required. |
| `client` | `AccountId` | The conversation, named by its client as everywhere in messaging. Required. |
| `kind` | `MESSAGE` \| `READ` | Required. |

Who gets one, decided in `MessagingService` (FR-002, FR-003):

| Use case outcome | Changes published |
|---|---|
| `send` stores a new message | `MESSAGE` for the client, `MESSAGE` for the trainer |
| `send` with an id already stored (same send repeated) | none |
| `send` refused (empty, too long, unknown set, not your conversation) | none (nothing is stored) |
| `markRead` on a stored conversation | `READ` for the reader only |
| `markRead` with no stored conversation yet | none |

## Event stream (in memory only, `messaging.infrastructure.input.ConversationStreams`)

One open `SseEmitter` for one tab.

| Field | Meaning |
|---|---|
| `person` | Account id of the signed-in person who opened it (the authenticated principal). |
| `signIn` | Session id of the sign-in that opened it. |
| `emitter` | The open response. |

Lifecycle:

```
open (GET, authenticated) ──► registered ──► receives notices for `person`
                                   │
     closed when any of: tab closes / write fails │ 30-minute timeout │ session gone at a heartbeat
                         │ 11th stream of the same sign-in opened (oldest closed) │ server shutting down
                                   ▼
                              removed
```

Invariants: at most 10 streams per `signIn` (FR-001); a stream receives only notices whose `person` equals its own (FR-006, SC-005).
