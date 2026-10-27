---
description: "Tasks for story #30, Client joins through an invite"
---

# Tasks: Client joins through an invite

**Input**: `specs/30-client-join/` (spec.md, plan.md, research.md, data-model.md, contracts/api.md, quickstart.md)

**Tests**: Required by the Definition of Done (`CLAUDE.md`) and principle V. Tests sit in the same phase as the code they cover.

**Organization**: Phases follow the GitHub tasks, not the spec's user stories, because the pull request carries one commit per task (`CLAUDE.md`, Workflow): #31 trainer link, #32 API, #33 join page, #34 email confirmation. Story labels show which spec story a task serves.

**Paths**: `B` = `backend/src/main/java/com/kinetiq`, `BT` = `backend/src/test/java/com/kinetiq`, `R` = `backend/src/main/resources`, `F` = `frontend/src`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: spec user story (US1 join, US2 link no longer works, US3 copied link and email confirmation, US4 the trainer has the client)

---

## Phase 1: Setup — spec commit

- [X] T001 On branch `feat/30-client-join` (already created from `master`), commit `specs/30-client-join/` as `docs: add spec for client joins through an invite`

---

## Phase 2: Foundational — task #31, trainer–client link model and migration

**Goal**: Every client account has exactly one trainer; a trainer's clients can be queried.

**Independent test**: `AccountRepositoryImplIT` against PostgreSQL.

- [X] T002 [US4] Write `R/db/migration/V10__add_client_trainer.sql`: `alter table accounts add column trainer_id uuid`, `add constraint fk_accounts_trainer foreign key (trainer_id) references accounts (id)`, `add constraint ck_accounts_client_trainer check ((role = 'CLIENT') = (trainer_id is not null)) not valid`; `create index ix_accounts_trainer_id on accounts (trainer_id)`; a `-- ponytail:` comment above the check: not valid only because local databases hold the demo client from before; `alter table accounts validate constraint ck_accounts_client_trainer` once every database has run the updated demo data (research R3)
- [X] T003 [US4] In `B/account/domain/model/Account.java` add field `private final AccountId trainerId` (nullable, no Lombok getter: `@Getter(AccessLevel.NONE)`) with `public Optional<AccountId> getTrainerId()`; `signUpTrainer` passes `null`; `restore(…)` takes `AccountId trainerId` after `role`; update every `restore` call in main and test sources (`AccountMapper`, `BT/account/domain/model/AccountTest.java`, `BT/account/domain/service/*Test.java`, `BT/account/infrastructure/output/AccountRepositoryImplIT.java` and any other caller found with grep). Client fixtures stored in PostgreSQL must now have a trainer, or `ck_accounts_client_trainer` refuses them: in `BT/KinetiqBackendApplicationIT.java` `saveAccount(email, Role.CLIENT)` first saves a trainer (or takes one as a parameter) and passes its id; in `AccountRepositoryImplIT.shouldStartWithKilogramsEnglishAndDeviceTheme_whenRowHasNoSettings` insert a trainer row first and add `trainer_id` to the client's native insert; grep `BT` for `'CLIENT'` and `Role.CLIENT` and fix any other stored client the same way
- [X] T004 [US4] In `B/account/infrastructure/output/`: `AccountEntity` gains `private UUID trainerId`; `AccountMapper` maps `AccountId` ↔ `UUID` for it (null stays null); `JpaAccountRepository` gains `List<AccountEntity> findByTrainerIdOrderByNameAsc(UUID trainerId)`; `AccountRepositoryImpl` implements the new port method and copies `trainerId` on save as it copies the other fields
- [X] T005 [US4] Add `List<Account> findClientsOf(AccountId trainerId)` with Javadoc ("The trainer's clients, ordered by name.") to `B/account/domain/output/AccountRepository.java`, and implement it in `BT/account/domain/service/InMemoryAccountRepository.java` (filter by trainer id, sort by name)
- [X] T006 [US4] Extend `BT/account/infrastructure/output/AccountRepositoryImplIT.java`: two trainers, clients stored via `Account.restore` with a trainer each → `findClientsOf` returns exactly each trainer's clients ordered by name, and empty for a trainer without clients; a trainer's `trainerId` stays empty after a round trip; inserting a `CLIENT` row without a trainer through `JdbcTemplate` fails with a `DataIntegrityViolationException` on `ck_accounts_client_trainer`
- [X] T007 [P] [US4] In `R/db/dev-data/R__demo_accounts.sql` add `trainer_id` to the insert column list (`'00000000-0000-4000-8000-000000000001'` for the demo client, `null` for trainers) and, after the insert, `update accounts set trainer_id = '00000000-0000-4000-8000-000000000001' where id = '00000000-0000-4000-8000-000000000003' and trainer_id is null;` with a comment that it fixes databases created before V10
- [X] T008 Run `./gradlew check` and `./gradlew bootRun` on an existing local database (V10 and demo data apply, Hibernate validates); commit `feat(backend): link each client to one trainer (#31)`

