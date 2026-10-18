# Data model: Trainer signs up

## Account (aggregate, feature `account`)

| Field | Type | Rules |
|---|---|---|
| id | `AccountId` (UUID) | Created in the application |
| name | text | Trimmed, 1–100 characters, no control characters (Unicode category `Cc`) |
| email | `Email` | Trimmed, lower-case, `^[^\s@]+@[^\s@]+\.[^\s@]+$`, at most 254 characters; unique |
| passwordHash | text | Output of the password hasher; never the password |
| role | `Role` | `TRAINER` or `CLIENT`; sign-up creates `TRAINER` |
| timeZone | `ZoneId` | One of `ZoneId.getAvailableZoneIds()`, such as `Europe/Warsaw` or `UTC`; offsets like `+02:00` are refused |
| emailConfirmed | boolean | `false` at sign-up |
| confirmation | `EmailConfirmation`, optional | Present only while a link works |
| mailSentAt | `Instant`, optional | When the last email for this account was sent |

**EmailConfirmation** (value object): `tokenHash` (SHA-256 of the token, 64 hex characters) and `expiresAt` (sent time + 24 h).

**ConfirmationToken** (value object): the raw token, 43 base64url characters from 32 random bytes. Exists only in memory and in the email; `hash()` gives the stored form.

**RawPassword** (value object): 8–128 characters with at least one `a–z`, one `A–Z`, one `0–9` and one character that is none of those. Never logged; `toString()` hides it.

### Behaviour

| Method | Effect |
|---|---|
| `Account.signUpTrainer(name, email, passwordHash, timeZone)` | New unconfirmed trainer, no confirmation, no mail sent |
| `canReceiveMail(now)` | `mailSentAt` is empty or at least 60 s before `now` |
| `issueConfirmation(token, now)` | Requires unconfirmed. Replaces any earlier confirmation with `token.hash()`, expiring `now + 24 h`; sets `mailSentAt = now` |
| `recordMailSent(now)` | Sets `mailSentAt = now` (the "already have an account" email) |
| `confirmEmail(now)` | Requires a confirmation not yet expired at `now`. Sets `emailConfirmed`, removes the confirmation. Otherwise throws `ConfirmationLinkInvalidException` |

### States

```
signUpTrainer          confirmEmail (link valid)
──────────────► UNCONFIRMED ─────────────────────► CONFIRMED
                 │  ▲
                 └──┘ issueConfirmation (new link replaces the old one)
```

A confirmed account never returns to unconfirmed in this story.

## Table `accounts` (`V1__create_accounts.sql`, task #3)

| Column | Type | Constraint |
|---|---|---|
| id | `uuid` | `pk_accounts` |
| name | `varchar(100)` | not null |
| email | `varchar(254)` | not null, `uq_accounts_email`, `ck_accounts_email_lower_case` (`email = lower(email)`) |
| password_hash | `varchar(255)` | not null |
| role | `varchar(16)` | not null, `ck_accounts_role` (`TRAINER`, `CLIENT`) |
| time_zone | `varchar(64)` | not null |
| email_confirmed | `boolean` | not null |
| created_at | `timestamptz` | not null |
| updated_at | `timestamptz` | not null |
| version | `bigint` | not null |

## Columns added by `V2__add_email_confirmation.sql` (task #6)

| Column | Type | Constraint |
|---|---|---|
| confirmation_token_hash | `char(64)` | null, `uq_accounts_confirmation_token_hash` |
| confirmation_expires_at | `timestamptz` | null |
| mail_sent_at | `timestamptz` | null |

`ck_accounts_confirmation` keeps `confirmation_token_hash` and `confirmation_expires_at` both null or both set.

## Shared base entity (`common.infrastructure.output.BaseEntity`, task #3)

`@MappedSuperclass` with `createdAt` and `updatedAt` (`Instant`, set by Hibernate) and `@Version Long version`. The version is a wrapper, so `null` tells Spring Data that an entity with an application-assigned id is new (`persist`, not `merge`).

## Demo data (`db/dev-data/R__demo_accounts.sql`, local only)

| Email | Role | Confirmed |
|---|---|---|
| `demo.trainer@example.com` | Trainer | yes |
| `demo.unconfirmed@example.com` | Trainer | no |

Both use the demo password `Kinetiq-demo-1`, stored as a PBKDF2 hash.
