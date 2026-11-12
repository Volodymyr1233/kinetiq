# Feature Specification: Personal and health data are protected

**Feature Branch**: `feat/167-data-protection`

**Created**: 2026-11-12

**Status**: Draft

**Input**: Story #167 "Personal and health data are protected" and its tasks #168 (data inventory and encryption ADR), #169 (privacy notice page), #170 (delete-account API) and #171 (delete account in settings). Screens: Kinetiq Screens has no board for this story; the boards "Account · sign-up" (the "How we handle your data: Privacy notice" line under "Create account"), "Trainer · settings" and "Today · settings sheet" are the starting points. Design: the Kinetiq design system. Builds on `docs/adr/0004-clients-sixteen-or-older.md`, spec `specs/2-trainer-sign-up` (unconfirmed accounts), `specs/26-invite-client` (ended invites), `specs/30-client-join` (the join form's "Privacy notice" link to `/privacy`), `specs/18-sign-in-out` (sign-ins, the sign-in lock, what signing out clears) and `specs/42-account-settings` (both settings screens); their rules are not repeated here.

## Clarifications

### Session 2026-11-12

- Q: Who does the privacy notice name as responsible for the data, and which address takes privacy requests? → A: "Kinetiq", with the contact `privacy@kinetiq.page`.
- Q: A trainer cannot remove a client today; must a trainer with clients wait until every client deletes their account? → A: Yes, the rule stays. Ending work with a client is backlog story #181 "Trainer ends work with a client", which unblocks the deletion once no client is left; it is not built here.
- Q: How does a person confirm the deletion of their account? → A: By entering their current password in the confirmation; a wrong password counts toward the sign-in lock of that email (FR-006).
- Q: How long are unconfirmed trainer accounts and ended invites kept? → A: An unconfirmed trainer account until 7 days after its last confirmation link was sent; an ended invite (used, revoked or expired) until 1 hour after it ended (FR-015).
- Q: Is the trainer told when a client deletes their account? → A: Not in this story: the client leaves every list silently. A notification without the client's name is task #296 of story #150 (in-app notifications), which also erases notifications that point to an erased client.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Read how Kinetiq handles personal data (Priority: P1)

Before creating an account, a trainer on the sign-up page or an invited person on the join form opens the privacy notice. It says, in their language, which data Kinetiq keeps, why, who receives it, for how long, how it is protected and how to delete it. They come back to the form with nothing lost.

**Why this priority**: Acceptance criterion of the story ("A privacy notice in English and Polish, linked from sign-up and join"). The join form asks for consent to process health data (ADR 0004); that consent is informed only when the notice exists. The join form already links to `/privacy`, which shows not-found today (spec 30).

**Independent Test**: Open the sign-up page, choose "Privacy notice": the notice opens in a new tab, in the page's language, and the sign-up form keeps what was typed. Switch the device's language to Polish: the notice reads in Polish.

**Acceptance Scenarios**:

1. **Given** the sign-up page, **When** the person looks under "Create account", **Then** they see "How we handle your data: Privacy notice", as on the board, and "Privacy notice" opens the notice in a new tab without changing the form.
2. **Given** the join form (spec 30, FR-005), **When** the person chooses "Privacy notice", **Then** the notice opens in a new tab instead of not-found.
3. **Given** anyone, signed in or not, **When** they open `/privacy`, **Then** they see the notice in the UI's language (spec 42, FR-006), with every section of FR-004.
4. **Given** a signed-in trainer or client, **When** they open their settings, **Then** a "Privacy notice" link is there too, so the notice can be found again after joining.

---

### User Story 2 - A client deletes their account and all their data (Priority: P1)

A client who no longer wants to use Kinetiq opens the settings sheet, chooses "Delete account", confirms with their password, and is signed out. Nothing of them remains: no account, quiz, plan, check-ins, logs, plan changes or conversation, and their trainer no longer sees them.

**Why this priority**: Acceptance criterion of the story ("A person can delete their account; a client's data is erased") and of task #170 ("Test proves nothing of the client remains"). Erasure is the right a person most expects for health data, and withdrawing the health-data consent of the join form means leaving Kinetiq (Assumptions).

**Independent Test**: As a demo client with a plan, logs, check-ins, a plan change and messages, delete the account from the sheet. The browser shows the sign-in page with "Your account was deleted.". Signing in with that email and password fails as for an unknown email. The demo trainer's client list, approvals and messages no longer show that client. A test proves that no stored record refers to the client or holds their email.

**Acceptance Scenarios**:

