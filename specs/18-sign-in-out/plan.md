# Implementation Plan: Trainer and client sign in and out

**Branch**: `feat/18-sign-in-out` | **Date**: 2026-10-24 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/18-sign-in-out/spec.md`

## Summary

Sign-in with email and password, sign-out, and staying signed in for 14 idle days across restarts, for trainers and clients. The backend stores sign-ins with Spring Session JDBC, signs in from a controller backed by a `SignInUseCase` that owns the per-email lock and the unconfirmed-trainer rule, adds the current-account API, limits sign-ups and new-link requests per address, and refuses the exercise library to clients. The frontend gets the sign-in screen with its four states, sign-out in both areas, role guards with a return path, and 401 handling in the API client. One pull request, one commit per task #19–#25, in that order.

## Technical Context

**Language/Version**: Java 25; TypeScript 6

**Primary Dependencies**: Spring Boot 4.1 (MVC, Security 7, Data JPA, Flyway, Validation), MapStruct, Lombok; React 19, React Router 8, TanStack Query 5, react-i18next, react-hook-form, zod, shadcn/ui. New: `spring-boot-starter-session-jdbc` (R1); shadcn `avatar` (R15).

**Storage**: PostgreSQL 18; new tables `spring_session`, `spring_session_attributes` (data-model.md). Failed sign-ins and per-address counts in memory (R8, R10).

**Testing**: JUnit 5 + AssertJ (unit with fakes and a fixed `Clock`, `@WebMvcTest`, `@SpringBootTest` + Testcontainers), ArchUnit; Vitest + Testing Library with a stubbed `fetch`

**Target Platform**: Browser SPA, same origin as the API

**Project Type**: Web application (`backend/` + `frontend/`)

**Performance Goals**: Sign-in answers in under 1 s, most of it the password hash; an unknown email takes as long (R9)

**Constraints**: No account enumeration by answer or timing (SC-002); no email in the session table or logs (R4); no token in browser storage; no server text in the UI

**Scale/Scope**: 1 screen with 4 states, 2 sign-out actions, 4 new endpoints, 2 changed, 2 tables

## Constitution Check

| Principle | Status |
|---|---|
| I. Hexagonal boundaries | Pass. `SignInService`, `FailedSignIns` and `CurrentAccountService` use only the JDK. Spring Security types stay in `account.infrastructure.input` and `common.infrastructure.config`. `DomainException.kind()` is a plain enum (R7). The per-address limit is an input-adapter concern (R10). |
| II. One contract | Pass. `schema.d.ts` regenerated in #20 and #22; new error codes translated by `code` in EN and PL. |
| III. Secure by default | Pass. One new public endpoint (`POST /sign-ins`), justified in the spec. Session id and CSRF token rotate on sign-in (R4); sign-out deletes the stored sign-in (R5); `HttpOnly`, `SameSite=Lax`, `Secure` cookie (R2); no anonymous sessions (R3); account id, not email, as principal; trainer-only exercise library (R7). |
| IV. Plan changes | Not affected. |
| V. Tests verify behaviour | Pass. Tests below target the lock window, equal answers, role and redirect rules, and sign-out clearing. |
| VI. Simplicity | Pass. In-memory counts instead of tables (R8, R10), loaders instead of middleware (R12), no "remember me", no session listing. Ceilings marked `ponytail:` (cookie age, parallel attempts, single instance). |
| VII. One design system | Pass. Existing shadcn components plus `avatar`; EN and PL with Polish plurals; browser check at 375 and 1280 px, light and dark. |

Re-checked after design: unchanged.

## Project Structure

### Documentation (this feature)

```text
specs/18-sign-in-out/
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
backend/
├── build.gradle.kts                          spring-boot-starter-session-jdbc
└── src/
    ├── main/java/com/kinetiq/
    │   ├── common/
    │   │   ├── domain/exception/DomainException.java     kind(), properties()
    │   │   └── infrastructure/
    │   │       ├── config/SecurityConfig.java            request cache off, context repository, session strategy,
    │   │       │                                         CSRF repository, logout off, sign-ins public, exercises trainer-only
    │   │       └── input/GlobalExceptionHandler.java     status by kind, properties copied
    │   └── account/
    │       ├── domain/
    │       │   ├── model/Account (changeTimeZone), FailedSignIns
    │       │   ├── input/SignInUseCase, SignInCommand, CurrentAccountUseCase
    │       │   ├── output/AccountRepository (findById), PasswordHasher (matches), FailedSignInsRepository
    │       │   ├── service/SignInService, CurrentAccountService
    │       │   └── exception/CredentialsInvalidException, SignInLockedException,
    │       │                 EmailNotConfirmedException, AccountNotFoundException
    │       └── infrastructure/
    │           ├── config/AccountConfig                  new use cases, limiter
    │           ├── input/AccountController (limits, time zone helper), SignInController,
    │           │         CurrentAccountController, SignInRequest, TimeZoneRequest,
    │           │         CurrentAccountResponse, SourceAddressLimiter
    │           └── output/AccountRepositoryImpl (findById), Pbkdf2PasswordHasher (matches),
    │                      InMemoryFailedSignInsRepository
    └── main/resources/
        ├── application.yaml                      session timeout, schema never, cookie
        ├── application-local.yaml                cookie not secure
        ├── db/migration/V5__create_spring_session.sql
        └── db/dev-data/R__demo_accounts.sql      demo client

