---
description: "Tasks for story #42, Trainer and client change their settings"
---

# Tasks: Trainer and client change their settings

**Input**: `specs/42-account-settings/` (spec.md, plan.md, research.md, data-model.md, contracts/api.md, quickstart.md), building on `specs/18-sign-in-out` and `specs/50-client-quiz`

**Tests**: Required by the Definition of Done (`CLAUDE.md`) and principle V. Tests sit in the same phase as the code they cover.

**Organization**: Phases follow the GitHub tasks, not the spec's user stories, because each task is its own pull request, squash merged (`CLAUDE.md`, Workflow). #43 builds the stored settings and applies them (US1), #44 the trainer screen (US2), #45 the client sheet (US3), #46 the weights in the chosen unit (US4). Story labels show which spec story a task serves.

**Paths**: `B` = `backend/src/main/java/com/kinetiq/account`, `BT` = `backend/src/test/java/com/kinetiq/account`, `R` = `backend/src/main/resources`, `F` = `frontend/src`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: spec user story (US1 settings follow the person, US2 trainer screen, US3 client sheet, US4 weights in the chosen unit)

---

## Phase 1: Setup

- [X] T001 Commit `specs/42-account-settings/` (spec, checklist, plan, research, data model, contract, quickstart, tasks) with the code of task #43, so the spec is reviewed in that pull request. `CONTEXT.md` needs no new term: units, language and theme are not training vocabulary

---

## Phase 2: task #43, settings stored with the account (US1)

**Goal**: Units, language and theme on every account, read with the current account and changed with one partial update; the app shows the account's language and theme after sign-in on any device.

**Coordination**: a client's starting settings at join belong to task #32 (plan, "Open items"). Before opening the pull request, check on `master` whether task #32 is merged; if it is, this task also sets them where task #32 creates the client: the trainer's units, the language of the join request and `SYSTEM` (spec, Clarifications).

