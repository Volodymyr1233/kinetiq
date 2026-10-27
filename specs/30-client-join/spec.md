# Feature Specification: Client joins through an invite

**Feature Branch**: `feat/30-client-join`

**Created**: 2026-10-26

**Status**: Draft

**Input**: Story #30 "Client joins through an invite" and its tasks #31–#34. Screens: Kinetiq Screens, board Account → "Client join from invite" (Form, Link no longer works); confirm-email banner on Client → "Today · check-in" (Email: Not confirmed). Inviting is story #26.

## Clarifications

### Session 2026-10-26

- Q: Where should a new client land right after "Create account" succeeds? → A: The board's "You're in, {first name}" state with "Start the quiz", built here (FR-010).
- Q: On which client pages should the "Confirm your email" banner show while the email is unconfirmed? → A: Only the tab-bar pages (Today, Plan, Progress, Messages); not the quiz or "You're in" (FR-016).
- Q: What should the form's "Privacy notice" link do, given that the privacy notice page (task #169) doesn't exist yet? → A: Link to `/privacy`, which shows not-found until task #169 builds the page (FR-005).
- Q: What should happen when someone who is already signed in to Kinetiq in this browser opens a working invite link? → A: The form shows as usual; joining ends the earlier sign-in in this browser and signs in the new client (FR-010).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Invited person creates their client account (Priority: P1)

A person opens the invite link from their email. Kinetiq shows who invited them and the invited email, asks for name, password, "I am 16 or older" and consent to processing health data, and on "Create account" creates their client account, links it to the inviting trainer and signs them in.

**Why this priority**: The invite is the only way a client gets an account; without joining, no client can train with Kinetiq.

**Independent Test**: As the demo trainer, invite `anna@example.com`; open the emailed link from the local mail log; the form shows "Marek Nowak invited you" and `anna@example.com` read-only; submit valid details; the person is signed in and sees "You're in, Anna" with "Start the quiz", the account is a confirmed client of the demo trainer, and the invite has left the trainer's open invites.

**Acceptance Scenarios**:

1. **Given** an open invite from Marek Nowak to `anna@example.com`, **When** a person opens its emailed link, **Then** the form shows Marek's initials and "Marek Nowak invited you", the heading "Join Marek on Kinetiq", the email `anna@example.com` read-only with "From your invite. You sign in with it.", and empty fields for name and password, two unticked boxes, "Create account" and "Have an account? Sign in".
2. **Given** the form, **When** the person enters name "Anna Kowalska", password `Kinetiq-2026`, ticks "I am 16 or older" and the consent, and chooses "Create account", **Then** a client account is created with that name, the invited email, the browser's time zone, the trainer's unit, the UI's language and the device's theme; it is linked to Marek; the consent time is stored; the invite is used; and the person is signed in and sees Marek's initials, "You're in, Anna", "Answer 7 questions. Marek uses them to build your plan." and "Start the quiz".
3. **Given** the "You're in" state, **When** the person chooses "Start the quiz", **Then** they reach the quiz's first step.
4. **Given** the person joined from the emailed link, **When** they reach the client app, **Then** their email is confirmed, no confirmation email is sent and no "Confirm your email" banner shows.
5. **Given** the form, **When** the name is empty, the password breaks a rule, or either box is unticked, **Then** nothing is sent and each faulty field shows its own message next to it.
6. **Given** a request that bypasses the form's checks, **When** it breaks any rule of scenario 5, **Then** it is rejected with a per-field error and nothing is created or changed, and the invite stays open.
7. **Given** the form, **When** the person chooses "Privacy notice", **Then** the privacy notice opens in a new tab and the form keeps what they entered.

---

### User Story 2 - A link that no longer works (Priority: P1)

A person who opens an invite link that expired, was used, was revoked, never existed, or whose email meanwhile got an account sees one "This link no longer works" page.

**Why this priority**: Every invite ends; a dead link must lead somewhere useful and must not reveal anything about the invite.

**Independent Test**: Open a revoked, an expired, a used and a made-up link; all four show the same page with "Sign in instead".

**Acceptance Scenarios**:

1. **Given** an invite link that expired, was used, was revoked or replaced, whose copied link was replaced, or that never existed, **When** anyone opens it, **Then** they see "This link no longer works", "The invite expired, was already used or was cancelled. Ask your trainer for a new one.", "Sign in instead" and "Use this if you already joined."; all cases look the same, and no email or trainer name shows.
2. **Given** an open invite whose email got an account after it was sent, **When** anyone opens it, **Then** they see the same page.
3. **Given** the form is showing, **When** the invite stops working before "Create account" (revoked, expired, used in another tab, or the email got an account) and the person submits, **Then** nothing is created and the page changes to "This link no longer works".
4. **Given** the page, **When** the person chooses "Sign in instead", **Then** they reach the sign-in page, or their own area if they are already signed in.

---

### User Story 3 - Joining from a copied link and confirming the email (Priority: P2)

A person who joined with the link their trainer copied and sent another way has not proved they control the invited email. They are signed in and can use everything, while a banner asks them to confirm their email, with "Resend".

**Why this priority**: The copied link reaches people whose invite email did not arrive; confirming the address keeps sign-in and password reset tied to a real inbox. Nothing is blocked meanwhile, so it ranks below joining itself.

**Independent Test**: Join with a copied link; the client app shows "Confirm your email: we sent a link to anna@example.com." with "Resend" and a confirmation link appears in the mail log; open it; on returning to the client app the banner is gone.

**Acceptance Scenarios**:

1. **Given** an open invite, **When** the person joins with its copied link, **Then** the account is created unconfirmed, a confirmation link (valid 24 hours) is sent to the invited email in the UI's language, and the person is signed in and sees the "You're in" state.
2. **Given** a signed-in client whose email is not confirmed, **When** any tab-bar page of the client app (Today, Plan, Progress, Messages) shows, **Then** the banner "Confirm your email: we sent a link to {email}." with "Resend" shows under the app bar, and every page and action works as for a confirmed client; the quiz and "You're in" show no banner.
3. **Given** the banner, **When** the client chooses "Resend", **Then** a new link is sent (the earlier one stops working), the button reads "Sent" and is disabled for 60 seconds, and "We sent a new link." is announced once to assistive technology.
4. **Given** an email was sent to the account less than 60 seconds ago (for example, right after joining), **When** the client chooses "Resend", **Then** they see the same result as in scenario 3 but no second email is sent.
5. **Given** the client confirmed their email from the link, in this browser or another device, **When** they return to the client app tab or reload it, **Then** the banner is gone and does not come back.
6. **Given** a client who is signed in, **When** they open the confirmation link in the same browser, **Then** "Email confirmed" offers "Continue", which leads to the client app; **given** nobody is signed in, **Then** it offers "Sign in".
7. **Given** a client whose email is confirmed, **When** any page of the client app shows, **Then** no banner shows.

---

### User Story 4 - The trainer has the client (Priority: P2)

Joining makes the person the inviting trainer's client, and only that trainer's.

**Why this priority**: Every later client feature (all clients, plans, logs, messages) is scoped by this link; without it a joined client belongs to nobody.

**Independent Test**: After two people join, one through each of two trainers' invites, each trainer's clients are exactly the one who joined through their invite.

**Acceptance Scenarios**:

1. **Given** a person joined through Marek's invite, **When** Marek's clients are listed, **Then** the person is among them, and is among no other trainer's clients.
2. **Given** Marek and Ola both have an open invite for `anna@example.com`, **When** Anna joins through Ola's, **Then** Anna is Ola's client only, and Marek's invite no longer works (User Story 2, scenario 2).
3. **Given** any client, **When** their trainer is looked up, **Then** there is exactly one.

---

### Edge Cases

- Opening a link never uses the invite or confirms anything by itself (for example, when a mail scanner previews it); only "Create account" does.
- Reloading the join page shows the form again for the same invite, without the password. Reloading it after joining shows "This link no longer works", whose "Sign in instead" takes the signed-in client to the client app.
- The page shows "Checking your invite" with a busy indicator until the link is known to work or not. A failed connection or unexpected error while checking shows the generic message with "Try again", not "This link no longer works".
- Name is trimmed; a name of only spaces counts as empty. Longer than 100 characters or containing control characters is rejected, as at trainer sign-up. A client cannot change their name later, so the form says nothing more about it.
- Password of exactly 8 and exactly 128 characters is accepted; 7 and 129 are rejected. Spaces and non-Latin characters are allowed and count as special characters. The password is never trimmed, never shown back, and never kept after leaving the page.
- The browser reports no time zone or one that is not real: the form sends `UTC`, as at trainer sign-up.
- The trainer's unit is copied when the client joins; a later change of the trainer's unit does not change the client's.
- The heading and description name the trainer by the first word of their name ("Join Marek on Kinetiq"); the line above shows the full name.
- A long trainer name or email wraps without horizontal scrolling at 375px.
- The same link submitted twice at the same moment, or two trainers' invites for the same email used at the same moment, create exactly one account; the other request sees "This link no longer works".
- A person already signed in to Kinetiq in this browser (trainer or client) can open the join page; joining ends that sign-in in this browser and signs in the new client.
- A signed-in client who opens an invite they already used sees "This link no longer works"; "Sign in instead" takes them to their own area.
- Joining with the emailed link confirms the email even if the person also has the copied link; the copied link stops working once the invite is used.
- A confirmation link sent after joining with a copied link that is lost or expires: "Resend" in the banner sends a new one; the "This link no longer works" page of the confirmation link also offers a new one, as for trainers.

