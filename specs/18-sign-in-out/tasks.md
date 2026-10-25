---
description: "Tasks for story #18, Trainer and client sign in and out"
---

# Tasks: Trainer and client sign in and out

**Input**: `specs/18-sign-in-out/` (spec.md, plan.md, research.md, data-model.md, contracts/api.md, quickstart.md)

**Tests**: Required by the Definition of Done (`CLAUDE.md`) and principle V. Tests sit in the same phase as the code they cover and are written to fail first where practical.

**Organization**: Phases follow the GitHub tasks, not the spec's user stories, because the pull request carries one commit per task (`CLAUDE.md`, Workflow). #19 is the foundation; #20–#22 are the backend, #23–#25 the frontend. Story labels show which spec story a task serves.

**Paths**: `B` = `backend/src/main/java/com/kinetiq`, `BT` = `backend/src/test/java/com/kinetiq`, `R` = `backend/src/main/resources`, `F` = `frontend/src`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: spec user story (US1 sign in and land, US2 wrong credentials and repeated attempts, US3 stay signed in, US4 areas per role and return path, US5 sign out)

---

## Phase 1: Setup — spec commit

- [X] T001 Commit `specs/18-sign-in-out/` and the `Sign-in` term in `CONTEXT.md` as `docs: add spec for sign in and out`

---

## Phase 2: Foundational — task #19, sign-ins stored in the database

**Goal**: Sign-ins live in PostgreSQL with a 14-day idle timeout and a hardened cookie; anonymous requests store nothing.

**Independent test**: a saved sign-in is a row in `spring_session`; an anonymous `401` adds none.

- [X] T002 [US3] Add `implementation("org.springframework.boot:spring-boot-starter-session-jdbc")` to `backend/build.gradle.kts` (confirm the artifact name for Boot 4.1 through context7 first)
- [X] T003 [P] [US3] Write `R/db/migration/V5__create_spring_session.sql` from Spring Session's `schema-postgresql.sql` in lower case, exactly as data-model.md "Sign-in": `spring_session` (`primary_id char(36)`, `session_id char(36)`, `creation_time bigint`, `last_access_time bigint`, `max_inactive_interval int`, `expiry_time bigint` all not null, `principal_name varchar(100)` null), `spring_session_pk`, unique `spring_session_ix1 (session_id)`, `spring_session_ix2 (expiry_time)`, `spring_session_ix3 (principal_name)`; `spring_session_attributes` (`session_primary_id char(36)`, `attribute_name varchar(200)`, `attribute_bytes bytea`, all not null), `spring_session_attributes_pk (session_primary_id, attribute_name)`, `spring_session_attributes_fk` → `spring_session(primary_id)` on delete cascade
- [X] T004 [P] [US3] In `R/application.yaml` set `spring.session.timeout: 14d`, `spring.session.jdbc.initialize-schema: never`, and `server.servlet.session.cookie`: `http-only: true`, `same-site: lax`, `secure: true`, `max-age: 400d` with a `ponytail:` comment (R2: the cookie is written only at sign-in, so a sign-in used daily ends after 400 days; rewriting it per response is the upgrade path); in `R/application-local.yaml` set `server.servlet.session.cookie.secure: false` (the Vite proxy is plain http)
- [X] T005 [US3] In `B/common/infrastructure/config/SecurityConfig.java` add `.requestCache(cache -> cache.requestCache(new NullRequestCache()))` with a comment on why (R3: the saved request would create a stored sign-in per anonymous request)
- [X] T006 [US3] Extend `BT/KinetiqBackendApplicationIT.java`: a session created and saved through the `SessionRepository` bean appears in `spring_session` (count through `JdbcClient`); an anonymous `GET /api/v1/anything` answers 401, sets no `SESSION` cookie and adds no row
- [X] T007 Run `./gradlew check` and `./gradlew bootRun` (V5 applied, Hibernate still validates); commit `feat(backend): store sign-ins in the database (#19)`

---

## Phase 3: Task #20, sign-in and sign-out API (US1, US4, US5 backend)

