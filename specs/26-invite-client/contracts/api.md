# API contract: Trainer invites a client

All under `/api/v1`, JSON. Every endpoint requires a signed-in trainer: anonymous → `401 request.unauthenticated`, client → `403 request.not-allowed`. Errors are problem details with `code` ([research R5](../research.md#r5-status-codes-and-error-codes)). State-changing requests carry `X-XSRF-TOKEN`. `Accept-Language` decides the email's language.

`InviteResponse`:

```json
{
  "id": "6f1d3c1e-2a4b-4c55-9a77-0e8f5b9d2c10",
  "email": "julia@example.com",
  "sentAt": "2026-10-25T09:14:03Z",
  "expiresAt": "2026-11-01T09:14:03Z"
}
```

## POST /invites

Request:

```json
{ "email": " Julia@Example.com " }
```

- `email`: `@NotBlank @Size(max = Email.MAX_LENGTH) @Pattern(regexp = SignUpRequest.EMAIL)`, as on sign-up; `Email` trims and lower-cases it.

| Status | Body |
|---|---|
| 201 | `CreatedInviteResponse`: `{ "invite": InviteResponse, "link": "http://localhost:5173/join#token=…" }`; the invite email has been handed to the mail capability; an earlier open invite of this trainer for the email is revoked |
| 400 | `validation.failed`, `errors: [{field: "email", code: "not-blank" \| "size" \| "pattern"}]` |
| 409 | `invite.account-exists` |
| 429 | `invite.limit-reached` |

## GET /invites

| Status | Body |
|---|---|
| 200 | `{ "invites": [InviteResponse, …] }`: this trainer's open invites, newest `sentAt` first; empty array when none |

## POST /invites/{id}/links

No body. Replaces the invite's link secret.

| Status | Body |
|---|---|
| 201 | `{ "link": "http://localhost:5173/join#token=…" }`; the previous copied link stops working; email link and `expiresAt` unchanged |
| 404 | `invite.not-found`: unknown id, another trainer's, used, revoked or expired; identical in every case |

## DELETE /invites/{id}

| Status | Body |
|---|---|
| 204 | none; the invite is revoked and both links stop working |
| 404 | `invite.not-found`, as above |

## Frontend error copy

| Code | EN | PL |
|---|---|---|
| `invite.account-exists` | This email already has an account. | Ten e-mail ma już konto. |
| `invite.limit-reached` | You've sent many invites. Try again later. | Wysłano dużo zaproszeń. Spróbuj później. |
| `invite.not-found` | This invite is no longer open. | To zaproszenie nie jest już otwarte. |