- [X] T002 [P] [US1] Create in `B/domain/model/`: enums `WeightUnit` (`KG`, `LB`), `Theme` (`LIGHT`, `DARK`, `SYSTEM`) and `Language` (`EN`, `PL`) with `static Language from(Locale locale)` ("Polish gives `PL`, anything else `EN`", data-model.md); record `AccountSettings(WeightUnit units, Language language, Theme theme)` with every field required, `static AccountSettings startingWith(Language language)` (`KG`, the language, `SYSTEM`) and `AccountSettings with(WeightUnit units, Language language, Theme theme)`, which keeps the stored value for each argument that is null (research R3)
- [X] T003 [P] [US1] Write `BT/domain/model/AccountSettingsTest.java` (`@Nested` per method): `startingWith` gives `KG`, the language and `SYSTEM`; `with` changes only the non-null arguments, and with all null returns equal settings; a missing field in the constructor is refused; and `BT/domain/model/LanguageTest.java`: `pl` and `pl-PL` give `PL`, `en`, `de` and `Locale.ROOT` give `EN`
- [X] T004 [US1] In `B/domain/model/Account.java`: a `settings` field; `signUpTrainer(name, email, passwordHash, timeZone, language)` starts with `AccountSettings.startingWith(language)`; `restore(...)` takes the settings; `changeSettings(AccountSettings settings)` replaces them (not null). Update every caller of `restore` and `signUpTrainer` in main and test sources
- [X] T005 [US1] Extend `BT/domain/model/AccountTest.java`: a new trainer starts with `KG`, the given language and `SYSTEM`; `changeSettings` replaces the settings and refuses null
- [X] T006 [US1] In `B/domain/service/SignUpService.java` pass `Language.from(command.locale())` to `Account.signUpTrainer` (research R4); extend `BT/domain/service/SignUpServiceTest.java`: a sign-up with locale `pl` stores `PL`, one with `en` stores `EN`
- [X] T007 [US1] Create `B/domain/input/UpdateAccountCommand.java`, a record `(WeightUnit units, Language language, Theme theme)` whose null fields keep the stored values (Javadoc says so); add `Account update(AccountId id, UpdateAccountCommand command)` to `B/domain/input/CurrentAccountUseCase.java` and implement it in `B/domain/service/CurrentAccountService.java`: find the account, `changeSettings(account.getSettings().with(...))`, save, return it
- [X] T008 [US1] Extend `BT/domain/service/CurrentAccountServiceTest.java` (hand-written fake repository): only the fields sent change and are saved; a command with every field null leaves the settings as they were; an unknown account is refused with `AccountNotFoundException`
- [X] T009 [US1] Write `R/db/migration/V6__add_account_settings.sql` with the number free on `master` (research R10), exactly as data-model.md "Table `accounts`": `units varchar(2) not null default 'KG'` with `ck_accounts_units` in (`KG`, `LB`); `language varchar(2) not null default 'EN'` with `ck_accounts_language` in (`EN`, `PL`); `theme varchar(6) not null default 'SYSTEM'` with `ck_accounts_theme` in (`LIGHT`, `DARK`, `SYSTEM`)
- [X] T010 [US1] In `B/infrastructure/output/AccountEntity.java` add `units`, `language` and `theme` with `@Enumerated(EnumType.STRING)`; in `B/infrastructure/output/AccountMapper.java` build `AccountSettings` in `toDomain` and map `settings.units`, `settings.language` and `settings.theme` in `update`
- [X] T011 [US1] Extend `BT/infrastructure/output/AccountRepositoryImplIT.java`: settings survive a save and a load; a row inserted without the three columns reads as `KG`, `EN`, `SYSTEM`; the database refuses `units = 'XX'`
- [X] T012 [US1] In `B/infrastructure/input/`: create `UpdateAccountRequest(@Pattern(regexp = "KG|LB") String units, @Pattern(regexp = "EN|PL") String language, @Pattern(regexp = "LIGHT|DARK|SYSTEM") String theme)` (contracts/api.md); add `@NotNull WeightUnit units`, `@NotNull Language language` and `@NotNull Theme theme` to `CurrentAccountResponse` and drop "Settings join with task #43" from its Javadoc; map the settings and the request (a missing value stays null) in `AccountWebMapper`; add `@PatchMapping` to `CurrentAccountController` taking `@Valid @RequestBody UpdateAccountRequest` and answering `200` with `CurrentAccountResponse`
- [X] T013 [US1] Extend `BT/infrastructure/input/CurrentAccountControllerTest.java` (`@WebMvcTest`, use case mocked): `GET` answers `units`, `language` and `theme`; `PATCH` with `{"theme":"DARK"}` answers `200` with the account the use case returns and passes a command with only the theme set; a wrong `units`, `language` or `theme` answers `400` `validation.failed` with that field and the code `pattern`; `PATCH` without a session answers `401`
- [X] T014 [US1] Run `./gradlew check` in `backend/`
- [X] T015 [US1] Start the backend, run `npm run gen:api` in `frontend/` and check that `F/lib/api/schema.d.ts` gained only the three fields and the `PATCH` operation; add `units: 'KG'`, `language: 'EN'` and `theme: 'SYSTEM'` to both accounts in `SIGNED_IN` of `F/test/page.tsx`
- [X] T016 [P] [US1] In `F/lib/i18n.ts` start with the language stored under `kinetiq-language` when it is `en` or `pl`, otherwise with the browser's as before, and store every language change in the existing `languageChanged` listener; reading and writing storage never throws (storage may be unavailable, as `F/features/quiz/quiz-draft.ts` handles). Replace the comment "no language switcher yet" (research R5)
- [X] T017 [US1] Write `F/lib/i18n.test.ts` for the exported `startLanguage()` of T016 (the start-up choice as a plain function, since i18next is one shared instance): a stored `pl` wins over an English browser; a stored unknown value is ignored; with nothing stored, a `pl-PL` browser gives `pl` and an `en-US` one `en`; and a language change is stored
- [X] T018 [US1] Create `F/app/account-settings-sync.tsx`: `AccountSettingsSync` renders nothing, observes `useQuery({ ...currentAccountQuery, enabled: false })` and, when the account's language (`EN`/`PL` as `en`/`pl`) or theme (as `light`/`dark`/`system`) differs from what the app shows, calls `i18n.changeLanguage` or the theme context's `setTheme` (research R5); mount it in `F/app/app.tsx` inside `QueryClientProvider`
- [X] T019 [US1] Write `F/app/account-settings-sync.test.tsx` (`ThemeProvider` and a fresh `QueryClientProvider` around `AccountSettingsSync`, `fetch` stubbed): with the device on `en` and `light`, putting an account with `PL` and `DARK` into the cache shows Polish and the dark theme and remembers `pl`; clearing the cache (sign-out) keeps them; with no account in the cache no request is sent
- [X] T020 [US1] Verify in the browser per `quickstart.md`, "Browser check, task #43", steps 1–6, at 1280 px (trainer) and 375 px (client); take screenshots
- [X] T021 Run `npm run check` in `frontend/` and `./gradlew check` in `backend/`; open the pull request for #43 after the user agrees (title `feat: store settings with the account`, `Closes #43`)

---

## Phase 3: task #44, trainer settings screen (US2)

