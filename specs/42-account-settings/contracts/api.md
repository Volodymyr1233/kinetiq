# Contract: account settings

Both operations need a signed-in person (spec 18) and address only that person's account; there is no id in the address. Errors are problem details with a `code` (constitution II). State-changing requests carry the anti-forgery token (spec 18, FR-013).

## `GET /api/v1/accounts/me`

Spec 18's operation. Task #43 adds the settings; task #44 adds the email.

```json
{
  "id": "00000000-0000-4000-8000-000000000001",
  "name": "Demo Trainer",
  "email": "demo.trainer@example.com",
  "role": "TRAINER",
  "timeZone": "Europe/Warsaw",
  "units": "KG",
  "language": "EN",
  "theme": "SYSTEM"
}
```

| Field | Values | Task |
|---|---|---|
| `units` | `KG`, `LB` | #43 |
| `language` | `EN`, `PL` | #43 |
| `theme` | `LIGHT`, `DARK`, `SYSTEM` | #43 |
| `email` | the account's email | #44 |

## `PATCH /api/v1/accounts/me`

Changes the fields sent; fields left out stay as stored (research R3).

```json
{ "units": "LB", "theme": "DARK" }
```

| Field | Rule | Task |
|---|---|---|
| `units` | `KG` or `LB` | #43 |
| `language` | `EN` or `PL` | #43 |
| `theme` | `LIGHT`, `DARK` or `SYSTEM` | #43 |
| `name` | trainers only; not blank, at most 100 characters, no control characters (the rules of `SignUpRequest`) | #44 |

| Answer | When |
|---|---|
| `200` with the body of `GET /api/v1/accounts/me` | changed, or nothing sent |
| `400` `validation.failed`, `errors: [{ "field": "units", "code": "pattern" }]` | a value outside its rule (codes `pattern`, and `size` for a name over 100 characters) |
| `401` `request.unauthenticated` | no sign-in |
| `403` `request.forbidden` | missing or wrong anti-forgery token (spec 18) |
| `403` `account.name-not-changeable` | a client sends `name` (task #44) |

## Frontend

| What | Where | Task |
|---|---|---|
| Applies `language` and `theme` from the cached current account | `app/` (research R5) | #43 |
| Remembers the language on the device | `localStorage` `kinetiq-language` | #43 |
| `useUpdateAccount()` mutation over `PATCH`, putting the answer into the cached current account | `features/account/api/account.ts` | #44 or #45, whichever comes first |
| The person's weight unit for any feature | context in `lib/`, provided by `app/` | #46 |
