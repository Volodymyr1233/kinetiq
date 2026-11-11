# Contract: conversation event stream

## `GET /api/v1/conversations/events`

Opens the signed-in person's stream of conversation events.

- **Auth**: signed-in client or trainer (session cookie). Signed out: `401` problem detail with `code` `request.unauthenticated`, as every other endpoint. Not public.
- **CSRF**: none needed (GET, changes nothing).
- **Response**: `200`, `Content-Type: text/event-stream`, held open up to 30 minutes, then ended normally; `EventSource` reconnects by itself.
- **OpenAPI**: the path appears in the generated document and `schema.d.ts`. The event payloads below are plain text, so they need no schema; the frontend types the URL against `paths`.

## Events

Each event is an SSE event with a name and the conversation's client id as its data:

```
event: message
data: 3f6c2a4e-8d1b-4c9e-a2f7-0b5e9d1c7a33

event: read
data: 3f6c2a4e-8d1b-4c9e-a2f7-0b5e9d1c7a33

event: ping
data: ping
```

| Name | Sent to | Means | The app then |
|---|---|---|---|
| `message` | every stream of the client and of the trainer of that conversation | a message was stored in it (also one sent by the same person from another tab or device, and a plan-change decline) | refreshes the list and the unread count; if that conversation is loaded, fetches its newest page and merges by id |
| `read` | every stream of the person who marked it read, never the other person | that person's last-read time moved | refreshes the list and the unread count |
| `ping` | every stream, at once on open and every 25 s | nothing changed; the stream is alive | notes that the stream is alive; after 60 s without any event it replaces the stream (a proxy or a sleeping network can leave a dead connection looking open) |

`data` is the client id as a UUID string. For a client it is their own id. The event carries no message text, no names and no last-read time; what the person sees is fetched through the endpoints of spec 58, with their access rules.

## Unchanged endpoints

`GET /api/v1/conversations`, `GET /api/v1/conversations/unread-count`, `GET|POST /api/v1/conversations/{clientId}/messages` and `PUT /api/v1/conversations/{clientId}/read` keep their contract (spec 58, spec 172). `POST .../messages` and `PUT .../read` now also publish the events above after their transaction commits.
