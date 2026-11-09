# Data Model: Client logs without a connection

No database change. The only new data lives in the browser.

## Pending log

A `log-set` mutation as TanStack Query dehydrates it.

| Field | Meaning |
|---|---|
| `mutationKey` | `['log-set', sessionId, entryPosition, setNumber]` |
| `variables.setLogId` | the client-generated id; stays the same for every send of this set |
| `variables.sessionId` | the session the set belongs to |
| `variables.body` | the `LogSetRequest`: place, load, amount, RPE |
| `state.status` | `pending` (paused or retrying) or `error` |
| `state.error` | the problem detail `code` and `status` of a refusal, or none for a network failure |
| `state.submittedAt` | when the client made it |

Rules:
- A place can hold several pending logs (one per change made offline), all with the same `setLogId`. They are sent one after another in the order made, so the server ends with the last values; a failed one is removed before a new change for that place is made.
- Removed on success, on Discard, on sign-out, and when the account differs.
- Holds no token, name or email.

## Log state (derived, never stored)

| State | When |
|---|---|
| Saved on this phone | a pending log exists and has not failed for good |
| Synced | the server holds the set and no pending log exists |
| Not synced | a pending log failed: refusal, or 3 failed tries; reason from the code (locked, invalid values, cannot reach the server) |

## Transitions

Saved on this phone → Synced (server stored it) · Saved on this phone → Not synced (refusal or 3 failures) · Not synced → Saved on this phone (Retry) · Not synced → Synced / not logged (Retry succeeds / Discard).