## Requirements *(mandatory)*

### Functional Requirements

**Checking an invite**

- **FR-001**: The join page MUST be reachable without signing in, at the address the invite links lead to (story #26, FR-013).
- **FR-002**: The system MUST tell the join page, for a link that works, the invited email and the inviting trainer's name, and nothing else; for any link that does not work it MUST give one and the same result with one stable code.
- **FR-003**: A link works only while its invite is open and unexpired, the secret is the invite's current emailed or copied secret, and no account uses the invited email. Expired, used, revoked, replaced, unknown and taken-email links MUST be indistinguishable.
- **FR-004**: Checking a link MUST NOT change anything.

**Joining**

- **FR-005**: The form MUST show the invited email read-only and ask for name, password, "I am 16 or older" and "I agree that Kinetiq processes my health data (sleep, soreness, stress, body weight and training logs) so {trainer first name} can plan my training.", with a "Privacy notice" link to `/privacy` that opens in a new tab.
- **FR-006**: The system MUST accept a password only if it is 8–128 characters long and contains at least one lower-case letter (a–z), one upper-case letter (A–Z), one digit (0–9) and one other character, as at trainer sign-up.
- **FR-007**: The system MUST refuse to join without "I am 16 or older" and without the consent, each with its own field code.
- **FR-008**: Joining MUST, in one step: create a client account with the name, the invited email, the password stored only as a slow one-way hash, the time zone sent by the browser, the trainer's unit, the language the UI was shown in and the theme system; link it to the inviting trainer; store when the consent was given; and mark the invite used. If any part fails, nothing is kept.
- **FR-009**: Joining with the emailed secret MUST mark the email confirmed. Joining with the copied secret MUST leave it unconfirmed and send a confirmation link to the invited email, in the UI's language, through the existing outgoing-mail capability with its local-development and logging rules (spec #2, FR-018 and FR-019).
- **FR-010**: Joining MUST sign the new client in, as a successful sign-in does (a new sign-in for this browser, replacing any earlier one here). The page MUST then show "You're in, {client first name}" with the trainer's initials, "Answer 7 questions. {trainer first name} uses them to build your plan." and "Start the quiz", which leads to the quiz's first step.
- **FR-011**: A join with a link that does not work (FR-003), including one that stopped working after the form was shown, MUST create and change nothing and give the result of FR-002.
- **FR-012**: The form MUST check the same rules as the server before sending and show each problem next to its field. Errors first appear when the person submits, then update as they change the field. The password rules show under the field from the start; when the password breaks them, that text becomes the field's error.
- **FR-013**: Each field MUST have one message per rule, the same whether the form or the server found it: name "Enter your name." (empty or invalid characters) and "Use 100 characters or fewer."; password the rules text "8–128 characters with at least one lower-case letter, one upper-case letter, one digit and one special character."; age "Confirm that you are 16 or older."; consent "Agree to this to join. Your plan depends on this data.".

**Trainer and client**

- **FR-014**: Every client MUST have exactly one trainer, set when they join; it does not change in this story.
- **FR-015**: The system MUST be able to list a trainer's clients, containing exactly the clients linked to that trainer. This story adds no screen for it.

**Confirming the email**

- **FR-016**: While a signed-in client's email is not confirmed, every tab-bar page of the client app (Today, Plan, Progress, Messages), and no other page, MUST show the banner "Confirm your email: we sent a link to {email}." with "Resend", and MUST NOT block any page or action.
- **FR-017**: "Resend" MUST request a new confirmation link for the client's own email under the existing rules (a new link replaces earlier ones; at most one email per account in any 60 seconds) and then show "Sent", disabled, for 60 seconds, announcing "We sent a new link." once.
- **FR-018**: The banner MUST disappear once the email is confirmed, at the latest when the client returns to the client app tab or reloads it, and MUST NOT show for a confirmed email.
- **FR-019**: "Email confirmed" MUST NOT name a role, and MUST offer "Continue" to the person's own area when they are signed in, otherwise "Sign in".

**Errors**

- **FR-020**: Every error MUST be a problem detail with a stable code, and for invalid input the code of each faulty field. The UI MUST show it by code in English or Polish, with the generic message for an unknown code, a response without a code, or a failed connection, and MUST NOT show server text or server internals.

**Screens**

- **FR-021**: The screens MUST follow the "Client join from invite" board's three states (Form, You're in, Link no longer works) plus "Checking your invite", and the "Today · check-in" board's banner, built from the Kinetiq design system, in English and Polish, light and dark, checked at 375px; at every width from 375px to 1280px all content and actions are visible without horizontal scrolling, and text stays readable at 200% zoom.
- **FR-022**: Every field MUST have a visible label, errors MUST be announced and linked to their field, the password field MUST offer show/hide with an accessible name, and both boxes MUST be checkboxes whose whole label toggles them, with a touch target of at least 44px.
- **FR-023**: While joining is in progress, "Create account" MUST be disabled, show a busy indicator and keep its label, the fields MUST be read-only, and a second submit MUST NOT be possible.
- **FR-024**: When the page changes state (checking to form, form to "You're in", checking or form to "This link no longer works"), focus MUST move to the new state's heading. After a submit with errors, focus MUST move to the first field in error. Messages not tied to a field MUST be announced as alerts.

