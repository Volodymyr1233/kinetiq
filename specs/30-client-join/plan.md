# Implementation Plan: Client joins through an invite

**Branch**: `feat/30-client-join` | **Date**: 2026-10-26 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/30-client-join/spec.md`

## Summary

An invited person opens the invite link, sees who invited them and their email, enters name and password, confirms being 16 or older and consents to processing health data, and becomes a signed-in client of that trainer. The backend adds the client's trainer and the consent time to `accounts`, two public endpoints (check an invite, accept it) in the `account` feature, and reuses sign-up's confirmation email for joins with a copied link. The frontend adds the public `/join` page (checking, form, "You're in", "This link no longer works"), the confirm-email banner in the client app, and makes "Email confirmed" role-neutral. One pull request, one commit per task #31–#34, in that order.

## Technical Context

**Language/Version**: Java 25; TypeScript 6

**Primary Dependencies**: Spring Boot 4.1 (MVC, Security 7, Data JPA, Flyway, Validation), MapStruct, Lombok; React 19, React Router 8, TanStack Query 5, react-i18next, react-hook-form, zod, shadcn/ui. No new dependency or shadcn component (`checkbox`, `field`, `card`, `alert`, `avatar`, `spinner` exist).

**Storage**: PostgreSQL 18; two columns on `accounts` (data-model.md)

**Testing**: JUnit 5 + AssertJ (unit with fakes and a fixed `Clock`, `@WebMvcTest`, `@DataJpaTest` + Testcontainers), ArchUnit; Vitest + Testing Library with a stubbed `fetch`

**Target Platform**: Browser SPA, same origin as the API

**Project Type**: Web application (`backend/` + `frontend/`)

**Performance Goals**: Normal web expectations; one password hash per successful join

**Constraints**: Invalid links indistinguishable (one code); secrets only in the URL fragment and request bodies; no server text in the UI; client screens at 375px

**Scale/Scope**: 1 page with 5 states, 1 banner, 2 new endpoints, 1 changed response, 2 migrations

## Constitution Check

| Principle | Status |
|---|---|
| I. Hexagonal boundaries | Pass. `Account`, `Invite` and `SignUpService` stay framework-free. The race translation (R6) and the shared sign-in (R7) are input-adapter code using Spring types only there. No new feature, no new dependency between features. |
| II. One contract | Pass. `schema.d.ts` regenerated in the commits that change the API (#32, #34). `invite.link-invalid` and the field codes are translated by code in EN and PL. |
| III. Secure by default | Pass. Two new public endpoints, justified in the spec's "Public endpoints": no account exists yet; both need a 256-bit secret and reveal nothing for a link that does not work. Session id and CSRF token rotate on join (R7). Password hashed (existing PBKDF2), never logged. No secret in a URL query or log, except the local mail log. |
| IV. Plan changes | Not affected. |
| V. Tests verify behaviour | Pass. Tests below target each link-invalid case, emailed vs copied secret, starting settings, the trainer link, the race, field errors, the banner lifecycle and the page states. |
| VI. Simplicity | Pass. Join added to the existing sign-up use case (R1); a column instead of a link table (R2); existing confirmation-link endpoint for Resend (R11); no limiter (R5). Shared code extracted only at its second use (R7, R10). `not valid` checks marked `ponytail:` (R3). |
| VII. One design system | Pass. `AuthLayout`/`AuthCard`, shadcn `Checkbox`/`Field`/`Avatar`, banner from tokens; board copy kept; EN and PL; browser check at 375px and 1280px, light and dark. |

Re-checked after design: unchanged.

## Project Structure

### Documentation (this feature)

```text
specs/30-client-join/
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
backend/src/main/
├── resources/db/migration/
│   ├── V10__add_client_trainer.sql                         #31
│   └── V11__add_health_data_consent.sql                    #32
├── resources/db/dev-data/R__demo_accounts.sql               #31, #32 (demo client's trainer, consent)
└── java/com/kinetiq/account/
    ├── domain/model/Account.java                            #31 trainerId; #32 joinAsClient, consent
    ├── domain/model/Invite.java                             #32 provesEmail
    ├── domain/exception/InviteLinkInvalidException.java     #32
    ├── domain/input/SignUpUseCase.java                      #32 checkInvite, joinAsClient
    ├── domain/input/JoinCommand.java                        #32
    ├── domain/input/InviteDetails.java                      #32 (email, trainer name)
    ├── domain/output/AccountRepository.java                 #31 findClientsOf
    ├── domain/service/SignUpService.java                    #32; #34 confirmation mail on copied link
    ├── infrastructure/output/AccountEntity, AccountMapper,
    │   JpaAccountRepository, AccountRepositoryImpl          #31, #32
    └── infrastructure/input/
        ├── SignIns.java                                     #32 (from SignInController)
        ├── SignInController.java                            #32 uses SignIns
        ├── InviteAcceptanceController.java                  #32 both endpoints, race translation
        ├── InviteCheckRequest/Response, AcceptInviteRequest #32
        └── CurrentAccountResponse.java                      #34 emailConfirmed
