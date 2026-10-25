# Research: Trainer and client sign in and out

Checked against the code on `master` (5181e9c) and current documentation (context7: Spring Session, Spring Security 7.0, Spring Boot 4.1, React Router) on 2026-10-24.

## R1. Where sign-ins are stored

- **Decision**: `spring-boot-starter-session-jdbc`. The tables `spring_session` and `spring_session_attributes` come from a Flyway migration copied from Spring Session's `schema-postgresql.sql`, with `spring.session.jdbc.initialize-schema: never`. `spring.session.timeout: 14d`; Spring Session's default clean-up job (every minute) deletes expired rows.
- **Rationale**: Task #19 names Spring Session JDBC. It keeps PostgreSQL as the only store, survives restarts (FR-011), and indexes the principal name, which password reset (#39) needs to end every sign-in of an account. Every request updates `last_access_time`, which gives the sliding 14 days. Hibernate's `validate` ignores tables without an entity.
- **Alternatives**: Redis (a second store to run); the servlet container's in-memory session (lost on restart).

## R2. Cookie

- **Decision**: Spring Session's `SESSION` cookie, configured through `server.servlet.session.cookie`: `http-only: true`, `same-site: lax`, `secure: true` in `application.yaml`, `secure: false` in `application-local.yaml`, `max-age: 400d`.
- **Rationale**: Boot applies these properties to Spring Session's `DefaultCookieSerializer`. Without `max-age` the cookie dies when the browser closes, which breaks "14 days of inactivity" on desktop. Spring Session writes the cookie only when the id changes (sign-in), so the cookie cannot slide; 400 days is the most Chrome accepts, and the server's 14-day idle limit is what actually ends a sign-in.
- **Known ceiling**: a sign-in used daily ends after 400 days, when the browser drops the cookie. Marked with a `ponytail:` comment; rewriting the cookie on each response is the upgrade path if anyone ever notices.
- **Local development**: `secure: false` because the Vite proxy serves over plain http.

## R3. No sign-in row for anonymous requests

- **Decision**: `http.requestCache(cache -> cache.requestCache(new NullRequestCache()))`.
- **Rationale**: Spring Security's `ExceptionTranslationFilter` saves the refused request in the session before answering `401`, which creates a session for every anonymous call (the follow-up noted in spec #2's plan). With a JDBC store that becomes a database row per anonymous request. The SPA keeps the page to return to itself (R12), so the saved request is never used. CSRF tokens live in a cookie and need no session either.

## R4. Signing in from a controller

- **Decision**: `POST /api/v1/sign-ins` in the account feature's controller. It calls `SignInUseCase`, then builds `UsernamePasswordAuthenticationToken.authenticated(accountId, null, [ROLE_TRAINER | ROLE_CLIENT])`, runs a `SessionAuthenticationStrategy` composed of `ChangeSessionIdAuthenticationStrategy` and `CsrfAuthenticationStrategy`, and saves the context with a `HttpSessionSecurityContextRepository`. `SecurityConfig` exposes the repository, the strategy and the CSRF token repository as beans and uses the same repository in the filter chain.
- **Rationale**: Spring Security has no JSON username/password filter, and its form-login filter would put the throttling and confirmation rules into filter handlers, outside a use case and outside the OpenAPI document. Spring Security 7's documentation shows exactly this manual pattern. A manual login does not run the strategies a login filter runs, so session fixation (FR-005) and CSRF token rotation have to be called explicitly. The principal is the account id, never the email: `spring_session.principal_name` holds no personal data.
- **CSRF token repository**: `csrf.spa()` creates its own cookie repository. Configuring `spa()` and then `csrfTokenRepository(<bean>)` keeps the SPA request handler and gives the controller the same repository; checked in the controller test by the cookie being cleared after sign-in.
- **Alternatives**: `formLogin()` with JSON-reading filter subclass (more code, hidden from OpenAPI); tokens (forbidden by the constitution).

## R5. Signing out

- **Decision**: `DELETE /api/v1/sign-ins/current`, answered `204`. The controller runs a `CompositeLogoutHandler` of `SecurityContextLogoutHandler` (invalidates the session, clears the context) and `CsrfLogoutHandler`. Spring Security's own `/logout` filter is disabled.
- **Rationale**: Spring's documentation warns that skipping `SecurityContextLogoutHandler` leaves the context available. Invalidating deletes the row, so a copied cookie stops working (FR-015); other rows of the account stay (scenario 5).

