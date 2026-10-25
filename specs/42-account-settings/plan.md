# Implementation Plan: Trainer and client change their settings

**Branch**: `feat/43-account-settings` (first task; later tasks use their own branches) | **Date**: 2026-10-25 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/42-account-settings/spec.md`, building on `specs/18-sign-in-out` and `specs/50-client-quiz`

## Summary

Units, language and theme become part of the account, with one partial update for the signed-in account and the app applying them wherever it holds the current account (task #43). The trainer gets a settings screen that also changes the name (task #44), the client a settings sheet that saves each choice at once (task #45), and every weight is shown and entered in the chosen unit through one shared module (task #46). One pull request per task, squash merged; the first one also carries the Spec Kit files. Tasks #44, #45 and #46 are planned in outline and refined when each starts.

## Technical Context

**Language/Version**: Java 25; TypeScript 6

**Primary Dependencies**: Spring Boot 4.1 (MVC, Security 7, Data JPA, Flyway, Validation), MapStruct, Lombok; React 19, React Router 8, TanStack Query 5, react-i18next, react-hook-form and zod (task #44), shadcn/ui (`ToggleGroup`, and `Select` added by task #44). No new library.

**Storage**: Three columns on `accounts` (data-model.md); `localStorage` on the device for the last language and theme.

**Testing**: JUnit 5 + AssertJ (unit with a fake repository, `@WebMvcTest`, `@DataJpaTest` + Testcontainers); Vitest + Testing Library with a stubbed `fetch`

**Target Platform**: Browser SPA (trainer at 1280 px, client at 375 px); REST API under `/api/v1`, same origin

**Project Type**: Web application; task #43 touches `backend/` and `frontend/`

**Performance Goals**: SC-002: a choice in the client sheet shows within 1 second

**Constraints**: Only the signed-in account is read or changed; weights always stored in kg; a merged migration is never edited

**Scale/Scope**: 3 settings, 2 operations (one extended), 1 trainer screen, 1 client sheet, 1 shared weight module

## Constitution Check

| Principle | Status |
|---|---|
| I. Hexagonal boundaries | Pass. The settings live in the `account` feature's domain (`AccountSettings`, three enums), changed through `CurrentAccountUseCase`; no new feature, no new dependency between features. |
| II. One contract | Pass. Task #43 regenerates `schema.d.ts` in its pull request; task #44 again when it adds `name` and `email`. Refusals are problem details with stable codes (`validation.failed`, `account.name-not-changeable`). |
| III. Secure by default | Pass. No public endpoint; both operations address only the signed-in account, so no other person's settings can be read or changed. Nothing is sent to an LLM. |
| IV. Plan changes | Not affected. |
| V. Tests verify behaviour | Pass. The test list below covers starting values, partial updates, refused values, the role rule for names, the round trip of weights and what the app shows after sign-in. |
| VI. Simplicity | Pass. Three columns, one partial update, one observer of the cached account (research R5), one weight module (R9). |
| VII. One design system | Pass. shadcn components and tokens; EN and PL; browser check at 1280 px and 375 px, light and dark. |

Re-checked after design: unchanged.

### Definition of Done items that do not apply

- *Another person's resource answers 404*: no operation addresses an account by id; both act on the signed-in account only (FR-004), so another person's settings cannot be named.
- *New entities get demo data*: no new entity. The column defaults give the demo accounts their settings (data-model.md).

## Project Structure

### Documentation (this feature)

```text
specs/42-account-settings/
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
backend/src/main/java/com/kinetiq/account/
├── domain/model/        AccountSettings, WeightUnit, Language, Theme (#43); Account: settings, changeSettings, rename (#44)
├── domain/input/        CurrentAccountUseCase.update with UpdateAccountCommand (#43: settings; #44: name)
├── domain/service/      CurrentAccountService (#43); SignUpService: starting language (#43)
├── domain/exception/    NameNotChangeableException (#44)
└── infrastructure/      input: CurrentAccountController PATCH, UpdateAccountRequest, CurrentAccountResponse, AccountWebMapper (#43);
                         output: AccountEntity, AccountMapper (#43)
backend/src/main/resources/db/migration/V6__add_account_settings.sql   (#43, number per research R10)

frontend/src/
├── app/                 the observer that applies language and theme (#43); client-area.tsx sheet (#45); router.ts settings route (#44)
├── lib/i18n.ts          starts with the remembered language (#43)
├── lib/weight.ts        conversion, formatting, parsing (+ .test.ts) (#46)
├── features/account/    api/account.ts update mutation (#44 or #45); pages/settings-page.tsx (#44); components/ display settings (#45)
├── features/quiz/       body weight and review in the person's unit (#46)
└── locales/en.json, pl.json   settings texts (#44, #45), weight texts (#46)
```

**Structure Decision**: The settings belong to the `account` feature on both sides, where the current account already lives; the weight module is shared, so it lives in `lib/`.

## Work per task (one pull request each, squash merged)

| Task | Pull request title | Contents |
|---|---|---|
| #43 | `feat: store settings with the account` | Spec Kit files of story #42; domain settings and their tests; migration; persistence; `GET` extended and `PATCH` with validation and tests; sign-up's starting language; `schema.d.ts`; the app applying language and theme from the cached account and remembering the language on the device; browser check per quickstart.md |
| #44 | `feat: add trainer settings screen` | `name` and `email` in the contract with the role rule; the settings page with form, validation, partial save and "Settings saved."; texts in EN and PL; tests |
| #45 | `feat(frontend): add display settings to the client sheet` | The sheet's units, language and theme with immediate save and revert on failure; texts; tests |
| #46 | `feat(frontend): show and enter weights in the chosen unit` | `lib/weight.ts` with unit tests; the unit context; the quiz's body weight and the review in the person's unit; texts; tests |

Task order: #43 first; #44, #45 and #46 each need only #43.

## Tests

| Test | Covers |
|---|---|
| `AccountTest` (#43, #44) | A new trainer starts with KG, the sign-up language and SYSTEM; `changeSettings` replaces them; `rename` follows the name rules and is refused for a client (#44) |
| `CurrentAccountServiceTest` (#43, fake repository) | Only the fields sent change; nothing sent changes nothing; an unknown account is refused |
| `SignUpServiceTest` (#43) | A Polish sign-up stores `PL`, any other `EN` |
| `CurrentAccountControllerTest` (#43, #44) | `GET` returns the settings; `PATCH` `200` with the new account; each field's `pattern` code; `401` without a session; `403` for a client's name (#44) |
| `AccountRepositoryImplIT` (#43) | Round trip of the settings; the database refuses a value outside its check |
| App test (#43) | Signed in with `PL` and `DARK`, the app shows Polish and the dark theme over the device's earlier choice; after sign-out the device keeps them |
| `i18n` start (#43) | The remembered language wins over the browser's; without one, the browser's |
| Settings page tests (#44) | Fields show the stored values; "Save changes" disabled until a change; only changed fields sent; "Settings saved."; name messages; failure keeps changes |
| Sheet tests (#45) | A choice applies and sends one field; failure puts the old value back and shows the message |
| `weight.test.ts` (#46) | kg ↔ lb, formatting per language, parsing with dot and comma, the round trip for 30.0–250.0 kg |
| Quiz tests (#46) | The body weight in lb, its bounds message in lb, the note naming the other unit, the review in lb |

## Open items

| Item | Task | How it is settled |
|---|---|---|
| A client's starting settings when joining (story #30) were asked of the owner of story #30 | #43 | If task #32 is merged before #43, #43 sets them at join; otherwise task #32 does |
| Migration number `V6` | #43 | Checked on `master` before merging (research R10) |
| `email` in the current account | #44, #45 | Added by whichever of the two starts first |

## Complexity Tracking

None.
