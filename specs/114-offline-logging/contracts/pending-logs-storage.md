# Contract: pending logs in browser storage

No HTTP contract changes: replays use `PUT /api/v1/session-logs/{id}/set-logs/{setLogId}` as it is, typed by the generated `schema.d.ts`. No `gen:api` run is needed.

## Storage

- Key: `kinetiq.pending-logs.<accountId>`, in `localStorage`.
- Value: the persisted client's JSON holding only `log-set` mutations (no queries).
- Written when the queue changes; read once when the client area mounts.
- Removed on sign-out; keys of other accounts are removed after a sign-in.
- A value that cannot be read, or has an unknown shape, is discarded and never sent.

## Replay request

Same body as a first send, with the stored `setLogId`. The server's answers decide the state:

| Answer | Row state |
|---|---|
| 200 | Synced; kept copy removed |
| 4xx problem detail (`session-log.locked`, a field code, `session-log.not-found`) | Not synced with that reason; no automatic retry |
| 401 | stays Saved on this phone; sign-in, then replay as the same account |
| network failure, 5xx | 3 tries with growing waits, then Not synced "cannot reach the server" |