**Goal**: `POST /api/v1/sign-ins` signs in with equal answers for unknown email and wrong password, refuses unconfirmed trainers after the right password, rotates session id and CSRF token; `DELETE /api/v1/sign-ins/current` ends the sign-in; clients cannot list exercises.

**Independent test**: quickstart scenarios 1, 2, 4 and the "checks by request" against the API alone.

- [X] T008 [P] Add to `B/common/domain/exception/DomainException.java` a nested `enum Kind { INVALID, UNAUTHENTICATED, NOT_ALLOWED, TOO_MANY_ATTEMPTS }`, `public Kind kind()` (default `INVALID`) and `public Map<String, Object> properties()` (default `Map.of()`); in `B/common/infrastructure/input/GlobalExceptionHandler.java` map the kind to 400 / 401 / 403 / 429 and copy `properties()` into the problem; extend `BT/common/infrastructure/input/GlobalExceptionHandlerTest.java` with one test exception per kind and one with a property
- [X] T009 [P] [US1] Create in `B/account/domain/exception/`: `CredentialsInvalidException` (code `account.credentials-invalid`, kind `UNAUTHENTICATED`), `EmailNotConfirmedException` (code `account.email-not-confirmed`, kind `NOT_ALLOWED`), `AccountNotFoundException` (code `request.unauthenticated`, kind `UNAUTHENTICATED`; a sign-in whose account no longer exists counts as signed out, R7); `AccountNotFoundException` moved to T030, where it is first used
- [X] T010 [P] [US1] Add `boolean matches(String raw, String hash)` to `B/account/domain/output/PasswordHasher.java` and implement it in `B/account/infrastructure/output/Pbkdf2PasswordHasher.java` with the same encoder
- [X] T011 [US1] Create `SignInUseCase` (`Account signIn(SignInCommand command)`, Javadoc: same answer for unknown email and wrong password) and `SignInCommand(String email, String password)` in `B/account/domain/input/`; create `SignInService` in `B/account/domain/service/` (constructor: `AccountRepository`, `PasswordHasher`): computes a dummy hash once in the constructor from a fixed valid `RawPassword`; `signIn`: email not parseable as `Email` or password longer than `RawPassword.MAX_LENGTH` → `CredentialsInvalidException` without hashing; account missing → `matches(password, dummyHash)` then `CredentialsInvalidException`; wrong password → `CredentialsInvalidException`; right password, role `TRAINER` and not `emailConfirmed` → `EmailNotConfirmedException`; else return the account (R8 order, without the lock yet). The password is never trimmed.
- [X] T012 [US1] Write `BT/account/domain/service/SignInServiceTest.java` with hand-written fakes (in-memory repository, recording hasher): right password returns the account in any email case; unknown email compares against the dummy hash and is invalid; wrong password invalid; malformed email and a 129-character password invalid without any hash call; unconfirmed trainer refused with the right password and invalid with a wrong one; unconfirmed client signed in
- [X] T013 [US1] Register `SignInService` as `SignInUseCase` in `B/account/infrastructure/config/AccountConfig.java`
- [X] T014 [US1] [US4] [US5] Extend `B/common/infrastructure/config/SecurityConfig.java` (R4, R5, R7): beans `SecurityContextRepository` (`HttpSessionSecurityContextRepository`), `CsrfTokenRepository` (`CookieCsrfTokenRepository.withHttpOnlyFalse()`), `SessionAuthenticationStrategy` (`CompositeSessionAuthenticationStrategy` of `ChangeSessionIdAuthenticationStrategy` and `CsrfAuthenticationStrategy(csrfTokenRepository)`), `LogoutHandler` (`CompositeLogoutHandler` of `SecurityContextLogoutHandler` and `CsrfLogoutHandler(csrfTokenRepository)`); the chain uses `securityContext(c -> c.securityContextRepository(repo))`, `csrf(c -> c.spa().csrfTokenRepository(repo))` (check through context7 that `spa()` keeps its request handler when the repository is replaced), `logout(LogoutConfigurer::disable)`; permit `POST /api/v1/sign-ins` with a comment citing spec "Public endpoints"; `/api/v1/exercises/**` → `hasRole("TRAINER")`; the access-denied handler writes `request.forbidden` for a `CsrfException` and `request.not-allowed` otherwise
- [X] T015 [US1] [US5] Create in `B/account/infrastructure/input/`: `SignInRequest(@NotBlank String email, @NotBlank String password)`; `CurrentAccountResponse(UUID id, String name, Role role, String timeZone)`; `AccountWebMapper` (MapStruct, `Account` → `CurrentAccountResponse`); `SignInController` with `POST /api/v1/sign-ins` → 200 `CurrentAccountResponse` (calls the use case, builds `UsernamePasswordAuthenticationToken.authenticated(account id string, null, [ROLE_<role>])`, runs the `SessionAuthenticationStrategy`, sets a fresh context through `SecurityContextHolder.getContextHolderStrategy()`, saves it with the `SecurityContextRepository`) and `DELETE /api/v1/sign-ins/current` → 204 (runs the `LogoutHandler`); Javadoc on both
- [X] T016 [US1] [US5] Write `BT/account/infrastructure/input/SignInControllerTest.java` (`@WebMvcTest(SignInController.class)`, `@Import({SecurityConfig.class, WebConfig.class})`, use case mocked): 200 with body; the session id changes against one created before; the `XSRF-TOKEN` cookie is cleared; `account.credentials-invalid` 401 and `account.email-not-confirmed` 403; blank email and password → 400 with `not-blank` field codes; no CSRF → 403 `request.forbidden`; sign-out 204 invalidates the session; anonymous sign-out 401 `request.unauthenticated`
- [X] T017 [P] [US1] Add a demo client to `R/db/dev-data/R__demo_accounts.sql`: id `00000000-0000-4000-8000-000000000003`, `Demo Client`, `demo.client@example.com`, `CLIENT`, `Europe/Warsaw`, confirmed, the same demo password hash; update the comment and `README.md` ("Both accounts" → the three accounts)
- [X] T018 [US1] [US4] [US5] Extend `BT/KinetiqBackendApplicationIT.java` against the real database: change `@WithMockUser` on the exercise test to `roles = "TRAINER"`; a `CLIENT` mock user gets 403 `request.not-allowed` without exercises; a trainer created in the test signs in with CSRF from `/api/v1/csrf` → 200, a `SESSION` cookie, a row whose `principal_name` is the account id; `DELETE /api/v1/sign-ins/current` with it → 204, then the same cookie on any authenticated request → 401
- [X] T019 Start the backend, run `npm run gen:api` in `frontend/`, keep the regenerated `F/lib/api/schema.d.ts`; run `./gradlew check` and `npm run check`; commit `feat(backend): add sign-in and sign-out api (#20)`