## R6. Serialization of the stored sign-in

- **Decision**: Default JDK serialization of `SecurityContextImpl` holding a `UsernamePasswordAuthenticationToken` of a `String` id and `SimpleGrantedAuthority` values.
- **Rationale**: Smallest possible graph of Spring Security classes, which keep serialization compatible within a major version. A major upgrade may make old rows unreadable; the plan for that upgrade must check whether those requests are treated as signed out or fail. JSON serialization with Spring Security's Jackson modules is the alternative if that ever matters.

## R7. Status codes and error codes

| Case | Status | Code |
|---|---|---|
| Wrong email or password, malformed email, password over 128 characters | 401 | `account.credentials-invalid` |
| Locked email | 429 | `account.sign-in-locked`, property `retryAfterSeconds` |
| Trainer with unconfirmed email, right password | 403 | `account.email-not-confirmed` |
| Empty email or password | 400 | `validation.failed` with field errors `not-blank` |
| Not signed in | 401 | `request.unauthenticated` (existing) |
| Signed in, wrong role | 403 | `request.not-allowed` (new) |
| Missing or wrong CSRF token | 403 | `request.forbidden` (existing) |
| Account of a sign-in no longer exists | 401 | `request.unauthenticated` |

- **Decision**: `DomainException` gains a `kind()` (`INVALID`, `UNAUTHENTICATED`, `NOT_ALLOWED`, `TOO_MANY_ATTEMPTS`; default `INVALID`) and a `properties()` map (default empty). `GlobalExceptionHandler` maps the kind to a status and copies the properties into the problem. The access-denied handler tells a `CsrfException` (`request.forbidden`) from a role refusal (`request.not-allowed`).
- **Rationale**: `common` may not know feature exceptions, and the handler is the one place that maps them. Four exceptions need three new statuses now. A plain enum keeps the domain free of Spring.
- **Copy**: `errors.request.forbidden` says "Your session has expired"; "session" is a training occasion (`CONTEXT.md`). It becomes "This page was open too long. Reload it and try again." in both languages.

## R8. Failed sign-ins

- **Decision**: Domain class `FailedSignIns` per email: the times of failures in the last 15 minutes (at most 5) and `lockedUntil`. `isLocked(now)`, `timeLeft(now)`, `recordFailure(now)` (the fifth failure within 15 minutes sets `lockedUntil = now + 15 min`). A right password removes the record. Port `FailedSignInsRepository` (`find`, `save`, `delete` by `Email`), adapter `InMemoryFailedSignInsRepository` (a `ConcurrentHashMap`) that drops expired records when it holds more than 10,000.
- **Rationale**: The spec accepts losing counts on restart. Each attempt costs a 600,000-iteration hash, which caps how fast records can be created, so memory stays small. A table would add a migration, a clean-up job and a write per failure for no gain on one instance.
- **Ceilings** (`ponytail:` comments): parallel attempts for one email can slip a few past five, since find and save are separate; a second backend instance would keep its own counts. A table is the upgrade path for both.
- **Order in `SignInService`**: email malformed or password over 128 → invalid, no hash; locked → locked, no hash, nothing counted; account missing → hash compared against a dummy hash made at start-up, failure counted; wrong password → failure counted; right password → record removed, then an unconfirmed trainer is refused.

## R9. Same answer, same time

- **Decision**: `PasswordHasher` gains `matches(String raw, String hash)`. For an unknown email the service compares against a hash of a fixed dummy password computed once when the service is created.
- **Rationale**: Without it, an unknown email answers in microseconds and a known one in about 0.3 s (SC-002). A wrong-format password is a plain `String` on this path: wrapping it in `RawPassword` would reject it with a field error and reveal the rules check.

## R10. Requests per network address

