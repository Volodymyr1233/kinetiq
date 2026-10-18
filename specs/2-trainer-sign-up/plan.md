# Implementation Plan: Trainer signs up

**Branch**: `feat/2-trainer-sign-up` | **Date**: 2026-10-18 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/2-trainer-sign-up/spec.md`

## Summary

A public sign-up flow for trainers: form, confirmation link by email, confirm page, and a new-link request, all revealing nothing about which emails have accounts. The backend gets its first feature (`account`), the shared error handler, the outgoing-mail port with a logging adapter, and the first migration. The frontend gets its first form, the API client's CSRF and language headers, and error translation by `code`. One pull request, one commit per task #3–#8, in that order.

## Technical Context

**Language/Version**: Java 25; TypeScript 6

**Primary Dependencies**: Spring Boot 4.1 (MVC, Security 7, Data JPA, Flyway, Validation), MapStruct, Lombok; React 19, React Router 8, TanStack Query 5, react-i18next, shadcn/ui. New: `react-hook-form`, `zod`, `@hookform/resolvers` (R11). No new backend dependency.

**Storage**: PostgreSQL 18, table `accounts` (data-model.md)

**Testing**: JUnit 5 + AssertJ (unit with fakes, `@WebMvcTest`, `@DataJpaTest` + Testcontainers), ArchUnit; Vitest + Testing Library with a stubbed `fetch`

**Target Platform**: Browser SPA, same origin as the API

**Project Type**: Web application (`backend/` + `frontend/`)

**Performance Goals**: Sign-up responds in under 1 s; most of it is password hashing (R1, R6)

**Constraints**: No enumeration of emails by content or timing (SC-002); no token in server logs or `Referer` (R5); no server text in the UI

**Scale/Scope**: 5 screen states on 2 routes, 4 endpoints, 1 table

## Constitution Check

| Principle | Status |
|---|---|
| I. Hexagonal boundaries | Pass. Domain uses only the JDK (`SecureRandom`, `MessageDigest`, `Clock`, `ZoneId`, `Locale`). Hashing and mail are output ports; the app URL is passed to the service as a plain `URI` when the bean is wired. |
| II. One contract | Pass. `schema.d.ts` regenerated in the commits that change the API (#6, #7); errors are problem details with `code` (#4). |
| III. Secure by default | Pass. Four public endpoints, justified in the spec. CSRF on every POST. Password PBKDF2 (R1); token stored hashed (R5); no internals in errors (R3). Recipient and link reach the log only with the local profile (R10a). |
| IV. Plan changes | Not affected. |
| V. Tests verify behaviour | Pass. Test list below targets rules and edge cases only. |
| VI. Simplicity | Pass. No confirmation table (R5), no throttling table (R7), no language stored, no password-input component until sign-in needs one. |
| VII. One design system | Pass. shadcn components with Kinetiq tokens; EN and PL; browser check at 375 and 1280 px, light and dark. |

Re-checked after design: unchanged.

## Project Structure

### Documentation (this feature)

```text
specs/2-trainer-sign-up/
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
├── build.gradle.kts                          bootRun activates the local profile (R10)
└── src/
    ├── main/java/com/kinetiq/
    │   ├── common/
    │   │   ├── domain/
    │   │   │   ├── model/Email.java
    │   │   │   ├── exception/DomainException.java, InvalidValueException.java
    │   │   │   └── output/MailSender.java, Mail.java
    │   │   └── infrastructure/
    │   │       ├── config/SecurityConfig.java (public endpoints), WebConfig.java (locale resolver)
    │   │       ├── input/GlobalExceptionHandler.java, CsrfController.java
    │   │       └── output/BaseEntity.java, LoggingMailSender.java
    │   └── account/
    │       ├── domain/
    │       │   ├── model/Account, AccountId, Role, EmailConfirmation, ConfirmationToken, RawPassword
    │       │   ├── input/SignUpUseCase, SignUpTrainerCommand
    │       │   ├── output/AccountRepository, PasswordHasher
    │       │   ├── service/SignUpService
    │       │   └── exception/ConfirmationLinkInvalidException
    │       └── infrastructure/
    │           ├── config/AccountConfig
    │           ├── input/AccountController, SignUpRequest, ConfirmEmailRequest, ConfirmationLinkRequest
    │           └── output/AccountEntity, JpaAccountRepository, AccountRepositoryImpl, AccountMapper, Pbkdf2PasswordHasher
    ├── main/resources/
    │   ├── application.yaml                  kinetiq.app-url, messages
    │   ├── application-local.yaml            adds the demo data location (R10)
    │   ├── db/dev-data/R__demo_accounts.sql
    │   ├── messages.properties, messages_pl.properties
    │   └── db/migration/V1__create_accounts.sql, V2__add_email_confirmation.sql
    └── test/java/com/kinetiq/...             tests listed below

