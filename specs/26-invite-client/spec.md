# Feature Specification: Trainer invites a client

**Feature Branch**: `feat/26-invite-client`

**Created**: 2026-10-25

**Status**: Draft

**Input**: Story #26 "Trainer invites a client" and its tasks #27–#29. Screens: Kinetiq Screens, board Trainer → "Trainer · invites" (List, Dialog: enter email, Dialog: link ready, Dialog: has an account, Empty). Joining through the link is story #30.

## Clarifications

### Session 2026-10-25

- Q: The board's per-row "Copy link" cannot show the original link, because both secrets are stored only as hashes (task #27). What does the row's copy action do? → A: "Copy new link" issues a new trainer secret and copies its link; the link copied earlier stops working, the emailed link and the expiry stay as they were.
- Q: How many invite requests may one trainer make per hour, counting refused ones? → A: 20 per rolling hour (FR-008).
- Q: What happens when a trainer invites an email they already have an open invite for? → A: The earlier invite is revoked and replaced by a new 7-day invite with a new email; this is also how a trainer resends (FR-007).
- Q: How long are ended invites (used, revoked, expired) kept? → A: Indefinitely for now; their retention is decided in task #168 (criterion added there).
- Q: The board says "This email already has an account.", the design system "This email already has a Kinetiq account." Which wording? → A: The board's wording, "This email already has an account." (FR-018).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Trainer invites a person by email (Priority: P1)

A trainer enters only the person's email. Kinetiq emails that person an invite and shows the trainer a separate link they can copy and send another way.

**Why this priority**: A trainer without clients cannot use Kinetiq; the invite is the only way a client gets an account.

**Independent Test**: As the demo trainer, invite `julia@example.com`; the dialog shows "Invite sent to julia@example.com" with a copyable link, an invite email with a different link appears in the outgoing mail, and the invite is listed with "In 7 days".

**Acceptance Scenarios**:

1. **Given** no account uses `julia@example.com`, **When** the trainer chooses "Invite client", enters ` Julia@Example.com ` and chooses "Send invite", **Then** an open invite for `julia@example.com` is created, valid 7 days, an invite email is sent to that address, and the dialog shows "Invite sent to julia@example.com" with the copyable invite link.
2. **Given** the "link ready" dialog, **When** the trainer chooses "Copy link", **Then** the link is on the clipboard, the button reads "Copied", and "Link copied" is announced once to assistive technology.
3. **Given** the "link ready" dialog, **When** the trainer chooses "Done" or closes the dialog, **Then** the new invite is at the top of the open invites.
4. **Given** the dialog, **When** the email is empty or malformed, **Then** nothing is sent and the field shows "Enter an email like name@example.com.".
5. **Given** an account (trainer or client, confirmed or not, any letter case) already uses `ola@example.com`, **When** the trainer invites `OLA@example.com`, **Then** no invite is created, no email is sent, and the field shows "This email already has an account.".
6. **Given** a request that bypasses the dialog's checks, **When** it breaks a rule of scenario 4 or 5, **Then** it is rejected with a stable code and nothing is created or sent.
7. **Given** the trainer already has an open invite for `julia@example.com`, **When** they invite that email again, **Then** the earlier invite stops working (both its links), a new 7-day invite replaces it in the list, and a new email is sent.

---

### User Story 2 - Trainer sees open invites (Priority: P1)

The trainer sees every invite that can still be used, with when it was sent and when it expires.

**Why this priority**: Without it the trainer cannot tell who has not joined yet or which links are about to expire.

**Independent Test**: Sign in as the demo trainer; the invites page lists the demo open invites, newest first, and none of another trainer's.

**Acceptance Scenarios**:

1. **Given** the trainer has open invites, **When** they open the invites page, **Then** each shows the email, the day it was sent ("Today", otherwise a short date) and the time left ("In N days"), newest first.
2. **Given** an invite with less than one day left, **When** the list shows, **Then** its time left reads "In 1 day" and is marked as expiring soon, by text as well as colour.
3. **Given** an invite that expired, was used or was revoked, **When** the list shows, **Then** it is not there.
4. **Given** the trainer has no open invites, **When** they open the invites page, **Then** they see "No open invites. Invites you send show up here until the client joins." with "Invite client".
5. **Given** another trainer's invites, **When** this trainer opens the list, **Then** none of them appear.
6. **Given** the list cannot be loaded, **When** the page shows, **Then** the generic error appears with a way to try again.

---

### User Story 3 - Trainer revokes an invite (Priority: P2)

A trainer who invited the wrong address, or changed their mind, revokes the invite so its links stop working.

**Why this priority**: A link anyone can use to join as the trainer's client must be stoppable, but the trainer can invite without it.