backend/src/main/java/com/kinetiq/common/infrastructure/config/SecurityConfig.java   #32 two public POSTs

frontend/src/
├── lib/api/schema.d.ts                                      #32, #34 (generated)
├── app/router.ts                                            #33 /join
├── app/client-area.tsx                                      #34 banner
├── features/account/
│   ├── api/account.ts                                       #33 useInviteCheck, useJoin
│   ├── sign-up-schema.ts                                    #33 nameSchema, passwordSchema, joinSchema
│   ├── field-errors.ts                                      #33 (from sign-up-page)
│   ├── pages/join-page.tsx (+ test)                         #33
│   ├── pages/sign-up-page.tsx                               #33 uses field-errors
│   ├── pages/confirm-email-page.tsx (+ test)                #34 neutral copy, Continue
│   └── components/confirm-email-banner.tsx (+ test)         #34
└── locales/en.json, pl.json                                 #33, #34
```

**Structure Decision**: Existing web-application layout; all work in the `account` feature on both sides, plus the client-area shell for the banner.

## Commits (one per task)

1. **#31** `feat(backend): link each client to one trainer (#31)`: V10, `Account.trainerId`, entity and mapper, `findClientsOf`, demo client linked. Tests: `AccountRepositoryImplIT` (a trainer's clients are exactly theirs, ordered; a client row without trainer is refused by the database).
2. **#32** `feat: add check-invite and accept-invite api (#32)`: V11, `joinAsClient`, `provesEmail`, `InviteLinkInvalidException`, use case and controller, `SignIns`, security, regenerated `schema.d.ts`, `invite.link-invalid` copy. Tests: `SignUpServiceTest` (each invalid case incl. taken email, emailed secret confirms, copied leaves unconfirmed, starting settings from trainer and locale, invite used, consent time, no hash for an invalid link), `AccountTest` (joinAsClient invariants), `InviteAcceptanceControllerTest` (public access, field codes, `400 invite.link-invalid`, race → link-invalid, session established), `AccountRepositoryImplIT` (consent persisted).
3. **#33** `feat(frontend): add join page and invalid-link page (#33)`: `/join`, states, schema, field errors shared with sign-up, privacy link, EN/PL. Tests: `join-page.test.tsx` (form shows email and trainer; client-side errors; server field errors; link-invalid page from check and from submit; check failure with Try again; "You're in" after join and "Start the quiz" target).
4. **#34** `feat: confirm the email after joining with a copied link (#34)`: confirmation mail on copied-link join, `emailConfirmed` in the response, regenerated types, banner in `ClientArea`, neutral "Email confirmed" with Continue. Tests: `SignUpServiceTest` (copied link mails a confirmation link), `CurrentAccountControllerTest` (field present), `confirm-email-banner.test.tsx` (shown only unconfirmed; Resend → Sent, disabled, announced), `confirm-email-page.test.tsx` (Continue when signed in, Sign in otherwise).

Every commit runs the quality gate of each side it touches.

## Complexity Tracking

None.
