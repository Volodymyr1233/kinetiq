# Feature Specification: Trainer and client change their settings

**Feature Branch**: `feat/43-account-settings`

**Created**: 2026-10-25

**Status**: Draft

**Input**: Story #42 "Trainer and client change their settings" and its tasks #43 (settings stored with the account), #44 (trainer settings screen), #45 (client settings sheet) and #46 (weight display and input in the chosen unit). Screens: Kinetiq Screens, board Trainer → "Trainer · settings" and board Client → "Today · settings sheet". Design: the Kinetiq design system. Builds on spec `specs/18-sign-in-out` (current account, the client settings sheet, the time zone) and spec `specs/50-client-quiz` (body weight); their rules are not repeated here.

## Clarifications

### Session 2026-10-25

- Q: What happens to changes on the trainer settings screen before "Save changes", and when the trainer leaves the screen? → A: They do not apply before saving; leaving the screen discards them without a warning, as the board shows no such dialog.
- Q: Does the quiz ask for the body weight in lb when the client's units are lb? → A: Yes. The quiz's body weight and the review show the client's unit; the weight is kept in kg.
- Q: Which settings does a new trainer account start with, and an account that exists before this story? → A: A new trainer: kg, the language the UI was shown in during sign-up, theme system. An existing account: kg, English, system.
- Q: Which settings does a client start with when joining through an invite? → A: The trainer's units (story #30), the language the join form was shown in, and theme system.
- Q: How precise is a weight in lb? → A: Shown with at most one decimal and kept in kg to 0.1 kg, so a typed lb value can show 0.1 lb different afterwards.
- Q: How does the trainer learn that saving the settings screen worked? → A: A short "Settings saved." shows under the button and is announced to assistive technology; "Save changes" can no longer be chosen until something changes again.
- Q: Which language do pages before sign-in use on a device where someone already chose one? → A: The language last used on that device, like the theme; without such a choice, the browser's language.
- Q: What does "Save changes" store when a setting was changed on another device after the screen opened? → A: Only the values changed on this screen; the others stay as stored.
- Q: What does the note under the quiz's body weight say for a client with units lb? → A: It names the other unit: "Switch to kg later in settings." for lb, the board's "Switch to lb later in settings." for kg.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Settings follow the person to every device (Priority: P1)

A trainer or a client chooses units, language and theme once. Wherever they sign in next, Kinetiq already looks and reads the way they chose.

**Why this priority**: Acceptance criterion of the story ("kept with the account") and of task #43 ("applied on sign-in on any device"). The two screens of the story store their choices here.

**Independent Test**: Change the language to Polish and the theme to dark on one device. Sign in with the same account in another browser: the app opens in Polish and dark at once.

**Acceptance Scenarios**:

1. **Given** an account with units lb, language Polish and theme dark, **When** the person signs in on any device, **Then** the app shows Polish texts, the dark theme and weights in lb, without asking anything.
2. **Given** a signed-in person, **When** they open or reload the app, **Then** it shows the account's settings, not the browser's language or the device's earlier theme.
3. **Given** a new trainer account, **When** it is created, **Then** it starts with units kg, the language the UI was shown in during sign-up, and theme system.
4. **Given** a request that sets a value outside the allowed ones (units other than kg or lb, a language other than English or Polish, a theme other than light, dark or system), **When** the server receives it, **Then** it is refused with a code for that field and nothing changes.

---

### User Story 2 - Trainer changes their name and display settings (Priority: P1)

A trainer opens Settings in the trainer area, changes their name, units, language or appearance, and saves.

**Why this priority**: Acceptance criteria of the story ("A trainer can change their name") and of task #44 ("Unsaved changes are kept until Save").

**Independent Test**: Sign in as a trainer, open Settings, change the name and the language, save: the sidebar shows the new name and the app switches to the new language. Reload: both stay.

**Acceptance Scenarios**:

1. **Given** the trainer area, **When** the trainer opens Settings, **Then** they see, as on the board "Trainer · settings", Profile with Name (editable) and Email (read-only), and Display with Units (kg, lb), Language (English, Polski) and Appearance (Light, Dark, System), each showing the stored value, and "Save changes".
2. **Given** the settings screen with nothing changed, **When** the trainer looks at "Save changes", **Then** it cannot be chosen.
3. **Given** the settings screen, **When** the trainer changes the language or the appearance, **Then** nothing changes in the app until they choose "Save changes"; after saving, the app shows the new language and appearance at once and "Settings saved." under the button.
4. **Given** a name of only spaces, longer than 100 characters, or with a line break, **When** the trainer saves, **Then** the name field shows its message and nothing is saved.
5. **Given** a save that fails, **When** the server does not accept it or does not answer, **Then** the screen keeps the trainer's changes and shows a translated message, so they can try again.

---

### User Story 3 - Client changes display settings in the sheet (Priority: P1)