**Blocked by**: task #43 (and #10, closed; both linked on GitHub). Re-read the merged code and refine T022–T029 before starting.

**Goal**: `/trainer/settings` as on the board "Trainer · settings": name, read-only email, units, language and appearance, saved together with "Save changes".

- [ ] T022 [US2] In `B/domain/model/Account.java` add `rename(String name)` with the sign-up name rules (`validName`), refused for a client by `B/domain/exception/NameNotChangeableException.java` (kind `NOT_ALLOWED`, code `account.name-not-changeable`, research R6); add `name` to `UpdateAccountCommand` and to `CurrentAccountService.update`; extend `AccountTest` and `CurrentAccountServiceTest`
- [ ] T023 [US2] Add `name` to `UpdateAccountRequest` with the annotations of `SignUpRequest.name` but allowed to be absent, and `@NotNull String email` to `CurrentAccountResponse`; extend `CurrentAccountControllerTest` (name codes `not-blank`, `size`, `pattern`; `403` `account.name-not-changeable`); `./gradlew check`; `npm run gen:api`
- [ ] T024 [P] [US2] Add `useUpdateAccount()` to `F/features/account/api/account.ts`: a mutation over `PATCH /api/v1/accounts/me` that puts the answer into the cached current account, throwing the problem detail on error as the other hooks there do
- [ ] T025 [P] [US2] Fetch the docs with `npx shadcn@latest docs select` and `toggle-group`, add the select in `frontend/` with `npx shadcn@latest add select`, and read `F/components/ui/select.tsx` as added
- [ ] T026 [US2] Create `F/features/account/pages/settings-page.tsx` (research R7) and route `/trainer/settings` to it in `F/app/router.ts` instead of the placeholder: react-hook-form with zod, the name rules shared with `F/features/account/sign-up-schema.ts`; Profile (Name, Email read-only) and Display (Units and Appearance as `ToggleGroup`s, Language as `Select`) with the board's hints except "Change it in your account."; "Save changes" disabled until a value differs; only changed fields sent; "Settings saved." in a `role="status"` region; failures by code; texts `settings.*` in `F/locales/en.json` and `F/locales/pl.json`
- [ ] T027 [US2] Write `F/features/account/pages/settings-page.test.tsx`: stored values shown; "Save changes" disabled, then enabled after a change; only changed fields sent; "Settings saved." after success; a blank name shows "Enter your name."; a failure keeps the changes and shows the message
- [ ] T028 [US2] Verify in the browser per `quickstart.md` for task #44, at 1280 px, light and dark, English and Polish, keyboard only, console
- [ ] T029 Run the gates; open the pull request for #44 after the user agrees (title `feat: add trainer settings screen`, `Closes #44`), naming in "What changed" that the board's "Change it in your account." has no issue behind it

---

## Phase 4: task #45, client settings sheet (US3)

**Blocked by**: task #43 (and #11, closed; both linked on GitHub). Uses `useUpdateAccount()` (T024) and `email` (T023); if task #44 is not merged when this task starts, those two tasks are done here. Refine T030–T033 before starting.

**Goal**: The sheet as on the board "Today · settings sheet": units, language and theme that save at once.

- [ ] T030 [US3] Create `F/features/account/components/display-settings.tsx` (research R8): Units, Language and Theme as `ToggleGroup`s with 48 px items (`frontend/CLAUDE.md`: client touch targets) and the board's hints ("Weights in your plan and logs show in kg." naming the chosen unit, "System follows your phone."); a choice updates the cached current account at once and sends that one field with `useUpdateAccount()`; on failure the earlier account is put back and a translated message shows; texts in `F/locales/en.json` and `F/locales/pl.json`
- [ ] T031 [US3] In `F/app/client-area.tsx` `SettingsSheet`: the email under the name, `DisplaySettings`, "Changes save right away." above "Sign out"; remove the comment "units, language and theme join the sheet with task #45"
- [ ] T032 [US3] Write `F/features/account/components/display-settings.test.tsx` and extend `F/app/client-area.test.tsx`: a choice applies at once and sends one field; a failure puts the old value back and shows the message; the sheet shows name, email and "Sign out"
- [ ] T033 In `specs/50-client-quiz/quickstart.md` and `specs/54-quiz-review-submit/quickstart.md`, step 9, replace "Set the browser's language to Polish and reload" with choosing Polski in the settings sheet, since a signed-in client's language comes from the account since task #43; verify in the browser per `quickstart.md` for task #45 at 375 px, light and dark, English and Polish, keyboard only, console; run `npm run check`; open the pull request for #45 after the user agrees (title `feat(frontend): add display settings to the client sheet`, `Closes #45`)

