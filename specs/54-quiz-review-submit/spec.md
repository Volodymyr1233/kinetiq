# Feature Specification: Client reviews and submits the quiz

**Feature Branch**: `feat/56-quiz-review-step`

**Created**: 2026-10-25

**Status**: Draft

**Input**: Story #54 "Client reviews and submits the quiz" and its tasks #56 (review step), #55 (answers model and submit API) and #57 (submit and waiting states). Screens: Kinetiq Screens, board Client → "Quiz · review" and "Quiz · building the plan". Design: the Kinetiq design system. Builds on spec `specs/50-client-quiz`: its answer rules (FR-001 to FR-008), its data model ("Submitted answers") and its stored draft are not repeated here.

## Clarifications

### Session 2026-10-25

- Q: How long does "Building your plan from 7 answers…" show while nothing drafts a plan yet? → A: Only while the submit is on its way; the reviewing state follows at once. When the plan stories add drafting, it lasts until the draft exists.
- Q: The board's reviewing state shows the trainer's avatar and name, a draft summary and "Message Marek". Are they shown? → A: No, not until the trainer–client link, drafted plans and messaging exist.
- Q: The board's texts name the trainer ("Marek checks it…"). What do they say? → A: "Your trainer", as in spec 50.
- Q: What does the review show for empty notes? → A: "No notes".
- Q: Where do Back and the device's back go on a step opened with "Change"? → A: Back to the review, like Continue.
- Q: Which task builds the status read? → A: Task #55, with the submit; tasks #57 and #53 read it.
- Q: What does a trainer get from the quiz operations? → A: A refusal by role ("not allowed"), as the sign-in story (#18) refuses a client in the trainer-only exercise library; a test proves it.
- Q: How long are submitted answers kept, given they hold health data? → A: Indefinitely for now; their retention and encryption are decided in task #168, which lists the personal and health data stored.
- Q: How does a client who keeps the waiting screen open learn that a plan was published? → A: The screen checks the status on reload and whenever the client returns to the tab or the app; it does not poll.
- Q: What does the client see while the waiting screen loads the status, and when that fails? → A: A neutral loading state; on failure a translated message chosen by the error's code and "Try again".
- Q: What happens when two submits of the same client arrive at the same moment? → A: Exactly one is stored; the other gets `quiz.already-submitted` and the client sees the waiting state. Storage itself allows one submission per client, and a test proves it.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Client checks the answers before sending them (Priority: P1)

After step 7 the client sees all seven answers on one screen. Each answer has a Change link that opens its step; continuing from there returns to the review.

**Why this priority**: Acceptance criterion of the story. The trainer's first plan is built from these answers, so the client gets one look at them before they are final.

**Independent Test**: Answer the seven steps, continue from step 7: the review lists all seven answers. Change the level and continue: the review shows the new level.

**Acceptance Scenarios**:

1. **Given** all seven steps answered, **When** the client continues from step 7, **Then** the review shows "Review" in the header, a progress bar with all seven parts done, the heading "Check your answers", the hint "Your first plan is built from these 7 answers. Your trainer checks it before you see it.", one row per step in step order with its label, the answer and "Change", the button "Build my plan" and the line "You can change them later with your trainer."
2. **Given** the review, **When** it shows the answers, **Then** single choices show their option's title, "About you" reads like "29 years · 168 cm · 62.5 kg" in the number format of the UI language, likes are listed in screen order, separated by commas ("Weights, Yoga and Pilates"), days per week show the number, and notes show the text as typed.
3. **Given** the review, **When** the client chooses "Change" on a row, **Then** that step opens with its answer, and Continue there returns to the review instead of the next step.
4. **Given** a step opened with "Change", **When** the client uses Back or the device's back, **Then** they return to the review.
5. **Given** the review, **When** the client uses Back, **Then** step 7 opens.
6. **Given** the review, **When** the page is reloaded, **Then** the review shows again with every answer.

---

### User Story 2 - Client submits the answers once (Priority: P1)

"Build my plan" sends the seven answers. They are stored for the client and cannot be changed afterwards.

**Why this priority**: Acceptance criterion of the story. The plan stories and the trainer read the stored answers; without them nothing can be planned.

**Independent Test**: Submit valid answers: they are stored for that client and the draft in the tab is gone. Submit again: the second submit is refused with a stable code and the stored answers stay as they were.

**Acceptance Scenarios**:

1. **Given** the review with valid answers, **When** the client chooses "Build my plan", **Then** the answers are stored for that client with the time they were submitted, the draft kept in the browser tab is removed, and the waiting state of User Story 3 shows.
2. **Given** a client who has already submitted, **When** another submit arrives (a second tab, a repeated request), **Then** it is refused with the stable code `quiz.already-submitted`, the stored answers do not change, and the screen shows the waiting state.
3. **Given** a submit that breaks any rule of spec 50 (sent around the screen), **When** it arrives, **Then** it is refused with a code per field, or, when the request cannot be read (an unknown code, a number sent as text), as unreadable with `request.invalid`; nothing is stored.
4. **Given** the client chooses "Build my plan", **When** the request fails (no connection, server error), **Then** the review stays with a message in the UI's language and "Try again"; the answers stay in the tab.
5. **Given** the button was chosen, **When** the request is on its way, **Then** the button cannot be chosen again, so one press sends one submit.

---

### User Story 3 - Client waits for the plan (Priority: P1)

After submitting, the client sees that their plan is being built, then that their trainer is reviewing it, until the trainer publishes a plan.

**Why this priority**: Acceptance criterion of the story. A client who submitted needs to know that nothing is expected of them now.

**Independent Test**: Submit, then reload the page: the client sees "Your trainer is reviewing your plan", not the quiz.

**Acceptance Scenarios**:

1. **Given** the client chose "Build my plan", **When** the submit is on its way, **Then** they see "Building your plan from 7 answers…" with "Sessions, exercises and starting weights for N days a week." (N from their answer) and a placeholder of the plan summary.
2. **Given** the submit succeeded, **When** the client looks at the screen, **Then** they see "Your trainer is reviewing your plan" with "Your trainer checks it before you see it. You get a notice here when it's ready."
3. **Given** a client who submitted and has no published plan, **When** they reload, sign in again or open the quiz or the review, **Then** they see "Your trainer is reviewing your plan".
4. **Given** the waiting screen is opened, **When** the status has not arrived yet, **Then** a neutral loading state shows and no waiting text; **when** loading the status fails, **then** a message in the UI's language and "Try again" show, and "Try again" asks again.

---

### Edge Cases

- Notes left empty show "No notes" on the review.
- Long notes show in full on the review and wrap; nothing is cut.
- Switching the UI language changes every label and the number format, never an answer (spec 50, FR-002).
- Opening the review's address before every step has a valid answer, or before step 7 was reached, shows the step that may open (spec 50, FR-021).
- A step opened with "Change" may be left invalid only by the device's back; the review's address then sends the client to that step (the rule above).
- A second tab of the same client that submits after the first one, or at the same moment, gets the "already submitted" refusal and shows the waiting state; nothing is stored twice.
- Signing in again clears the tab's draft (sign-in, story #18), so a client whose session ended before submitting answers the quiz again; their answers never stay on a shared device.
- No error response contains server internals.

## Requirements *(mandatory)*

### Functional Requirements

**Review step** (task #56)

- **FR-001**: Continue on step 7 MUST open the review. It shows "Review" in the header with Back, a progress bar with all seven parts done, the heading "Check your answers", the hint, the seven rows and "Build my plan" with the line under it (User Story 1, scenario 1).
- **FR-002**: The rows MUST be, in step order: Goal, Training for, Level, About you, Likes, Days a week, Notes. Each shows the answer as in User Story 1, scenario 2, and a "Change" link whose accessible name names its row ("Change goal").
- **FR-003**: "Change" MUST open its step with the answer as it is. Continue on that step returns to the review; Back and the device's back return to the review too.
- **FR-004**: Back on the review MUST open step 7.
- **FR-005**: The review MUST open only when all seven steps have a valid answer and step 7 was reached; otherwise its address shows the step that may open (spec 50, FR-021).
- **FR-006**: The review MUST read the answers from the draft in the browser tab (spec 50), so a reload shows it again unchanged.

**Submitting** (tasks #55 and #57)

- **FR-007**: "Build my plan" MUST send the seven answers once. While the request is on its way the button cannot be chosen again.
- **FR-008**: The server MUST check every rule of spec 50 (FR-001 to FR-008, data model "Submitted answers") and refuse a submit that breaks one with a code per field. A request that cannot be read (an unknown code, an empty like, a number sent as text) is refused as a whole with `request.invalid`, as the exercise API does (contracts/api.md). Nothing is stored on a refusal.
- **FR-009**: The server MUST store a client's answers once, with the time of submitting. A later submit for the same client is refused with the stable code `quiz.already-submitted`; stored answers never change. This holds even when two submits arrive at the same moment: storage itself allows one submission per client.
- **FR-010**: Only a signed-in client can submit or read their own quiz. Another role is refused by role with the standard "not allowed" refusal, as in the sign-in story (#18); without a session the request gets the standard unauthenticated refusal. No operation addresses a quiz by id, so no client can reach another client's quiz.
- **FR-011**: The server MUST tell the signed-in client whether their quiz was submitted and whether a plan is published. Until plans exist, "published" is always false. This is the status read task #53 needs (spec 50, contracts).
- **FR-012**: After a successful submit, the draft in the browser tab MUST be removed (spec 50, contracts).
- **FR-013**: When a submit fails, the review MUST stay with a translated message chosen by the error's code (generic when the code is unknown or there is no response) and "Try again". When it is refused as already submitted, the waiting state shows instead. Server text is never shown.

**Waiting states** (task #57)

- **FR-014**: While the submit is on its way, the screen MUST show the building state of User Story 3, scenario 1.
- **FR-015**: After a successful submit, and for a client who submitted and has no published plan, the screen MUST show the reviewing state of User Story 3, scenario 2.
- **FR-016**: The waiting state MUST persist across reloads and sign-ins until a plan is published. A client who submitted gets it instead of the quiz or the review. The screen checks the status again on reload and whenever the client returns to the tab or the app; it does not poll. While the status loads, it shows a neutral loading state; when loading fails, a translated message chosen by the error's code (generic when unknown or with no response) and "Try again" (User Story 3, scenario 4).
- **FR-017**: A change between the two waiting states MUST be announced to assistive technology, and focus moves to the new heading.

**Screen**

- **FR-018**: The review and the waiting states MUST follow the board's screens at 375 px, in the client column up to 640 px wide, without the tab bar. "Build my plan" is the view's one forward action in brand lime; the review's progress bar shows all seven parts as done, with no current part.
- **FR-019**: Every text MUST exist in English and Polish; numbers follow the UI language.
- **FR-020**: The screens MUST be built from Kinetiq tokens and shadcn components, in light and dark.

### Public endpoints

None. Submitting and the status read need a signed-in client (FR-010).

### Key Entities

- **Quiz submission**: One client's answers to the seven steps (spec 50, data model "Submitted answers") and the time they were submitted. At most one per client; it never changes. It holds health data (body weight, notes); it is kept indefinitely for now, and its retention and encryption are decided in task #168.
- **Quiz status**: For the signed-in client: not submitted, waiting for a plan, or plan published. "Plan published" is supplied by the plan stories and is never true before they exist.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A client goes from the review to the waiting state in under 30 seconds, including one change of an answer.
- **SC-002**: In tests, 0 second submissions of the same client are stored, and 0 submits that break a rule are stored.
- **SC-003**: 100% of reloads and sign-ins of a client who submitted show the waiting state, never the quiz.
- **SC-004**: Every text of the review and the waiting states exists in English and Polish.
- **SC-005**: The review and the waiting states pass WCAG 2.2 AA checks for contrast, labels, focus order and visible focus, in light and dark, at 375 px.
- **SC-006**: No error response of this story contains server-internal text, shown by tests that force a refused submit and an unexpected failure.

## Assumptions

- The board's review hint and waiting text name the trainer ("Marek checks it…"). Nothing gives the client their trainer's name yet, so the text says "your trainer", as spec 50 does for the notes.
- The board's reviewing state also shows the trainer's avatar, a draft summary (sessions a week, length, focus) and "Message Marek". They need the trainer–client link (task #31), a drafted plan (the plan stories) and messaging, none of which exist yet, so the state is shown without them. The design system's quiz section already describes the waiting screen without the draft summary for when no draft could be built.
- Nothing builds a plan yet: drafting the first plan from the answers belongs to the plan stories. Until then the building state is shown only while the submit is on its way, and the reviewing state follows at once. When the plan stories add drafting, the building state lasts until the draft exists.
- The review's labels come from the board, including "Training for" for the experience step.
- After submitting, the client cannot change the answers; "You can change them later with your trainer." refers to the trainer adjusting the plan, which the plan stories cover.
- Submitting needs sign-in (story #18) and the trainer–client link (task #31), which task #55 is blocked by. The status read of FR-011 is built in task #55, with the submit, and read by tasks #57 and #53.
- A demo client with submitted answers is added to the local demo data (Definition of Done: new entities get demo data).
- No operation addresses another person's quiz by id, so the "another person's resource answers 404" rule has nothing to apply to; a trainer is refused by role (FR-010), and a test proves it.
- Not in this story: drafting the plan and the "plan ready" screen (plan stories); notifications; the trainer's view of the answers; sending clients to the quiz or the waiting state on entry (task #53).
