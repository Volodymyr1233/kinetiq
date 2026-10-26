# Research: Trainer invites a client

Checked against the code on `master` (6f7be66) on 2026-10-25. context7 was unreachable this session; shadcn component sources were read through `npx shadcn@latest view`.

## R1. Which feature owns invites

- **Decision**: The `account` feature, on both sides (`com.kinetiq.account`, `frontend/src/features/account`).
- **Rationale**: An invite turns into an account. Story #30 accepts it by creating the client account, confirming the email and signing the person in, which uses `AccountRepository`, `PasswordHasher` and the sign-in strategy that live in `account`. A separate `invite` feature would need `account` for the existence check now, and `account` would need `invite` to accept: a cycle `HexagonalArchitectureTest` forbids, or an extra port in each direction. One feature keeps the existence check, the trainer's name and the join step as plain calls.
- **Alternatives**: A new `invite` feature calling `account` through a `domain.input` port, with acceptance there too; rejected because acceptance must also sign in, which is `account` infrastructure.

## R2. Secrets

- **Decision**: Rename `ConfirmationToken` to `SecretToken` and use it for confirmation links and both invite secrets: 32 random bytes, URL-safe Base64, stored as SHA-256 hex in `char(64)` columns with unique constraints. An invite has `email_token_hash` and `link_token_hash`. `InviteRepository.findByTokenHash(hash)` matches either column.
- **Rationale**: The existing class already does exactly this (spec #2, FR-011) and a second use is the point at which the constitution allows sharing it. 256 random bits need no salt or slow hash. Two columns, not a child table, because an invite always has exactly two.
- **Link**: `<kinetiq.app-url>/join#token=<secret>` for both secrets, like `/confirm-email#token=`: a fragment never reaches a server, so secrets stay out of access logs and `Referer` headers. The join page itself is story #30.

## R3. Invite lifecycle

- **Decision**: `status` column `OPEN | USED | REVOKED`; `sent_at` and `expires_at` set by the domain from its `Clock` (`expires_at = sent_at + 7 days`). Open means `status = OPEN` and `now < expires_at`. Methods: `Invite.send(trainerId, email, now)` returns the invite and both secrets; `revoke(now)` and `use(now)` throw `InviteNotFoundException` unless open; `replaceLink(now)` returns a new link secret and throws the same unless open; `supersede()` marks any `OPEN` row revoked, expired or not.
- **Rationale**: Task #27 asks for single use, 7 days and revocable as domain rules. Expired is derived, so no job has to flip rows. `use` is built now because #27 names single use; #30 calls it. `sent_at` is separate from the technical `created_at` because the domain needs it for the list and tests need it from a fixed clock.

## R4. One open invite per trainer and email

- **Decision**: Partial unique index `uq_invites_open_trainer_email on invites (trainer_id, email) where status = 'OPEN'`. Creating looks up the trainer's `OPEN` row for that email, calls `supersede()`, saves it, then saves the new invite in the same transaction.
- **Rationale**: FR-007 and the edge case "two identical requests leave one open invite". The index also serves the list query (`trainer_id = ? and status = 'OPEN'`). An expired row still marked `OPEN` is superseded too, so it never blocks a new invite.
- **Known ceiling**: of two simultaneous identical requests one fails on the index and the trainer sees the generic error; the other succeeds. Marked `ponytail:` in the service.

## R5. Status codes and error codes

| Case | Status | Code |
|---|---|---|
| Email empty, malformed or over 254 characters | 400 | `validation.failed`, `errors: [{field: "email", code}]` |
| Email has an account | 409 | `invite.account-exists` |
| More than 20 create requests in an hour | 429 | `invite.limit-reached` |
| Invite not open, not the trainer's or unknown (copy new link, revoke) | 404 | `invite.not-found` |
| Client calls any invite endpoint | 403 | `request.not-allowed` (existing) |

- **Decision**: `DomainException.Kind` gains `NOT_FOUND` (404) and `CONFLICT` (409); `GlobalExceptionHandler` maps them.
- **Rationale**: These are the first domain "not found" and "conflict" refusals. A status per kind keeps `common` free of feature exceptions, as in spec #18 (R7).

## R6. Limit of 20 invites an hour

- **Decision**: Generalise `SourceAddressLimiter` into `RequestLimiter`: each `Kind` carries its limit (`SIGN_UP` 10, `CONFIRMATION_LINK` 10, `INVITE` 20) and the key is any string (a network address, or the trainer's id). The invite controller asks it before calling the use case and throws `InviteLimitReachedException` (`TOO_MANY_ATTEMPTS`) when refused. Requests are counted after input validation, so a malformed email does not use up the limit, but one refused for an existing account does.
- **Rationale**: Same sliding-window logic, same in-memory ceiling (already marked `ponytail:`); one class instead of a copy. The limit protects against probing, which only requests with a valid email can do.
- **Alternatives**: A domain rule over stored invites (would not count refusals, which are the probing case); a table of attempts (a migration and clean-up for one instance).

## R7. Email

- **Decision**: `Mail(to, "invite", locale, {link, trainerName, validDays})` through the existing `MailSender`; subject key `mail.invite.subject` in `messages.properties` and `messages_pl.properties`. The locale is the request's `Accept-Language`, which the SPA sets to the UI language.
- **Rationale**: Same capability and rules as spec #2 (FR-018, FR-019). `LoggingMailSender` logs recipient, subject and link only under the local profile. The body template arrives with real delivery (#65); `trainerName` and `validDays` are in the values for it (FR-009).

## R8. API shape

- **Decision**: `POST /invites` (201, invite and link), `GET /invites` (open invites, newest first), `POST /invites/{id}/links` (201, new link), `DELETE /invites/{id}` (204, revoke). `/api/v1/invites/**` requires the `TRAINER` role in `SecurityConfig`; ownership is checked in the use case.
- **Rationale**: A revoked invite stays stored, but for the trainer it is gone, which `DELETE` expresses. A new link is a new resource, so `POST`. No paging (spec assumptions). Details in [contracts/api.md](contracts/api.md).

## R9. Toasts

- **Decision**: shadcn `sonner` for "Link copied", "New link copied", "Invite revoked" and the not-found refresh. The generated `sonner.tsx` imports `next-themes`; it is edited to read the theme from `@/lib/theme`'s `useTheme`, and `next-themes` is not installed. `<Toaster />` is mounted once in the trainer area.
- **Rationale**: The design system names Sonner for confirmations of the user's own action, bottom-right on desktop, 4 seconds; errors stay until dismissed, so error toasts get `duration: Infinity` and a close button. Sonner announces toasts in a live region, which covers "announced once" (FR-020). The app has its own theme provider; a second one would disagree with it.

## R10. Copying after a request

- **Decision**: `copyText(promise)` in `features/account`: where `ClipboardItem` exists, `navigator.clipboard.write([new ClipboardItem({'text/plain': promise.then(toBlob)})])`, started synchronously in the click handler; otherwise `navigator.clipboard.writeText` after the promise resolves. A rejection shows "Couldn't copy. Select the link and copy it.".
- **Rationale**: "Copy new link" must request the link first. Safari only allows clipboard writes inside the user gesture; a `ClipboardItem` holding a promise keeps the gesture while the request runs. Chrome and Firefox accept both.

## R11. Dates and days left

- **Decision**: The API returns `sentAt` and `expiresAt` as instants. The page formats `sentAt` as "Today" when it is the current day in the browser's time zone (which `refreshTimeZone` keeps equal to the account's), otherwise `Intl.DateTimeFormat(locale, {weekday: 'short', day: 'numeric', month: 'short'})`. Days left = `ceil((expiresAt − now) / 24 h)`; 1 means the last day and uses the `warning` badge, otherwise `outline` (design system: "Invite: In {n} days (outline), In 1 day (warning)"). Polish plurals `_one`, `_few`, `_many`.
- **Rationale**: Display rules belong to the UI; the backend stays free of time zones and languages for this list.

## R12. Where the page lives

- **Decision**: Route `/trainer/clients/invites`; the Clients nav item stays active through its existing `end: false` match. Until task #37, `/trainer/clients` keeps its placeholder with one added link, "Open invites". Page heading "Open invites" with the description and "Invite client" from the board.
- **Rationale**: The board's breadcrumb puts invites under Clients. The header bar shows the section title, as on every trainer page. The board's footer and bell are out of scope (spec assumptions).

## R13. Dialogs

- **Decision**: shadcn `dialog` for invite (form, then link ready, in one dialog) and `alert-dialog` for revoke. The form uses react-hook-form with zod (`z.email()` plus the 254 limit) and `Field` primitives, as on the sign-up page. After success, focus moves to the "Invite sent to …" heading; Done closes and returns focus to "Invite client". The alert dialog's initial focus is Cancel; it does not close on an outside click (Radix default for alert dialogs). Trainer screens stay at 1280px, so the design system's drawer below 768px is not built.
- **Rationale**: The design system: dialog for forms, alert dialog for one decision with the question as title and the verb on the button, focus on the safe action.
