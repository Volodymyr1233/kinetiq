# Feature Specification: Client sees their trainer and a draft summary while waiting for a plan

**Feature Branch**: `feat/248-trainer-on-waiting-state`

**Created**: 2026-11-07

**Status**: Draft

**Input**: Story #247 "Client sees their trainer and a draft summary while waiting for a plan" and its tasks #248 (trainer's avatar and name on the waiting state), #249 (draft summary API) and #250 (draft card on the waiting state). Screens: Kinetiq Screens, board "Quiz · building the plan". Design: the Kinetiq design system. Builds on spec `54-quiz-review-submit` (the waiting states; its Clarifications left the trainer and the card out until their data existed), spec `79-plan-drafting` (drafts, the building state) and spec `58-client-trainer-messaging` (the conversation list). The message button came with #238. The story, the tasks and the board are starting points; every place this spec departs from them, or from an earlier spec, is listed under "Departures from the story, the board and earlier specs".

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Client sees who reviews their plan (Priority: P1)

A client who submitted the quiz sees their trainer on the waiting screen: the trainer's avatar, and the trainer's first name in the text, instead of an anonymous "your trainer".

**Why this priority**: The client has just shared a lot about themselves. Knowing that a named person checks the plan is what the waiting screen is for, and it needs no new data.

**Independent Test**: Sign in as a waiting demo client and open the waiting screen: the avatar shows the trainer's initials and the text names the trainer. Make the conversation list fail: the text reads "your trainer", without an avatar, and everything else works.

**Acceptance Scenarios**:

1. **Given** a client on the reviewing state whose trainer is Marek Nowak, **When** the screen shows, **Then** it shows the avatar "MN" above the heading, the heading "Your trainer is reviewing your plan", the text "Marek checks it before you see it. You get a notice here when it's ready." and the button "Message your trainer".
2. **Given** a client on the building state, **When** the screen shows, **Then** the spinner, the heading, the text, the skeleton and the button stay as spec 79 describes, without an avatar.
3. **Given** the UI in Polish, **When** the reviewing state shows, **Then** the text uses the name as it is, without declining it ("Marek sprawdza plan…"), and the button reads "Napisz do trenera".
4. **Given** the trainer's name is still loading or could not be loaded, **When** the screen shows, **Then** the text says "your trainer", there is no avatar, no error is shown, and the button still opens the conversation.
5. **Given** the name could not be loaded at first, **When** a later read succeeds, **Then** the screen shows the avatar and the name without a reload, keeping keyboard focus and whatever the client has open.

---

### User Story 2 - Client reads a summary of their draft (Priority: P1)

While the trainer reviews a draft with sessions, the client sees a card with its training days, length and focus, and a line saying the trainer can still change them.

**Why this priority**: This is what the client most wants to know while waiting: roughly what the plan holds. It gives them something to expect without showing work a trainer has not checked.

**Independent Test**: Sign in as a waiting client whose draft has sessions: the card shows the draft's days, length and focus. Fail the draft or empty it: the card is gone and the rest of the screen stays.

**Acceptance Scenarios**:

1. **Given** a client on the reviewing state with a draft of three sessions on Monday, Wednesday and Friday, 12 weeks and the goal "Build muscle", **When** the screen shows, **Then** a card between the text and the button shows the label "Draft", the badge "Waiting for your trainer", the rows "Training days: Mon, Wed, Fri", "Length: 12 weeks" and "Focus: Build muscle", and the line "Your trainer can still change these."
2. **Given** the UI in Polish, **When** the card shows, **Then** the weekdays, the plural of the weeks and the goal are in Polish.
3. **Given** a client on the building state, **When** the screen shows, **Then** there is no card.
4. **Given** a failed draft or a draft without sessions, **When** the reviewing state shows, **Then** there is no card and the rest of the screen is as it is with a card.
5. **Given** the building state, **When** drafting ends in a draft with sessions, **Then** the card appears without a reload, keyboard focus stays where it is, and only the heading is announced, as in spec 79.
6. **Given** the summary cannot be loaded, **When** the reviewing state shows, **Then** the waiting state still shows, without a card and without an error.
7. **Given** a trainer who changed the draft, **When** the client next opens the waiting screen, **Then** the card shows the draft as it stands; a draft the trainer emptied shows no card.

---

### User Story 3 - The client gets nothing else of a draft (Priority: P1)

A client can read only the summary of their own draft. Session names, exercises and loads stay with the trainer until the plan is published.

