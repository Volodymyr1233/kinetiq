# API contract: Trainer signs up

All endpoints are public (see spec, "Public endpoints"). Every `POST` carries `X-XSRF-TOKEN` and `Accept-Language: en|pl`. The OpenAPI document generated from the controllers is authoritative; this file is the design it must match. API paths name the resource a request creates; page addresses (`/sign-up`, `/confirm-email`) are written for people.

## `GET /api/v1/csrf`

Sets the `XSRF-TOKEN` cookie. `204`, no body.

## `POST /api/v1/accounts`

Signs up a trainer.

```json
{ "name": "Marek Nowak", "email": "marek@example.com", "password": "Kinetiq-2026", "adult": true, "timeZone": "Europe/Warsaw" }
```

| Field | Rules (field code on failure) |
|---|---|
| name | not blank (`not-blank`), at most 100 (`size`), no `\p{Cc}` characters (`pattern`) |
| email | not blank (`not-blank`), `^\s*[^\s@]+@[^\s@]+\.[^\s@]+\s*$` (`pattern`; surrounding spaces allowed, trimmed by the server), at most 254 after trimming (`size`) |
| password | not null (`not-null`), 8–128 (`size`), lower, upper, digit, other (`pattern`) |
| adult | must be `true` (`assert-true`) |
| timeZone | not blank (`not-blank`); a value outside `ZoneId.getAvailableZoneIds()` fails as `validation.failed` without field errors |

- `202`, no body: the account was created, or the email already had one. The two are indistinguishable.
- `400 validation.failed` with `errors`.

## `POST /api/v1/email-confirmations`

Confirms the email of the account the token belongs to.

```json
{ "token": "q3V…43 characters" }
```

- `204`: the email is confirmed.
- `400 account.confirmation-link-invalid`: the token is used, expired, replaced or unknown.
- `400 validation.failed`: the token is blank.

## `POST /api/v1/confirmation-links`

Sends a new confirmation link.

```json
{ "email": "marek@example.com" }
```

- `202`, no body, whatever the email. A link is sent only to an unconfirmed account that has not received an email in the last 60 seconds.
- `400 validation.failed`: the email is blank or malformed.

## Problem details

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "instance": "/api/v1/accounts",
  "code": "validation.failed",
  "errors": [{ "field": "password", "code": "pattern" }]
}
```

| Code | Status | When |
|---|---|---|
| `validation.failed` | 400 | Request body breaks a rule; `errors` lists the fields when known |
| `request.invalid` | 400 (other 4xx) | Body is not valid JSON, or another malformed request |
| `request.not-found` | 404 | No such endpoint |
| `request.method-not-allowed` | 405 | Wrong method |
| `request.unsupported-media-type` | 415 | Body is not JSON |
| `account.confirmation-link-invalid` | 400 | See above |
| `request.unauthenticated` | 401 | The endpoint needs a signed-in person |
| `request.forbidden` | 403 | Missing or wrong `X-XSRF-TOKEN`, or not allowed |
| `internal.error` | 500 | Anything unexpected; also carries `correlationId` |

`detail` is never set from an exception. The `401` and `403` produced by the security filters use the same format.

## Email sent (logging adapter)

No email leaves the system yet. With `kinetiq.mail.log-content: true` (set only in `application-local.yaml`) the adapter logs one line per email at `INFO`: recipient, subject in the request's language, and the link. Without it, the line names only the template.

| Template | Subject (en) | Link |
|---|---|---|
| `confirm-email` | Confirm your Kinetiq account | `<app-url>/confirm-email#token=<token>` |
| `account-exists` | You already have a Kinetiq account | `<app-url>/sign-in` |