---

## Phase 3: Task #32, check-invite and accept-invite API (US1, US2, US4 backend)

**Goal**: Two public endpoints: check a link (email and trainer name, or one stable code) and join (client created, linked, consent stored, invite used, email confirmed for the emailed secret, signed in).

**Independent test**: contracts/api.md against the API alone; quickstart scenarios 3, 6, 7 and 12 through the API.

- [X] T009 [US1] Write `R/db/migration/V11__add_health_data_consent.sql`: `alter table accounts add column health_data_consent_at timestamptz`, `add constraint ck_accounts_client_consent check ((role = 'CLIENT') = (health_data_consent_at is not null)) not valid`, with the same `-- ponytail:` note as V10
- [X] T010 [P] [US1] In `R/db/dev-data/R__demo_accounts.sql` add `health_data_consent_at` to the insert (`now()` for the demo client, `null` for trainers) and to the demo client's update (`set … health_data_consent_at = coalesce(health_data_consent_at, now())`, condition `trainer_id is null or health_data_consent_at is null`)
- [X] T011 [US1] In `B/account/domain/model/Account.java`: field `healthDataConsentAt` (nullable, `Optional<Instant> getHealthDataConsentAt()`), added to `restore(…)` after `passwordReset`; `public static Account joinAsClient(String name, Email email, String passwordHash, ZoneId timeZone, AccountSettings settings, AccountId trainerId, Instant consentAt, boolean emailConfirmed)` with Javadoc: role `CLIENT`, `validName(name)`, every argument non-null; update all `restore` callers; `AccountEntity`/`AccountMapper`/`AccountRepositoryImpl` carry `healthDataConsentAt`; every client fixture fixed in T003 also gets a consent time (`Instant` in `restore`, `health_data_consent_at = now()` in the native insert), or `ck_accounts_client_consent` refuses it
- [X] T012 [P] [US1] Add `public boolean provesEmail(SecretToken token)` to `B/account/domain/model/Invite.java` (Javadoc: "Whether the token is the emailed secret, the one that proves control of the address."): `token.hash().equals(emailTokenHash)`
- [X] T013 [P] [US2] Create `B/account/domain/exception/InviteLinkInvalidException.java`: Javadoc "One answer for an expired, used, revoked, replaced or unknown invite link, and one whose email already has an account."; code `invite.link-invalid`; kind default `INVALID`
- [X] T014 [US1] Create in `B/account/domain/input/`: `record InviteDetails(Email email, String trainerName)`; `record JoinCommand(String token, String name, RawPassword password, ZoneId timeZone, Locale locale)`; add to `SignUpUseCase` with Javadoc: `InviteDetails checkInvite(String token)` (changes nothing; refuses with `InviteLinkInvalidException`) and `Account joinAsClient(JoinCommand command)` (creates the client, links the trainer, stores the consent time, uses the invite)
- [X] T015 [US1] [US2] [US4] In `B/account/domain/service/SignUpService.java` add `InviteRepository invites` to the constructor (and `AccountConfig`); private `Invite workingInvite(SecretToken token, Instant now)` = `invites.findByTokenHash(token.hash()).filter(i -> i.isOpen(now) && !accounts.existsByEmail(i.getEmail())).orElseThrow(InviteLinkInvalidException::new)`; `checkInvite` → `InviteDetails(invite.getEmail(), trainer.getName())` with the trainer from `accounts.findById(invite.getTrainerId()).orElseThrow()`; `joinAsClient` → working invite first, then `hasher.hash(command.password())` (comment: hashed only for a working link, so a wrong link costs nothing), settings `new AccountSettings(trainer.getSettings().units(), Language.from(command.locale()), Theme.SYSTEM)`, `Account.joinAsClient(…, now, invite.provesEmail(token))`, `invite.use(now)`, save both, return the account
- [X] T016 [US1] [US2] [US4] Extend `BT/account/domain/service/SignUpServiceTest.java` (`@Nested CheckInvite`, `@Nested JoinAsClient`; `InMemoryInviteRepository`, `MutableClock`, `RecordingPasswordHasher`): check returns email and trainer name for the emailed and the copied secret and changes nothing; check and join refuse with `InviteLinkInvalidException`, change nothing and hash nothing for an expired (at exactly 7 days), used, revoked, superseded, replaced copied link, unknown token, and an invite whose email has an account; join with the emailed secret → `CLIENT`, confirmed, trainer set, units of the trainer (`LB` trainer → `LB`), language from the locale, theme `SYSTEM`, consent time = clock, invite `USED`, a second join with the same token refused; join with the copied secret → unconfirmed
- [X] T017 [P] [US1] Extend `BT/account/domain/model/AccountTest.java`: `joinAsClient` trims the name, refuses blank, 101-character and control-character names like sign-up, and requires a trainer and a consent time
- [X] T018 [US1] Extract `B/account/infrastructure/input/SignIns.java` (`@Component`, package-private, Javadoc "Signs a person in on this browser: a new session id and CSRF token replace the earlier ones, and the account id becomes the principal."): `void start(Account account, HttpServletRequest request, HttpServletResponse response)` with the body now in `SignInController.signIn` after the use case; `SignInController` calls it; `BT/account/infrastructure/input/SignInControllerTest.java` stays green unchanged
- [X] T019 [US1] [US2] Create in `B/account/infrastructure/input/` per contracts/api.md: `InviteCheckRequest(@NotBlank String token)`; `InviteCheckResponse(@NotNull String email, @NotNull String trainerName)`; `AcceptInviteRequest(@NotBlank String token, @NotBlank @Size(max = Account.NAME_MAX_LENGTH) @Pattern(regexp = "^\\P{Cc}*$") String name, @NotNull @Size(min = RawPassword.MIN_LENGTH, max = RawPassword.MAX_LENGTH) @Pattern(regexp = RawPassword.CHARACTER_RULES) String password, @AssertTrue boolean atLeastSixteen, @AssertTrue boolean healthDataConsent, @NotBlank String timeZone)`; `InviteAcceptanceController` (`/api/v1`): `POST /invite-checks` → 200; `POST /invite-acceptances` → 201 `CurrentAccountResponse` via `AccountWebMapper`, `TimeZones.parse`, `Locale` parameter, then `signIns.start(…)`; the use-case call wrapped in `try/catch`: `OptimisticLockingFailureException` → `throw new InviteLinkInvalidException()`; `DataIntegrityViolationException` → `InviteLinkInvalidException` only when `NestedExceptionUtils` or a cause walk finds `org.hibernate.exception.ConstraintViolationException` with `getConstraintName()` equal (ignoring case) to `uq_accounts_email`, otherwise rethrow; one comment: the loser of two simultaneous joins, any other violation is a bug and stays a 500 (research R6); Javadoc on both endpoints
- [X] T020 [US1] In `B/common/infrastructure/config/SecurityConfig.java` add `"/api/v1/invite-checks"` and `"/api/v1/invite-acceptances"` to the public `POST` list and mention joining in its comment
- [X] T021 [US1] [US2] Write `BT/account/infrastructure/input/InviteAcceptanceControllerTest.java` (`@WebMvcTest`, same imports as `InviteControllerTest`, `SignUpUseCase` mocked, real `SignIns` imported): anonymous check → 200 `{email, trainerName}`; anonymous accept → 201 with the account body, session cookie, authenticated session afterwards; command carries trimmed name, time zone and `Accept-Language` locale; blank name, 101 characters, `Kinetiq2026` (no special), `false` for each box, blank time zone, missing token → 400 with field codes per the contract; `InviteLinkInvalidException` → `400 invite.link-invalid` on both; `ObjectOptimisticLockingFailureException` and a `DataIntegrityViolationException` caused by a Hibernate `ConstraintViolationException` named `uq_accounts_email` → `400 invite.link-invalid`; one caused by `ck_accounts_client_trainer` → `500 internal.error`; no CSRF → `403 request.forbidden`; a signed-in trainer who joins becomes the client (principal replaced)
- [X] T022 [US1] [US4] Extend `BT/account/infrastructure/output/AccountRepositoryImplIT.java`: a client from `Account.joinAsClient` round-trips with trainer and consent time; `findClientsOf` includes it; saving and flushing a second account with the same email throws a `DataIntegrityViolationException` whose cause chain holds a Hibernate `ConstraintViolationException` named `uq_accounts_email` (the shape T019 matches)
- [X] T023 [US1] [US2] Extend `BT/KinetiqBackendApplicationIT.java` with the existing helpers (`saveAccount`, `signIn`, `send`, `csrfToken`): two trainers each invite `it.joiner@example.com` with `POST /api/v1/invites`; the token is the part of `$.link` after `#token=`; anonymous check with the first token → 200 with the email; accept → 201 and a `SESSION` cookie; `GET /api/v1/accounts/me` with it → `CLIENT`; the database row has the first trainer's id as `trainer_id` and a consent time; accept again with the first token → `400 invite.link-invalid`; check with the second trainer's token → `400 invite.link-invalid` (email now taken)
- [X] T024 [P] [US2] Add `errors.invite.link-invalid` to `F/locales/en.json` ("This link no longer works. Ask your trainer for a new one.") and `F/locales/pl.json` ("Ten link już nie działa. Poproś trenera o nowy.")
- [X] T025 Start the backend, run `npm run gen:api` in `frontend/`, keep the regenerated `F/lib/api/schema.d.ts`; run `./gradlew check` and `npm run check`; commit `feat: add check-invite and accept-invite api (#32)`

