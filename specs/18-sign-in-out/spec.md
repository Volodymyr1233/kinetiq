# Feature Specification: Trainer and client sign in and out

**Feature Branch**: `feat/18-sign-in-out`

**Created**: 2026-10-24

**Status**: Draft

**Input**: Story #18 "Trainer and client sign in and out" and its tasks #19–#25. Screens: Kinetiq Screens, board Account → "Sign in" (Default, Wrong email or password, Locked, Email not confirmed). Signing out also clears what the tab keeps, such as unfinished quiz answers.

## Clarifications

### Session 2026-10-24

- Q: Story #18 and task #21 say a locked email gets "the same answer" as a wrong password, while task #24 and the board give Locked its own message. Which? → A: Locked gets its own code and message, given for every email with or without an account; "the same answer" means the same for known and unknown emails.
- Q: Should the locked message say how long is left? → A: Yes, the real number of minutes left, rounded up.
- Q: "Session" already means a training occasion. What is the signed-in state called? → A: **Sign-in**, added to `CONTEXT.md` (_Avoid_: Session, login).
- Q: Should sign-in also be limited per network address, not only per email? → A: No, per email only, as the story asks.
- Q: How many sign-ups and new-link requests may one network address make? → A: 10 of each per hour.
- Q: Should a sign-in end after a fixed maximum age even when used daily? → A: No maximum; only the 14-day idle limit.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Sign in and land in the right area (Priority: P1)

A trainer or client opens the sign-in page, enters email and password, and lands in their own area: a trainer in the trainer area, a client in the client app.

**Why this priority**: Every other feature needs a signed-in person; without it no data can be shown to its owner.

**Independent Test**: Sign in with the demo trainer and the demo client; each lands in their own area, and the trainer area's first request that changes something passes the anti-forgery check.

**Acceptance Scenarios**:

1. **Given** a trainer with a confirmed email, **When** they sign in with the right email (any letter case, surrounding spaces) and password, **Then** they land in the trainer area (`/trainer`).
2. **Given** a client, **When** they sign in with the right email and password, **Then** they land in the client app (`/app`), whether or not their email is confirmed.
3. **Given** the sign-in page, **When** the email or the password is empty, **Then** the form is not sent and each empty field shows its own message next to it.
4. **Given** a trainer whose email is not confirmed, **When** they sign in with the right password, **Then** they are not signed in and see "Confirm your email", naming the entered email, with "Resend link"; **when** the password is wrong, **Then** they see the wrong-email-or-password answer instead.
5. **Given** "Confirm your email", **When** the person chooses "Resend link", **Then** a new confirmation link is requested for that email, the button is disabled for 60 seconds, and "Link sent again." is announced once.
6. **Given** a signed-in person, **When** they open the sign-in page, **Then** they are taken to their own area.

---

### User Story 2 - Wrong credentials and repeated attempts reveal nothing (Priority: P1)

A wrong email and a wrong password look exactly the same. Someone guessing passwords for one email is stopped after five tries.

**Why this priority**: Without it, the sign-in page tells anyone which emails have accounts and lets them guess passwords without limit.

**Independent Test**: Sign in with an unknown email and with a known email and a wrong password; both answers are identical. Fail five times for one email; the sixth attempt, even with the right password, is refused.

**Acceptance Scenarios**:

1. **Given** an email with no account, and an email with an account, **When** each is used with a wrong password, **Then** both get the same answer, and the page shows "Email or password is wrong. Check both and try again."
2. **Given** four failed attempts for one email in the last 15 minutes, **When** a fifth one fails, **Then** every sign-in for that email is refused for 15 minutes from that attempt, including one with the right password, whether or not the email has an account.
3. **Given** an email whose sign-in is refused, **When** anyone tries it, **Then** they get the locked answer, the same whether or not the email has an account, and the page shows "Too many attempts. Try again in N minutes.", N being the minutes left, rounded up (1 to 15, with Polish plural forms).
4. **Given** an email whose sign-in is refused, **When** 15 minutes have passed since the attempt that caused it, **Then** sign-in for that email works again and its count of failures starts from zero.
5. **Given** some failed attempts for an email, **When** the right password is entered, **Then** the count of failures for that email starts from zero.
6. **Given** one network address, **When** it makes more than 10 sign-ups, or more than 10 requests for a new confirmation link, in one hour, **Then** each further request of that kind gets the usual answer but no account is created and no email is sent.

---

### User Story 3 - Stay signed in (Priority: P1)

A signed-in person stays signed in when the backend restarts and for 14 days without using Kinetiq. Using it in that time keeps them signed in.

