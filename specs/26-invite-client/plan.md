# Implementation Plan: Trainer invites a client

**Branch**: `feat/26-invite-client` | **Date**: 2026-10-25 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/26-invite-client/spec.md`

## Summary

A trainer invites a person by email, copies a separate link, sees open invites with their expiry, gets a new copyable link for one, and revokes one. The backend adds the `Invite` aggregate to the `account` feature with two hashed secrets (email and link), a 7-day single-use lifecycle and one open invite per trainer and email, four trainer-only endpoints, the invite email, and a limit of 20 create requests an hour per trainer. The frontend adds the "Open invites" page under Clients with the invite dialog, the revoke confirmation and toasts. One pull request, one commit per task #27–#29, in that order.

## Technical Context

**Language/Version**: Java 25; TypeScript 6

**Primary Dependencies**: Spring Boot 4.1 (MVC, Security 7, Data JPA, Flyway, Validation), MapStruct, Lombok; React 19, React Router 8, TanStack Query 5, react-i18next, react-hook-form, zod, shadcn/ui. New: shadcn `dialog`, `alert-dialog`, `sonner` (npm `sonner`; not `next-themes`, R9).

**Storage**: PostgreSQL 18; new table `invites` (data-model.md). Invite request counts in memory (R6).

**Testing**: JUnit 5 + AssertJ (unit with fakes and a fixed `Clock`, `@WebMvcTest`, `@DataJpaTest` + Testcontainers, one `@SpringBootTest`), ArchUnit; Vitest + Testing Library with a stubbed `fetch`

**Target Platform**: Browser SPA, same origin as the API

**Project Type**: Web application (`backend/` + `frontend/`)

**Performance Goals**: Normal web expectations; the list is at most a few dozen rows (spec assumptions)

**Constraints**: Secrets only as hashes and never in responses except the create (link secret) and the email (email secret); another trainer's invite answers `404`; no server text in the UI

**Scale/Scope**: 1 page with 5 states and a confirmation, 4 new endpoints, 1 table

## Constitution Check

| Principle | Status |
|---|---|
| I. Hexagonal boundaries | Pass. `Invite`, `SecretToken` and `InviteManagementService` use only the JDK. The limiter stays an input-adapter concern (R6). Invites live in `account`, so no new cross-feature dependency or cycle (R1). |
| II. One contract | Pass. `schema.d.ts` regenerated in #28; three new codes translated by `code` in EN and PL. |
| III. Secure by default | Pass. No public endpoint; `/api/v1/invites/**` requires `TRAINER`; ownership in the use case with `404` for anything not the trainer's or not open (FR-016). Secrets hashed (R2), in the URL fragment, out of logs except the local mail log. 20 requests an hour limit account probing (R6). |
| IV. Plan changes | Not affected. |
| V. Tests verify behaviour | Pass. Tests below target expiry at exactly 7 days, single use, supersede, ownership, the limit and the UI states. |
| VI. Simplicity | Pass. One feature instead of two (R1); one generalised limiter instead of a copy (R6); expired derived, no job (R3); no paging; no history. Ceilings marked `ponytail:` (simultaneous identical invites, in-memory counts). |
| VII. One design system | Pass. shadcn components only, Sonner themed from the app's own provider; the board's "This email already has an account." chosen over the design system's "…a Kinetiq account."; EN and PL with Polish plurals; browser check at 1280px, light and dark. |

Re-checked after design: unchanged.

## Project Structure

### Documentation (this feature)

```text
specs/26-invite-client/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/api.md
├── checklists/requirements.md
└── tasks.md             (/speckit-tasks)
```

### Source Code

```text
backend/src/
├── main/java/com/kinetiq/
│   ├── common/
│   │   ├── domain/exception/DomainException.java        Kind NOT_FOUND, CONFLICT
│   │   └── infrastructure/
│   │       ├── config/SecurityConfig.java               /api/v1/invites/** trainer-only
│   │       └── input/GlobalExceptionHandler.java        404, 409
│   └── account/
│       ├── domain/
│       │   ├── model/SecretToken (renamed from ConfirmationToken), Invite, InviteId,
│       │   │         InviteStatus, SentInvite
│       │   ├── input/ManageInvitesUseCase, CreateInviteCommand, CreatedInvite
│       │   ├── output/InviteRepository, AccountRepository (existsByEmail)
│       │   ├── service/InviteManagementService, SignUpService (SecretToken)
│       │   └── exception/InviteNotFoundException, InviteAccountExistsException,
│       │                 InviteLimitReachedException
│       └── infrastructure/
│           ├── config/AccountConfig                     invite use case
│           ├── input/InviteController, CreateInviteRequest, InviteResponse,
│           │         CreatedInviteResponse, InviteListResponse, InviteLinkResponse,
│           │         InviteWebMapper, RequestLimiter (renamed from SourceAddressLimiter),
│           │         AccountController (RequestLimiter)
│           └── output/InviteEntity, InviteMapper, JpaInviteRepository, InviteRepositoryImpl,
│                      AccountRepositoryImpl + JpaAccountRepository (existsByEmail)
└── main/resources/
    ├── db/migration/V6__create_invites.sql
    ├── db/dev-data/R__demo_invites.sql
    └── messages.properties, messages_pl.properties      mail.invite.subject

frontend/src/
├── app/
│   ├── router.ts                                        clients/invites
│   └── trainer-area.tsx                                 <Toaster />, clients placeholder link
├── components/ui/dialog.tsx, alert-dialog.tsx, sonner.tsx   shadcn CLI; sonner uses @/lib/theme
├── features/account/
│   ├── api/invites.ts                                   useInvites, useCreateInvite, useReplaceInviteLink, useRevokeInvite
│   ├── copy-text.ts (+ .test.ts)                        ClipboardItem with a promise, writeText fallback
│   ├── invite-format.ts (+ .test.ts)                    daysLeft, sent label
│   ├── invite-schema.ts
│   ├── components/invite-dialog.tsx                     form → link ready
│   ├── components/revoke-invite-dialog.tsx
│   ├── components/invite-table.tsx
│   └── pages/invites-page.tsx (+ .test.tsx)
├── lib/api/schema.d.ts                                  npm run gen:api
└── locales/en.json, pl.json
```

**Structure Decision**: Everything belongs to the `account` feature on both sides (R1). `common` changes only for two new error kinds and one security rule.

## Work per task (one commit each)

| Task | Commit | Contents |
|---|---|---|
| #27 | `feat(backend): add invite model and migration (#27)` | `ConfirmationToken` → `SecretToken`; `Invite`, `InviteId`, `InviteStatus`, `SentInvite`, `InviteNotFoundException`; `InviteRepository` + JPA adapter; `V6`; `R__demo_invites.sql`; `InviteTest`, `InviteRepositoryImplIT` |
| #28 | `feat(backend): add create, list and revoke invite api (#28)` | `Kind.NOT_FOUND/CONFLICT` and handler; `ManageInvitesUseCase` + `InviteManagementService` (existence check, supersede, mail, list, replace link, revoke, ownership); `existsByEmail`; `RequestLimiter` with `INVITE`; `InviteController` and records; security rule; mail subject EN/PL; `schema.d.ts` |
| #29 | `feat(frontend): add invite dialog and open-invites list (#29)` | shadcn `dialog`, `alert-dialog`, `sonner`; route and placeholder link; invites page with five states, dialog, revoke confirmation, copy and toasts; EN/PL copy and codes; tests; browser verification |

Spec Kit files go in a first `docs: add spec for trainer invites a client` commit.

## Tests

| Test | Covers |
|---|---|
| `InviteTest` (fixed times) | `send`: open, expiry 7 days, two different secrets matching the stored hashes; open at 7 days − 1 s, expired at exactly 7 days; `revoke` open → revoked, again → not found, expired → not found; `use` once, twice → not found, after revoke → not found; `replaceLink` → new hash, email hash and expiry unchanged, old secret no longer matches; not open → not found; `supersede` works on an expired `OPEN` invite |
| `InviteManagementServiceTest` (in-memory repositories, recording mail sender, fixed `Clock`) | create: email with an account (any role, unconfirmed) → `invite.account-exists`, nothing saved or sent; success → saved, mail `invite` to the address in the given locale with the email link and trainer name, returned link carries the other secret; same trainer and email again → earlier one revoked, new one open, second mail; another trainer's open invite for the email untouched. list: only own, only open, newest first, expired and revoked excluded. replace link and revoke: own open → done; another trainer's, used, revoked, expired, unknown → `invite.not-found` with nothing changed |
| `RequestLimiterTest` | renamed from `SourceAddressLimiterTest`; extended: `INVITE` allows 20, refuses the 21st, allows again an hour after the first; kinds counted apart |
| `InviteControllerTest` (`@WebMvcTest`) | `201` with invite and link; blank, long and malformed email → field errors; `409`, `429`, `404` codes; limiter refusing → `429` and use case not called; client role → `403 request.not-allowed`; anonymous → `401`; missing CSRF → `403 request.forbidden`; `GET` shape; `DELETE` `204` |
| `GlobalExceptionHandlerTest` | extended: `NOT_FOUND` → 404, `CONFLICT` → 409 |
| `InviteRepositoryImplIT` | round trip; `findByTokenHash` finds the same invite by either hash; `findOpenByTrainer` order and filters; second `OPEN` row for the same trainer and email rejected by the index, allowed once the first is revoked |
| `AccountRepositoryImplIT` | extended: `existsByEmail` |
| `KinetiqBackendApplicationIT` | extended: as the demo-like trainer create → list → replace link → revoke against the real database; another trainer's revoke → `404` |
| `copy-text.test.ts` | `ClipboardItem` path writes the resolved text; fallback to `writeText`; rejection propagates |
| `invite-format.test.ts` | days left rounds up (7 at creation, 1 under 24 h); "Today" versus a date in the browser's zone |
| `invites-page.test.tsx` | list rows with sent and days left, warning text on the last day; empty state; load error with retry; invite: field error for malformed email, `409` message on the field, `429` message, success shows link-ready with focus on its heading and the list refreshes; copy → "Copied" and announcement; copy failure message; "Copy new link" → request then copy; revoke asks first, Cancel changes nothing, confirm sends `DELETE` and toasts; `404` → "This invite is no longer open." and refetch; busy buttons block a second request |
| `router.test.tsx` | extended: `/trainer/clients/invites` renders the page with Clients active |

## Follow-ups outside this story

- #30 builds `/join`, calls `Invite.use`, tells the email secret from the link secret, and adds the demo secrets to the README.
- #37 replaces the Clients placeholder and adds its own invite action.
- #168 decides how long ended invites are kept.
- #65 delivers the invite email with a body naming the trainer and the 7 days, outside the request, so a delivery failure never undoes the invite (spec edge case).

## Complexity Tracking

None.