---

## Phase 4: Task #33, join screen and invalid-link page (US1, US2 frontend)

**Goal**: The public `/join` page: checking, check failed, form, "You're in", "This link no longer works".

**Independent test**: quickstart scenarios 1–7, 12–14 in the built-in browser at 375px and 1280px.

- [ ] T026 [P] [US1] In `F/features/account/sign-up-schema.ts` export `nameSchema` and `passwordSchema` (the existing `signUpSchema` fields, now composed from them) and add `joinSchema` = `{ name: nameSchema, password: passwordSchema, atLeastSixteen: z.boolean().refine(v => v, 'account.errors.atLeastSixteen'), healthDataConsent: z.boolean().refine(v => v, 'account.errors.healthDataConsent') }` with `JoinValues`; extend `sign-up-schema.test.ts` for the two booleans
- [ ] T027 [P] [US1] Move `serverFieldErrors` and `serverMessage` from `F/features/account/pages/sign-up-page.tsx` to `F/features/account/field-errors.ts`, generic over the form's field names; `serverMessage` maps `name` + `size` → `account.errors.nameLength`, other `name` → `account.errors.name`, `password` → `account.passwordRules`, any other field → `account.errors.<field>`; sign-up page imports it; `sign-up-page.test.tsx` stays green
- [ ] T028 [US1] [US2] In `F/features/account/api/account.ts` add `inviteCheckQuery(token)` (`queryOptions`, key `['invite-check', token]`, `POST /api/v1/invite-checks`, `retry` only for failures worth retrying via the default) and `useJoin()` (`POST /api/v1/invite-acceptances`, `receive`; `onSuccess`: `clearTabStorage()` and `setQueryData(currentAccountQuery.queryKey, account)`)
- [ ] T029 [US1] [US2] Add route `{ path: '/join', HydrateFallback: BlankWhileLoading, lazy: … JoinPage }` beside `/confirm-email` in `F/app/router.ts`
- [ ] T030 [US1] [US2] Create `F/features/account/pages/join-page.tsx` on `AuthLayout`/`AuthCard`/`AuthFooter`, token from `window.location.hash` (`token` parameter, left in place so a reload works; research R9):
  - no token or `invite.link-invalid` (from the check or the join) → `LinkInvalidState`: `Link2OffIcon`, focused heading "This link no longer works", description "The invite expired, was already used or was cancelled. Ask your trainer for a new one.", outline touch button "Sign in instead" → `/sign-in`, note "Use this if you already joined."
  - check pending → "Checking your invite" with `Spinner` and an `sr-only` `role="status"`, like the confirming state
  - check failed otherwise → `Alert` with `t(errorMessageKey(error))` and "Try again" (`refetch`)
  - form (heading focused when shown): `AccountAvatar` of the trainer name + "{trainerName} invited you"; title "Join {trainerFirst} on Kinetiq", description "Create your account. Then 7 questions, and {trainerFirst} builds your plan."; read-only email `Input` with `FieldDescription` "From your invite. You sign in with it."; name (`autoComplete="name"`, placeholder "e.g. Anna Kowalska"); `PasswordInput` with the rules as description; `Checkbox` "I am 16 or older"; `Checkbox` consent "I agree that Kinetiq processes my health data (sleep, soreness, stress, body weight and training logs) so {trainerFirst} can plan my training." with `<a href="/privacy" target="_blank" rel="noopener">Privacy notice<span className="sr-only"> (opens in a new tab)</span></a>` whose click does not toggle the box; each box is a `Field orientation="horizontal"` whose whole label toggles it, with the label row at least 44px tall (`min-h-11`, FR-022); touch submit "Create account" (busy: disabled, `Spinner`, label kept, fields read-only); footer "Have an account? Sign in"; `react-hook-form` + `zodResolver(joinSchema)`, `mode: 'onSubmit'`, `reValidateMode: 'onChange'`, `shouldFocusError`; server field errors via `field-errors.ts`; other failures in a destructive `Alert`; time zone `browserTimeZone() ?? 'UTC'`
  - joined → `AccountAvatar` (size xl) of the trainer, focused heading "You're in, {clientFirst}", "Answer 7 questions. {trainerFirst} uses them to build your plan.", brand touch button link "Start the quiz" → `/app/quiz`
  - first names: first whitespace-separated word of the trimmed name
