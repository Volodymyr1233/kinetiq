---
description: "Tasks for story #2, Trainer signs up"
---

# Tasks: Trainer signs up

**Input**: `specs/2-trainer-sign-up/` (spec.md, plan.md, research.md, data-model.md, contracts/api.md, quickstart.md)

**Tests**: Required by the Definition of Done (`CLAUDE.md`) and principle V. Tests sit in the same phase as the code they cover and are written to fail first where practical.

**Organization**: Phases follow the GitHub tasks, not the spec's user stories, because the pull request carries one commit per task (`CLAUDE.md`, Workflow). Tasks #3–#5 are the foundation every story needs. #6 delivers the backend of US1, #7 the backend of US2 and US3, and #8 the screens of US1–US4. Story labels show which spec story a task serves.

**Paths**: `B` = `backend/src/main/java/com/kinetiq`, `BT` = `backend/src/test/java/com/kinetiq`, `R` = `backend/src/main/resources`, `F` = `frontend/src`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: spec user story (US1 sign up, US2 confirm, US3 new link, US4 errors in the person's language)

---

## Phase 1: Setup — spec commit

- [X] T001 Commit `specs/2-trainer-sign-up/` and the `Account` and `Confirmation link` terms in `CONTEXT.md` as `docs: add spec for trainer sign-up`

---

## Phase 2: Foundational — task #3, account model and first migration

**Goal**: The `accounts` table exists, Hibernate validates it on start, and an account can be saved and found by email in any letter case.

- [X] T002 [P] Create `DomainException` (abstract, unchecked, `String code()`, message for logs only) and `InvalidValueException` (code `validation.failed`) in `B/common/domain/exception/`
- [X] T003 [P] Create `Email` record in `B/common/domain/model/Email.java`: compact constructor trims, lower-cases with `Locale.ROOT`, requires `^[^\s@]+@[^\s@]+\.[^\s@]+$` and "at most 254 characters", else throws `InvalidValueException`
- [X] T004 [P] Write `BT/common/domain/model/EmailTest.java`: normalisation (`" Marek@Example.com "` → `marek@example.com`), malformed values, 254 accepted / 255 rejected
- [X] T005 Create `AccountId` (record wrapping `UUID`, `random()`), `Role` (`TRAINER`, `CLIENT`) and `Account` aggregate in `B/account/domain/model/`: `@Getter`, no setters; `static Account signUpTrainer(String name, Email email, String passwordHash, ZoneId timeZone)` enforcing name "Trimmed, 1–100 characters, no control characters (Unicode category `Cc`)" via `\p{Cc}`, role `TRAINER`, `emailConfirmed` false; a `restore(...)` factory for persistence
- [X] T006 [P] Write `BT/account/domain/model/AccountTest.java` (`@Nested` per method): name trimmed, blank / 101 characters / line break rejected, new trainer is unconfirmed
- [X] T007 [P] Create `BaseEntity` (`@MappedSuperclass`, `Instant createdAt` and `updatedAt` set by Hibernate, `@Version Long version`) in `B/common/infrastructure/output/BaseEntity.java`
- [X] T008 [P] Write `R/db/migration/V1__create_accounts.sql` exactly as data-model.md "Table `accounts`": `pk_accounts`, `uq_accounts_email`, `ck_accounts_email_lower_case` (`email = lower(email)`), `ck_accounts_role` (`TRAINER`, `CLIENT`), `name varchar(100)`, `email varchar(254)`, `password_hash varchar(255)`, `role varchar(16)`, `time_zone varchar(64)`, `email_confirmed boolean`, `created_at`/`updated_at timestamptz`, `version bigint`, all not null
- [X] T009 Create `AccountRepository` port (`save`, `Optional<Account> findByEmail(Email)`) in `B/account/domain/output/`, and `AccountEntity` (table `accounts`, `@Id UUID`, role as string), `JpaAccountRepository`, `AccountMapper` (MapStruct, typed ids and `ZoneId` ↔ string) and `AccountRepositoryImpl` (`@Repository`) in `B/account/infrastructure/output/`
- [X] T010 Write `BT/account/infrastructure/output/AccountRepositoryImplIT.java` (`@DataJpaTest`, `TestcontainersConfiguration`): saves and finds by email given in another case; second account with the same email fails; `createdAt`, `updatedAt`, `version` set
- [X] T011 Add the `local` profile: `R/application-local.yaml` with `spring.flyway.locations: classpath:db/migration,classpath:db/dev-data`; `backend/build.gradle.kts` `bootRun` passes `--spring.profiles.active=local`; `R/db/dev-data/R__demo_accounts.sql` inserts `demo.trainer@example.com` (confirmed) and `demo.unconfirmed@example.com` (unconfirmed) with a PBKDF2 hash of `Kinetiq-demo-1` (R1 parameters), `on conflict do nothing`; README: IntelliJ "Active profiles: local" and the demo accounts
- [X] T012 Run `./gradlew check` and `./gradlew bootRun` (V1 and demo data applied, Hibernate validates); commit `feat(backend): add account model and first migration (#3)`

---

## Phase 3: Foundational — task #4, global error handler with stable codes

**Goal**: Every error is a problem detail with a `code`; the frontend turns codes into EN/PL text.

- [ ] T013 Create `GlobalExceptionHandler` in `B/common/infrastructure/input/` extending `ResponseEntityExceptionHandler` (research R3): `handleExceptionInternal` sets `code` by status (`request.invalid`, `request.not-found`, `request.method-not-allowed`, `request.unsupported-media-type`) and clears `detail`; `handleMethodArgumentNotValid` → 400 `validation.failed` with `errors: [{field, code}]`, code = `FieldError.getCode()` in kebab case; `DomainException` → 400 with its code; any other exception → 500 `internal.error` with `correlationId`, exception logged with that id only
- [ ] T014 In `B/common/infrastructure/config/SecurityConfig.java` replace `HttpStatusEntryPoint` with an entry point and an access-denied handler that write problem details: 401 `request.unauthenticated`, 403 `request.forbidden` (covers a missing CSRF token)
- [ ] T015 Write `BT/common/infrastructure/input/GlobalExceptionHandlerTest.java` (`@WebMvcTest` with a test-only controller in `BT`): unknown exception → 500 without exception text, with `correlationId`; domain exception → its code; malformed JSON → `request.invalid` without `detail`; field errors listed; POST without CSRF → 403 `request.forbidden`; anonymous → 401 `request.unauthenticated`
- [ ] T016 [P] Create `F/lib/api/problem.ts`: `errorMessageKey(error: unknown): string` → `errors.<code>` when the key exists in i18n, else `errors.generic` (also for no body, no code, network failure); `F/lib/api/problem.test.ts`
- [ ] T017 [P] Add `errors.*` keys to `F/locales/en.json` and `F/locales/pl.json`: `generic`, `validation.failed`, `request.invalid`, `request.not-found`, `request.method-not-allowed`, `request.unsupported-media-type`, `request.unauthenticated`, `request.forbidden`, `internal.error`
- [ ] T018 Run `./gradlew check` and `npm run check`; commit `feat: add global error handler with stable error codes (#4)`

---

## Phase 4: Foundational — task #5, outgoing-mail port with logging adapter

**Goal**: Domain code sends mail only through a port; locally the link can be copied from the log.

- [ ] T019 [P] Create `Mail` record (`Email to`, `String template`, `Locale locale`, `Map<String, String> values`) and `MailSender` port (`void send(Mail mail)`) in `B/common/domain/output/`
- [ ] T020 Create `LoggingMailSender` (`@Component`) in `B/common/infrastructure/output/`: subject from `MessageSource` key `mail.<template>.subject`; with `kinetiq.mail.log-content=true` logs recipient, subject and `values.link` at INFO, otherwise logs only the template name (research R10a)
- [ ] T021 [P] Add `R/messages.properties` and `R/messages_pl.properties` with `mail.confirm-email.subject` (`Confirm your Kinetiq account` / `Potwierdź konto w Kinetiq`) and `mail.account-exists.subject` (`You already have a Kinetiq account` / `Masz już konto w Kinetiq`); in `R/application.yaml` add `spring.messages.fallback-to-system-locale: false` and `kinetiq.app-url: ${KINETIQ_APP_URL:http://localhost:5173}`; in `R/application-local.yaml` add `kinetiq.mail.log-content: true`
- [ ] T022 Write `BT/common/infrastructure/output/LoggingMailSenderTest.java` (`OutputCaptureExtension`): recipient and link logged only when `log-content` is on; Polish subject for `pl`
- [ ] T023 Run `./gradlew check`; commit `feat(backend): add outgoing-mail port with logging adapter (#5)`

**Checkpoint**: foundation ready; stories can start.

---

## Phase 5: Task #6, sign-up API (US1 backend)

**Goal**: `POST /api/v1/accounts` creates an unconfirmed trainer and sends a link, or sends the right email to an existing account, with an identical response.

**Independent test**: quickstart scenarios 3, 7 and 9 against the API alone.

- [ ] T024 [P] [US1] Create `RawPassword` in `B/account/domain/model/`: "8–128 characters with at least one `a–z`, one `A–Z`, one `0–9` and one character that is none of those", length in UTF-16 units (R9), `toString()` returns `***`
- [ ] T025 [P] [US1] Create `ConfirmationToken` (32 bytes from `SecureRandom`, base64url without padding, `hash()` = SHA-256 hex) and `EmailConfirmation` (`tokenHash`, `expiresAt`) in `B/account/domain/model/`
- [ ] T026 [US1] Extend `Account` with `confirmation` (optional) and `mailSentAt` (optional) and `canReceiveMail(now)` (empty or ≥ 60 s ago), `issueConfirmation(token, now)` (only unconfirmed; replaces any earlier one, `expiresAt = now + 24 h`, `mailSentAt = now`), `recordMailSent(now)`; update `restore(...)`
- [ ] T027 [P] [US1] Write `BT/account/domain/model/RawPasswordTest.java` (7/8/128/129 characters, each missing class, spaces and `ą` count as "other", `toString` hides) and extend `AccountTest` (`issueConfirmation` replaces; refused when confirmed; 60-second window at 59 s and 60 s)
- [ ] T028 [P] [US1] Write `R/db/migration/V2__add_email_confirmation.sql`: `confirmation_token_hash char(64)` with `uq_accounts_confirmation_token_hash`, `confirmation_expires_at timestamptz`, `mail_sent_at timestamptz`, all null, and `ck_accounts_confirmation` (hash and expiry both null or both set); map the columns in `AccountEntity` and `AccountMapper`
- [ ] T029 [P] [US1] Create `PasswordHasher` port (`String hash(RawPassword)`) in `B/account/domain/output/` and `Pbkdf2PasswordHasher` in `B/account/infrastructure/output/` using `DelegatingPasswordEncoder("pbkdf2", Pbkdf2PasswordEncoder("", 16, 600_000, PBKDF2WithHmacSHA256))` (R1)
- [ ] T030 [US1] Create `SignUpUseCase` with `void signUpTrainer(SignUpTrainerCommand)` and `SignUpTrainerCommand(String name, Email email, RawPassword password, ZoneId timeZone, Locale locale)` in `B/account/domain/input/`; `SignUpService` in `B/account/domain/service/` (constructor: repository, hasher, mail sender, `Clock`, `URI appUrl`): hash first (R6), then new email → `signUpTrainer` + `issueConfirmation` + `confirm-email` mail with `link = appUrl/confirm-email#token=<token>`; existing unconfirmed → if `canReceiveMail`, `issueConfirmation` + same mail; existing confirmed → if `canReceiveMail`, `recordMailSent` + `account-exists` mail with `link = appUrl/sign-in`; a `ponytail:` comment notes that hashing inside the use-case transaction holds a database connection for its duration, and that hashing moves before the transaction if sign-up load ever matters
- [ ] T031 [US1] Write `BT/account/domain/service/SignUpServiceTest.java` with hand-written fakes (in-memory repository, recording hasher and mail sender) and a fixed `Clock`: each branch above; hasher called on every branch; within 60 s no mail; old link hash replaced
- [ ] T032 [US1] Create `AccountConfig` in `B/account/infrastructure/config/` registering `SignUpService` (app URL from `kinetiq.app-url`, `Clock.systemUTC()`); create `WebConfig` in `B/common/infrastructure/config/` with an `AcceptHeaderLocaleResolver` limited to `en`, `pl`, default `en` (R4); create `CsrfController` in `B/common/infrastructure/input/` (`GET /api/v1/csrf` → 204)
- [ ] T033 [US1] Create `AccountController` and `SignUpRequest` in `B/account/infrastructure/input/`: `POST /api/v1/accounts` → 202; request annotations per contracts/api.md (`@NotBlank @Size(max=100) @Pattern("^\P{Cc}*$")` name, `@NotBlank @Pattern("^\s*[^\s@]+@[^\s@]+\.[^\s@]+\s*$")` email with its 254 limit checked by `Email` after trimming, `@NotNull @Size(min=8,max=128) @Pattern(rules)` password, `@AssertTrue` adult, `@NotBlank` timeZone); a zone outside `ZoneId.getAvailableZoneIds()` → `InvalidValueException`; `Locale` from the resolver into the command. Permit `POST /api/v1/accounts` and `GET /api/v1/csrf` in `SecurityConfig`
- [ ] T034 [US1] Write `BT/account/infrastructure/input/AccountControllerTest.java` (`@WebMvcTest`, use case mocked): 202 and the command passed (email with surrounding spaces normalised, `pl` locale); each field rule gives its field code; unknown time zone → `validation.failed`; missing CSRF → 403
- [ ] T035 [US1] Extend `BT/KinetiqBackendApplicationIT.java`: anonymous sign-up with CSRF cookie and header → 202, and other endpoints still 401
- [ ] T036 [US1] Start the backend, run `npm run gen:api` in `frontend/`, keep the regenerated `F/lib/api/schema.d.ts`; run `./gradlew check` and `npm run check`; commit `feat(backend): add sign-up api (#6)`

---

## Phase 6: Task #7, email confirmation and new-link API (US2, US3 backend)

**Goal**: A valid token confirms once; any other gives one code. A new link always answers the same and invalidates earlier ones.

**Independent test**: quickstart scenarios 4–6 and 8 against the API alone.

- [ ] T037 [US2] Add `Account.confirmEmail(now)` (valid only while `now` is before `expiresAt`; sets `emailConfirmed`, removes the confirmation) and `ConfirmationLinkInvalidException` (code `account.confirmation-link-invalid`) in `B/account/domain/exception/`; extend `AccountTest` (confirm once, exactly 24 h is expired, confirmed has no confirmation)
- [ ] T038 [US2] Add `Optional<Account> findByConfirmationTokenHash(String)` to `AccountRepository`, `JpaAccountRepository` and `AccountRepositoryImpl`; extend `AccountRepositoryImplIT`
- [ ] T039 [US2] [US3] Add `void confirmEmail(String token)` and `void sendConfirmationLink(Email email, Locale locale)` to `SignUpUseCase` and `SignUpService`: unknown hash → `ConfirmationLinkInvalidException`; new link only for an unconfirmed account that `canReceiveMail`, otherwise nothing
- [ ] T040 [US2] [US3] Extend `SignUpServiceTest`: confirm valid / used / expired / replaced / unknown; new link for unconfirmed, confirmed, unknown, within 60 s
- [ ] T041 [US2] [US3] Add `POST /api/v1/email-confirmations` (`ConfirmEmailRequest`, `@NotBlank token`, → 204) and `POST /api/v1/confirmation-links` (`ConfirmationLinkRequest`, email rules as in T033, → 202) to `AccountController`; permit both in `SecurityConfig`; extend `AccountControllerTest` (statuses, invalid link code, blank token, malformed email, email with surrounding spaces accepted); add `errors.account.confirmation-link-invalid` to `F/locales/en.json` and `F/locales/pl.json`
- [ ] T042 [US2] Regenerate `F/lib/api/schema.d.ts`; run `./gradlew check` and `npm run check`; commit `feat(backend): add email confirmation and new-link api (#7)`

---

## Phase 7: Task #8, sign-up and email confirmation screens (US1–US4 UI)

**Goal**: The five states, built from the design system, in EN and PL, light and dark, 375–1280 px.

**Independent test**: quickstart scenarios 1–11 in the built-in browser.

- [ ] T043 Install `react-hook-form@^7.90`, `zod@^4.6`, `@hookform/resolvers@^5.9` in `frontend/`
- [ ] T044 Add shadcn components with the CLI after reading `npx shadcn@latest docs` for each: `card`, `field`, `input`, `input-group`, `checkbox`, `label`, `spinner`, `alert`; read the added files in `F/components/ui/` and compare with the design system (`kq.css` `.auth`, `.auth-card`, `.icon-chip`, `.brandmark`, touch inputs)
- [ ] T045 [US4] Update `F/lib/api/client.ts`: late-bound `fetch` (R12); middleware sets `Accept-Language` to `i18n.language` and, for POST/PUT/PATCH/DELETE, `X-XSRF-TOKEN` from the `XSRF-TOKEN` cookie, calling `GET /api/v1/csrf` first when it is missing (R2); `F/lib/api/client.test.ts`
- [ ] T046 [P] [US4] Update `F/lib/i18n.ts`: start language `pl` when `navigator.language` starts with `pl`, else `en`; keep `document.documentElement.lang` in sync on `languageChanged`
- [ ] T047 [P] [US1] Create `F/features/account/sign-up-schema.ts` (zod; password rule as an exported function matching FR-002; name trimmed, 1–100, no `/\p{Cc}/u`; email trimmed then the R8 pattern; `adult` true) and `F/features/account/sign-up-schema.test.ts`
- [ ] T048 [P] [US1] Create `F/features/account/api/account.ts`: `useSignUp`, `useConfirmEmail`, `useRequestConfirmationLink` mutations over `api`
- [ ] T049 [P] [US1] Create `F/features/account/components/auth-layout.tsx` (brandmark, centred card column, footer slot) and `check-email-card.tsx` (heading focused on mount, the address, Resend with busy state and a 60-second lock plus one polite live note, "Change it" callback)
- [ ] T050 [US1] Create `F/features/account/pages/sign-up-page.tsx`: react-hook-form (`onSubmit` mode, `onChange` revalidation, `shouldFocusError`); fields per FR-025 copy; password show/hide with `aria-label`; rules text as description that becomes the error; time zone from `Intl` with `UTC` fallback; busy submit (FR-026); server `errors` → `setError`; other failures → alert with `errorMessageKey`; success → `CheckEmailCard`; "Change it" restores values without the password
- [ ] T051 [US2] [US3] Create `F/features/account/pages/confirm-email-page.tsx`: read `#token`, clear it with `history.replaceState`, confirm once (ref guard, R5); states "Confirming your email" (busy), "Email confirmed" (Sign in → `/sign-in`), "This link no longer works" (email field + "Send new link" → `CheckEmailCard`, footer "Already confirmed? Sign in"); focus to each new heading (FR-027)
- [ ] T052 [US1] Add `/sign-up` and `/confirm-email` to `F/app/router.ts`
- [ ] T053 [P] [US4] Add all copy for the five states to `F/locales/en.json` and `F/locales/pl.json` (board copy, FR-025 messages, Resend note, confirming state), with no hard-coded strings left
- [ ] T054 [US1] Write `F/features/account/pages/sign-up-page.test.tsx` with stubbed `fetch`: messages per field; server field errors; focus on first error; busy button blocks a second submit; network failure keeps values; success focuses the check heading; "Change it" drops only the password; resend lock and note
- [ ] T055 [US2] [US3] Write `F/features/account/pages/confirm-email-page.test.tsx` with stubbed `fetch`: confirming state; one request despite StrictMode; success; invalid → expired → new link → check state
- [ ] T056 Run `npm run check`; verify in the built-in browser per quickstart (light and dark, 375 and 1280 px, 200% zoom, EN and PL, keyboard only, console clean), screenshots of each state; review with the ui-ux-pro-max skill for WCAG 2.2 AA and UX issues without restyling the design system; fix findings; commit `feat(frontend): add sign-up and email confirmation screens (#8)`

---

## Phase 8: Polish

- [ ] T057 Run quickstart end to end with backend and frontend; measure SC-002 (20 sign-ups each for an existing and a new email, compare medians) and record the result in the pull request
- [ ] T058 Run a security review of the branch diff (enumeration, token handling, CSRF, logs, error bodies) and fix findings in the commit of the task they belong to
- [ ] T059 Ask before pushing; then push `feat/2-trainer-sign-up` and open one pull request closing #2–#8, to be merged with rebase or a merge commit

---

## Dependencies

- T001 → #3 (T002–T012) → #4 (T013–T018) and #5 (T019–T023) → #6 (T024–T036) → #7 (T037–T042) → #8 (T043–T056) → T057–T059.
- #4 and #5 touch different files and can be built in either order; their commits stay in issue order.
- #8 needs the regenerated `schema.d.ts` from #6 and #7.

## Parallel examples

- #3: T002, T003, T007, T008 together; then T004 and T006 alongside T009.
- #6: T024, T025, T028, T029 together; T027 once T024–T026 exist.
- #8: T046, T047, T048, T049, T053 together after T045.

## Implementation strategy

MVP is US1 end to end (#3–#6 plus the sign-up page of #8). It is not shippable alone, because an account nobody can confirm is useless, so the story merges only when #7 and #8 are done. Each task ends with green gates and its own commit, so review can follow the commits one by one.
