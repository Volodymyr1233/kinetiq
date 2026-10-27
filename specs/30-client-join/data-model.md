# Data model: Client joins through an invite

## Account (changed)

Table `accounts`. New columns:

| Column | Type | Rule |
|---|---|---|
| `trainer_id` | `uuid` null | `fk_accounts_trainer` → `accounts(id)`; set exactly when `role = 'CLIENT'` (`ck_accounts_client_trainer`, `not valid`, R3); index `ix_accounts_trainer_id` |
| `health_data_consent_at` | `timestamptz` null | set exactly when `role = 'CLIENT'` (`ck_accounts_client_consent`, `not valid`, R3) |

Domain (`Account`):

- `Optional<AccountId> getTrainerId()`, `Optional<Instant> getHealthDataConsentAt()`.
- `static Account joinAsClient(String name, Email email, String passwordHash, ZoneId timeZone, AccountSettings settings, AccountId trainerId, Instant consentAt, boolean emailConfirmed)`: role `CLIENT`, name validated as at sign-up, trainer and consent required; `emailConfirmed` is true only for the emailed invite secret.
- `restore(…)` takes the two new values.

Starting settings: `new AccountSettings(trainer.getSettings().units(), Language.from(locale), Theme.SYSTEM)`.

Repository: `List<Account> findClientsOf(AccountId trainerId)`, ordered by name.

## Invite (changed)

- `boolean provesEmail(SecretToken token)`: whether `token` is the emailed secret.
- `use(Instant now)`: existing; open → used.

A link works when (R5): the invite is found by the token's hash, `isOpen(now)`, and no account has its email.

## State transitions

```
Invite:  OPEN --join--> USED
Account (client, emailed secret): created confirmed
Account (client, copied secret):  created unconfirmed --confirmation link--> confirmed
```

## Demo data (local only)

`R__demo_accounts.sql`: the demo client gets the demo trainer and a consent time, on insert and by an update when missing. Demo invites already exist (`R__demo_invites.sql`); their secrets are `demo-invite-<n>-email` and `demo-invite-<n>-link`.