**Independent Test**: Revoke an open invite after confirming; it leaves the list, "Invite revoked" is announced, and both of its links stop working.

**Acceptance Scenarios**:

1. **Given** an open invite, **When** the trainer chooses "Revoke", **Then** they are asked "Revoke the invite for julia@example.com?" with "Cancel" and "Revoke invite", and nothing changes until they confirm.
2. **Given** the confirmation, **When** the trainer chooses "Revoke invite", **Then** the invite is revoked, leaves the list, and "Invite revoked" is announced once.
3. **Given** the confirmation, **When** the trainer chooses "Cancel" or presses Escape, **Then** the invite stays open and focus returns to its "Revoke" button.
4. **Given** an invite that is no longer open (used, expired or already revoked), or that belongs to another trainer, or that does not exist, **When** a revoke is requested for it, **Then** the answer is "not found", identical in all these cases, and nothing changes; the page refreshes the list and says "This invite is no longer open.".

---

### User Story 4 - Copy an open invite's link again (Priority: P3)

A trainer who closed the dialog without copying the link gets a link for an open invite from the list.

**Why this priority**: The emailed link is enough for most clients; copying from the list covers an email that did not arrive.

**Independent Test**: From the list, choose "Copy new link" for an open invite; the new link reaches the clipboard and works, the link copied in the dialog no longer does, and the emailed link still does.

**Acceptance Scenarios**:

1. **Given** an open invite in the list, **When** the trainer chooses "Copy new link", **Then** a new link for that invite is on the clipboard, the button reads "Copied", and "New link copied" is announced once.
2. **Given** a new link was copied, **When** anyone uses the link copied earlier for that invite, **Then** it no longer works; the emailed link and the expiry are unchanged.
3. **Given** the "Copy new link" button, **When** it gets focus or the pointer rests on it, **Then** it explains "The link you copied before stops working.", also to assistive technology.

---

### Edge Cases