frontend/src/
├── app/
│   ├── app.tsx                                   shared query client
│   ├── router.ts                                 /sign-in, loaders, error boundaries
│   ├── guards.ts (+ .test.ts)                    requireRole, landingFor, safeNext
│   ├── area-error-page.tsx                       generic error with "Try again"
│   ├── trainer-area.tsx                          footer: avatar, name, Sign out
│   └── client-area.tsx                           header Settings → sheet with Sign out
├── components/ui/avatar.tsx                      shadcn CLI
├── features/account/
│   ├── api/account.ts                            useSignIn, useSignOut, currentAccountQuery, refreshTimeZone
│   ├── components/password-input.tsx             moved from the sign-up page
│   ├── pages/sign-in-page.tsx (+ .test.tsx)
│   ├── sign-in-schema.ts
│   └── time-zone.ts                              browserTimeZone, moved
├── lib/api/client.ts (+ .test.ts)                401 → sign-in with next
├── lib/api/sign-in-redirect.ts                   window.location.assign, mockable
├── lib/query-client.ts
└── locales/en.json, pl.json
```

**Structure Decision**: Everything belongs to the existing `account` feature on both sides; the guards live in `app/` beside the router because they decide which area loads. `common` changes only where every feature benefits: error statuses and security configuration.

## Work per task (one commit each)

| Task | Commit | Contents |
|---|---|---|
| #19 | `feat(backend): store sign-ins in the database (#19)` | Starter, `V5`, timeout and cookie properties, local cookie override, `NullRequestCache`; IT: a saved session is a row; an anonymous `401` writes none |
| #20 | `feat(backend): add sign-in and sign-out api (#20)` | `DomainException.kind()/properties()` and handler, `SignInUseCase` + service (equal answers, dummy hash, unconfirmed trainer), `PasswordHasher.matches`, `SignInController` with session and CSRF rotation, logout handlers, `request.not-allowed`, trainer-only exercises, demo client + README, `schema.d.ts` |
| #21 | `feat(backend): throttle failed sign-ins (#21)` | `FailedSignIns`, port + in-memory adapter, service integration, `retryAfterSeconds`, `SourceAddressLimiter` on sign-up and new link |
| #22 | `feat(backend): add current-account api (#22)` | `CurrentAccountUseCase` + service, `Account.changeTimeZone`, `findById`, `GET /accounts/me`, `PUT /accounts/me/time-zone`, shared zone check, `schema.d.ts` |
| #23 | `feat(frontend): send to sign-in on 401 (#23)` | Shared query client, `onResponse` 401 redirect with `next`, exclusions, `request.forbidden` copy without "session", `request.not-allowed` and new account codes in EN and PL |
| #24 | `feat(frontend): add sign-in screen and sign-out actions (#24)` | `avatar`, `PasswordInput`, sign-in page with four states, resend, busy and focus rules, minutes plural, sign-out in sidebar and client sheet, storage and cache clearing, browser verification |
| #25 | `feat(frontend): guard areas per role (#25)` | `guards.ts`, loaders on `/trainer`, `/app`, `/app/quiz`, `/sign-in`; `next` handling on sign-in; area error page; time zone refresh; router tests |

Spec Kit files go in a first `docs: add spec for sign in and out` commit together with the `CONTEXT.md` term.

## Tests

| Test | Covers |
|---|---|
| `FailedSignInsTest` (fixed times) | 4 failures not locked; the fifth locks for 15 min from it; failures older than 15 min drop out; attempts while locked are ignored; at exactly 15 min unlocked and counting from zero; `timeLeft` |
| `SignInServiceTest` (fake repository, hasher, failed sign-ins; fixed `Clock`) | right password → account, record removed; unknown email → invalid, dummy hash compared, failure counted; wrong password → invalid, counted; malformed email or 129-character password → invalid, no hash; locked → locked with time left, also with the right password, no hash; unconfirmed trainer → refused after the right password only; unconfirmed client → signed in; right password resets the count |
| `CurrentAccountServiceTest` | returns the account; missing account → not found; zone stored only when different |
| `AccountTest` | extended: `changeTimeZone` |
| `SourceAddressLimiterTest` (fixed clock) | 10 allowed, 11th refused, allowed again an hour after the first; kinds and addresses counted apart |
| `SignInControllerTest` (`@WebMvcTest`) | `200` with body and a new session id; CSRF cookie cleared; `401`/`403`/`429` codes and `retryAfterSeconds`; blank fields → field errors; missing CSRF → `403 request.forbidden`; sign-out `204` invalidates, anonymous `401` |
| `CurrentAccountControllerTest` (`@WebMvcTest`) | anonymous `401`; signed-in body; time zone `204`, unknown zone `400`; missing account → `401` |
| `AccountControllerTest` | extended: over the limit → `202` and the use case is not called |
| `GlobalExceptionHandlerTest` | extended: kind → status, properties copied |
| `KinetiqBackendApplicationIT` | sign in against the real database → `SESSION` cookie, row with the account id as principal; `/accounts/me` with it; sign-out → same cookie `401`; client → exercises `403 request.not-allowed`; trainer → `200`; anonymous requests create no row |
| `client.test.ts` | extended: `401` on an area page → sign-in with `next`; `401` from sign-in or `/accounts/me` → no redirect; a POST after the CSRF cookie was cleared fetches a new token |
| `guards.test.ts` | `safeNext` rejects `//x`, `/\x`, `https://x`, other area; keeps path and query |
| `sign-in-page.test.tsx` | empty fields; wrong → alert, password cleared, focus on password; locked → minutes with plural; unconfirmed → resend → disabled and note; busy blocks a second submit; network failure → generic; success → landing or `next`; tab storage cleared |
| `router.test.tsx` | extended: anonymous → sign-in with `next`; client at `/trainer` → `/app/today`; trainer at `/app/quiz` → `/trainer/clients`; signed in at `/sign-in` → area; `/accounts/me` failing → "Try again" |
| `trainer-area.test.tsx`, `client-area.test.tsx` | extended: sign out → request sent, tab storage cleared, sign-in shown; failure → stays with the generic error |

## Follow-ups outside this story

- #39 ends every sign-in of the account through Spring Session's principal index.
- #65 sets `server.forward-headers-strategy` so the per-address limit sees the visitor, not the proxy (R10).
- #43 adds settings to `GET /accounts/me`; #45 fills the client settings sheet.
- #41 builds the reset page "Forgot password?" links to.

## Complexity Tracking

None.