**Why this priority**: Nothing a trainer has not checked may reach the client, because training load affects injury risk, and the summary must not become a way around that.

**Independent Test**: As a client with a draft, ask for the summary and check that the answer holds only days, weeks and goal. As a client with a failed draft, with a draft without sessions, with no plan and with a published plan, ask again: the answers are the same. As a trainer: refused by role. With no sign-in: the standard refusal.

**Acceptance Scenarios**:

1. **Given** a client with a draft with sessions, **When** they ask for the summary, **Then** the answer holds the training days, the number of weeks and the goal, and no session name, exercise or load.
2. **Given** a client with a failed draft, a draft without sessions, no plan or a published plan, **When** they ask for the summary, **Then** the answer is the same "no summary" for all four, and it does not say which.
3. **Given** a client with a draft, **When** they ask for their plan, **Then** the answer is still "no plan" (spec 75, FR-003).
4. **Given** a trainer or a visitor who is not signed in, **When** they ask for the summary, **Then** a trainer is refused by role and a visitor is asked to sign in.
5. **Given** any request, **When** it is answered, **Then** the summary is always the signed-in client's own, taken from their sign-in; no request can name another client.

---

### Edge Cases

- A trainer's name of one word: the avatar shows one letter, and the first name is the whole name.
- A long first name wraps in the text, and the page does not scroll sideways.
- A conversation list without a conversation is treated like a name that could not be loaded.
- A failed draft that the trainer then fills and saves is a draft with sessions, so the card shows; the same goes for a draft the trainer built by hand for a waiting client.
- A draft the trainer saved with fewer sessions shows the days it has, even one; a draft need not be complete (spec 68).
- A session without exercises still has a day, so it counts as a training day.
- A plan published while the client is on the screen takes them to the plan-ready screen as before, so the card never stands next to a published plan.
- A status check that fails keeps the state on screen, with the trainer and the card as they were.
- No error response contains server internals.

## Requirements *(mandatory)*

### Functional Requirements

