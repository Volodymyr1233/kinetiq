# Feature Specification: Trainer signs up

**Feature Branch**: `feat/2-trainer-sign-up`

**Created**: 2026-10-18

**Status**: Draft

**Input**: Story #2 "Trainer signs up" and its tasks #3–#8. Screens: Kinetiq Screens, board Account → "Trainer sign-up" (Form, Check your email, Email confirmed, Link expired).

## Clarifications

### Session 2026-10-18

- Q: Should refusing sign-in for an unconfirmed email be built in this story or with sign-in in story #18? → A: Story #18 (task #20, criterion added there); this story only stores whether the email is confirmed.
- Q: When someone signs up with an email that already has an account, should that address get an email? → A: Yes: a fresh confirmation link if unconfirmed, a "You already have an account" email if confirmed; the account never changes.
- Q: How long should an account that never confirmed its email be kept? → A: Indefinitely for now; the retention rule is decided in task #168 (criterion added there).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Trainer creates an account (Priority: P1)

A trainer opens the sign-up page, enters name, email and password, confirms being 18 or older, and is told to check their email. A confirmation link is sent to that address.

**Why this priority**: Nothing else in Kinetiq works without a trainer account.

**Independent Test**: Submit the form with valid data; the "Check your email" state shows the entered address, an unconfirmed trainer account exists, and a link for it appears in the outgoing mail.

**Acceptance Scenarios**:

1. **Given** no account uses `marek@example.com`, **When** a person signs up with name "Marek Nowak", that email, password `Kinetiq-2026` and the 18-or-older box ticked, **Then** an unconfirmed trainer account is created with the browser's time zone, a confirmation link is sent to that email, and the "Check your email" state names the address.
2. **Given** an account already uses `marek@example.com` (confirmed or not, any letter case), **When** a person signs up with `Marek@Example.com`, **Then** they see the same "Check your email" result as in scenario 1 and no account is created or changed; the address gets a fresh confirmation link if the account is unconfirmed, or a "You already have an account" email with a way to sign in if it is confirmed.
3. **Given** the form, **When** the name is empty, the email is malformed, the password breaks a rule or the 18-or-older box is not ticked, **Then** the form is not sent and each faulty field shows its own message next to it.
4. **Given** a request that bypasses the form's checks, **When** it breaks any of the rules in scenario 3, **Then** it is rejected with a per-field error and no account is created.
5. **Given** the "Check your email" state, **When** the person chooses "Change it", **Then** the form returns with the values they entered, except the password.

---

### User Story 2 - Trainer confirms their email (Priority: P1)

The trainer opens the link from the email. Kinetiq confirms the address and offers to sign in.

**Why this priority**: An unconfirmed account cannot be used, so sign-up is only complete once this works.

**Independent Test**: Open the link sent in story 1; the "Email confirmed" state shows and the account is marked confirmed. Open the same link again; the "This link no longer works" state shows.

**Acceptance Scenarios**:

1. **Given** a link sent less than 24 hours ago and never used, **When** the trainer opens it, **Then** the account becomes confirmed and the "Email confirmed" state offers "Sign in".
2. **Given** a link that was already used, is older than 24 hours, was replaced by a newer one, or never existed, **When** anyone opens it, **Then** they see "This link no longer works" with a way to request a new link, and nothing changes. All four cases look the same.

---

### User Story 3 - Trainer gets a new link (Priority: P2)

A trainer whose email did not arrive, or whose link expired, asks for a new one.

**Why this priority**: Without it, a lost or expired link leaves an account nobody can confirm, and the email can never be used again.

**Independent Test**: From "Check your email", choose "Resend link"; a new link is sent, and the previous link no longer works.

**Acceptance Scenarios**:

1. **Given** an unconfirmed account, **When** a new link is requested for its email, **Then** a new 24-hour link is sent and every earlier link for that account stops working.
2. **Given** an email with no account, or with a confirmed account, **When** a new link is requested for it, **Then** the person sees the same result as in scenario 1, and nothing is sent.
3. **Given** "Check your email", **When** the person chooses "Resend link", **Then** the button is disabled for 60 seconds and a note, announced once to assistive technology, says the link was sent again and that another can be sent in a minute. The button label does not count down.
4. **Given** "This link no longer works", **When** the person enters their email and chooses "Send new link", **Then** they reach "Check your email" for that address.
5. **Given** a link was sent for an account less than 60 seconds ago, **When** another is requested, **Then** the person sees the usual result but no second email is sent.
6. **Given** "Check your email", **When** the page is reloaded, **Then** the empty form shows, because the entered email is not kept anywhere; signing up again sends a fresh link (FR-006).

---

### User Story 4 - Errors in the person's language (Priority: P2)

Every failure the server reports reaches the person as a sentence in the language the UI is shown in, never as server text.

**Why this priority**: The first screens a trainer sees set the quality bar; Polish trainers must not see English server messages.

**Independent Test**: With the UI in Polish, send an invalid sign-up past the form's checks and open an invalid link; both messages appear in Polish.

**Acceptance Scenarios**:

1. **Given** the UI in Polish, **When** the server rejects input, **Then** the message for each field is shown in Polish.
2. **Given** an unexpected server failure, **When** any of these screens shows it, **Then** the person sees a generic "Something went wrong, try again" message in the UI's language, and the response contains nothing about the server's internals.
3. **Given** a failure the UI has no translation for, a response without a code, or no response at all (network down, server unreachable), **When** it is shown, **Then** the generic message is used and the person can try again without losing what they entered, except the password.

---

### Edge Cases

- Email letter case and surrounding spaces do not matter: `Marek@Example.com ` and `marek@example.com` are the same account.
- Name is trimmed; a name of only spaces counts as empty. Longer than 100 characters, or containing control characters such as line breaks or tabs, is rejected. Letters of any alphabet, spaces, apostrophes, hyphens and other punctuation are allowed.
- Email longer than 254 characters is rejected.
- Password of exactly 8 and exactly 128 characters is accepted; 7 and 129 are rejected. Spaces and non-Latin characters are allowed and count as special characters.
- Password is never trimmed, never shown back, and never kept in the form after leaving it.
- Two sign-ups with the same email at the same moment create exactly one account.
- A link opened on another device or browser than the one used to sign up still confirms the account.
- A link opened twice in quick succession confirms once; the second opening shows "This link no longer works".
- A confirmed trainer who opens an old link sees "This link no longer works" and a "Sign in" option.
- The form sends the browser's time zone. If the browser reports none, or one that is not a real zone (some report `Etc/Unknown`), the form sends `UTC` instead; the trainer can change it in settings (story #42). A request that still names an unknown zone was altered outside the form: it is rejected and the person sees the generic message.
- How long a sign-up or resend takes does not reveal whether the email has an account.

## Requirements *(mandatory)*

### Functional Requirements

**Sign-up**

- **FR-001**: The sign-up page MUST ask for name, email, password and "I am 18 or older", and MUST be reachable without signing in.
- **FR-002**: The system MUST accept a password only if it is 8–128 characters long and contains at least one lower-case letter (a–z), one upper-case letter (A–Z), one digit (0–9) and one other character.
- **FR-003**: The system MUST refuse a sign-up that does not confirm the person is 18 or older.
- **FR-004**: The system MUST create the account with the role Trainer, unconfirmed, with the browser's time zone.
- **FR-005**: The system MUST treat emails as equal regardless of letter case and surrounding spaces, and MUST allow at most one account per email.
- **FR-006**: A sign-up with an email that already has an account MUST produce a response identical to a successful sign-up, and MUST NOT create or change any account (recording when an email was last sent, for FR-016, is not a change to the account). It MUST send that address a new confirmation link (replacing earlier ones, as in FR-014) if the account is unconfirmed, or a "You already have an account" email with a sign-in link if it is confirmed.
- **FR-007**: The system MUST store passwords only as a one-way hash with a modern, slow algorithm, never in readable form, and never in logs.
- **FR-008**: The form MUST check the same rules as the server before sending, and MUST show each problem next to its field. Errors first appear when the person submits, then update as they type. The password rules are shown under the field from the start; when the password breaks them, that same text becomes the field's error.

**Confirmation link**

- **FR-009**: After a sign-up, the system MUST send the trainer an email with a confirmation link. The link works while less than 24 hours have passed since it was sent; at exactly 24 hours it has expired.
- **FR-010**: A link MUST confirm its account at most once. A used, expired, replaced or unknown link MUST give one and the same result, and MUST NOT reveal which case applies or which email it belongs to.
- **FR-011**: The system MUST NOT keep the link's secret in readable form; a copy of the stored data MUST NOT be enough to confirm an account.
- **FR-012**: Opening a link MUST NOT confirm the account by itself being fetched (for example by a mail scanner previewing it); confirmation happens only when the page the link opens asks for it.
- **FR-013**: A person MUST be able to request a new link by email, from "Check your email" and from "This link no longer works".
- **FR-014**: A new link MUST make every earlier link of that account stop working.
- **FR-015**: A request for a new link MUST give the same result whether the email has an unconfirmed account, a confirmed account or none; an email is sent only for an unconfirmed account.
- **FR-016**: The system MUST NOT send more than one email per account in any 60 seconds, counting confirmation links and "You already have an account" emails together.
- **FR-017**: After a successful confirmation the page MUST offer to sign in. Confirmation MUST NOT sign the person in.

**Outgoing mail**

- **FR-018**: Every email MUST go through one outgoing-mail capability. Until real delivery exists, no email leaves the system. In local development only, the recipient, subject and link are written to the backend log so a developer can copy the link; this is the one permitted exception to "no personal data in logs". Anywhere else, the log records only that an email of a given kind was not delivered, without recipient or link.
- **FR-019**: Every email MUST be written in the language the UI was shown in when it was requested (English or Polish).

**Errors**

