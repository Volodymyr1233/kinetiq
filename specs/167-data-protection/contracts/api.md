# Contract: account deletion and the privacy notice, story #167

The OpenAPI document generated from code is the source; this file says what it must contain. Paths are under `/api/v1`, both endpoints need a sign-in (an ended sign-in gets the standard `401`), state-changing requests carry the CSRF header, and errors are problem details with a stable `code`.

## `GET /accounts/me/deletion` (#170)

What the signed-in person needs to know before deleting.

**200**

```json
{ "clients": 3 }
```

`clients` is the number of client accounts linked to the signed-in trainer, and `0` for a client. A client also gets `trainerName`, their trainer's name, which the confirmation names; a trainer does not.

## `POST /accounts/me/deletion` (#170)

Deletes the signed-in account and everything of it (spec FR-008, FR-009). The request names no account.

```json
{ "password": "the account's current password" }
```

`password` is required and not blank (`400 validation.failed`, field `password`, code `not-blank`).

**204** The account is gone, every sign-in of it has ended, and the response clears this browser's session cookie as sign-out does.

| Status | `code` | When | Erased |
|---|---|---|---|
| 400 | `account.password-incorrect` | the password does not match; counts as a failed sign-in of that email | nothing |
| 429 | `account.sign-in-locked` | the email is locked after failed sign-ins (spec 18); carries `retryAfterSeconds` as sign-in does | nothing |
| 409 | `account.has-clients` | the signed-in trainer has at least one client; carries `clients` | nothing |
| 500 | `internal.error` | anything else | nothing |

Order of the checks: the lock, the password, then the clients.

## `POST /invite-acceptances` (changed behaviour, #170)

A join whose trainer was deleted at the same moment answers `invite.link-invalid` with the same status as any link that no longer works (spec 30), not `500`.

## Frontend addresses

| Address | Who | What |
|---|---|---|
| `/privacy` | anyone, signed in or not | the privacy notice (#169) |
| `/sign-in` with router state `{ accountDeleted: true }` | after a deletion | the sign-in page with "Your account was deleted." (#171) |

## Error texts (both languages)

`errors.account.password-incorrect`, `errors.account.has-clients` (plural, with `{{count}}`). `account.sign-in-locked` reuses the sign-in page's locked message with the minutes left.