**Trainer on the waiting state** (task #248)

- **FR-001**: The reviewing state MUST show the trainer's avatar above the heading, as on the board: the initials of their full name, as elsewhere in the app. The avatar is hidden from assistive technology, because the text names the trainer.
- **FR-002**: The reviewing text MUST name the trainer by first name. The heading stays "Your trainer is reviewing your plan", as on the board.
- **FR-003**: The action to message the trainer MUST stay as it is in both waiting states and in both languages, "Message your trainer" and "Napisz do trenera", without the trainer's name.
- **FR-004**: The building state otherwise MUST stay as spec 79 describes it, with the spinner in the place of the avatar.
- **FR-005**: The name MUST come from the client's conversation list (spec 58). This task changes no API.
- **FR-006**: While the name loads, when it cannot be loaded and when the list holds no conversation, the text MUST read "your trainer", without an avatar and without an error. A name that arrives later is shown without a reload.

**Draft summary API** (task #249)

- **FR-007**: A signed-in client MUST be able to read the summary of their own draft: its training days in week order, Monday first, its number of weeks and its goal.
- **FR-008**: The answer MUST NOT hold anything else of the draft: no session names, exercises, targets or loads. A test proves it.
- **FR-009**: A summary MUST exist only for an unpublished plan with at least one session. A failed draft, a draft without sessions, a client with no plan and a client with a published plan MUST all get the same "no summary" answer. Tests prove each.
- **FR-010**: The summary MUST always be the signed-in client's own, taken from their sign-in; the request MUST NOT take a client from its path or body.
- **FR-011**: A trainer and a visitor MUST be refused: a trainer by role, a visitor with the standard unauthenticated answer. Tests prove each.
- **FR-012**: The client's plan and quiz status MUST stay as they are: a draft answers like no plan (spec 75, FR-003), and the status says nothing about the draft's content (spec 79, FR-003).
- **FR-013**: `CONTEXT.md` MUST gain the term "Draft summary", and "Draft" MUST change to match, because it says the client cannot see a draft.

**Draft card** (task #250)

- **FR-014**: The reviewing state MUST show a card between the text and the action when a summary exists: the label "Draft", the badge "Waiting for your trainer", the rows Training days, Length and Focus, and a line saying the trainer can still change them.
- **FR-015**: Training days MUST show as short weekday names in the UI language, in week order. Length MUST show in weeks with the language's plural forms. Focus MUST show the goal in the words the quiz uses.
- **FR-016**: There MUST be no card on the building state, when no summary exists, while the summary loads or when it cannot be loaded. None of these shows an error, and the rest of the waiting state stays.
- **FR-017**: When the screen moves by itself from building to reviewing, the summary MUST be read then and the card shown without a reload, without moving keyboard focus and without announcing more than the heading (spec 79, User Story 3, scenario 2).

**Quality**

- **FR-018**: Every new text MUST exist in English and Polish, with Polish plural forms. A person's name is not declined: Polish texts use it in the nominative or after a colon.
- **FR-019**: The waiting states MUST follow the board at 375 px, in the client column up to 640 px wide, built from Kinetiq tokens and shadcn components, in light and dark, with visible focus; at 375 px and at 320 px (200% zoom) the page MUST NOT scroll sideways.

### Public endpoints

None. Every operation needs a signed-in client.

### Key Entities

- **Draft summary**: What a client may read of their own draft: its training days, number of weeks and goal. It is derived from the draft and not stored.
- **Draft**: Unchanged from spec `79-plan-drafting`, except that its client may read its summary. The draft itself stays with the trainer until it is published.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A client on the reviewing state can tell who reviews their plan and what it roughly holds within 10 seconds of opening the screen.
- **SC-002**: In tests, a failed draft, a draft without sessions, a missing plan and a published plan return the same answer 100% of the time.
- **SC-003**: In tests, the summary never contains a session name, an exercise or a load, and no request returns another client's summary.
- **SC-004**: When the trainer's name or the summary cannot be loaded, the waiting state shows in 100% of cases, without an error.
- **SC-005**: The waiting states pass WCAG 2.2 AA checks for contrast, labels, focus order and visible focus, in light and dark, at 375 px.
- **SC-006**: Every new text exists in English and Polish.
- **SC-007**: No error response of this story contains server-internal text.

## Departures from the story, the board and earlier specs

| Source | It says | This spec | Why |
|---|---|---|---|
| Spec 79, Departures ("Building board"), User Story 1 scenario 3, FR-003, FR-029 | The client sees nothing of a draft, and the board's summary card is not shown | The client reads a summary of three values; the whole draft stays with the trainer. Spec 79, FR-003 and the plan's "no plan" answer stay true of the status and the plan themselves | The client wants to know what to expect. Days, length and focus say that without showing anything a trainer has not checked. |
| Spec 79, User Story 3 scenario 3 | After a failed draft the screen looks the same as after a valid one | No card after a failed draft; the rest looks the same | A failed draft has nothing to summarise. |
| Board | Row "Sessions a week" | Row "Training days" | In an automatic draft the session count and the focus repeat the quiz and the length is always 12 weeks (spec 79, FR-013), so only the days are new to the client. |
| Board | Length "16 weeks" | The draft's own length, 12 weeks for an automatic draft | The board's value is an example. |
| Board | Button "Message Marek" | "Message your trainer" and "Napisz do trenera", without the name, in both languages | Polish needs the genitive after "do", and an account holds one name, with no declined form and no gender. The app declines no name elsewhere, and the text just above the button names the trainer. |

## Texts

The main English and Polish texts. Polish uses informal "ty" and should be confirmed by a copywriter, as the design system says (8.5).

| Where | English | Polish |
|---|---|---|
| Reviewing text, with a name | {firstName} checks it before you see it. You get a notice here when it's ready. | {firstName} sprawdza plan, zanim go zobaczysz. Gdy będzie gotowy, dostaniesz tu powiadomienie. |
| Card label | Draft | Szkic |
| Card badge | Waiting for your trainer | Czeka na trenera |
| Card rows | Training days · Length · Focus | Dni treningowe · Długość · Cel |
| Card line | Your trainer can still change these. | Trener może to jeszcze zmienić. |

## Assumptions

- A client has one trainer, and the conversation list holds that pair whether or not a quiz, a plan or a message exists (spec `58-client-trainer-messaging`).
- The board shows the avatar on the reviewing state only; on the building state the spinner stands in its place, and that state's text does not name the trainer.
- Kinetiq has no profile photos at the time of this story, so an avatar is the person's initials.
- The screen reads the summary on arrival and whenever it reads data again by itself; it does not poll, because checking stops once the reviewing state shows (spec 79, FR-027). A trainer's change reaches the client on their next visit.
- A draft a trainer built or saved counts like an automatic one: the story speaks of a draft with sessions, not of how it arose.
- Session names and starting loads are not in the summary because no trainer has checked them yet; session length is not in the plan's data.
- Nothing is stored, so this story needs no migration and no new demo entity.
- Notifying the client that the plan is ready is story #150.