- [ ] T031 [P] [US1] [US2] Add EN and PL copy under `account.join.*` (checking, invitedBy, title, description, emailHelp, namePlaceholder, atLeastSixteen, consent, privacyNotice, opensInNewTab, submit, haveAccount, joinedTitle, joinedDescription, startQuiz, invalidTitle, invalidDescription, signInInstead, invalidNote) and `account.errors.atLeastSixteen` ("Confirm that you are 16 or older."), `account.errors.healthDataConsent` ("Agree to this to join. Your plan depends on this data.") to `F/locales/en.json` and `F/locales/pl.json`; Polish informal "ty", interpolation for names
- [ ] T032 [US1] [US2] Write `F/features/account/pages/join-page.test.tsx` (stubbed `fetch`, router with `/join`, `/sign-in`, `/app/quiz`): form shows trainer, email read-only and empty fields; submit empty → four messages, focus on name; server `validation.failed` for `name` `size` → "Use 100 characters or fewer."; check answers `invite.link-invalid` → invalid page and "Sign in instead" leads to sign-in; join answers `invite.link-invalid` → invalid page; no token → invalid page without a request; check fails with 500 → generic message, "Try again" refetches; join success → "You're in, Anna", "Start the quiz" leads to the quiz, request body carries both booleans and the time zone
- [ ] T033 [US1] [US2] Verify in the built-in browser per `frontend/CLAUDE.md`: quickstart 1–7 and 12–13, light and dark, 375px and 1280px, EN and PL, keyboard focus order and visible focus, labels, both checkbox rows measure at least 44px tall and toggle from anywhere on the label, 200% zoom; review with the `ui-ux-pro-max` skill for WCAG 2.2 AA; fix findings here
- [ ] T034 Run `npm run check`; commit `feat(frontend): add join page and invalid-link page (#33)`

