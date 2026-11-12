# Implementation Plan: Personal and health data are protected

**Branch**: `feat/167-data-protection` | **Date**: 2026-11-12 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/167-data-protection/spec.md`

## Summary

#168 writes `docs/adr/0005-data-inventory-and-encryption.md` (inventory, retention, why TLS, host disk encryption and hashing instead of column encryption) and adds `RetentionSweep`, which every 10 minutes erases trainer accounts unconfirmed 7 days after their last link and invites ended over an hour ago. #169 adds the public `/privacy` page, its text in English and Polish under `privacy.*`, and the links from sign-up and both settings screens; the join form's existing link starts working (bug #242). #170 makes erasure one delete of the account row: migration `V27` recreates every foreign key that hangs data off an account with `on delete cascade`, except a client's link to their trainer, which stays restrictive and so refuses a trainer with clients even in a race. `AccountDeletionService` checks the sign-in lock and the password, erases the client's invites from their trainer, deletes the account and ends every sign-in; `GET`/`POST /api/v1/accounts/me/deletion` expose it, and an IT proves no column of any table still holds the client. #171 adds `DeleteAccountDialog` to the trainer Settings page and the client sheet, and the sign-in page's "Your account was deleted.". No dependency is added.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1) backend; TypeScript, React 19 frontend

**Primary Dependencies**: Existing only: Spring MVC, Spring Data JPA, Spring Session JDBC (`FindByIndexNameSessionRepository` through `AccountSessions`), Spring Security `LogoutHandler`, `@Scheduled`; React Router 8 (router state), TanStack Query, react-i18next (`Trans`, `returnObjects`), shadcn `alert-dialog`, `card`, `alert`, `button`, `field` (all present), Lucide

**Storage**: PostgreSQL; one migration, `V27__erase_with_account.sql`, that only changes foreign-key actions ([data-model.md](data-model.md))

**Testing**: JUnit 5 + AssertJ (services with fake ports), `@WebMvcTest` (controller), `@DataJpaTest` with Testcontainers (`AccountErasureIT`, repository deletes, retention selection), Vitest + Testing Library (frontend). Docker must be running for the backend gate.

**Target Platform**: Browser; client screens at 375 px, trainer screens at 1280 px, the notice at both; same-origin `/api/v1`

**Project Type**: Web application (backend + frontend in one repository)

**Performance Goals**: A deletion is one transaction of a handful of statements; the sweep makes two indexed deletes per tick.

**Constraints**: No feature may call another feature's erase code (dependency cycle, research R1); one sign-in lock per email; erasure is all or nothing; no personal data in logs; every string in English and Polish; the notice readable without a sign-in.

**Scale/Scope**: One account at a time; tens of tables reached by cascade; one public page.

## Constitution Check

*GATE: passes before research; re-checked after design.*

| Principle | Check | Result |
|---|---|---|
| I. Hexagonal boundaries | All new code is in `account`: `AccountDeletionService`, `RetentionService` (domain, plain classes), their ports on `AccountRepository` and `InviteRepository`, `AccountDeletionController` and `RetentionSweep` (input adapters), JPA queries (output adapters). Other features change only through the database's cascades and one guard in `PlanDraftingJob` that uses its own port. No new cross-feature import; `HexagonalArchitectureTest` runs in `check`. | Pass |
| II. One contract | Two endpoints and one changed behaviour, documented in [contracts/api.md](contracts/api.md); `schema.d.ts` regenerated in the commit of #170. New codes `account.password-incorrect` and `account.has-clients` get `errors.*` texts in both languages; the frontend never shows backend text. | Pass |
| III. Secure by default | Both endpoints need a sign-in and act only on the signed-in account (no id in the path), so nobody can delete another person; the password and the shared lock guard against a stolen sign-in. `/privacy` is a frontend route with no server data, justified in the spec. No error carries internals. Nothing new reaches an LLM; a retry after a deletion is stopped (R8). | Pass |
| IV. Explainable plan changes | Not touched; plan changes of an erased client are erased with them. | n/a |
| V. Tests verify behaviour | Service tests: lock, wrong password counted, right password clears failures, trainer with clients refused, client's invites erased, sign-ins ended last. `AccountErasureIT` proves nothing of the client remains and controls are unchanged (SC-001). Retention IT for each boundary. Controller tests for codes and validation. Frontend tests for the dialog, its errors, the sections, the notice route and links, the sign-in notice. No getter tests. | Pass |
| VI. Simplicity | Database cascades instead of a listener per feature; one small read endpoint; one dialog for both roles; a static page from locale files; no dependency (R1, R4, R10, R11). | Pass |
| VII. One design system | `AlertDialog`, `Card`, `Alert`, `Button` (`destructive`, `ghost`), `Field` from shadcn with Kinetiq tokens; checked in the browser, light and dark, at 375 px and 1280 px, in English and Polish, keyboard and labels. | Pass |

No violations, so no Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/167-data-protection/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── api.md
├── checklists/
│   └── requirements.md
└── tasks.md             # /speckit-tasks
```