1. **Given** the client settings sheet, **When** the client looks below "Sign out", **Then** they see "Delete account".
2. **Given** the client chooses "Delete account", **When** the confirmation opens, **Then** it says what is erased ("your account, your plan, your check-ins, your logs and your conversation with {trainer first name}"), that sets saved on this phone are discarded, and that it cannot be undone; it asks for the password, and offers "Cancel" and "Delete account".
3. **Given** the confirmation, **When** the client enters their password and chooses "Delete account", **Then** the account and all of its data are erased (FR-008), every sign-in of the account ends on every device, what this browser keeps for the account is cleared as on sign-out (spec 18), and the sign-in page shows "Your account was deleted.", announced to assistive technology.
4. **Given** the confirmation, **When** the password is wrong, **Then** nothing is erased and the password field shows "That password is incorrect."; the attempt counts toward the sign-in lock of that email (spec 18).
5. **Given** the confirmation, **When** the client chooses "Cancel" or closes it, **Then** nothing changes and focus returns to "Delete account".
6. **Given** a client was erased, **When** their trainer opens the client list, approvals or messages, or reloads a page of that client, **Then** the client is not there and the client's pages answer not-found; the trainer gets no message about it (task #296 adds one later).

---

### User Story 3 - A trainer without clients deletes their account (Priority: P2)

A trainer who has no clients opens Settings, chooses "Delete account", confirms with their password, and is signed out. Their account, their own exercises and their invites are erased. A trainer who still has clients sees why they cannot delete it yet.

**Why this priority**: Acceptance criterion of the story ("a trainer can only once they have no clients"). It ranks below the client's erasure because a trainer's account holds no health data.

**Independent Test**: Sign up a new trainer, add an own exercise and an open invite, delete the account from Settings: the sign-in page shows "Your account was deleted.", the invite's links show "This link no longer works", and signing in fails as for an unknown email. As the demo trainer with clients, Settings explains that the account can be deleted once no clients are left, and a delete request sent anyway is refused with its own code.

**Acceptance Scenarios**:

1. **Given** a trainer with no clients, **When** they open Settings, **Then** below Display they see a "Delete account" section saying "This erases your account, your own exercises and your invites. It can't be undone." with a "Delete account" button.
2. **Given** a trainer with clients, **When** they open Settings, **Then** the section says "You have {n} clients. You can delete your account once none are left." (with English and Polish plural forms) and offers no delete button.
3. **Given** a trainer with no clients, **When** they confirm with their password, **Then** the account, its own exercises and all its invites are erased, every sign-in of the account ends, and the sign-in page shows "Your account was deleted.".
4. **Given** a trainer whose client list was empty when Settings opened, **When** a client joins before the trainer confirms, **Then** the deletion is refused with its own code, nothing is erased, and the confirmation shows the "You have {n} clients…" message.
5. **Given** a trainer, **When** the password is wrong or the trainer cancels, **Then** the behaviour is that of User Story 2, scenarios 4 and 5.

---

### User Story 4 - Data nobody uses is not kept, and the decision is recorded (Priority: P2)

Kinetiq stops keeping data that serves no one: trainer sign-ups whose email was never confirmed, and invites that ended. The team has one record of which personal and health data Kinetiq stores, where, why, for how long, who receives it, and why it is protected by TLS, host disk encryption and password hashing rather than per-column encryption.

**Why this priority**: Acceptance criterion of the story ("An ADR records the data inventory and why encryption is…") and the two retention decisions task #168 asks for, both left open by specs 2 and 26. The privacy notice states the same periods, so they must exist before it is accurate.

**Independent Test**: Create a trainer sign-up and leave it unconfirmed; after 7 days since its last confirmation link was sent, the account no longer exists and the same email can sign up afresh. Revoke an invite; within 1 hour it no longer exists. Read `docs/adr/0005-*`: every table holding personal or health data appears in its inventory.

**Acceptance Scenarios**:

1. **Given** a trainer account whose email is unconfirmed, **When** 7 days have passed since its last confirmation link was sent, **Then** the account is erased.
2. **Given** a client account whose email is unconfirmed (joined with a copied link, spec 30), **When** any time passes, **Then** it is kept, because the client uses Kinetiq.
3. **Given** an invite that was used, revoked or has expired, **When** 1 hour has passed since it ended, **Then** it is erased, and its links still show "This link no longer works" (spec 30, FR-003).
4. **Given** the repository, **When** someone reads the data inventory ADR, **Then** it lists each kind of personal and health data, and the retention and encryption decisions of FR-015 and FR-016.

---

### Edge Cases