---

## Phase 4: Task #21, failed sign-in throttling (US2 backend)

**Goal**: Five failures for one email within 15 minutes lock it for 15 minutes with its own answer and the time left; sign-ups and new-link requests are limited to 10 per address per hour.

**Independent test**: quickstart scenario 3 against the API alone; `FailedSignInsTest` and `SourceAddressLimiterTest` with fixed clocks.

- [X] T020 [P] [US2] Create `FailedSignIns` in `B/account/domain/model/` per data-model.md: `Email email`, failures within the last 15 minutes ("at most 5"), optional `lockedUntil`; `isLocked(now)`, `timeLeft(now)`, `recordFailure(now)` (ignored while locked; drops failures older than 15 minutes; the fifth sets `lockedUntil = now + 15 min` and clears failures), `isExpired(now)`
- [X] T021 [P] [US2] Write `BT/account/domain/model/FailedSignInsTest.java`: four failures not locked; the fifth locks; failures older than 15 minutes do not count; attempts while locked change nothing; locked at 14:59 after the fifth, unlocked at exactly 15:00 with no failures counted; `timeLeft`
- [X] T022 [P] [US2] Create `SignInLockedException(Duration timeLeft)` in `B/account/domain/exception/`: code `account.sign-in-locked`, kind `TOO_MANY_ATTEMPTS`, `properties()` = `retryAfterSeconds` rounded up, at least 1
- [X] T023 [US2] Create `FailedSignInsRepository` port (`Optional<FailedSignIns> find(Email)`, `void save(FailedSignIns)`, `void delete(Email)`) in `B/account/domain/output/` and `InMemoryFailedSignInsRepository` (`@Repository`, `ConcurrentHashMap`, drops expired records when it holds more than 10,000) in `B/account/infrastructure/output/`, with `ponytail:` comments for parallel attempts slipping past five and counts per instance (R8)
- [X] T024 [US2] Wire the lock into `SignInService` (constructor adds `FailedSignInsRepository` and `Clock`; `AccountConfig` passes `Clock.systemUTC()`): after the email parses, a locked email → `SignInLockedException` without hashing or counting; every failure from an unknown email or wrong password is recorded and saved before throwing; a right password deletes the record before the unconfirmed check; extend `SignInServiceTest` (fixed `Clock`, in-memory fake): fifth failure answers invalid, sixth answers locked even with the right password and without hashing; unknown emails lock the same way; right password resets the count; unconfirmed trainer's right password resets it too
- [X] T025 [US2] Create `SourceAddressLimiter` in `B/account/infrastructure/input/` (R10, data-model.md): `enum Kind { SIGN_UP, CONFIRMATION_LINK }`, `boolean tryAcquire(Kind kind, String address)` allows when fewer than 10 requests of that kind from that address fall within the last hour and records the time, drops stale keys above 10,000; register it in `AccountConfig` with `Clock.systemUTC()`; write `BT/account/infrastructure/input/SourceAddressLimiterTest.java` with a mutable fixed clock: 10 allowed, 11th refused, allowed again one hour after the first, kinds and addresses counted apart
- [X] T026 [US2] In `B/account/infrastructure/input/AccountController.java` ask the limiter with `HttpServletRequest.getRemoteAddr()` before `signUpTrainer` and `sendConfirmationLink`; refused → 202 without calling the use case; extend `AccountControllerTest` (limiter as `@MockitoBean`, stubbed in a `@BeforeEach` to return `true` so the existing tests keep reaching the use case; one test stubs `false`: refused → 202 and the use case not called) and `SignInControllerTest` (locked → 429 `account.sign-in-locked` with `retryAfterSeconds`)
- [X] T027 Run `./gradlew check`; commit `feat(backend): throttle failed sign-ins (#21)`