**Why this priority**: Clients log sets on a phone in the gym; being asked for a password at every visit or after every deployment would stop them logging.

**Independent Test**: Sign in, restart the backend, reload the page; the person is still signed in.

**Acceptance Scenarios**:

1. **Given** a signed-in person, **When** the backend restarts, **Then** they are still signed in on their next request.
2. **Given** a signed-in person, **When** 14 days pass without any request from that browser, **Then** they are signed out; **when** they make a request before that, **Then** the 14 days start again.
3. **Given** a signed-in person, **When** the app loads, **Then** it reports the browser's time zone, and the account's stored time zone is updated if it is a real zone and differs.

---

### User Story 4 - Each area only for its role, and back to the page asked for (Priority: P1)

A visitor who is not signed in and opens a page of either area is sent to sign in and, after signing in, back to that page. A trainer cannot open the client app and a client cannot open the trainer area.

**Why this priority**: Links from emails and notifications point into the areas; losing the target on the way through sign-in, or showing one role's pages to the other, breaks every later story.

**Independent Test**: Sign out, open `/trainer/exercises`, sign in as the demo trainer and land on the exercise library. Signed in as the demo client, open `/trainer` and land in `/app`.

**Acceptance Scenarios**:

1. **Given** a visitor who is not signed in, **When** they open any page under `/trainer` or `/app`, **Then** they see the sign-in page, and after signing in they land on the page they opened, with the same query.
2. **Given** that visitor signs in with the other role's account, **When** sign-in succeeds, **Then** they land in their own area, not on the page they opened.
3. **Given** a signed-in client, **When** they open any page under `/trainer`, **Then** they land in `/app`; **given** a signed-in trainer, **When** they open any page under `/app`, including the quiz, **Then** they land in `/trainer`.
4. **Given** a signed-in person, **When** their sign-in ends while a page is open (14 days passed, or they signed out in another tab), **Then** the next request from that page sends them to sign in, and back to that page afterwards.
5. **Given** a signed-in client, **When** they call an operation only trainers may use, such as listing the exercise library, **Then** it is refused and no data is returned.

---

### User Story 5 - Sign out (Priority: P2)

A trainer or client signs out. Nothing of theirs stays in the browser and the sign-in can no longer be used.

**Why this priority**: Phones and gym computers are shared; signing out must leave nothing behind for the next person.

**Independent Test**: As the demo client, answer two quiz steps, sign out, then sign in as another client in the same tab; the quiz starts empty. A copy of the signed-out browser's credential is refused.

**Acceptance Scenarios**:

1. **Given** a signed-in trainer, **When** they choose "Sign out" in the sidebar, **Then** they are signed out and see the sign-in page.
2. **Given** a signed-in client, **When** they open settings from the header and choose "Sign out", **Then** they are signed out and see the sign-in page.
3. **Given** a person who signed out, **When** anyone repeats a request with the credential that browser had, **Then** it is answered as not signed in.
4. **Given** a client with unfinished quiz answers kept by the tab, **When** they sign out, **Then** the answers are gone, and no data loaded for them is shown if the browser goes back.
5. **Given** a person signed in on two devices, **When** they sign out on one, **Then** the other stays signed in.
6. **Given** a sign-out that cannot reach the server, **When** it fails, **Then** the person stays where they are, signed in, and sees the generic error.

---

### Edge Cases

- Email letter case and surrounding spaces do not matter, for sign-in and for counting failures. The password is never trimmed.
- An email longer than 254 characters, a malformed email or a password longer than 128 characters gets the wrong-email-or-password answer, since no account can match; such a password is not hashed.
- A trainer with an unconfirmed email who is locked gets the locked answer, even with the right password.
- The right password for an unconfirmed trainer resets the failure count, as any right password does.
- Attempts while an email is locked are not counted and do not extend the lock.
- With 14 minutes and 1 second left the message says 15 minutes; with 30 seconds left it says 1 minute. The number shown is the one at the time of the attempt; it does not count down.
- A failure count may be lost when the backend restarts; nobody outside can restart it.
- Two sign-ins for the same account at the same moment both succeed; each browser has its own sign-in.
- After a failed attempt the email stays filled in and the password is cleared.
- The page to return to is only ever a page of Kinetiq: a target pointing at another site, or one that is not a path within Kinetiq, is ignored and the person lands in their area.
- A target in the other role's area, or one that no longer exists, lands the person in their area or on the not-found page as usual, never in an error.
- If asking who is signed in fails for a reason other than "not signed in" (network down, server error), the page shows the generic error with "Try again", not the sign-in page.
- A browser that reports no time zone, or one that is not a real zone, leaves the stored zone unchanged; this is not an error.
- Signing in clears what the tab kept from before, so a person who signs in after someone else's sign-in expired in the same tab does not see their quiz answers.
- Several tabs: signing out in one makes the next request in the others go to sign-in.