- **FR-020**: Every error response MUST be a problem detail with a stable machine-readable code, and for invalid input, the code of each faulty field. This includes refusals made before a request reaches the feature: a missing or wrong anti-forgery token, and a request that needs a signed-in person.
- **FR-021**: Error responses MUST NOT contain exception messages, SQL, class names, stack traces or any other server internals; an unexpected failure MUST return a generic code only.
- **FR-022**: The UI MUST show every error by its code, in English or Polish, falling back to a generic message for an unknown code, a response without a code, or a failed connection. It MUST NOT show text sent by the server.
- **FR-025**: Each form field MUST have one message per rule it can break, the same whether the form or the server found the failure: name "Enter your name." (empty or invalid characters) and "Use 100 characters or fewer."; email "Enter an email like name@example.com."; password the rules text of FR-008; 18 or older "Confirm that you are 18 or older.". A server failure for a field without its own message shows the generic message.

**Screens**

- **FR-023**: The screens MUST follow the "Trainer sign-up" board's four states (Form, Check your email, Email confirmed, Link expired) plus "Confirming your email" (FR-026), built from the Kinetiq design system, in English and Polish, light and dark. At every width from 375px to 1280px all content and actions are visible without horizontal scrolling, and text stays readable at 200% zoom.
- **FR-024**: Every field MUST have a visible label, errors MUST be announced to assistive technology and linked to their field, and the password field MUST offer show/hide with an accessible name.
- **FR-026**: While a request is in progress, its button MUST be disabled, show a busy indicator and keep its label, and a second submit MUST NOT be possible; the form's fields are read-only meanwhile. Opening a link MUST show a fifth state, "Confirming your email", with a busy indicator, until the result is known.
- **FR-027**: After the view changes state (form to "Check your email", confirming to confirmed or expired, expired to "Check your email"), keyboard focus MUST move to the new state's heading. After a submit with errors, focus MUST move to the first field in error. Messages not tied to a field MUST be announced as alerts.

### Public endpoints

The sign-up, confirm-link and request-new-link operations MUST be reachable without signing in, because the person has no account or session yet. Each one reveals nothing about whether an email is registered (FR-006, FR-010, FR-015). Fetching the anti-forgery token those three requests carry is public for the same reason; it returns no data.

### Key Entities

- **Account**: A person who can sign in. Name, email (unique, case-insensitive), password hash, role (Trainer or Client), time zone, whether the email is confirmed, and when it was created and last changed. A trainer account is created by sign-up; client accounts come with invites (later story).
- **Confirmation link**: A single-use secret that proves control of an account's email. Belongs to one account; has an expiry 24 hours after it was sent and stops working once used. At most one link per account works at a time.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A new trainer goes from opening the sign-up page to "Email confirmed" in under 3 minutes, not counting email delivery.
- **SC-002**: For an email with and without an account, the sign-up and new-link responses are indistinguishable: same status and same body, and over 20 requests of each kind the median response times differ by less than 100 ms.
- **SC-003**: 100% of used, expired, replaced and unknown links produce the same "This link no longer works" result.
- **SC-004**: Every user-facing string on the five states exists in English and Polish; switching language changes all of them, including server errors.
- **SC-005**: The five states pass WCAG 2.2 AA checks for contrast, labels, focus order and visible focus, in light and dark, at 375px and 1280px.
- **SC-006**: No error response in this feature contains server-internal text, shown by tests that force validation and unexpected failures.

## Assumptions

- Refusing sign-in with "confirm your email" until the link is used (story #2, criterion 4) is delivered with sign-in (task #20), after a correct password only. This feature stores whether an email is confirmed, which sign-in will check.
- The "Email confirmed" state offers "Sign in", not "Go to clients" as on the board, because confirming does not sign the person in (task #8). The sign-in screen itself comes with story #18 (task #24); until then "Sign in" leads to the app's not-found page. This is accepted because nothing is deployed before story #63.
- "This link no longer works" asks for the email instead of naming it as the board does, because an expired or unknown link must not reveal its email (FR-010).
- The "Privacy notice" link on the board appears once the privacy notice page exists (story #167).
- Unconfirmed accounts are kept indefinitely; their retention is decided in task #168. Someone who signs up with another person's email cannot block it: the owner gets a fresh link whenever they sign up or ask for one (FR-006, FR-013), and can take the account over by confirming it and resetting the password (story #39).
- Accepted risk: whoever signed up with another person's email chose its password, so once the owner confirms, that person can sign in until the owner resets the password. Closing this would mean a later sign-up replaces the password, which breaks "no account is changed" (FR-006). Password reset ending every session of the account (story #39) limits the window.
- Real email delivery comes with deployment (task #65), which must send outside the request so response times stay equal (SC-002).
- Limiting how many sign-ups one source can make is out of scope. FR-016 already stops one inbox being flooded; a scripted mass sign-up costs the attacker a password hash per request and creates only unconfirmed accounts. Per-source limits belong with the sign-in throttling of task #21.
- Demo accounts for local development use a documented demo password, stored hashed. It opens nothing outside a local database, so it is not a credential in the sense of "never put credentials in SQL".
- A trainer chooses no language or time zone on the form: the time zone is the browser's, and the language is the UI's at the time of the request.