frontend/src/
├── app/router.ts                             /sign-up, /confirm-email
├── components/ui/                            card, field, input, input-group, checkbox, label, spinner (shadcn CLI)
├── features/account/
│   ├── api/account.ts                        useSignUp, useConfirmEmail, useRequestConfirmationLink
│   ├── components/auth-layout.tsx, check-email-card.tsx
│   ├── pages/sign-up-page.tsx, confirm-email-page.tsx (+ .test.tsx)
│   └── sign-up-schema.ts (+ .test.ts)
├── lib/api/client.ts                         CSRF + Accept-Language middleware, late-bound fetch
├── lib/api/problem.ts                        error code → translation key
├── lib/i18n.ts                               start language from navigator.language, sync <html lang>
└── locales/en.json, pl.json
```

**Structure Decision**: Feature `account` on both sides, matching `CONTEXT.md`'s Account term (added with this plan). Shared pieces needed by later features (email, mail port, error handler, base entity) go into `common` now because the issues name them as shared.

## Work per task (one commit each)

| Task | Commit | Contents |
|---|---|---|
| #3 | `feat(backend): add account model and first migration (#3)` | `Email`, `DomainException`, `InvalidValueException`, `Account` (sign-up and invariants only), `AccountId`, `Role`, `BaseEntity`, `AccountEntity`, mapper, `AccountRepository` + impl (`save`, `findByEmail`), `V1`, `local` profile and demo data, README step |
| #4 | `feat: add global error handler with stable error codes (#4)` | `GlobalExceptionHandler`, problem-detail entry point and access-denied handler in `SecurityConfig` (R3), `problem.ts`, `errors.*` keys in EN and PL |
| #5 | `feat(backend): add outgoing-mail port with logging adapter (#5)` | `MailSender`, `Mail`, `LoggingMailSender` with `kinetiq.mail.log-content` (R10a), `messages*.properties`, `kinetiq.app-url` |
| #6 | `feat(backend): add sign-up api (#6)` | `RawPassword`, `ConfirmationToken`, `EmailConfirmation`, `PasswordHasher` + PBKDF2 adapter, `SignUpService.signUpTrainer`, `AccountController` sign-up, `CsrfController`, `WebConfig`, security permits, `V2`, `schema.d.ts` |
| #7 | `feat(backend): add email confirmation and new-link api (#7)` | `confirmEmail`, `sendConfirmationLink`, `findByConfirmationTokenHash`, two endpoints, `schema.d.ts` |
| #8 | `feat(frontend): add sign-up and email confirmation screens (#8)` | Form libraries, shadcn components, client middleware, i18n start language, pages with five states, busy and focus handling (FR-026, FR-027), locales, browser verification |

Spec Kit files go in a first `docs: add spec for trainer sign-up` commit together with the `CONTEXT.md` term.

## Tests

| Test | Covers |
|---|---|
| `EmailTest` | normalisation, pattern, 254 limit |
| `RawPasswordTest` | 7/8/128/129 characters; each missing class; `toString` hides the value |
| `AccountTest` | sign-up starts unconfirmed; confirmation replaces earlier; confirm once; expired at exactly 24 h; confirmed account cannot issue; 60-second mail window |
| `SignUpServiceTest` (fakes for repository, hasher, mail; fixed `Clock`) | new email → account + link mail; existing unconfirmed → new link, old one fails; existing confirmed → account-exists mail, account unchanged; hashing happens on both paths; within 60 s → no mail; confirm valid / used / expired / replaced / unknown; resend for unconfirmed / confirmed / unknown |
| `AccountControllerTest` (`@WebMvcTest`) | 202 / 204 statuses; field codes per rule; malformed JSON; CSRF missing → 403; locale passed from `Accept-Language`; invalid link code |
| `GlobalExceptionHandlerTest` (`@WebMvcTest` with a test controller) | unknown exception → 500 without internals and with `correlationId`; domain exception code; built-in exception codes and no `detail`; missing CSRF token → 403 `request.forbidden` |
| `LoggingMailSenderTest` | recipient and link logged only when `log-content` is on |
| `AccountRepositoryImplIT` | save and find by email; unique email; find by token hash; version and timestamps set |
| `KinetiqBackendApplicationIT` | extended: public sign-up is reachable anonymously with CSRF, everything else still 401 |
| `sign-up-schema.test.ts` | password rule and field rules match the server |
| `sign-up-page.test.tsx` | messages per field; server `errors` mapped to fields; focus on first error; busy button blocks a second submit; network failure → generic message, values kept; success moves focus to the check-state heading; "Change it" keeps values but not password; resend disabled with its note |
| `confirm-email-page.test.tsx` | "Confirming your email" while pending; token read from the fragment and confirmed once; success state; invalid → expired state → new link → check state |

## Follow-ups outside this story

- #23 shrinks to 401 handling in the client: the CSRF header and problem-detail bodies for 401 and 403 are done here (R2, R3).
- #65 must send mail outside the request to keep response times equal (R6, SC-002).
- #24 builds `/sign-in`; until then "Sign in" leads to the router's not-found page.
- #19 should check why an anonymous `401` currently creates a session (`JSESSIONID` on `GET /api/v1/x`).

## Complexity Tracking

None.