## Requirements *(mandatory)*

### Functional Requirements

**Sign-in**

- **FR-001**: The sign-in page MUST ask for email and password, MUST be reachable without signing in, and MUST link to trainer sign-up ("New trainer? Create an account. Clients join from their trainer's invite.") and to password reset ("Forgot password?").
- **FR-002**: The system MUST treat emails as equal regardless of letter case and surrounding spaces.
- **FR-003**: An unknown email and a wrong password MUST give one identical answer, and MUST take about as long, so that neither reveals whether the email has an account.
- **FR-004**: A trainer whose email is not confirmed MUST be refused with its own answer, and only after the right password; they are not signed in. A client whose email is not confirmed signs in as usual.
- **FR-005**: A successful sign-in MUST replace the browser's earlier sign-in identifier and anti-forgery token with new ones, so an identifier known before sign-in cannot be used after it.
- **FR-006**: The form MUST require both fields before sending: "Enter your email." and "Enter your password.". The server MUST reject an empty email or password with a field error, the same messages.

**Repeated failures**

- **FR-007**: After 5 failed sign-ins for one email within 15 minutes, the system MUST refuse every sign-in for that email for 15 minutes from the fifth failure, with the right password too, whether or not the email has an account.
- **FR-008**: A refused (locked) sign-in MUST get an answer with its own code and the time left until the lock ends, the same for every email with or without an account. The page MUST show the minutes left, rounded up.
- **FR-009**: Attempts while an email is locked MUST NOT count as failures or extend the lock. A sign-in with the right password, and the end of a lock, MUST reset that email's count to zero.
- **FR-010**: The system MUST accept at most 10 sign-ups and at most 10 requests for a new confirmation link per network address in any hour. A request over the limit MUST get the same answer as one within it, and MUST NOT create an account or send an email. Sign-in itself is limited per email only (FR-007), not per address.

**Staying signed in**

- **FR-011**: A sign-in MUST survive a backend restart, MUST end after 14 days without a request from its browser, and every request MUST restart those 14 days. Kinetiq sets no maximum age: a sign-in used at least every 14 days does not end on its own, within the browser's own cookie limit (Assumptions).
- **FR-012**: What the browser holds to stay signed in MUST NOT be readable by the page's scripts, MUST NOT be sent with requests from other sites that change something, and outside local development MUST only travel over encrypted connections. Nothing that proves a sign-in is kept in browser storage.
- **FR-013**: Every request that changes something MUST carry the anti-forgery token, and one without it, or with a wrong one, MUST be refused with a code. The app's requests MUST pass, including the first after signing in or out.

**Signing out**

- **FR-014**: "Sign out" MUST be available in the trainer area's sidebar and in the client app's settings sheet, opened from the header.
- **FR-015**: Signing out MUST end the sign-in on the server, so a copy of what the browser held no longer works. Other devices of the same account stay signed in.
- **FR-016**: After signing out, and before showing the sign-in page, the app MUST forget everything it holds for that person: data it loaded and everything the tab keeps, such as unfinished quiz answers. Signing in MUST also clear what the tab kept from before.
- **FR-017**: A sign-out the server answers with "not signed in" MUST count as signed out. A sign-out that fails otherwise MUST leave the person signed in where they were, with the generic error.

**Current account**

- **FR-018**: The app MUST be able to ask who is signed in and get the account's id, name, role and time zone. A request that is not signed in MUST be answered "not signed in" with its code.
- **FR-019**: When the app loads for a signed-in person, it MUST report the browser's time zone; the system MUST store it on the account if it is a real region zone that differs from the stored one, and MUST otherwise leave the stored zone unchanged. A refused zone is never shown to the person as an error.

**Areas and redirects**

- **FR-020**: After sign-in, a trainer MUST land in `/trainer` and a client in `/app`, unless a page to return to applies (FR-021).
- **FR-021**: A visitor who is not signed in and opens a page under `/trainer` or `/app` MUST be sent to sign-in, and after signing in MUST land on that page with its query, if it is in their own role's area. The page to return to MUST be a path within Kinetiq; anything else MUST be ignored.
- **FR-022**: A client opening a page under `/trainer` MUST land in `/app`; a trainer opening a page under `/app` (the quiz included) MUST land in `/trainer`. Neither sees the other area's page, even briefly.
- **FR-023**: When any request from a page in an area is answered "not signed in", the app MUST send the person to sign-in and back to that page afterwards (FR-021). The sign-in request itself is excluded.
- **FR-024**: The server MUST refuse operations meant for one role when called by the other, with a code and no data. In this story that is the exercise library, which only trainers may list.
- **FR-025**: While the app finds out who is signed in, area pages MUST show nothing; if that fails for a reason other than "not signed in", they MUST show the generic error with "Try again".