---

## Phase 5: Task #34, email confirmation after joining with a copied link (US3)

**Goal**: A copied-link join sends a confirmation link; unconfirmed clients see the non-blocking banner with Resend on the tab pages until they confirm; "Email confirmed" fits both roles.

**Independent test**: quickstart scenarios 8–11 in the built-in browser.

- [ ] T035 [US3] In `B/account/domain/service/SignUpService.java` `joinAsClient`: when the token is not the emailed secret, after saving call the existing `sendConfirmation(account, command.locale(), now)`; extend `SignUpServiceTest`: copied secret → one `confirm-email` mail to the invited email in the join locale whose link token confirms the account via `confirmEmail`; emailed secret → no mail
- [ ] T036 [P] [US3] Add `@NotNull boolean emailConfirmed` to `B/account/infrastructure/input/CurrentAccountResponse.java` (mapped by `AccountWebMapper` from `isEmailConfirmed`); extend `BT/account/infrastructure/input/CurrentAccountControllerTest.java` with the field for a confirmed and an unconfirmed account
- [ ] T037 [US3] Start the backend, run `npm run gen:api`, keep `F/lib/api/schema.d.ts`; add `emailConfirmed` to every test fixture of `CurrentAccount` in `F/**/*.test.tsx`
- [ ] T038 [US3] Create `F/features/account/components/confirm-email-banner.tsx`: reads `currentAccountQuery`; renders nothing unless `emailConfirmed === false`; full-width `bg-muted` strip, `MailIcon` (`aria-hidden`), text "Confirm your email: we sent a link to {email}." (`min-w-0`, wraps), `Button variant="link"` at least 44px tall: "Resend" → `useResendLink().send(email)`; while `locked`: label "Sent", disabled; on success an `sr-only` `role="status"` says "We sent a new link."; a failure shows `t(errorMessageKey(error))` in the status; render it in `F/app/client-area.tsx` directly under the header, inside the 640px column
- [ ] T039 [P] [US3] In `F/features/account/pages/confirm-email-page.tsx`: description `account.confirmed.description` becomes role-neutral (EN "Your email is confirmed.", PL "Twój e-mail jest potwierdzony."); `useQuery({ ...currentAccountQuery, retry: false })` decides the action: account → "Continue" (`account.confirmed.continue`, link to `landingFor(account.role)`), otherwise "Sign in"; on success `invalidateQueries(currentAccountQuery)`
- [ ] T040 [P] [US3] Add EN and PL copy `account.banner.text`, `account.banner.resend`, `account.banner.sent`, `account.banner.resent` ("We sent a new link."), `account.confirmed.continue` to `F/locales/en.json` and `F/locales/pl.json`
- [ ] T041 [US3] Write `F/features/account/components/confirm-email-banner.test.tsx`: unconfirmed → text with the email and "Resend"; confirmed → nothing; "Resend" posts `{email}` to `/api/v1/confirmation-links`, then "Sent" disabled and "We sent a new link." announced; after 60 s (fake timers) "Resend" again; extend `F/app/client-area.test.tsx`: banner on a tab page for an unconfirmed account
- [ ] T042 [US3] Extend `F/features/account/pages/confirm-email-page.test.tsx`: signed-in client → "Continue" to `/app`; anonymous → "Sign in"; the role-neutral description
- [ ] T043 [US3] Verify in the built-in browser: quickstart 8–11, light and dark, 375px and 1280px, EN and PL; banner wraps without horizontal scroll, Resend reachable and focus visible, banner gone after confirming in another tab and returning; review with `ui-ux-pro-max`; fix findings here
- [ ] T044 Run `./gradlew check` and `npm run check`; commit `feat: confirm the email after joining with a copied link (#34)`