---

## Phase 5: Task #22, current-account API (US3, US4 backend)

**Goal**: The app can ask who is signed in and refresh the stored time zone.

**Independent test**: `GET /api/v1/accounts/me` signed in and anonymous; `PUT /api/v1/accounts/me/time-zone` with a real and an unknown zone.

- [X] T028 [P] [US3] Make `timeZone` in `B/account/domain/model/Account.java` non-final with `changeTimeZone(ZoneId)` (`requireNonNull`); extend `AccountTest`
- [X] T029 [P] [US3] Add `Optional<Account> findById(AccountId)` to `AccountRepository` and `AccountRepositoryImpl`; extend `BT/account/infrastructure/output/AccountRepositoryImplIT.java`
- [X] T030 [US3] Create `CurrentAccountUseCase` (`Account find(AccountId)`, `void changeTimeZone(AccountId, ZoneId)`) in `B/account/domain/input/` and `CurrentAccountService` in `B/account/domain/service/`: missing account → `AccountNotFoundException`; save only when the zone differs; register in `AccountConfig`; write `BT/account/domain/service/CurrentAccountServiceTest.java` (found, missing, same zone not saved, new zone saved)
- [X] T031 [US3] Move the region-zone check out of `AccountController` into a package-private `TimeZones.parse(String)` in `B/account/infrastructure/input/` ("region ids only", unknown → `InvalidValueException`), used by both controllers
- [X] T032 [US3] [US4] Create `TimeZoneRequest(@NotBlank String timeZone)` and `CurrentAccountController` in `B/account/infrastructure/input/`: `GET /api/v1/accounts/me` → `CurrentAccountResponse` for `new AccountId(UUID.fromString(authentication.getName()))`; `PUT /api/v1/accounts/me/time-zone` → 204; write `BT/account/infrastructure/input/CurrentAccountControllerTest.java`: anonymous 401 `request.unauthenticated` on both; signed-in body; 204 with the zone passed; unknown zone 400 `validation.failed`; missing account → 401 `request.unauthenticated`
- [X] T033 Regenerate `F/lib/api/schema.d.ts`; run `./gradlew check` and `npm run check`; commit `feat(backend): add current-account api (#22)`