- A client deletes their account while their first plan is being drafted (spec 79): the draft is never stored, and nothing about the client is sent to the drafting service after the deletion.
- A client deletes their account while their trainer has the conversation open: the trainer's live updates for that conversation stop (spec 147), and the trainer's next action on it shows not-found.
- A client deletes their account while another of their devices holds pending logs: that device's next request finds no sign-in, and signing in fails as for an unknown email, so the pending logs are never stored; they are removed from that phone when someone signs out or signs in as another account there (spec 114, FR-005).
- A trainer's messages, plan, plan changes and decline notes about an erased client are erased with the client: they exist only as part of that client's training.
- The trainer's own exercises used in an erased client's plan are kept; they belong to the trainer.
- An open invite of the client's trainer for the erased client's email is erased with the client. Another trainer's open invite for the same email is that trainer's and is erased by the invite retention rule once it ends.
- After a deletion the email is free: the person can be invited again or sign up again, and starts with nothing.
- A trainer and a person joining through that trainer's invite act at the same moment: either the join succeeds and the deletion is refused, or the deletion succeeds and the join shows "This link no longer works"; never both.
- Two delete requests for the same account at the same moment erase it once; the second gets the answer for a request without a sign-in.
- A delete request whose sign-in has ended gets the standard unauthenticated refusal (spec 18) and erases nothing.
- An email that is locked after failed sign-ins (spec 18) refuses the deletion with the locked message, even with the right password.
- No error response contains server internals.

## Requirements *(mandatory)*

### Functional Requirements