- **Decision**: `SourceAddressLimiter` in `account.infrastructure.input`: per (kind, address), the times of the last 10 requests; a request is allowed when fewer than 10 fall in the last hour. `AccountController` asks it before calling `signUpTrainer` or `sendConfirmationLink` and answers `202` without calling when refused. The address is `HttpServletRequest.getRemoteAddr()`. Wired as a bean with `Clock.systemUTC()`, tested with a fixed clock.
- **Rationale**: The address is a web concern, so the limit sits in the input adapter, before any use case runs. An exact sliding window over 10 timestamps is as small as a fixed window and has no burst at the boundary.
- **Deployment**: behind a proxy `getRemoteAddr()` is the proxy; `server.forward-headers-strategy` is set with task #65.

## R11. Current account and time zone

- **Decision**: `GET /api/v1/accounts/me` → `{id, name, role, timeZone}`. `PUT /api/v1/accounts/me/time-zone` `{timeZone}` → `204`; an unknown zone → `400 validation.failed`. The frontend sends the PUT only when the browser's zone is known (not empty, not `Etc/Unknown`) and differs from the answer, and ignores its failure.
- **Rationale**: A `GET` that writes would be surprising and could be repeated by caches. Comparing in the browser means the normal load is one read. Zone validation moves from `AccountController`'s private method into a shared static method used by both endpoints.
- **Sign-in answer**: `POST /api/v1/sign-ins` returns the same body as `GET /accounts/me`, so the app can land without another request.

## R12. Guards and the page to return to

- **Decision**: A shared `QueryClient` in `src/lib/query-client.ts`, used by `App` and by route loaders. The `/trainer` and `/app/*` routes (the quiz included) get a loader `requireRole(role)` that calls `queryClient.ensureQueryData(currentAccountQuery)`: `401` → `redirect('/sign-in?next=<path+query>')`; other role → `redirect` to its landing; any other failure is thrown to the route's `ErrorBoundary`, which shows the generic message with "Try again" (revalidate). `/sign-in` gets a loader that redirects a signed-in person to their landing and renders the page on any failure.
- **`next`**: accepted only when it starts with `/` but not `//` or `/\`, and starts with the person's own area (`/trainer` or `/app`); otherwise the landing is used.
- **Rationale**: A loader runs before the area renders, so the other role never sees the area (FR-022), and React Router's `redirect` keeps the SPA. Parent loaders do not re-run on every child navigation; a sign-in that ends meanwhile is caught by the API client (R13). Middleware would do the same with more setup.

## R13. 401 from any request

- **Decision**: An `onResponse` middleware in `lib/api/client.ts`: a `401` sends the browser to `/sign-in?next=<current path+query>` with `window.location.assign`, unless the request was `POST /api/v1/sign-ins` or `GET /api/v1/accounts/me` (their callers handle `401`) or the current page is not in an area. The redirect lives in its own tiny module so tests can mock it.
- **Rationale**: The client cannot import the router (the router imports the areas, which import the client). A full page load also drops every in-memory cache of the previous person.

## R14. Signing out in the browser

- **Decision**: `useSignOut()`: `DELETE /api/v1/sign-ins/current`; on `204` or `401` → `sessionStorage.clear()` (in `try`), `queryClient.clear()`, `navigate('/sign-in', { replace: true })`; on other failures the person stays with the generic error. Signing in also runs `sessionStorage.clear()` before landing.
- **Rationale**: The quiz draft is the only thing in tab storage today (`kinetiq.quiz`); clearing all of it also covers whatever later stories put there. `replace` plus the cleared cache means "back" lands on a guard that finds nobody signed in.

## R15. Screens

- **Decision**: shadcn components already in the project: `card`, `field`, `input`, `input-group`, `alert`, `button`, `spinner`, `sheet`, `sidebar`. New through the CLI: `avatar` (initials in the trainer sidebar footer and the client settings sheet). The show/hide password input moves out of `sign-up-page.tsx` into `features/account/components/password-input.tsx`, now that sign-in needs it too. `browserTimeZone()` moves to `features/account/time-zone.ts`; sign-up keeps its `UTC` fallback.
- **Client settings**: the client header gets a "Settings" icon button opening a `Sheet` with the client's initials, name and "Sign out", as on the board; units, language and theme come with #45.
- **Locked minutes**: `Math.ceil(retryAfterSeconds / 60)`, at least 1, through i18next plurals (`_one`, `_few`, `_many`, `_other` in Polish).