- Email letter case and surrounding spaces do not matter: ` Julia@Example.com ` and `julia@example.com` are the same address, for the account check and for "already invited".
- Email longer than 254 characters is rejected with the same message as a malformed one.
- A trainer inviting their own email gets "This email already has an account.".
- Two trainers may each have an open invite for the same email; they do not affect each other. Whichever the person uses first decides their trainer; the other stops working once the account exists (story #30).
- Two identical invite requests at the same moment leave exactly one open invite for that email and trainer.
- An invite is open while less than 7 days have passed since it was sent; at exactly 7 days it has expired.
- An email that gets an account after the invite was sent (for example, by trainer sign-up) keeps its invite in the list until it expires or is revoked; using it is refused by story #30.
- "Sent" and "Today" use the trainer's time zone.
- Clipboard access refused by the browser: the "link ready" dialog says "Couldn't copy. Select the link and copy it." and the link field stays selectable; the list's copy action has already replaced the link, so it shows the new link in a dialog to copy by hand instead of losing it.
- The trainer closes the "link ready" dialog without copying: the invite still exists and the email was still sent.
- Email delivery fails or is not configured: the invite is still created and the copyable link still shown; the failure does not reach the trainer.
- A trainer who makes more than 20 invite requests in one hour, refused ones with a well-formed email included, gets "You've sent many invites. Try again later." and nothing is created or sent.

## Requirements *(mandatory)*

### Functional Requirements

**Invite**

- **FR-001**: Only a signed-in trainer MUST be able to create, list, copy and revoke invites, and only their own.
- **FR-002**: An invite MUST hold the invited email (trimmed, lower case, at most 254 characters), the inviting trainer, when it was sent, when it expires (7 days after sending) and its state: open, used or revoked. Expired is not stored; it follows from the time.
- **FR-003**: An invite MUST be usable at most once, only while open and unexpired. Revoking or using it ends it for good.
- **FR-004**: An invite MUST have two secrets: one only in the email, one only for the trainer to copy. Either one MUST identify the same invite, and only that invite; the join step (story #30) learns which one was used, because only the emailed one proves the person controls the address.
- **FR-005**: Secrets MUST be stored only as hashes; a copy of the stored data MUST NOT be enough to use an invite. Secrets MUST NOT appear in logs, except the local-development mail log of FR-010.
- **FR-006**: The system MUST refuse to invite an email that has an account, of any role and whether confirmed or not, with its own stable code.
- **FR-007**: Inviting an email the same trainer already has an open invite for MUST revoke the earlier invite and create a new one, with a new email, in a single step. This is the only way to resend an invite.
- **FR-008**: A trainer MUST NOT make more than 20 invite requests per rolling hour, counting refused ones with a well-formed email (a malformed email reveals nothing and is not counted); further requests are refused with their own stable code and change nothing.

**Email and link**

- **FR-009**: Creating an invite MUST send the invited email a message carrying the join link with the email secret, the trainer's name and the link's validity (7 days), in the language the trainer's UI was shown in when the invite was sent (English or Polish). Until real delivery exists (#65) only the subject is written out; the body that shows the name and validity comes with it.
- **FR-010**: The email MUST go through the existing outgoing-mail capability with its existing local-development and logging rules (spec #2, FR-018).
- **FR-011**: Creating an invite MUST return the copyable join link with the trainer secret; the trainer MUST NOT receive the email secret.
- **FR-012**: "Copy new link" on an open invite MUST replace its trainer secret with a new one and return the new link; the previous trainer link stops working, and the email secret and the expiry do not change.
- **FR-013**: The join link MUST lead to the join page of story #30, on the address Kinetiq is served from.

**List and revoke**

- **FR-014**: The list MUST contain exactly the trainer's open, unexpired invites, newest first, each with email, day sent and days left (rounded up, so "In 1 day" on the last day), and MUST mark invites on their last day as expiring soon.
- **FR-015**: Revoking MUST ask for confirmation first.
- **FR-016**: Copying a new link for, or revoking, an invite that is not open, is another trainer's or does not exist MUST answer "not found", identically in every case and never "forbidden".

**Errors**

- **FR-017**: Every error MUST be a problem detail with a stable code, and for invalid input the code of the faulty field. The UI MUST show it by code in English or Polish, with the generic message for an unknown code or a failed connection, and MUST NOT show server text.
- **FR-018**: The email field MUST have one message per rule, the same whether the dialog or the server found it: "Enter an email like name@example.com." (empty, malformed or too long) and "This email already has an account.". The limit of FR-008 shows "You've sent many invites. Try again later." in the dialog, not tied to the field.

**Screens**

- **FR-019**: The screens MUST follow the "Trainer · invites" board's five states, built from the Kinetiq design system, in English and Polish, light and dark, checked at 1280px. They are reached from the Clients section of the trainer area, which stays marked as current.
- **FR-020**: The dialogs MUST trap focus, close on Escape, and return focus to the control that opened them; after "Send invite" succeeds, focus moves to the "link ready" heading. The email field has a visible label and its errors are announced and linked to it; after a failed send, focus moves to the field. Messages not tied to a field are announced: as an alert inside a dialog, otherwise as a toast. Error toasts stay until dismissed.
- **FR-021**: While a send, copy-from-list or revoke is in progress, its button MUST be disabled, show a busy indicator and keep its label, and a second request MUST NOT be possible.
- **FR-022**: The "link ready" dialog MUST show the link read-only and warn "Anyone with the link can join as this client. Send it only to them.".
- **FR-023**: Editing the email after an error MUST clear that error until the next send.

### Key Entities

- **Invite**: A trainer's single-use offer to one email to join as their client. Email, inviting trainer, sent and expiry times, state (open, used, revoked), the hash of the email secret and the hash of the trainer secret. Expired follows from the expiry time.
- **Account**: Existing (spec #2). Used here only to refuse emails that already have one and to name the trainer in the email.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A trainer goes from opening the invites page to a copied invite link in under 30 seconds.
- **SC-002**: 100% of list, copy and revoke requests for another trainer's invite answer "not found", shown by tests.
- **SC-003**: 100% of used, revoked and expired invites are absent from the list and cannot be revoked again.
- **SC-004**: Every user-facing string of the five states, the revoke confirmation, the messages and the invite email's subject exists in English and Polish.
- **SC-005**: The five states and the revoke confirmation pass WCAG 2.2 AA checks for contrast, labels, focus order and visible focus, in light and dark, at 1280px.
- **SC-006**: Neither secret can be read back from stored data, the list or any response other than the create (trainer secret only) and the email (email secret only).

## Assumptions

- Telling a trainer that an email already has an account reveals that the address uses Kinetiq. The story asks for it; trainers are signed-in, confirmed accounts, and FR-008 limits probing to 20 addresses an hour.
- The page shows only open invites; a history of used, expired and revoked invites is not needed by the story.
- Ended invites are kept indefinitely; their retention is decided in task #168.
- No paging: 20 invites an hour and a 7-day life keep the list small.
- The board's footer ("All clients · 14 active") and the notification bell are left out: the all-clients table comes with task #37, and notifications have no story yet.
- The all-clients screen (task #37) does not exist yet, so the invites page is reached from the Clients section's current placeholder; task #37 adds its own invite action.
- Joining, the join page and the "this link no longer works" page are story #30. Until it is built the emailed and copied links lead to the app's not-found page, accepted because nothing is deployed before story #63.
- Demo open invites for the demo trainer are added to local-only data, including one on its last day.