**Privacy notice** (task #169)

- **FR-001**: Kinetiq MUST show a privacy notice at `/privacy`, readable signed in or not, in English and Polish, following the UI language of the device or the account (spec 42, FR-006).
- **FR-002**: The sign-up page MUST show "How we handle your data: Privacy notice" under "Create account", as on the board; the join form keeps its link (spec 30, FR-005). Both open the notice in a new tab and keep what the form holds.
- **FR-003**: Both settings screens MUST link to the privacy notice.
- **FR-004**: The notice MUST say, in plain language: who is responsible for the data and how to contact them; which data Kinetiq keeps, for trainers and for clients, naming the health data (quiz answers, check-ins, logs) separately; why each is kept and on what lawful basis (the account to provide the service, health data on the client's explicit consent, ADR 0004); who receives which data (the client's trainer; the email delivery service; the AI service that drafts plans and plan changes, which receives no names or email addresses); that these services may process data outside the European Economic Area; how long each kind is kept (FR-015); how the data is protected (FR-016); that clients must be 16 or older and trainers 18 or older; how to delete the account and what that erases; the person's rights, including withdrawing consent and complaining to the Polish data protection authority (Prezes Urzędu Ochrony Danych Osobowych); and the date it was last updated.
- **FR-005**: The notice MUST name "Kinetiq" as responsible for the data and `privacy@kinetiq.page` as the address for privacy requests, shown as an email link.

**Deleting an account** (tasks #170, #171)

- **FR-006**: A signed-in person MUST be able to delete their own account and only their own; the request names no account. Deletion MUST require the account's current password. A wrong password erases nothing, gets its own code, and counts toward the sign-in lock of that email (spec 18); while that email is locked, deletion is refused with the locked code.
- **FR-007**: A trainer account MUST be deletable only when no client account is linked to it. Otherwise the deletion is refused with its own code and erases nothing. A join through one of the trainer's invites and that trainer's deletion MUST NOT both succeed.
- **FR-008**: Deleting a client account MUST erase, in one step that is kept whole or not at all: the account; its quiz answers; its plan, the plan's sessions and exercise entries, and any draft or drafting in progress; its session logs and set logs; its check-ins and weekly checks; its plan changes with their content, figures and decisions; its conversation with every message in it from either side and the read markers; its sign-ins; and the invites of its trainer for its email. Afterwards no stored record MUST refer to the account or hold its email or name, and a test MUST prove this for a client with data of every kind.
- **FR-009**: Deleting a trainer account MUST erase, in one step: the account, its own exercises, all its invites (open ones stop working), its records of plan build requests, and its sign-ins.
- **FR-010**: Deleting MUST NOT change any other person's data, other than what FR-008 erases from the client's conversation with their trainer. A test MUST prove that another client's and another trainer's data are unchanged.
- **FR-011**: After a successful deletion, every sign-in of the account MUST end on every device, the browser that deleted MUST clear what it keeps for the account as on sign-out (spec 18), and MUST show the sign-in page with "Your account was deleted.", announced to assistive technology.
- **FR-012**: The client settings sheet MUST show "Delete account" below "Sign out"; trainer Settings MUST show a "Delete account" section below Display. A trainer with clients sees "You have {n} clients. You can delete your account once none are left." and no delete button.
- **FR-013**: "Delete account" MUST open a confirmation that says what is erased for that role, that it cannot be undone (and, for a client, that sets saved on this phone are discarded), asks for the password with a visible label, and offers "Cancel" and a destructive "Delete account". Cancelling or closing changes nothing and returns focus to the button that opened it. While the request runs, "Delete account" cannot be chosen again.
- **FR-014**: A refused deletion MUST keep the confirmation open and show a translated message chosen by the error's code: wrong password on the field ("That password is incorrect."), locked (spec 18's message with the minutes left), trainer with clients (FR-012's message), and a generic message for any other code or no response. Server text is never shown.

**Retention and protection** (task #168)

- **FR-015**: Kinetiq MUST keep personal data only as long as follows, and erase it automatically: an account and its data until the person deletes it (FR-008, FR-009); a trainer account whose email was never confirmed until 7 days after its last confirmation link was sent; an invite until 1 hour after it was used, revoked or expired; a sign-in until it ends (spec 18). Client accounts are never erased for an unconfirmed email.
- **FR-016**: The decision on protecting stored data MUST be recorded as `docs/adr/0005-…`: data travels only over TLS, the database host's disks are encrypted, passwords and link secrets are kept only as one-way hashes, and columns are not encrypted one by one; the ADR gives the reason (what each measure defends against, and why per-column encryption with a key held by the same application adds little) and contains the data inventory: each kind of personal or health data, where it is kept, why, its lawful basis, who receives it and how long it is kept.

**Screens**

- **FR-017**: The privacy notice MUST read well at 375 px and at 1280 px; the trainer section at 1280 px; the client sheet and its confirmation at 375 px. All are built from Kinetiq tokens and shadcn components, in light and dark, with headings in order and a visible focus.
- **FR-018**: Every text of this story MUST exist in English and Polish, including the notice itself.

### Public endpoints

None on the server. The privacy notice is a page of the app with no data of its own, readable without signing in because it must be read before an account exists. Deleting an account needs a signed-in person (FR-006).

### Key Entities

- **Privacy notice**: The public page that tells a person which of their data Kinetiq keeps, why, who receives it, for how long and how to delete it. One version, in English and Polish, with its last-updated date.
- **Account deletion**: The erasure of one account and everything that exists only because of it (FR-008, FR-009). Not reversible and not kept as a record.
- **Data inventory**: The list in the ADR of each kind of personal or health data Kinetiq stores, with where, why, lawful basis, recipients and retention.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: After a client deletes their account, a test that searches every stored record finds 0 records referring to that client or holding their email, for a client with data of every kind.
- **SC-002**: A client deletes their account from the settings sheet in under 1 minute, in at most three choices plus typing the password.
- **SC-003**: 100% of delete requests for a trainer with at least one client are refused and erase nothing.
- **SC-004**: 100% of unconfirmed trainer accounts older than 7 days since their last confirmation link, and of invites ended more than 1 hour ago, are gone after the next automatic clean-up.
- **SC-005**: Every text of the notice, the delete section, the confirmation and the messages exists in English and Polish.
- **SC-006**: The notice at 375 px and 1280 px, the trainer section at 1280 px and the client sheet with its confirmation at 375 px pass WCAG 2.2 AA checks for contrast, labels, focus order and visible focus, in light and dark.
- **SC-007**: No error response of this story contains server-internal text, shown by tests that force a wrong password, a trainer with clients and an unexpected failure.

## Assumptions

- Withdrawing the health-data consent of the join form is done by deleting the account: a client's plan cannot be built or followed without that data. The notice says so.
- The rights to access, correct and receive a copy of one's data are exercised by writing to the contact address of FR-005; a self-service export is not in this story.
- Kinetiq is the controller of all data, including what trainers write about their clients; trainers are not treated as separate controllers.
- The notice names the kinds of services that receive data and the services in use today (email delivery and the AI service); the hosting provider is named once the deployment story chooses it.
- No email is sent after a deletion: it would need the address after it was erased. Requiring the password limits a deletion by someone else on an unattended device.
- A trainer with clients cannot remove a client in Kinetiq today, so such a trainer can delete their account only once every client deleted theirs. Backlog story #181 "Trainer ends work with a client" closes this gap; the delete section's text already holds once it exists.
- The mailbox `privacy@kinetiq.page` exists before deployment (story #63); the notice's text is confirmed by whoever owns privacy, as bug #242 asks.
- This story's pull request also closes bug #242 (the join form's "Privacy notice" link leads to not-found).
- A copy of one's data is backlog story #186 "Person downloads their data".
- Retention periods are chosen as: 7 days for an unconfirmed trainer account (its confirmation link lasts 24 hours, and the owner can sign up again at any time, spec 2); 1 hour for an ended invite (nothing reads it after it ends, since an ended or unknown link shows the same page, spec 30, FR-003).
- Data already deleted stays in the database host's backups until they expire; the backup period is set with deployment and stated in the ADR then.
- The new terms "Privacy notice" and "Account deletion" are added to `CONTEXT.md` with this story.
- Not in this story: clients under 16 (ADR 0004); a data export; a cookie banner (Kinetiq sets only the sign-in and anti-forgery cookies it needs to work, which need no consent); changing the email or password.
