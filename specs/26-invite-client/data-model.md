# Data model: Trainer invites a client

## Invite (aggregate, `account.domain.model`)

| Field | Type | Rule |
|---|---|---|
| `id` | `InviteId` (UUID) | created in the application |
| `trainerId` | `AccountId` | the inviting trainer; never changes |
| `email` | `Email` | trimmed, lower case, at most 254 characters (existing value object) |
| `status` | `InviteStatus` | `OPEN`, `USED`, `REVOKED` |
| `emailTokenHash` | `String` | SHA-256 hex of the email secret; never changes |
| `linkTokenHash` | `String` | SHA-256 hex of the current link secret (the spec's "trainer secret"); replaced by "Copy new link" |
| `sentAt` | `Instant` | when it was created, from the domain clock |
| `expiresAt` | `Instant` | `sentAt + 7 days` |

Expired is not stored: an invite is **open** when `status = OPEN` and `now < expiresAt`.

### Operations

| Method | Allowed when | Effect |
|---|---|---|
| `send(trainerId, email, now)` (static) | always | new `OPEN` invite; returns it with both secrets (`SentInvite`) |
| `replaceLink(now)` | open | new link secret, returned; email secret and expiry unchanged |
| `revoke(now)` | open | `REVOKED` |
| `use(now)` | open | `USED` (called by story #30) |
| `supersede()` | `status = OPEN`, expired or not | `REVOKED`; used when the same trainer invites the same email again |
| `isOpen(now)` | always | the rule above |

`replaceLink`, `revoke` and `use` throw `InviteNotFoundException` (`invite.not-found`, 404) unless open, so a used, revoked or expired invite answers like an unknown one.

### State transitions

```text
          send
           │
           ▼
        ┌──────┐  revoke / supersede  ┌─────────┐
        │ OPEN │ ───────────────────▶ │ REVOKED │
        └──────┘                      └─────────┘
           │ use
           ▼
        ┌──────┐
        │ USED │
        └──────┘
OPEN and now ≥ expiresAt: expired (derived); only supersede still applies.
```

## SecretToken (value object, `account.domain.model`)

Renamed from `ConfirmationToken`; unchanged otherwise. `generate()`, `value()`, `hash()` (SHA-256 hex), `toString()` hides the value. Used by confirmation links and both invite secrets.

## Table `invites` (`V6__create_invites.sql`)

| Column | Type | Constraint |
|---|---|---|
| `id` | `uuid` | `pk_invites` |
| `trainer_id` | `uuid not null` | `fk_invites_trainer` → `accounts (id)` |
| `email` | `varchar(254) not null` | `ck_invites_email_lower_case` (`email = lower(email)`) |
| `status` | `varchar(16) not null` | `ck_invites_status` (`OPEN`, `USED`, `REVOKED`) |
| `email_token_hash` | `char(64) not null` | `uq_invites_email_token_hash` |
| `link_token_hash` | `char(64) not null` | `uq_invites_link_token_hash` |
| `sent_at` | `timestamptz not null` | |
| `expires_at` | `timestamptz not null` | `ck_invites_expiry` (`expires_at > sent_at`) |
| `created_at`, `updated_at`, `version` | from `BaseEntity` | |

Index: `uq_invites_open_trainer_email` unique on `(trainer_id, email) where status = 'OPEN'`; it also serves the list query.

## Repository port `InviteRepository` (`account.domain.output`)

| Method | Returns |
|---|---|
| `save(Invite)` | — |
| `findById(InviteId)` | `Optional<Invite>` |
| `findOpenByTrainer(AccountId, Instant now)` | `List<Invite>`, `status = OPEN` and `expires_at > now`, newest `sent_at` first |
| `findOpenByTrainerAndEmail(AccountId, Email)` | `Optional<Invite>`, `status = OPEN`, expiry ignored |
| `findByTokenHash(String)` | `Optional<Invite>` matching either hash |

`AccountRepository` gains `existsByEmail(Email)`.

## Demo data (`db/dev-data/R__demo_invites.sql`, local only)

For the demo trainer: one invite sent today, one sent 6 days 2 hours ago (on its last day), one expired, one revoked. One open invite of the demo unconfirmed trainer, to try another trainer's invite. Hashes are `encode(sha256(convert_to('<demo secret>', 'UTF8')), 'hex')`; inserts use fixed ids and `on conflict do nothing`.