A client opens the settings sheet from the app's header and switches units, language or theme. Each choice applies and is saved at once.

**Why this priority**: Acceptance criterion of task #45 ("Changes apply immediately"). Clients use Kinetiq mostly on a phone and expect a sheet that saves without a button.

**Independent Test**: Sign in as a client, open the sheet, choose Polski: the sheet and the app behind it switch to Polish at once. Sign in on another device: Polish.

**Acceptance Scenarios**:

1. **Given** the client app, **When** the client opens the settings sheet, **Then** they see, as on the board "Today · settings sheet", their avatar, name and email, Units (kg, lb), Language (English, Polski), Theme (Light, Dark, System) with the board's hints, "Changes save right away." and "Sign out" (spec 18).
2. **Given** the sheet, **When** the client chooses another option, **Then** it applies at once and is saved without a further step.
3. **Given** a choice whose saving fails, **When** the server does not accept it or does not answer, **Then** the option returns to the stored value and a translated message shows.
4. **Given** the sheet, **When** the client looks at their name, **Then** it is shown but cannot be changed there.

---

### User Story 4 - Weights in the chosen unit (Priority: P1)

Whoever chose lb sees and enters every weight in lb; Kinetiq keeps it in kg.

**Why this priority**: Acceptance criterion of the story ("Weights are stored in kg and shown and entered in the chosen unit everywhere") and of task #46.

**Independent Test**: As a client with units lb, enter the body weight in the quiz as 137.8 lb: the review shows 137.8 lb, and what is kept is 62.5 kg. Switch to kg: the review shows 62.5 kg.

**Acceptance Scenarios**:

1. **Given** a client with units lb, **When** they reach the quiz's body weight (spec 50, step 4), **Then** the field shows lb, its example and its range message are in lb, the note under it reads "Switch to kg later in settings.", and the value they type is kept in kg.
2. **Given** a stored weight, **When** it is shown in lb and saved back without a change, **Then** the stored kg value is the same as before.
3. **Given** a person who switches between kg and lb, **When** they look at a weight, **Then** it shows in the new unit and no stored weight has changed.

---

### Edge Cases