---

## Phase 6: Task #23, 401 handling in the API client (US4 frontend)

**Goal**: Any 401 on an area page sends the person to sign-in with the page to return to; state-changing requests keep passing CSRF after the token rotates.

**Independent test**: `client.test.ts`.

- [X] T034 [P] [US4] Create `F/lib/query-client.ts` exporting the app's `QueryClient` (the retry rule now in `F/app/app.tsx`) and use it in `app.tsx`
- [X] T035 [P] [US4] Create `F/lib/api/sign-in-redirect.ts` with `redirectToSignIn()`: when `location.pathname` starts with `/trainer` or `/app`, `window.location.assign('/sign-in?next=' + encodeURIComponent(pathname + search))`
- [X] T036 [US4] In `F/lib/api/client.ts` add `onResponse`: status 401 → `redirectToSignIn()`, except for `POST /api/v1/sign-ins` and `GET /api/v1/accounts/me` (their callers handle 401, R13)
- [X] T037 [US4] Extend `F/lib/api/client.test.ts` (`vi.mock` the redirect module): 401 calls it; 401 from sign-in or `/accounts/me` does not; a POST after the `XSRF-TOKEN` cookie was cleared fetches `/api/v1/csrf` first and passes the new token
- [X] T038 [P] [US2] Add to `F/locales/en.json` and `F/locales/pl.json`: `errors.request.forbidden` reworded without "session" ("This page was open too long. Reload it and try again." / Polish equivalent), `errors.request.not-allowed`, `errors.account.credentials-invalid`, `errors.account.email-not-confirmed`, `errors.account.sign-in-locked` (fallback text without a number)
- [X] T039 Run `npm run check`; commit `feat(frontend): send to sign-in on 401 (#23)`

---

## Phase 7: Task #24, sign-in screen and sign-out action (US1, US2, US5 frontend)

**Goal**: The board's four sign-in states and sign-out in both areas, clearing everything the tab holds.

**Independent test**: quickstart scenarios 1–4, 8 and 11 in the built-in browser.

