# Data Model: Trainer and client change their settings

The account itself is spec 2's and spec 18's (`specs/2-trainer-sign-up`, `specs/18-sign-in-out`). This file adds its settings and the rules for weights.

## Account settings (domain, task #43)

`AccountSettings` record on `Account`:

| Field | Type | Values | Starts as |
|---|---|---|---|
| `units` | `WeightUnit` | `KG`, `LB` | `KG` for a new trainer and for accounts that exist before this story; a client's is set when joining (story #30) |
| `language` | `Language` | `EN`, `PL` | the UI language at sign-up for a new trainer; `EN` for accounts that exist before this story |
| `theme` | `Theme` | `LIGHT`, `DARK`, `SYSTEM` | `SYSTEM` |

- `Account.changeSettings(AccountSettings)` replaces the settings; the service builds the new record from the stored one and the fields the request sent (research R3).
- `Language.from(Locale)`: Polish gives `PL`, anything else `EN`.
- Task #44: `Account.rename(String)` with the sign-up name rules (trimmed, 1–100 characters, no control characters), refused for a client (research R6).

## Table `accounts` (migration `V6__add_account_settings.sql`, task #43)

| Column | Type | Constraint |
|---|---|---|
| `units` | `varchar(2) not null default 'KG'` | `ck_accounts_units` in (`KG`, `LB`) |
| `language` | `varchar(2) not null default 'EN'` | `ck_accounts_language` in (`EN`, `PL`) |
| `theme` | `varchar(6) not null default 'SYSTEM'` | `ck_accounts_theme` in (`LIGHT`, `DARK`, `SYSTEM`) |

The defaults give existing accounts, the local demo accounts included, their starting settings; the demo data needs no change.

## Settings on the device (frontend, task #43)

| Key in `localStorage` | Holds | Written |
|---|---|---|
| `kinetiq-theme` (exists) | the last theme used on the device | on every theme change |
| `kinetiq-language` (new) | the last language used on the device | on every language change |

Before sign-in, the app starts with these, falling back to the browser's language and theme system (spec FR-006).

## Weight (task #46)

- Stored in kg, always.
- 1 lb = 0.45359237 kg.
- Shown with at most one decimal in either unit, in the number format of the UI language.
- Entered in the person's unit with a dot or a comma; an lb value is converted to kg and kept to 0.1 kg.
- Every kg value from 30.0 to 250.0 in 0.1 steps, shown in lb and converted back, gives the same kg value (spec SC-003).
- Body weight bounds stay 30–250 kg (spec 50, FR-003), checked after conversion.