### Public endpoints

Checking an invite and joining MUST be reachable without signing in, because the person has no account yet. Both need a secret of 256 random bits; neither reveals anything for a link that does not work (FR-002, FR-003), and a working link only shows its own email and trainer to whoever holds it. Fetching the anti-forgery token for the join request is already public (spec #2).

### Key Entities

- **Account**: Existing (spec #2). A client account is created here; it additionally records when its holder consented to processing health data. Clients start with their trainer's unit (spec #42).
- **Client's trainer**: The link from a client account to exactly one trainer account, made when the client joins. A trainer has any number of clients.
- **Invite**: Existing (spec #26). Joining marks it used; which of its two secrets was used decides whether the email is confirmed.
- **Confirmation link**: Existing (spec #2). Sent after joining with a copied link and from the banner.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An invited person goes from opening the link to "You're in", signed in, in under 2 minutes.
- **SC-002**: 100% of expired, used, revoked, replaced, unknown and taken-email links give the same result, on the page and in the response.
- **SC-003**: 100% of clients who joined from the emailed link are confirmed and never see the banner; 100% of clients who joined from a copied link see it until they confirm.
- **SC-004**: In tests, every trainer's clients are exactly the people who joined through that trainer's invites, and every client has exactly one trainer.
- **SC-005**: Every user-facing string of the four join states, the banner and the changed "Email confirmed" state exists in English and Polish.
- **SC-006**: The join states and the banner pass WCAG 2.2 AA checks for contrast, labels, focus order and visible focus, in light and dark, at 375px.
- **SC-007**: No response of this feature contains server-internal text, shown by tests that force validation and unexpected failures.

## Assumptions

- After joining, the client sees the board's "You're in" state and starts the quiz from it. Sending a client who has not submitted the quiz to it on every other entry stays with client entry routing (task #53).
- The quiz, a focused flow with its own layout, shows no banner; the client sees it once they reach the tab-bar pages, and the confirmation email has arrived meanwhile.
- The "Privacy notice" link leads to `/privacy`, which the privacy notice page (task #169) builds; until then it shows the app's not-found page. This is accepted because nothing is deployed before story #63.
- Only the consent time is stored, not a version of the notice or the person's age: ADR 0004 decides that no birth date is kept, and the notice has no versions yet (task #169).
- No per-source limit on checking or joining: guessing a 256-bit secret is not feasible, and a working secret can be used once. Mail flooding from "Resend" is already limited to one email per account per 60 seconds.
- Joining while signed in replaces the sign-in in this browser rather than refusing, so a trainer trying their own invite link or a shared device does not get stuck.
- The trainer's name is shown to anyone holding a working link; the trainer chose to send it to them.
- Switching trainers is a separate backlog story; a client keeps the trainer they joined.
- Demo data adds the client–trainer link for the demo client, so local development has a client with a trainer.
