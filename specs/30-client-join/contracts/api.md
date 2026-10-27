# API contract: Client joins through an invite

All under `/api/v1`, JSON. Errors are problem details with `code`. State-changing requests carry `X-XSRF-TOKEN` (anonymous visitors get it from `GET /csrf`). `Accept-Language` decides the client's starting language and the confirmation email's language.

## POST /invite-checks

Public. Changes nothing.

Request: `{ "token": "…" }`, `token`: `@NotBlank`.

| Status | Body |
|---|---|
| 200 | `InviteCheckResponse`: `{ "email": "anna@example.com", "trainerName": "Marek Nowak" }` |
| 400 | `invite.link-invalid`: expired, used, revoked, replaced, unknown, or its email has an account; identical in every case |
| 400 | `validation.failed`, `errors: [{field: "token", code: "not-blank"}]` |

## POST /invite-acceptances

Public. Creates the client, links the trainer, uses the invite, signs in.

Request:

```json
{
  "token": "…",
  "name": "Anna Kowalska",
  "password": "Kinetiq-2026",
  "atLeastSixteen": true,
  "healthDataConsent": true,
  "timeZone": "Europe/Warsaw"
}
```

- `token`: `@NotBlank`
- `name`: `@NotBlank @Size(max = Account.NAME_MAX_LENGTH) @Pattern(regexp = "^\\P{Cc}*$")`, as on sign-up
- `password`: `@NotNull @Size(min = 8, max = 128) @Pattern(regexp = RawPassword.CHARACTER_RULES)`, as on sign-up
- `atLeastSixteen`, `healthDataConsent`: `@AssertTrue`
- `timeZone`: `@NotBlank`, parsed by `TimeZones.parse` as on sign-up

| Status | Body |
|---|---|
| 201 | `CurrentAccountResponse` of the new client; new session id and CSRF token cookies (R7) |
| 400 | `validation.failed`, `errors: [{field, code}]`: `name` (`not-blank` \| `size` \| `pattern`), `password` (`not-null` \| `size` \| `pattern`), `atLeastSixteen` / `healthDataConsent` (`assert-true`), `timeZone` (`not-blank`), `token` (`not-blank`) |
| 400 | `invite.link-invalid`, as above, also when the invite stopped working after the check or another request won the race (R6); nothing is created |

Email confirmation: confirmed when `token` is the emailed secret; otherwise unconfirmed and a `confirm-email` mail is sent (R1).

## GET /accounts/me (changed)

`CurrentAccountResponse` gains `"emailConfirmed": true|false` (required).

## Unchanged, used by the banner

`POST /confirmation-links` `{ "email": "…" }` → `202` whatever the email (spec #2).

## Frontend copy for the new code

| Code | EN | PL |
|---|---|---|
| `invite.link-invalid` | This link no longer works. Ask your trainer for a new one. | Ten link już nie działa. Poproś trenera o nowy. |