- A setting changed on one device reaches another device that is already open when that device reloads or signs in; open tabs are not updated while they stay open.
- Theme "system" follows the device's light or dark setting, also when the device switches while Kinetiq is open.
- Switching the language changes every text and the number format at once, without a reload.
- Two quick choices in the client sheet: the last one is what stays stored and shown.
- A trainer who changes only the name while the theme was changed on another device keeps that other device's theme: saving stores only what changed on the screen.
- A trainer who leaves the settings screen without saving loses the unsaved changes; the stored settings stay as they were.
- A name is trimmed before it is checked and stored; "  Marek  " is stored as "Marek".
- Before sign-in, the pages use the language and theme last used on that device (the browser's language when none was chosen); after sign-in, the account's settings apply and become that device's language and theme, so after signing out the sign-in page keeps them.
- A weight typed in lb is kept to 0.1 kg, so when it is shown again it can differ from what was typed by 0.1 lb (137.9 lb is kept as 62.6 kg and shows as 138.0 lb).
- A body weight in lb outside the bounds of spec 50 once converted (below 30 kg or above 250 kg) is refused with the field's message, which names the bounds in lb.
- No error response contains server internals.

## Requirements *(mandatory)*

### Functional Requirements

**Settings on the account** (task #43)

- **FR-001**: Every account MUST have three settings: units (kg or lb), language (English or Polish) and theme (light, dark or system).
- **FR-002**: A new trainer account MUST start with units kg, the language the UI was shown in during sign-up, and theme system. A client's starting settings are set when the client joins (Assumptions).
- **FR-003**: The app MUST be able to ask for the signed-in account's settings, name and email, together with what it already reads about the current account (spec 18, FR-018).
- **FR-004**: The signed-in person MUST be able to change their own settings; a trainer MUST also be able to change their own name. The operations address only the signed-in account, so nobody can read or change another person's settings. A client's request to change a name is refused with a code; a request without a session gets the standard unauthenticated refusal (spec 18).
- **FR-005**: The server MUST refuse a value outside FR-001, and a name that breaks the sign-up rules (spec 2: trimmed, 1–100 characters, no control characters), with a code per field. Nothing changes on a refusal.
- **FR-006**: After sign-in, and whenever the app loads for a signed-in person, it MUST show the account's language, theme and units on any device (issue #43). Before sign-in, pages use the language and theme last used on that device; without such a choice, the browser's language and theme system.

**Trainer settings screen** (task #44)

- **FR-007**: Settings in the trainer area MUST show, as on the board "Trainer · settings": Profile with Name and Email, and Display with Units, Language and Appearance with the board's hints, and "Save changes". Email is shown, not changed. Table density and notification settings are not shown (issue #42).
- **FR-008**: Changes on the screen MUST NOT apply until the trainer chooses "Save changes", which can be chosen only when a value differs from the stored one (issue #44). Saving stores only the values changed on the screen; the others stay as stored, also when another device changed them after the screen opened. After a successful save, the new name shows wherever the trainer's name shows, the new language, appearance and units apply at once, and "Settings saved." shows under the button and is announced to assistive technology until the trainer changes something again.
- **FR-009**: The name field MUST show the sign-up messages (spec 2, FR-025) when the name breaks a rule, the same whether the screen or the server found it.
- **FR-010**: When a save fails, the screen MUST keep the trainer's changes and show a translated message chosen by the error's code (generic when the code is unknown or there is no response). Server text is never shown.

**Client settings sheet** (task #45)

- **FR-011**: The settings sheet MUST show, as on the board "Today · settings sheet": the avatar, name and email; Units, Language and Theme with the board's hints; "Changes save right away."; and "Sign out" (spec 18, FR-014). The name is not editable.
- **FR-012**: A choice in the sheet MUST apply and be saved at once (issue #45). When saving fails, the choice MUST return to the stored value and a translated message chosen by the error's code shows.

**Weights in the chosen unit** (task #46)

- **FR-013**: Weights MUST be stored in kg and shown and entered in the person's unit wherever Kinetiq shows or asks for one (issue #42). At the time of this story, that is the quiz's body weight (spec 50, step 4) and the review's "About you" row (spec 54). The note under the quiz's body weight names the other unit: "Switch to lb later in settings." for kg, "Switch to kg later in settings." for lb.
- **FR-014**: A weight in lb MUST show with at most one decimal, in the number format of the UI language. A weight entered in lb is converted to kg and kept to 0.1 kg. Converting a stored weight to lb and back MUST give the same kg value, so a 0.5 kg step is never lost (issue #46).
- **FR-015**: A weight entered in lb MUST meet the same bounds as in kg once converted (spec 50, FR-003: 30 to 250 kg); the field's message names the bounds in lb.
- **FR-016**: Changing units MUST NOT change any stored weight. Units apply to weights only; height stays in cm.

**Screens**

- **FR-017**: The trainer screen MUST follow the board at 1280 px in the trainer area; the client sheet at 375 px. Both are built from Kinetiq tokens and shadcn components, in light and dark.
- **FR-018**: Each group of options MUST be labelled, reachable and operable by keyboard, with the chosen option shown by more than colour.
- **FR-019**: Every text MUST exist in English and Polish.

### Public endpoints

None. Reading and changing settings need a signed-in person (FR-004).

### Key Entities

- **Account settings**: Part of an account (spec 2, spec 18): units (kg or lb), language (English or Polish) and theme (light, dark or system). Every account has all three; the person changes their own.
- **Weight**: Always stored in kg; shown and entered in the person's unit.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: After changing settings on one device, 100% of sign-ins with that account on another device show all three settings as chosen, with no extra step.
- **SC-002**: A client changes a setting in the sheet with one choice, and the app shows it within 1 second.
- **SC-003**: In tests, every weight from 30.0 to 250.0 kg in 0.1 kg steps, shown in lb and converted back, returns the same kg value.
- **SC-004**: Every text of the trainer screen, the client sheet and the weight fields exists in English and Polish.
- **SC-005**: The trainer screen at 1280 px and the client sheet at 375 px pass WCAG 2.2 AA checks for contrast, labels, focus order and visible focus, in light and dark.
- **SC-006**: No error response of this story contains server-internal text, shown by tests that force a refused value and an unexpected failure.

## Assumptions

- A client's starting settings are set when the client joins through an invite: story #30 starts the client with the trainer's unit, and the board's hint for trainers says "New clients start with this. Each client can switch." The client's language starts as the one the UI was shown in when joining, and the theme as system. This story stores the settings on the account, which story #30 sets.
- Accounts that exist before this story start with units kg, language English and theme system.
- The time zone is not a setting: it follows the browser (spec 18, FR-019). This replaces the earlier note that a trainer could change it in settings (spec 2, Assumptions).
- The board's hint under the trainer's email, "Change it in your account.", is left out: no issue plans a way to change an email, and task #44 shows the email read-only. The pull request of task #44 raises this gap between the board and the backlog.
- The board's hints are kept: units "New clients start with this. Each client can switch." (trainer) and "Weights in your plan and logs show in kg." (client, naming the chosen unit); language "Polski uses informal "ty" with clients." (trainer); theme "System follows your phone." (client).
- The trainer screen offers the language as a list and the client sheet as two options, as on the board.
- Emails keep the language of the UI at the moment they are requested (spec 2, FR-019); for a signed-in person that is the account's language.
- Not in this story: table density and notification settings (issue #42); changing the email or the password; the time zone; weights in screens that do not exist yet (plan, logs), which use the same display and input rules when they are built.