- [X] T040 Add shadcn `avatar` with the CLI after `npx shadcn@latest docs avatar`; read `F/components/ui/avatar.tsx`
- [X] T041 [P] [US1] Move the show/hide password input out of `F/features/account/pages/sign-up-page.tsx` into `F/features/account/components/password-input.tsx` (props for id, autocomplete, read-only, invalid, described-by); the sign-up page uses it and its tests still pass
- [X] T042 [P] [US1] Create `F/features/account/sign-in-schema.ts` (zod: email trimmed and non-empty → `account.errors.signInEmail` "Enter your email."; password non-empty, not trimmed → `account.errors.signInPassword` "Enter your password.")
- [X] T043 [US1] [US5] Extend `F/features/account/api/account.ts`: `currentAccountQuery` (`queryOptions`, key `['current-account']`, `GET /api/v1/accounts/me`); `useSignIn` (on success writes the response into that query); `useSignOut` (`DELETE /api/v1/sign-ins/current`; 204 or 401 → `sessionStorage.clear()` in `try`, `queryClient.clear()`, `navigate('/sign-in', { replace: true })`; other failures leave the person where they are, R14)
- [X] T044 [US1] [US2] Create `F/features/account/pages/sign-in-page.tsx` per the board and FR-001–FR-008, FR-026–FR-029: `AuthLayout`/`AuthCard` "Sign in", "Trainers and clients sign in here."; email (`autocomplete="email"`) and `PasswordInput` (`current-password`) with "Forgot password?" link to `/reset-password`; alerts by code: credentials invalid (destructive), locked (destructive, minutes = `Math.ceil(retryAfterSeconds / 60)`, at least 1, plural), email not confirmed (info, names the entered email, "Resend link" via `useRequestConfirmationLink`, disabled 60 s with one polite "Link sent again." note, reusing the lock from `check-email-card.tsx` and extracting a hook if that removes duplication); every alert `role="alert"`, then password cleared and focused; "Sign in" stays enabled when locked; busy button with spinner, fields read-only; other failures → generic alert; success → `sessionStorage.clear()` in `try`, then `/trainer` for a trainer, `/app` for a client; footer "New trainer? Create an account. Clients join from their trainer's invite."
- [X] T045 [US1] Add `/sign-in` (lazy, `BlankWhileLoading`) to `F/app/router.ts`
- [X] T046 [P] [US5] In `F/app/trainer-area.tsx` add a `SidebarFooter`: `Avatar` with initials, name and "Trainer" from `currentAccountQuery`, and a "Sign out" button (Lucide `LogOutIcon`, visible label) using `useSignOut`, busy while pending, generic error on failure
- [X] T047 [P] [US5] In `F/app/client-area.tsx` add a header "Settings" icon button (`aria-label`, 48 px target, Lucide `SlidersHorizontalIcon`) opening a `Sheet` titled "Settings" with `Avatar`, the client's name and an outline "Sign out" button using `useSignOut`, busy while pending, generic error on failure
- [X] T048 [P] [US1] [US2] [US5] Add every new string to `F/locales/en.json` and `F/locales/pl.json`: sign-in title, description, labels, placeholders, alert titles and bodies, locked minutes with `_one`/`_other` (EN) and `_one`/`_few`/`_many`/`_other` (PL), resend note, footer, "Forgot password?", "Sign out", "Settings", role "Trainer"
- [X] T049 [US1] [US2] Write `F/features/account/pages/sign-in-page.test.tsx` (stubbed `fetch`, `renderPage`): empty fields show both messages and send nothing; wrong → alert, password empty and focused, email kept; locked with 841 s → "15 minutes", 840 s → "14 minutes", 30 s → "1 minute", Polish plural for 2 and 5; unconfirmed → address named, resend sends the email, button disabled, note shown; second submit blocked while busy; network failure → generic; trainer success → `/trainer`, client → `/app`; tab storage cleared on success
- [X] T050 [US5] Extend `F/app/trainer-area.test.tsx` and `F/app/client-area.test.tsx` (existing tests get a `GET /api/v1/accounts/me` stub for a signed-in account): sign out sends `DELETE /api/v1/sign-ins/current`, clears `sessionStorage` (quiz key set before) and shows sign-in; a 500 keeps the area and shows the generic error
- [X] T051 Run `npm run check`; verify in the built-in browser per quickstart (sign-in at 375 and 1280 px, trainer footer at 1280, client sheet at 375, light and dark, EN and PL, 200% zoom, keyboard only, console clean), screenshots of the four states; review with the ui-ux-pro-max skill for WCAG 2.2 AA and UX issues without restyling the design system; fix findings; commit `feat(frontend): add sign-in screen and sign-out actions (#24)`

---

## Phase 8: Task #25, route guards per role (US4, US3 frontend)

**Goal**: Each area only for its role, anonymous visitors through sign-in and back, a signed-in person never on the sign-in page, the time zone refreshed on load.

**Independent test**: quickstart scenarios 5, 6, 9 and 10; `guards.test.ts`, `router.test.tsx`.

