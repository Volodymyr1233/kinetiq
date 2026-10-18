# Research: Trainer signs up

Facts were checked against the running backend and the current documentation (context7: Spring Security 7.0, Spring Framework 7) on 2026-10-18.

## R1. Password hashing

- **Decision**: `DelegatingPasswordEncoder` whose only encoder is `Pbkdf2PasswordEncoder` (PBKDF2-HMAC-SHA256, 16-byte salt, 600,000 iterations), stored with its `{pbkdf2}` prefix.
- **Rationale**: Spring's default encoder is bcrypt, and bcrypt reads at most 72 bytes; since Spring Security 6.3 it rejects longer input, so the story's 128-character maximum breaks it. PBKDF2 has no length limit, ships with Spring Security (no new dependency), and 600,000 iterations is OWASP's current figure for PBKDF2-SHA256. The prefix lets a later encoder take over without rehashing everyone at once.
- **Alternatives**: bcrypt with a 72-byte cap (contradicts the story); Argon2id (OWASP's first choice, but needs Bouncy Castle and 16+ MiB of memory per hash, which concurrent sign-ups multiply).

## R2. CSRF token for a fresh page

- **Decision**: Keep `csrf.spa()`. Add a public `GET /api/v1/csrf` that answers `204`. The API client calls it once before a state-changing request when the `XSRF-TOKEN` cookie is missing, then sends `X-XSRF-TOKEN` on every POST, PUT, PATCH and DELETE.
- **Rationale**: Measured on the running backend, `spa()` writes the cookie on every backend response, including `401` and `403`. A visitor who lands on `/sign-up` has not made a backend request yet, so the first POST would be refused. Spring's documentation recommends an endpoint for exactly this case.
- **Effect on other issues**: The CSRF header half of task #23 is done here, because sign-up is the first POST; #23 keeps the 401 handling.

## R3. Error responses

- **Decision**: One `GlobalExceptionHandler` (`@RestControllerAdvice` extending `ResponseEntityExceptionHandler`) in `common.infrastructure.input`:
  - every Spring MVC exception keeps its status, gets a `code` from its status (`request.invalid`, `request.not-found`, `request.method-not-allowed`, `request.unsupported-media-type`, otherwise `request.invalid` for 4xx) and loses its `detail`;
  - `MethodArgumentNotValidException` → `400`, `code: validation.failed`, `errors: [{field, code}]`, where the field code is the constraint name in kebab case (`not-blank`, `size`, `email`, `pattern`, `assert-true`);
  - `DomainException` → `400` with its own code;
  - anything else → `500`, `code: internal.error`, a `correlationId`, and the exception only in the log.
- **Rationale**: Field codes come from `FieldError.getCode()`, so no annotation has to remember a `message`, and no default message text (which is localised server text) leaks. Boot's own problem-details handler backs off when a `ResponseEntityExceptionHandler` bean exists.
- **Security filters**: the `401` and `403` raised before a controller (no session, missing CSRF token) are written as problem details too, by the authentication entry point and access-denied handler in `SecurityConfig`, with `request.unauthenticated` and `request.forbidden`. Otherwise the CSRF refusal would reach the SPA without a `code` (spec FR-020).

## R4. Language of emails and errors

- **Decision**: The frontend sends `Accept-Language: <current UI language>` on every request. The backend resolves it with an `AcceptHeaderLocaleResolver` limited to `en` and `pl` (default `en`), and the controller passes the `Locale` into the command. Mail subjects come from `messages.properties` / `messages_pl.properties`.
- **Rationale**: The browser's own `Accept-Language` reflects the operating system, not the language the person chose in Kinetiq. No language is stored with the account yet; settings (#43) add that.
- **Frontend default**: i18next starts in Polish when `navigator.language` starts with `pl`, otherwise English, and keeps `<html lang>` in sync. There is no language switcher on these screens.

## R5. Confirmation link

- **Decision**: 32 random bytes from `SecureRandom`, base64url (43 characters). Only its SHA-256 hash is stored, on the account, with its expiry. The link is `<app-url>/confirm-email#token=<token>`; `app-url` is the property `kinetiq.app-url` (default `http://localhost:5173`, set in production through `KINETIQ_APP_URL`).
- **Rationale**: One working link per account (FR-014) makes a separate table unnecessary: a new link overwrites the hash, confirming clears it. A hash is enough because the token has 256 bits of entropy, so no salt or slow hash is needed. The fragment is never sent to a server, so the token stays out of access logs and `Referer` headers. The page removes it from the address bar with `history.replaceState` before confirming.
- **Scanner safety (FR-012)**: Opening the link loads the SPA, which confirms with a `POST`. Mail scanners fetch pages and do not run scripts, so they cannot confirm.
- **React development mode**: StrictMode runs effects twice, which would confirm twice and show "link no longer works" on success. The page starts the confirmation from a ref-guarded effect so it runs once.

## R6. Not revealing whether an email exists

- **Decision**: `signUpTrainer` hashes the password before it looks up the email, and both branches send one email. `sendConfirmationLink` does the lookup and sends only for an unconfirmed account.
- **Rationale**: Hashing costs about 0.3–0.5 s, so skipping it for a known email would make the response time reveal the account. The logging mail adapter takes microseconds. Once real SMTP delivery arrives (#65), sending must not happen inside the request, or a confirmed and an unknown email will differ in time again. The plan lists this as a follow-up for #65.
- **Simultaneous sign-ups**: the unique constraint on `email` guarantees one account. The losing request ends in `500 internal.error`. This is accepted: it needs two sign-ups with the same email within milliseconds, and it reveals nothing more than the account the person just created.

## R7. Throttling

- **Decision**: The account stores `mail_sent_at`. Any email for the account (confirmation link or "you already have an account") is skipped when the previous one was sent less than 60 seconds ago; the response is unchanged.
- **Rationale**: FR-016 counts both kinds together, and an account sends one email at a time, so one column is enough.

## R8. Email equality

- **Decision**: An `Email` value object trims the address, lower-cases it with `Locale.ROOT`, checks `^[^\s@]+@[^\s@]+\.[^\s@]+$` and at most 254 characters. The column is unique, and a check constraint keeps it lower-case. The request rule accepts surrounding whitespace (`^\s*[^\s@]+@[^\s@]+\.[^\s@]+\s*$`) because trimming happens in `Email`; the form also trims before sending.
- **Rationale**: Normalising once on the way in makes every comparison a plain equality, and the database can enforce uniqueness without a functional index. The frontend uses the same pattern.

## R9. Length counting

- **Decision**: "Characters" means UTF-16 code units on both sides (Java `String.length()`, JavaScript `.length`).
- **Control characters**: "no control characters" means Unicode category `Cc` on both sides (Java `\p{Cc}`, JavaScript `/\p{Cc}/u`); Java's `\p{Cntrl}` covers ASCII only.
- **Rationale**: Both sides then agree by construction. The only difference from code points is that an emoji counts as two, which affects nobody in practice.

## R10. Development data

- **Decision**: A `local` profile. `application-local.yaml` sets `spring.flyway.locations` to `classpath:db/migration,classpath:db/dev-data`; the seed is `src/main/resources/db/dev-data/R__demo_accounts.sql`. `bootRun` activates `local`; in IntelliJ the run configuration sets "Active profiles: local". Tests and production run without a profile, so the seed never runs there.
- **Rationale**: Profiles are the conventional Spring mechanism and work the same from Gradle and the IDE. A repeatable migration (`R__`) never collides with the numbered ones and reruns when edited. Inserts use `on conflict do nothing`.
- **Alternatives**: a Gradle `dev` source set on the `developmentOnly` classpath keeps the seed out of the jar entirely, but is unusual and its IDE support is unproven. The seed in the jar is inert without the profile.
- **Demo password**: the demo accounts use a hash of a documented demo password. It is not a secret: it opens nothing outside a local database.

## R10a. Mail content in the log

- **Decision**: `LoggingMailSender` writes recipient and link only when `kinetiq.mail.log-content` is `true`, which only `application-local.yaml` sets. Otherwise it logs the template name.
- **Rationale**: Spec FR-018. A link in a log is a way into an account, and the recipient is personal data; a forgotten production setting must fail safe.

## R11. Forms

- **Decision**: `react-hook-form` 7.90, `zod` 4.6 and `@hookform/resolvers` 5.9 (its peer range accepts zod 4), installed in task #8. One zod schema per form; the password rule is one exported function used by the schema and its test.
- **Rationale**: Chosen earlier for the project (`frontend/CLAUDE.md`) and first needed by this form.
- **Behaviour**: `mode: "onSubmit"`, `reValidateMode: "onChange"`, `shouldFocusError: true` (spec FR-008, FR-027). Server field errors go through `setError`, so they render and focus exactly like the form's own.
- **Time zone**: `Intl.DateTimeFormat().resolvedOptions().timeZone`; `UTC` when it is empty or `Etc/Unknown` (spec, Edge Cases).

## R12. Testing the API client

- **Decision**: `createClient<paths>({ fetch: (request) => fetch(request) })`, so tests can replace the global `fetch` after the module was imported.
- **Rationale**: openapi-fetch captures `globalThis.fetch` when the client is created. Page tests then exercise the real hooks and client against a stubbed `fetch`, with no extra library.