---

## Phase 5: task #46, weights in the chosen unit (US4)

**Blocked by**: task #43 (linked on GitHub). T034–T038 are refined to the code of task #43 and spec 54's review.

**Goal**: Every weight stored in kg and shown and entered in the person's unit; at the time of this story the quiz's body weight and the review.

- [X] T034 [P] [US4] Create `F/lib/weight.ts` (research R9, data-model.md "Weight") with the type `WeightUnit` (the account's `units`, from `F/lib/api/schema.d.ts`), `KG_PER_LB = 0.45359237`, `parseWeight(text)` moved from `F/features/quiz/quiz-schema.ts` (a dot or a comma, at most one decimal), `toKg(value, unit)` (kept to 0.1 kg) and `fromKg(kg, unit)` (to 0.1 of the unit); numbers are formatted by the texts' `number` format, as the review already does. Write `F/lib/weight.test.ts`: parsing with both separators, both directions in both units, the bounds 30 kg = 66.1 lb and 250 kg = 551.2 lb, and the round trip for every value from 30.0 to 250.0 kg in 0.1 steps
- [X] T035 [US4] Add `WeightUnitContext` (default `KG`) and `useWeightUnit()` to `F/lib/weight.ts`; create `F/app/weight-unit-provider.tsx`, which reads the cached current account as `AccountSettingsSync` does and provides its `units`, and wrap the router with it in `F/app/app.tsx`; write `F/app/weight-unit-provider.test.tsx` (an account with `LB` gives `LB`, no account gives `KG`)
- [X] T036 [US4] In `F/features/quiz/quiz-schema.ts`: `answersSchemaFor(unit)` replaces `answersSchema`, its weight rule checks 30–250 kg after `toKg` with the message `quiz.about.weight.<KG|LB>.error`; `isStepValid`, `allowedStep` and `isComplete` take the unit (default `KG`); the draft keeps any well-formed weight text, so a draft is not dropped by a unit. The draft holds the weight as typed in the unit shown; the submit of task #57 converts it to kg. In `F/features/quiz/pages/quiz-page.tsx` step 4 shows the unit, the example, the range message and the note of the person's unit (the note names the other unit, spec Clarifications); `F/features/quiz/pages/review-page.tsx` shows "About you" with the unit. Texts: `quiz.about.weight.{KG,LB}.{unit,placeholder,error,note}` and `{{unit}}` in `quiz.review.about`, in `F/locales/en.json` and `F/locales/pl.json` (lb: "e.g. 137.8", "Enter a weight between 66.1 and 551.2 lb.", "Switch to kg later in settings.")
- [X] T037 [US4] Extend `F/features/quiz/quiz-schema.test.ts` (lb bounds 66.1 and 551.2 valid, 66.0 and 551.3 not; "300" valid in lb, not in kg), `F/features/quiz/pages/quiz-page.test.tsx` (step 4 in lb: unit, example, note, range message) and `F/features/quiz/pages/review-page.test.tsx` ("29 years · 168 cm · 137.8 lb"), with kg unchanged
- [X] T038 Verify in the browser per `quickstart.md` for task #46 at 375 px, light and dark, English and Polish; run `npm run check`; open the pull request for #46 after the user agrees (title `feat(frontend): show and enter weights in the chosen unit`, `Closes #46`)

---

## Phase 6: Convergence

- [ ] T039 Run `/speckit-converge` on the branch of the story's last task, add `Closes #42` to that pull request, and walk through `quickstart.md` once more

---

## Dependencies

- T001 with #43. #43 (T002–T021) → #44 (T022–T029), #45 (T030–T033) and #46 (T034–T038), each needing only #43; #45 also needs T023 and T024 from #44 or does them itself. T039 last.
- Inside #43: T002 and T009 are independent; T003 follows T002; T004 needs T002; T005 follows T004; T006 and T007 need T004; T008 follows T007; T010 needs T004 and T009; T011 follows T010; T012 needs T007; T013 follows T012; T014 needs T002–T013; T015 needs T014; T016 is independent of the backend; T017 follows T016; T018 needs T015; T019 follows T018; T020 needs everything before it.
- Inside #44: T024 and T025 are independent; T026 needs T023–T025.
- Inside #46: T035 needs T034; T036 needs T035.

## Parallel examples

- #43: T002, T009 and T016 at once.
- #44: T024 and T025 at once.

## Implementation strategy

Build #43 first and open one pull request per task after its gates and the user's approval. #43 makes the settings real and applies them; #44 and #45 let each role change them; #46 makes the unit visible in every weight. The last of #44, #45 and #46 to merge also closes the story.
