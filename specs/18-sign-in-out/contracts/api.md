# API contract: Trainer and client sign in and out

All under `/api/v1`, JSON. Errors are problem details with `code` (R7). Every state-changing request carries `X-XSRF-TOKEN`.

## POST /sign-ins — public

Request:

```json
{ "email": "marek@example.com", "password": "Kinetiq-2026" }
```

- `email`: `@NotBlank`, otherwise any string (malformed → `401`).
- `password`: `@NotBlank`, never trimmed (over 128 characters → `401`).

| Status | Body |
|---|---|
| 200 | `CurrentAccountResponse`; `Set-Cookie: SESSION=<new id>`; `XSRF-TOKEN` cleared |
| 400 | `validation.failed`, `errors: [{field: "email" \| "password", code: "not-blank"}]` |
| 401 | `account.credentials-invalid`, identical for unknown email and wrong password |
| 403 | `account.email-not-confirmed` (trainer, right password) |
| 429 | `account.sign-in-locked`, `retryAfterSeconds: <1..900>` |

## DELETE /sign-ins/current — signed in

| Status | Body |
|---|---|
| 204 | none; the `SESSION` row is deleted, `XSRF-TOKEN` cleared |
| 401 | `request.unauthenticated` |

## GET /accounts/me — signed in

`CurrentAccountResponse`:

```json
{ "id": "00000000-0000-4000-8000-000000000001", "name": "Demo Trainer", "role": "TRAINER", "timeZone": "Europe/Warsaw" }
```

`role` is `TRAINER` or `CLIENT`. `401 request.unauthenticated` when not signed in or when the account no longer exists.

## PUT /accounts/me/time-zone — signed in

```json
{ "timeZone": "Europe/London" }
```

| Status | Body |
|---|---|
| 204 | none; stored if it differs |
| 400 | `validation.failed` (blank or not a region zone) |
| 401 | `request.unauthenticated` |

## Changed

| Endpoint | Change |
|---|---|
| `POST /accounts`, `POST /confirmation-links` | More than 10 per address in an hour → `202` as usual, nothing created or sent |
| `GET /exercises` | Trainers only; a client gets `403 request.not-allowed` |
| Any refused CSRF check | Still `403 request.forbidden`; a role refusal is now `request.not-allowed` |

No endpoint in this story takes another person's id, so no `404` ownership case exists here.