**Errors and screens**

- **FR-026**: Every refusal MUST be a problem detail with a stable machine-readable code: wrong email or password, locked, email not confirmed, not signed in, not allowed for this role, missing or wrong anti-forgery token, invalid field. The UI MUST show them by code in English or Polish, never server text, and fall back to the generic message.
- **FR-027**: The sign-in screen MUST follow the board's four states, built from the Kinetiq design system, in English and Polish, light and dark. At every width from 375px to 1280px all content and actions are visible without horizontal scrolling, and text stays readable at 200% zoom.
- **FR-028**: Every field MUST have a visible label; field errors MUST be announced and linked to their field; the password field MUST offer show/hide with an accessible name. The wrong, locked and unconfirmed messages MUST be announced as alerts, and after them focus MUST move to the password field.
- **FR-029**: While sign-in or sign-out is in progress, its button MUST be disabled, show a busy indicator and keep its label, and a second submit MUST NOT be possible; the fields are read-only meanwhile.

### Public endpoints

Signing in MUST be reachable without signing in, because the person is not signed in yet. It reveals nothing about whether an email is registered (FR-003, FR-008). Asking who is signed in and signing out are not public: a visitor who is not signed in gets "not signed in".

### Key Entities

- **Account**: Existing. Its role decides the area; its time zone is refreshed from the browser (FR-019).
- **Sign-in**: One browser's signed-in state for one account: when it started and when it was last used. Ends on sign-out or after 14 days without use. One account can have several, one per device or browser. Called "sign-in", not "session", which is a training occasion (`CONTEXT.md`).
- **Failed sign-ins**: Per email (with or without an account): how many sign-ins failed in the current 15 minutes and until when the email is locked.
- **Request count per address**: How many sign-ups and new-link requests one network address made in the last hour.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A trainer or client goes from opening the sign-in page to their area in under 30 seconds.
- **SC-002**: For an unknown email and for a known email with a wrong password, the answers are identical in status and body, and over 20 attempts of each the median response times differ by less than 100 ms.
- **SC-003**: The sixth attempt within 15 minutes of five failures is refused, also with the right password; an attempt 15 minutes after the fifth failure succeeds with the right password.
- **SC-004**: A person signed in before a backend restart is still signed in after it; one idle for 14 days is signed out.
- **SC-005**: Every page under `/trainer` and `/app` sends a visitor who is not signed in to sign-in and back, and sends the other role to its own area, in 100% of tested routes.
- **SC-006**: After sign-out, the tab keeps no data of the person, and a copy of the browser's credential is refused.
- **SC-007**: Every user-facing string on the sign-in page and the sign-out actions exists in English and Polish, and both pass WCAG 2.2 AA checks for contrast, labels, focus order and visible focus, in light and dark, at 375px and 1280px.

## Assumptions

- The current account carries no settings yet: units, language and theme are stored with the account in task #43, which adds them to this answer. Email is left out until a screen needs it.
- The client settings sheet holds only the account's name and "Sign out" until task #45 adds units, language and theme.
- The trainer sidebar's footer shows the trainer's initials, name and role with a "Sign out" button; the board's client count comes with the clients story.
- "Forgot password?" leads to the reset page of task #41; until then it shows the not-found page. Nothing is deployed before story #63.
- The board disables "Sign in" in the Locked state; it stays enabled here, because the lock is per email and another email may be tried, and a disabled button gives no reason.
- The board's alert says the link was sent to the entered email; no link is sent on sign-in itself, only on "Resend link", which uses the existing new-link request and its 60-second limit.
- Browsers keep a cookie at most 400 days, and the sign-in cookie is written when signing in, so a sign-in used every day still ends 400 days after it started. Accepted: nobody is likely to notice, and rewriting the cookie on every answer would lift it if needed.
- Signing out everywhere comes with password reset (story #39). There is no "remember me": every sign-in lasts as in FR-011.
- Accepted risk: one address trying one common password against many emails is not slowed down, since each email only counts its own failures. Protection in front of the backend can be added with deployment (task #65) if needed.
- The network address is the one the backend sees. Behind a proxy it must be the visitor's address; that is set up with deployment (task #65).
- Local development gets a demo client account with the documented demo password, beside the existing demo trainers, so both areas can be tried.
- The home page (`/`) stays as it is.
