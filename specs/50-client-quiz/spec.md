# Feature Specification: Client answers the quiz

**Feature Branch**: `feat/52-quiz-question-components`

**Created**: 2026-10-23

**Status**: Draft

**Input**: Story #50 "Client answers the quiz" and its tasks #52 (question components), #51 (step flow) and #53 (client entry routing). Screens: Kinetiq Screens, board Client → "Quiz · single choice" and "Quiz · numbers and multi-select". Design: the Kinetiq design system.

## Clarifications

### Session 2026-10-23

- Q: Where are the answers kept before submitting? → A: Only in the browser tab. They survive a reload, but not closing the tab, so health data does not stay on a shared device.
- Q: Does step 1 show Back, as on the board? → A: No. There is nothing to go back to.
- Q: The board labels the notes "Notes for Marek", but nothing gives the client their trainer's name yet. What is the label? → A: "Notes for your trainer", until a source for the name exists.
- Q: The board opens step 5 with "Weights" chosen. Should it? → A: No. Nothing is chosen at first.
- Q: The board shows "Your answers stay if you go back." on steps 1–3 only. Where does it appear? → A: On every step.
- Q: Which number formats are accepted? → A: Age and height take whole numbers. Body weight takes at most one decimal place, typed with a dot or a comma.
- Q: How do the device's back and a reload behave? → A: The device's back moves between steps like the quiz's Back. A reload returns to the same step.
- Q: The board has no message for height. What is it? → A: "Enter a height between 120 and 230 cm.", following the other two.
- Q: What happens to spaces in the notes? → A: Surrounding spaces and line breaks are not kept. Notes of only spaces count as empty.
- Q: What does the client see while it is checked whether they submitted the quiz, and when that check fails? → A: A neutral loading state, never a page they may not be allowed to see. On failure, a translated message with "Try again". A missing session is left to the route guards (task #25).
- Q: Where does keyboard focus go when the client moves to another step? → A: To the new step's heading, so a screen reader reads the new question.
- Q: What happens to answers kept in the tab that no longer fit the quiz's rules (an option removed by an update, data changed by hand)? → A: Only those answers are dropped. The client answers those steps again.
- Q: Which steps can the client open directly by address? → A: Only steps they have already reached with Continue, so step 6, which starts at 3, cannot be skipped unseen.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Client answers seven questions, one per step (Priority: P1)

A new client goes through seven steps: goal, experience, level, about you, what they like to train, days per week and notes. Each step asks one thing, shows how far along they are, and lets them continue only when the answer is valid.

**Why this priority**: The trainer's first plan is drafted from these answers (story #54 and the plan stories). Without them there is nothing to plan from.

**Independent Test**: Open the quiz as a client with no answers, answer every step with valid values, and reach the end of step 7. Every step's Continue stays disabled until its answer is valid.

**Acceptance Scenarios**:

1. **Given** a client with no answers, **When** the quiz opens, **Then** step 1 "What do you train for?" shows "Step 1 of 7", five options with none chosen, and a disabled Continue.
2. **Given** a single-choice step (goal, experience, level), **When** the client chooses an option, **Then** that option is marked as chosen by more than colour, a previously chosen option is cleared, and Continue becomes enabled.
3. **Given** step 4 "About you", **When** age, height and body weight are all within their ranges, **Then** Continue is enabled. **When** a value is outside its range or not a number and the client leaves the field, **Then** that field shows its own message (for example "Enter an age between 16 and 99.") and Continue stays disabled. **When** the value becomes valid, **Then** the message disappears at once.
4. **Given** step 5 "What do you like to train?", **When** no option is chosen, **Then** Continue is disabled. The client may choose several. Clearing the last one disables Continue again.
5. **Given** step 6 "How many days a week can you train?", **When** it opens for the first time, **Then** it shows 3 and Continue is enabled. The − and + buttons change the number by one, − is disabled at 2 and + at 6.
6. **Given** step 7 "Anything your trainer should know?", **When** the notes are empty, **Then** the button reads "Skip and continue". **When** the client types, **Then** it reads "Continue", the counter shows "N of 500 characters", and no more text is accepted after 500 characters.
7. **Given** step 7, **When** the client continues, **Then** they go to the review step (story #54).

---

### User Story 2 - Answers are kept when going back and after a reload (Priority: P1)

The client can step back to change an answer without losing the others, and a reload or an accidental refresh does not throw their answers away.

**Why this priority**: Acceptance criterion of the story. On a phone a reload happens easily; seven steps lost would make clients give up.

**Independent Test**: Answer steps 1–5, go back to step 2, then reload the page. All five answers are still there, and the quiz shows the step the client was on.

**Acceptance Scenarios**:

1. **Given** answers on steps 1–4, **When** the client goes back from step 4 to step 2, **Then** step 2 shows the earlier choice, and going forward shows steps 3 and 4 with their answers.
2. **Given** a client on step 5 with answers on steps 1–4, **When** the page is reloaded, **Then** the quiz shows step 5 and every answer given so far.
3. **Given** a value on step 4 that is not valid yet, **When** the client goes back and returns, **Then** the value is still there as typed, with its message.
4. **Given** a client on step 4, **When** they use the browser's or phone's back, **Then** they go to step 3, the same as with the quiz's Back.
5. **Given** a client who has reached step 3, **When** they open the address of step 5 directly, **Then** the quiz shows step 3, the furthest step they reached.
6. **Given** a client who has answered steps 1–5 and reached step 6, **When** they open the address of step 7 directly, **Then** the quiz shows step 6, although its starting value of 3 is already valid.
7. **Given** answers kept in the browser, **When** a different account signs in in the same browser, **Then** it never sees them.

---

### User Story 3 - A client who has not submitted the quiz is always sent to it (Priority: P1)

The quiz is the client's way into Kinetiq. Until they submit it, every client page leads to the quiz. After submitting, they wait for their plan. Once a plan is published, they use the app normally.

**Why this priority**: Acceptance criterion of the story. A client who skipped the quiz would see an empty app, and the trainer would have nothing to plan from.

**Independent Test**: As a client without a submitted quiz, open Today, Plan and Messages. Each one shows the quiz instead. Repeat as a client who submitted (the waiting state) and as one with a published plan (the page itself).

**Acceptance Scenarios**:

1. **Given** a signed-in client who has not submitted the quiz, **When** they open any client page, **Then** they get the quiz.
2. **Given** a client who submitted the quiz and has no published plan, **When** they open any client page or the quiz, **Then** they get the waiting state (story #54, task #57).
3. **Given** a client with a published plan, **When** they open a client page, **Then** they get that page. **When** they open the quiz, **Then** they are sent into the app.
4. **Given** a client opening a client page, **When** the check whether they submitted the quiz has not answered yet, **Then** they see a neutral loading state and no other page. **When** the check fails, **Then** they see a message in the UI's language and "Try again", which repeats the check.

---

### Edge Cases

- Numbers may be typed with surrounding spaces, which are ignored. Body weight may use a dot or a comma as the decimal separator ("62.5" and "62,5"). Values are shown in the format of the UI language.
- Age and height take whole numbers only. Body weight takes at most one decimal place. Anything else (letters, "29.5" as an age, "62.55" as a weight) counts as invalid and shows the field's range message.
- An empty field on step 4 shows no message. Continue stays disabled until all three fields are valid.
- Pasting more than 500 characters into the notes keeps the first 500. The screen and the server count characters the same way, so notes the screen accepts are never refused for their length.
- Notes made only of spaces or line breaks count as empty. Surrounding spaces and line breaks are not kept.
- Switching the UI language during the quiz changes every text, but no answer: answers are recorded as options, not as displayed text.
- On a reload, only answers that meet the rules are restored. A value typed on step 4 that was not valid yet, and an answer that no longer fits the rules (an option removed by an update, data changed by hand), are dropped one by one. The quiz then opens on the step the client was on, or on the first earlier step without a valid answer.
- After submitting (story #54), the answers kept in the browser are removed. They are also removed when the client signs out.
- The mockup's height field accepts 100 without a message. The story's range (120–230 cm) wins.

## Requirements *(mandatory)*

### Functional Requirements

**Questions and answers**

- **FR-001**: The quiz MUST have seven steps, one per screen, in this order:

  | # | Step | Pattern | Heading | Hint | Answer |
  |---|---|---|---|---|---|
  | 1 | Goal | Single choice | What do you train for? | Pick one. Your trainer can change it later. | One of: Build muscle (More volume, 8–12 reps); Get stronger (Heavier weights, fewer reps); Lose fat (Strength plus conditioning); Move better (Range of motion and control); General fitness (A bit of everything) |
  | 2 | Experience | Single choice | How long have you trained regularly? | Count the time you trained at least twice a week. | One of: Less than 3 months; 3–6 months; 6–12 months; 1–2 years; 2–5 years; 5+ years |
  | 3 | Level | Single choice | Which fits you best? | Your trainer uses this to set your first weights. | One of: Beginner (New to structured training); Intermediate (Comfortable with the basic lifts); Advanced (Consistent training for 2+ years); Athlete (You compete or train like it) |
  | 4 | About you | Number with unit, three fields | About you | Your trainer uses these to set starting weights. | Age (years), Height (cm), Body weight (kg), with the note "Switch to lb later in settings." under body weight |
  | 5 | Likes | Multiple choice | What do you like to train? | Pick all that fit. | One or more of: Weights (Barbells, dumbbells, machines); Cardio (Running, cycling, rowing); HIIT and circuits (Short, hard intervals); Yoga and Pilates (Mobility and control); Bodyweight (No equipment needed) |
  | 6 | Days per week | Stepper | How many days a week can you train? | Count the days you can really keep. | A number with the unit "days a week" and the line "At least 2, at most 6." |
  | 7 | Notes | Free text with a character count | Anything your trainer should know? | Injuries, limits, equipment you don't have. Optional. | Text labelled "Notes for your trainer", with the example "e.g. Left knee gets stiff on deep squats" |

- **FR-002**: Every answer MUST be recorded as the chosen option, independent of the UI language.
- **FR-003**: Age MUST be a whole number from 16 to 99. Height MUST be a whole number from 120 to 230 cm. Body weight MUST be from 30 to 250 kg with at most one decimal place.
- **FR-004**: Body weight MUST be recorded in kg. In this story it is entered in kg. Entering it in lb, within the same range converted to lb, comes with the client's unit setting (story #42, task #46).
- **FR-005**: Likes MUST hold at least one option, and each option at most once.
- **FR-006**: Days per week MUST be a whole number from 2 to 6. It starts at 3.
- **FR-007**: Notes MUST be optional and at most 500 characters long. Notes of only spaces or line breaks count as empty. Surrounding spaces and line breaks are not kept.
- **FR-008**: FR-001 to FR-007 MUST be the single definition of valid answers. The submit API of story #54 (task #55) refuses answers that break any of them and accepts every answer the screen accepts.

**Question patterns** (task #52)

- **FR-009**: Single choice MUST work as a radio group named by the step's heading. Each option has a title and may have a description. The chosen option is marked by more than colour.
- **FR-010**: Multiple choice MUST work as a group of checkboxes named by the step's heading. Each option is chosen or cleared on its own.
- **FR-011**: Number with unit MUST be a labelled field with its unit shown inside it and included in what assistive technology announces. It opens a number keypad on phones and shows an example value ("e.g. 29", "e.g. 168", "e.g. 62.5"). Its message appears when the client leaves the field with an invalid value, is linked to the field and announced, and disappears as soon as the value is valid. The messages are "Enter an age between 16 and 99.", "Enter a height between 120 and 230 cm." and "Enter a weight between 30 and 250 kg."
- **FR-012**: The stepper MUST show the number large with its unit below. The − and + buttons have accessible names ("Fewer days", "More days") and are disabled at the bounds. A change of the number is announced.
- **FR-013**: Free text with a character count MUST be a labelled multi-line field. The counter "N of 500 characters" is linked to the field. The field accepts no more than the limit, whether typed or pasted.
- **FR-014**: Every option, button and field MUST have a touch target of at least 48 px and a visible keyboard focus. The whole quiz MUST be usable with the keyboard alone.

**Step flow** (task #51)

- **FR-015**: Every step MUST show "Step N of 7", a progress bar of seven parts (done, current, still to come), the heading, the hint, the answer and Continue at the bottom, with "Your answers stay if you go back." under it. When a step opens after Continue, Back or the device's back, keyboard focus MUST move to its heading.
- **FR-016**: Continue MUST be enabled only when the step's answer is valid. On step 7 it reads "Skip and continue" while the notes are empty.
- **FR-017**: Back MUST appear on steps 2–7 and go to the previous step. Step 1 has no Back.
- **FR-018**: Going back and forward MUST keep every answer, including a value typed but not valid yet.
- **FR-019**: The answers and the current step MUST survive a page reload; answers that do not meet the rules are dropped (see Edge Cases). They are kept only in the browser tab until the quiz is submitted (story #54), and removed after submitting and on sign-out.
- **FR-020**: Answers kept in the browser MUST belong to the signed-in client. Another account in the same browser never sees them.
- **FR-021**: The browser's and phone's back and forward MUST move between steps like Back and Continue. Opening a step beyond the furthest step the client reached with Continue MUST show that furthest step, or the first earlier step without a valid answer if there is one.
- **FR-022**: Continue on step 7 MUST lead to the review step (story #54, task #56).

**Entry routing** (task #53)

- **FR-023**: A signed-in client who has not submitted the quiz MUST get the quiz from every client page.
- **FR-024**: A client who submitted the quiz and has no published plan MUST get the waiting state (story #54, task #57) from every client page and from the quiz.
- **FR-025**: A client with a published plan MUST get the client pages normally. Opening the quiz sends them into the app.
- **FR-026**: While the check in FR-023 to FR-025 has not answered, client pages MUST show a neutral loading state and no page the client may not be allowed to see. If the check fails, they MUST show a translated message chosen by the error's code (a generic one when the code is unknown or there is no response) and "Try again", which repeats the check. Server text is never shown.
- **FR-027**: Signing in, and keeping trainers and anonymous visitors out of client pages, is done by the route guards of task #25. This story adds only the checks above, for signed-in clients.

**Screen**

- **FR-028**: The quiz MUST follow the board's quiz screens at 375 px: the client column up to 640 px wide, no tab bar, and the main button fixed at the bottom. Continue uses the default action colour. Brand lime marks only the current part of the progress bar.
- **FR-029**: Every text of the quiz MUST exist in English and Polish. The Polish texts are natural Polish, in the same informal form as the rest of the app. Numbers follow the UI language ("62.5" in English, "62,5" in Polish).
- **FR-030**: The quiz MUST be built from Kinetiq tokens and shadcn components, in light and dark.

### Public endpoints

None. This story adds no endpoint. The entry routing reads whether the quiz was submitted and whether a plan is published from authenticated endpoints of other tasks (see Assumptions).

### Key Entities

- **Quiz answers**: One client's answers to the seven steps: goal, experience, level, age, height in cm, body weight in kg, likes (one or more), days per week and notes (optional). With them the browser keeps the current step and the furthest step the client reached. In this story they live only in the client's browser tab. Story #54 stores them on the server once, when the client submits.
- **Quiz step**: One of the seven screens. It has a heading, a hint, one question pattern and a rule for when its answer is valid.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A client completes all seven steps on a phone in under 2 minutes.
- **SC-002**: 100% of the answers given so far are still there after a reload on any step, and after going back to any earlier step.
- **SC-003**: Every step can be completed with the keyboard alone. Assistive technology announces the question, every option, the chosen state, the unit, the counter and each message.
- **SC-004**: Every text of the quiz exists in English and Polish. Switching the language changes all of them, messages included.
- **SC-005**: The quiz passes WCAG 2.2 AA checks for contrast, labels, focus order and visible focus, in light and dark, at 375 px.
- **SC-006**: In tests, a client without a submitted quiz reaches no client page other than the quiz.

## Assumptions

- The question list was an open product decision. The story settles the seven steps and their ranges. The options, headings, hints and messages come from the board's quiz screens. The height message is not on the board; it follows the pattern of the other two.
- The board shows "Notes for Marek", with the trainer's name. Nothing gives the client their trainer's name yet: the trainer–client link is task #31, and no endpoint exists to read it. The label is "Notes for your trainer" until one does.
- On the board, step 5 opens with "Weights" already chosen. The quiz opens it with nothing chosen, so every like is the client's own choice.
- On the board, "Your answers stay if you go back." appears only on steps 1–3, and step 1 shows Back. The quiz shows the line on every step, and no Back on step 1, where there is nothing to go back to.
- Answers are kept in the browser tab only: they survive a reload, but not closing the tab. Body weight and notes are health data, and they should not stay on a shared device. Losing seven short steps costs about a minute.
- Removing the answers on sign-out, and keeping them apart per account (FR-019, FR-020), needs sign-in and sign-out (story #18). Until those exist no other account can be signed in, so nothing can leak.
- A new client's weight unit is kg, the default of story #42, and the client cannot reach settings before the quiz. So in practice the quiz is answered in kg, and the note "Switch to lb later in settings." explains how to change that afterwards.
- Continue on step 7 needs the review step (task #56). Until it exists, step 7 is the end of the flow.
- The entry routing (task #53) needs the route guards (task #25), and the information whether the quiz was submitted (task #55). Plans do not exist yet, so until they do no client has a published plan, and a client who submitted always waits. Plan publication is supplied by the plan stories.
- Every operation of this story acts on the signed-in client's own quiz. Nothing addresses another person's quiz by id, so the "another person's resource answers 404" rule has nothing to apply to here. It applies to task #55 if that task reads answers by id.
- Not in this story: the review step, submitting, and the waiting states (story #54); storing answers on the server (task #55); the unit setting and entering weight in lb (story #42); route guards per role (task #25); drafting the plan from the answers (plan stories).
