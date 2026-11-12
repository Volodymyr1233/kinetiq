# Research: Personal and health data are protected

## R1. How a deletion reaches every feature's tables

- **Decision**: The database does it. One migration, `V27__erase_with_account.sql`, recreates the foreign keys that hang data off an account with `on delete cascade` (list in [data-model.md](data-model.md)). The account feature deletes the `accounts` row and the database erases the rest in the same transaction.
- **Rationale**: Every feature already depends on `account` (`client`, `plan`, `quiz`, `sessionlog`, `readiness`, `planchange`, `messaging`, `progress`, `exercise`), so `account` cannot call them without a cycle (constitution I). Cascading keys need no Java in other features, cannot forget a table that has a key, and keep the erasure one statement inside one transaction (FR-008 "kept whole or not at all").
- **Alternatives considered**: An "account deleted" event with a listener per feature: about ten adapters, and a new table without a listener leaves data behind silently. An erase port per feature called by `account`: a cycle. A deletion orchestrator in `app`-like glue: the backend has none, and it would still need every feature's port.

## R2. A trainer with clients cannot be deleted, also in a race

- **Decision**: `fk_accounts_trainer` (a client's `trainer_id`) keeps `no action`. The service checks the client count first and refuses with `account.has-clients`; if a join commits between the check and the delete, the delete fails on that key and the adapter turns the violation into the same exception. On the join side, a trainer deleted mid-join makes the client insert fail on that key or the invite update find no row; `InviteAcceptanceController` already maps an optimistic-lock failure to `invite.link-invalid`, and the key violation is added to the violations it maps (FR-007).
- **Rationale**: The database enforces FR-007 whatever happens in Java; the pre-check only gives the normal path a clean answer.
- **Alternatives considered**: Locking the trainer row `for update` in both flows: more code, same outcome.

## R3. Confirming with the password

- **Decision**: `AccountDeletionService` reuses `PasswordHasher` and `FailedSignInsRepository` as `SignInService` does: a locked email throws `SignInLockedException` (its code and minutes left, spec 18); a wrong password records a failure and throws `PasswordIncorrectException` (`account.password-incorrect`, kind `INVALID`, 400); the right password clears the failures. A password longer than `RawPassword.MAX_LENGTH` is wrong without hashing.
- **Rationale**: One lock for one email, wherever the password is typed; a stolen sign-in cannot try passwords faster here than on the sign-in page.
- **Alternatives considered**: `401` for a wrong password: the frontend reads 401 as "signed out" (`useSignOut`, the guards). `403`: means "not your resource" elsewhere.

## R4. The endpoints

- **Decision**: `GET /api/v1/accounts/me/deletion` answers `{ "clients": n }` (0 for a client); `POST /api/v1/accounts/me/deletion` with `{ "password" }` deletes and answers `204`, ending this browser's sign-in with the existing `LogoutHandler` and every other one with `AccountSessions.endAll` ([contracts/api.md](contracts/api.md)).
- **Rationale**: Trainer Settings needs the count when it opens (FR-012) and the account feature already holds it (`AccountRepository.findClientsOf`); the frontend's `account` feature cannot import the `client` feature's list. `POST` because a `DELETE` with a body is dropped by some intermediaries and is awkward in OpenAPI.
- **Alternatives considered**: A `clients` field on `GET /accounts/me`: that response is cached for the whole app and would be stale on Settings. `DELETE /accounts/me` with the password in a header: a secret in a header ends up in logs more easily.

## R5. Sign-ins of a deleted account

- **Decision**: The service calls `AccountSessions.endAll` after deleting the row; the controller then runs `LogoutHandler` for the current request, as sign-out does. Spring Session's JDBC repository writes in its own transaction, so ending sign-ins last means a refused deletion signs nobody out; a failure at the final commit could sign the person out without deleting, which is accepted (they sign in again and retry).
- **Rationale**: Reuses what password reset already does (story #39).

## R6. Invites of the erased client

- **Decision**: Deleting a client also runs `InviteRepository.deleteByTrainerAndEmail(trainer, email)` in the same use case, before the account row goes (FR-008). A trainer's own invites go by cascade on `fk_invites_trainer`.
- **Rationale**: An invite refers to the client by email, not by key, so no cascade reaches it.

## R7. Retention clean-up

- **Decision**: `RetentionSweep` in `account.infrastructure.input`, `@Scheduled(fixedDelay = 10 min)`, calls `EraseExpiredDataUseCase.eraseExpired()`, which deletes:
  - trainer accounts with `email_confirmed = false` whose `confirmation_expires_at` is more than 6 days in the past (a link is valid 24 hours from sending, so this is 7 days after the last link was sent);
  - invites with state `USED` or `REVOKED` and `updated_at` more than 1 hour ago, and `OPEN` invites with `expires_at` more than 1 hour ago.
  Client accounts are never touched (FR-015). The sweep logs counts only.
- **Rationale**: `confirmation_expires_at` is set on every new link and is never null while the email is unconfirmed (`ck_accounts_confirmation`); `mail_sent_at` also moves for other mails. A 10-minute tick keeps "within 1 hour" true and costs two indexed deletes. An unconfirmed trainer cannot sign in, so it has no exercises, invites or clients that would block the delete.
- **Alternatives considered**: Deleting an invite at the moment it is used or revoked: changes two flows for the same result, and expired invites still need the sweep.

## R8. A late result for an erased client

- **Decision**: No change in `PlanDraftingService.storeBuilt`/`storeFailed`: they start by removing the `plan_draftings` row, which the cascade already removed, and then store nothing. `PlanDraftingJob` re-reads `drafting.input(client)` before its second attempt and stops when it is empty, so no prompt about an erased client is sent after the deletion (spec edge case). `PlanChangeSweep` already checks each client in its own transaction; a client erased meanwhile fails that one check, which is logged without identifiers and not retried, since `due()` no longer lists them.
- **Rationale**: The existing "whoever removes the row stores" rule covers the race; only the retry needed a guard.

## R9. Proving that nothing remains (SC-001)

- **Decision**: `AccountErasureIT` (`@DataJpaTest` with Testcontainers, on `AccountRepositoryImpl` and `InviteRepositoryImpl`, since the erasure is the database's) loads a fixture script that gives one client a row in every table that refers to an account, plus a second client and trainer as controls; deletes the client through the two adapters, in the order the service uses; then reads `information_schema.columns` and checks every `uuid` column of every table for the client's id and every character column for the email and the name. Before deleting, it asserts that every table with a foreign key to `accounts` (also through its parent) has a row of the client, so a future table without a fixture row fails the test instead of passing it. Spring Session tables are skipped by the scan: their rows are ended by `AccountSessions.endAll`, which the service test covers with a fake.
- **Rationale**: The proof does not depend on knowing today's table list.

## R10. The privacy notice in two languages

- **Decision**: A static page, `frontend/src/app/privacy-page.tsx`, at the public route `/privacy`, outside any area guard. Its text lives in `locales/en.json` and `pl.json` under `privacy.*`, one key per heading and paragraph, lists as arrays read with `returnObjects`, and the contact address rendered with `Trans` as a `mailto:` link. Layout: the brandmark linking to `/`, one column of `max-w-prose` text with `h1`/`h2` headings, readable at 375 px and 1280 px, using the existing `Card`-free typography tokens.
- **Rationale**: Every user-facing string goes through `t()` (frontend/CLAUDE.md); the page has no data, so no endpoint (spec: Public endpoints).
- **Alternatives considered**: Two Markdown files: a Markdown renderer is a new dependency for one page. Text in the TSX per language: breaks the i18n rule.

## R11. The delete flow in the UI

- **Decision**: One `DeleteAccountDialog` in `features/account/components`, built on shadcn `AlertDialog` like `RevokeInviteDialog` (focus starts on Cancel, stays open while the request runs, a `destructive` button that is not `AlertDialogAction`), with a `PasswordInput` field and its error linked by `aria-describedby`. `useDeleteAccount` in `features/account/api/account.ts` shares the success path of `useSignOut` (clear tab storage, clear pending logs, clear the query cache) and navigates to `/sign-in` with router state `{ accountDeleted: true }`; the sign-in page shows a status `Alert` "Your account was deleted." for that state. Trainer Settings gets a `Card` "Delete account" below Display, fed by `GET /accounts/me/deletion`; the client sheet gets a ghost destructive button below Sign out. On `account.has-clients` the dialog refetches the count and shows the section's message.
- **Rationale**: Reuses the existing dialog pattern and sign-out clean-up; no new component kind.

## R12. Encryption decision for the ADR

- **Decision**: `docs/adr/0005-data-inventory-and-encryption.md`: TLS for every connection (browser to app, app to database when they are on different hosts, app to email and AI services); encrypted disks on the database host and its backups; PBKDF2 hashes for passwords (`Pbkdf2PasswordHasher`) and SHA-256 hashes for link secrets; no column encryption. Reason: the threats that remain after these (a compromised application or database user) can read decrypted columns too, because the key would sit in the same environment; column encryption would block querying logs for progress, rules and plan changes. The inventory table lists each kind of data, its tables, purpose, lawful basis, recipients and retention.
- **Rationale**: Task #168 and the story's third criterion.