### Source Code (repository root)

```text
docs/adr/0005-data-inventory-and-encryption.md               # #168
CONTEXT.md                                                   # #169: + Privacy notice; #170: + Account deletion

backend/src/main/resources/db/
├── migration/V27__erase_with_account.sql                    # #170
└── dev-data/R__demo_accounts.sql                            # #168: the unconfirmed demo trainer survives the sweep

backend/src/main/java/com/kinetiq/account/
├── domain/
│   ├── input/{EraseExpiredDataUseCase,DeleteAccountUseCase,DeletionCheck}.java   # #168, #170
│   ├── output/{AccountRepository,InviteRepository}.java               # #168, #170: deletes and a count
│   ├── exception/{PasswordIncorrectException,TrainerHasClientsException}.java   # #170
│   └── service/{RetentionService,AccountDeletionService}.java         # #168, #170
└── infrastructure/
    ├── config/AccountConfig.java                                      # #168, #170: wiring
    ├── input/RetentionSweep.java                                      # #168
    ├── input/{AccountDeletionController,DeleteAccountRequest,AccountDeletionResponse}.java   # #170
    ├── input/InviteAcceptanceController.java                          # #170: a trainer gone mid-join
    └── output/{AccountRepositoryImpl,JpaAccountRepository,InviteRepositoryImpl,JpaInviteRepository}.java   # #168, #170
backend/src/main/java/com/kinetiq/plan/domain/service/PlanDraftingJob.java   # #170: no retry for an erased client
backend/src/test/java/com/kinetiq/account/...                # RetentionServiceTest, AccountDeletionServiceTest, AccountDeletionControllerTest, RetentionIT, AccountErasureIT (+ fixture SQL)
backend/src/test/java/com/kinetiq/plan/...                   # PlanDraftingJobTest: the retry guard

frontend/src/
├── lib/api/schema.d.ts                                      # #170: regenerated
├── app/privacy-page.tsx                                     # #169: new
├── app/router.ts                                            # #169: /privacy
├── app/settings-sheet.tsx                                   # #169: notice link; #171: Delete account
├── features/account/
│   ├── api/account.ts                                       # #171: deletion query and mutation, shared sign-out clean-up
│   ├── components/delete-account-dialog.tsx                 # #171: new
│   ├── pages/sign-up-page.tsx                               # #169: "How we handle your data"
│   ├── pages/settings-page.tsx                              # #169: notice link; #171: Delete account section
│   └── pages/sign-in-page.tsx                               # #171: "Your account was deleted."
└── locales/{en,pl}.json                                     # #169: privacy.*; #170: errors; #171: texts
```

**Structure Decision**: Everything is the `account` feature's on both sides, plus the public page in `app/` beside `home-page.tsx` and `not-found-page.tsx`. Other features are reached only through the database's cascades (backend) and through the shared sign-out clean-up already used by `useSignOut` (frontend).

## Phases

One pull request for the story, one commit per task, in this order. It closes #167, #168, #169, #170, #171 and bug #242.

- **#168 (ADR, retention)**: the ADR with the inventory and the encryption decision; `RetentionService`, its two repository deletes, `RetentionSweep`; the demo trainer's link; tests at each boundary (research R7, R12).
- **#169 (frontend)**: `/privacy`, its English and Polish text, links from sign-up and both settings screens, the join form's link working; `CONTEXT.md` + Privacy notice; tests for the route without a sign-in and the links' new-tab behaviour.
- **#170 (backend, generated types)**: `V27`, the deletion service and endpoints, the join race, the drafting retry guard, `AccountErasureIT`, controller and service tests, `schema.d.ts`, the error texts, `CONTEXT.md` + Account deletion.
- **#171 (frontend)**: the dialog, the trainer section and the client button, the sign-in notice, the shared clean-up; tests. Blocked by #170.

After each frontend task: the browser check of `frontend/CLAUDE.md`, light and dark, 375 px (client and notice) and 1280 px (trainer and notice), English and Polish, keyboard and labels. Before opening the pull request: `gh pr list --json number,files` for another `V27`.