---

## Phase 6: Polish

- [ ] T045 Run the whole quickstart end to end, including the Polish and dark pass (scenario 14)
- [ ] T046 Run a security review of the branch diff (both public endpoints reveal nothing for a bad link; no hash work before the link is valid; session and CSRF rotate on join; secrets only in fragment and bodies, not logged; consent and trainer required by the database; no server text in responses) and fix findings in the commit of the task they belong to
- [ ] T047 Before opening the pull request, check `gh pr list --json number,files` for migrations V10/V11 and renumber if taken; ask before pushing; push `feat/30-client-join` and open one pull request closing #30–#34, to be merged with rebase or a merge commit

---

## Dependencies

- T001 → #31 (T002–T008) → #32 (T009–T025) → #33 (T026–T034) and #34 (T035–T044) → T045–T047, matching the GitHub "blocked by" links (#32 needs #31; #33 and #34 need #32). #34 is committed after #33 because both change `F/locales/*.json` and `F/lib/api/schema.d.ts`.
- Inside #31: T003 before T004; T005 before T006.
- Inside #32: T011 needs T009; T015 needs T011–T014; T019 needs T015 and T018; T021 needs T019, T020.
- Inside #33: T030 needs T026–T029; T032 needs T030, T031.
- Inside #34: T038 needs T037; T041 needs T038, T040.

## Parallel examples

- #31: T007 alongside T003–T006.
- #32: T010, T012, T013, T017, T024 together after T009; T018 alongside T015.
- #33: T026, T027, T031 together; T028 alongside them.
- #34: T036, T039, T040 together after T035.

## Implementation strategy

MVP is US1 + US2 end to end: joining from the emailed link and the one invalid-link page (#31, #32, #33). US3 (#34) completes the copied-link path, which the story's criteria require, so the pull request merges only when all four tasks are done. Each task ends with green gates and its own commit, so review can follow the commits one by one.
