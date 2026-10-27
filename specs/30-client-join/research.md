# Research: Client joins through an invite

## R1. Where joining lives

- **Decision**: In the `account` feature. `SignUpUseCase` gains `checkInvite` and `joinAsClient`; `SignUpService` implements them next to trainer sign-up.
- **Rationale**: Joining creates an account, as trainer sign-up does, and needs what `SignUpService` already has: the password hasher, the account repository, and the private `sendConfirmation` that issues a confirmation link and mails it (FR-009). Invites already live in `account` (spec #26, R1), so no new feature or cross-feature port.
- **Alternatives**: A separate `ClientJoinService` with its own port; rejected because it would copy `sendConfirmation` or force a shared helper for one second use. A new `client` feature; rejected, nothing outside accounts and invites is involved yet.

## R2. The client's trainer

- **Decision**: A nullable `trainer_id` column on `accounts` referencing `accounts(id)`, with a check that a row has a trainer exactly when its role is `CLIENT`, and an index for "a trainer's clients". `Account` carries `Optional<AccountId> trainerId`. `AccountRepository.findClientsOf(AccountId trainerId)` returns the trainer's clients, ordered by name.
- **Rationale**: "Exactly one trainer" (FR-014) is a column, not a relation table. The check makes it hold in the database too. The repository query is the "query: a trainer's clients" of task #31; it gets its first caller with the all-clients screen (task #37), so it is proven by a persistence test only.
- **Alternatives**: A `trainer_clients` table; rejected, a client never has two trainers (switching is a backlog story, and would be an update of the column). A use-case port for listing clients now; rejected, no caller yet (Simplicity).

## R3. Migrating databases that already have the demo client

- **Decision**: The two new checks (`ck_accounts_client_trainer`, `ck_accounts_client_consent`) are added `not valid`. `R__demo_accounts.sql` inserts the demo client with trainer and consent, and also updates an existing demo client that has none.
- **Rationale**: Local databases already hold `demo.client@example.com` without a trainer; a validated check would stop Flyway on every developer's machine. `not valid` still enforces the rule on every insert and update; no other environment has data (nothing is deployed before story #63). Marked `ponytail:` in the migration with the upgrade: `validate constraint` once every database has run the updated demo data.
- **Alternatives**: Ask everyone to drop their database; rejected, the migration should run where it lands. Leaving the database rule out and enforcing it only in the domain; rejected, a column that must be set for one role is cheap to guard.

## R4. Which secret was used

- **Decision**: `Invite.provesEmail(SecretToken)` compares the token's hash with `emailTokenHash`. The existing `InviteRepository.findByTokenHash` finds the invite by either hash.
- **Rationale**: Only the emailed secret proves control of the address (spec #26, FR-004). One comparison, no stored flag.

## R5. When a link works

- **Decision**: One private method in `SignUpService` finds the invite by hash and accepts it only if `invite.isOpen(now)` and `!accounts.existsByEmail(invite.getEmail())`; otherwise `InviteLinkInvalidException` (`invite.link-invalid`, kind `INVALID` → `400`). Both `checkInvite` and `joinAsClient` use it. The password is hashed only after the link is known to work.
- **Rationale**: FR-003 and FR-011 need one indistinguishable answer. `400` and a `…-link-invalid` code follow the confirmation and password-reset links (`account.confirmation-link-invalid`, `account.password-reset-link-invalid`). Hashing after the check means an invalid link costs no hash, which is why no per-source limit is needed (spec Assumptions).
- **Alternatives**: Reusing `invite.not-found` (`404`); rejected, that code belongs to the trainer's invite list and its copy ("This invite is no longer open.") is written for the trainer.

## R6. Two joins at the same moment

- **Decision**: The use-case transaction commits when `joinAsClient` returns, so the losing request fails on the unique email (`uq_accounts_email`) or on the invite's `@Version`. The join controller catches, from that call, `OptimisticLockingFailureException` and a `DataIntegrityViolationException` whose cause chain holds Hibernate's `ConstraintViolationException` named `uq_accounts_email`, and throws `InviteLinkInvalidException` for those two only. Any other integrity violation (for example a client without trainer or consent) is rethrown and stays a `500`.
- **Rationale**: The spec's edge case says the other request sees "This link no longer works", not the generic error. Translating every integrity violation would hide a bug behind an "expired link" page, so only the two races are translated, matched by constraint name. The controller may use Spring's and Hibernate's exception types; it never touches the output adapter. The persistence test proves that a duplicate email surfaces with that constraint name.
- **Alternatives**: Accept the generic error, as invite creation does (spec #26); rejected, here a person may submit from two tabs and must see the right page.

## R7. Signing in after joining

- **Decision**: Move what `SignInController.signIn` does after the use case (authentication token with the account id and role, `SessionAuthenticationStrategy`, context save) into a package-private `SignIns` component in `account.infrastructure.input`, used by both controllers. Named after the domain term "Sign-in", which `CONTEXT.md` asks to keep apart from "Session".
- **Rationale**: FR-010 requires a sign-in exactly like sign-in: new session id and CSRF token, account id as principal. The Spring Security 7 reference shows the same manual pattern (`createEmptyContext`, `setContext`, `saveContext`). Two callers justify the extraction. A sign-in already present in the browser is replaced, because the session id rotates and the context is overwritten (clarification 4).

## R8. Endpoints

- **Decision**: `POST /api/v1/invite-checks` (`{token}` → `{email, trainerName}`) and `POST /api/v1/invite-acceptances` (join → `201` with `CurrentAccountResponse`). Both public in `SecurityConfig`, listed with the other public `POST`s.
- **Rationale**: A `POST` body keeps the secret out of URLs and access logs, as `/email-confirmations` does. Paths outside `/api/v1/invites/**` avoid that trainer-only matcher. Returning the current account lets the SPA put it in the cache, as sign-in does.

## R9. The join page

- **Decision**: `/join` route, public, lazy, `features/account/pages/join-page.tsx`, built on `AuthLayout` and `AuthCard`. The token is read from the fragment and left there, so a reload shows the form again (spec edge case). States: checking, check failed (generic message and "Try again"), form, "You're in", "This link no longer works".
- **Rationale**: The design system (§6.12) makes account pages one centred card at most 400px wide under the brand mark; the board draws the phone frame only because it is designed at 375px. Keeping the fragment differs from `/confirm-email`, which removes it because opening that link uses it; here opening changes nothing (FR-004).
- **Check query**: `useQuery` keyed by the token, `retry` only for failures worth retrying (the app default), so `invite.link-invalid` shows the page at once.

## R10. Form rules shared with sign-up

- **Decision**: Export `nameSchema` and `passwordSchema` from `sign-up-schema.ts` and compose `joinSchema` from them plus `atLeastSixteen` and `healthDataConsent` booleans that must be true. Move sign-up's `serverFieldErrors` and per-field message mapping to `features/account/field-errors.ts`, used by both pages.
- **Rationale**: FR-006, FR-012 and FR-013 repeat the sign-up rules and messages; the second use is the moment to share them.

## R11. The banner

- **Decision**: `CurrentAccountResponse` gains `emailConfirmed`. A `ConfirmEmailBanner` in `features/account/components` renders inside `ClientArea`, under the header, when the cached account is unconfirmed. "Resend" calls the existing `useResendLink` with the account's own email; while locked (60 s) it reads "Sent" and is disabled, and an `sr-only` `role="status"` says "We sent a new link.".
- **Rationale**: `ClientArea` is the layout of exactly the four tab pages (clarification 2). `currentAccountQuery` has `staleTime` 0 and `ClientArea` observes it, so TanStack Query's default `refetchOnWindowFocus` refetches it when the client returns to the tab: the banner disappears without new code (FR-018). The design system's banner is a full-width muted strip (`bg-muted`, 13px text, mail icon, link button); no shadcn component matches a full-bleed strip, so it is composed from tokens and `Button variant="link"`, with the button at least 44px tall (design system §7).
- **Resend endpoint**: the existing public `POST /confirmation-links`, which sends only for an unconfirmed account and at most once per 60 seconds; nothing new on the backend.

## R12. "Email confirmed" for both roles

- **Decision**: Description becomes role-neutral ("Your email is confirmed."). The page reads `currentAccountQuery` without blocking: signed in → "Continue" to `landingFor(role)`; otherwise "Sign in". On success it invalidates `currentAccountQuery`.
- **Rationale**: FR-019; the old copy said "Your trainer account is ready" to clients.

## R13. Privacy notice link

- **Decision**: `<a href="/privacy" target="_blank" rel="noopener">` inside the consent label, with a visually hidden "(opens in a new tab)". Clicking it must not toggle the checkbox.
- **Rationale**: Clarification 3; task #169 builds the page at that address.

## R14. Migration number

- **Decision**: `V10__add_client_trainer.sql` (#31) and `V11__add_health_data_consent.sql` (#32). No open pull request adds a migration (checked with `gh pr list`); re-check before opening the pull request.