- [X] T052 [P] [US4] Create `F/app/guards.ts`: `landingFor(role)` (`/trainer` or `/app`); `safeNext(next, role)` returns `next` only when it starts with `/`, not `//` or `/\`, and starts with the role's area, else `undefined`; loader `requireRole(role)`: `queryClient.ensureQueryData(currentAccountQuery)`, 401 → `redirect('/sign-in?next=' + encodeURIComponent(path + search))`, other role → `redirect(landingFor(account.role))`, other failures rethrown; loader `redirectIfSignedIn`: signed in → `redirect(landingFor(role))`, any failure → `null`
- [X] T053 [P] [US3] Move `browserTimeZone()` from the sign-up page to `F/features/account/time-zone.ts`, returning `undefined` for empty or `Etc/Unknown` (sign-up keeps `?? 'UTC'`); add `refreshTimeZone(account)` to `F/features/account/api/account.ts` that sends `PUT /api/v1/accounts/me/time-zone` only when the browser zone is known and differs, ignores failures and then invalidates `currentAccountQuery`; call it from `requireRole` without awaiting
- [X] T054 [P] [US4] Create `F/app/area-error-page.tsx`: the generic message as an `Alert` and a "Try again" button calling `useRevalidator().revalidate()`; strings in EN and PL
- [X] T055 [US4] In `F/app/router.ts` add `loader: requireRole('TRAINER')` to `/trainer`, `requireRole('CLIENT')` to `/app` and `/app/quiz/:step?`, `ErrorBoundary: AreaErrorPage` to those three, `loader: redirectIfSignedIn` to `/sign-in`
- [X] T056 [US4] In `F/features/account/pages/sign-in-page.tsx` land on `safeNext(searchParams.get('next'), role) ?? landingFor(role)`; extend its test: `next` in own area kept with its query; other area, `//evil.example` and `https://evil.example` → landing
- [X] T057 [US4] Write `F/app/guards.test.ts` (`safeNext` cases) and extend `F/app/router.test.tsx` (reset the shared query client per test, stub `fetch`): anonymous at `/trainer/exercises?from=mail` → sign-in, sign in → that page; client at `/trainer` → `/app/today`; trainer at `/app/quiz` → `/trainer/clients`; signed in at `/sign-in` → own area; `/accounts/me` 500 → "Try again"; the existing "loads the area" and not-found cases get a signed-in `/accounts/me` stub of the matching role; the time zone PUT sent only when the zone differs
- [X] T058 Run `npm run check`; verify scenarios 5, 6, 9, 10 in the built-in browser (console clean, no flash of the other area); commit `feat(frontend): guard areas per role (#25)`

---

## Phase 9: Polish

- [X] T059 Run quickstart end to end with backend and frontend, including the restart check (scenario 7) and the `spring_session` row count; measure SC-002 (20 sign-ins each for an unknown email and a known email with a wrong password, compare medians) and record the result in the pull request
- [X] T060 Run a security review of the branch diff (enumeration by answer and timing, session fixation, CSRF rotation, cookie flags, open redirect through `next`, role checks, logs and error bodies) and fix findings in the commit of the task they belong to
- [X] T061 Ask before pushing; then push `feat/18-sign-in-out` and open one pull request closing #18–#25, to be merged with rebase or a merge commit

---

## Dependencies

- T001 → #19 (T002–T007) → #20 (T008–T019) → #21 (T020–T027) and #22 (T028–T033) → #23 (T034–T039) → #24 (T040–T051) → #25 (T052–T058) → T059–T061.
- #21 and #22 touch different files apart from `AccountConfig` and can be built in either order; their commits stay in issue order.
- #23 needs nothing from #21/#22 but sits after them so `schema.d.ts` is final before the frontend work; #24 needs #20 and #23 (GitHub "blocked by"); #25 needs #22.

## Parallel examples

- #20: T008, T009, T010, T017 together; T012 after T011.
- #21: T020, T021, T022 together; T025 alongside T023.
- #22: T028 and T029 together.
- #24: T041, T042 together after T040; T046, T047, T048 together after T043.
- #25: T052, T053, T054 together.

## Implementation strategy

MVP is US1 end to end: #19, #20 and the sign-in page of #24. It is not shippable alone, because without guards (#25) and throttling (#21) the areas are open to the wrong role and passwords can be guessed without limit, so the story merges only when all seven tasks are done. Each task ends with green gates and its own commit, so review can follow the commits one by one.
