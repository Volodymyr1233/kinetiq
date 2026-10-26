# Feature Specification: Trainer adds and edits their own exercise

**Feature Branch**: `feat/47-own-exercises`

**Created**: 2026-10-26

**Status**: Draft

**Input**: Story #47 "Trainer adds and edits their own exercise" and its tasks #48 (create and update exercise API) and #49 (add and edit exercise form). Screens: Kinetiq Screens, board Trainer → "Trainer · exercise library" (page, "Add exercise" dialog, empty state, toast). Design: the Kinetiq design system (Input and field, Dialog, copy rules). Builds on spec #13 (the library a trainer browses).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Trainer adds an exercise of their own (Priority: P1)

A trainer misses a movement in the library, chooses "Add exercise", fills in a form and saves. The exercise joins the library, is selected, and no other trainer ever sees it.

**Why this priority**: Plans are limited to the starter set until trainers can add their own exercises; this is the story.

**Independent Test**: As the demo trainer, add "Goblet squat" in Legs with the defaults; the library shows it selected, "N of N exercises" has grown by one, and a second trainer's library does not contain it.

**Acceptance Scenarios**:

1. **Given** the library, **When** the trainer chooses "Add exercise", **Then** a dialog "Add exercise" opens with the fields Name, Category, Measure, Equipment, Sets, Reps (Seconds when the measure is seconds), Rest and Cues, focus on Name, and the defaults of the board: Category "Legs", Measure "Repetitions", 3 sets, 10 repetitions, 90 s rest.
2. **Given** the dialog with a name, **When** the trainer chooses "Save exercise", **Then** the exercise is created for that trainer, the dialog closes, the filters are reset to "All" and an empty search, the new exercise is listed and selected, and "{name} added to the library." is announced.
3. **Given** the dialog, **When** the name is empty or only spaces, **Then** nothing is saved and the field shows "Enter a name."; the error is linked to the field and focus moves to it.
4. **Given** the name of a starter exercise, or of an exercise the trainer added earlier (any letter case, surrounding spaces, in English or Polish for starter ones), **When** the trainer saves, **Then** nothing is saved and the field shows "An exercise with this name is already in the library.".
5. **Given** the dialog, **When** the trainer chooses "Seconds" as the measure, **Then** the amount field is labelled "Seconds", and the library shows the default of the new exercise as, for example, "3 × 45 s".
6. **Given** a number outside its range, or text longer than its limit, **When** the trainer saves, **Then** nothing is saved and the field shows what is allowed (for example "Enter 1 to 20 sets.").
7. **Given** the dialog, **When** the trainer chooses "Cancel" or presses Escape, **Then** nothing is saved, the dialog closes and focus returns to "Add exercise".
8. **Given** a request that bypasses the dialog's checks, **When** it breaks a rule of scenarios 3, 4 or 6, **Then** it is rejected with a stable code naming the faulty field and nothing is created.
9. **Given** another trainer added "Goblet squat", **When** this trainer adds "Goblet squat", **Then** it is saved: trainers cannot see each other's exercises, so neither learns about the other's.
10. **Given** a trainer who owns 200 exercises, **When** they save a new one, **Then** nothing is saved and the dialog shows "You've reached the limit of 200 own exercises." as an alert, with their entries kept.

---

### User Story 2 - Trainer edits their own exercise (Priority: P1)

A trainer corrects a name, adds cues or changes a default of an exercise they added.

**Why this priority**: A first version of an own exercise is rarely complete (the board creates one with no cues), so editing is part of adding.

**Independent Test**: Select an own exercise, choose "Edit", change its rest and add cues, save; the panel shows both changes.

**Acceptance Scenarios**:

1. **Given** an own exercise is selected, **When** the trainer looks at the panel, **Then** it shows "Edit"; **given** a starter exercise is selected, **then** the panel shows no "Edit" and says it is a starter exercise.
2. **Given** an own exercise, **When** the trainer chooses "Edit", **Then** the dialog "Edit exercise" opens with every field filled with the current values, the measure shown but not changeable, and focus on Name.
3. **Given** changed values, **When** the trainer chooses "Save exercise", **Then** the exercise is updated, the dialog closes, the filters are reset, the exercise is selected showing the new values, and "{name} saved." is announced.
4. **Given** the dialog opened for an exercise, **When** the trainer saves without changing the name, **Then** the name does not clash with the exercise itself.
5. **Given** the name of another exercise in the trainer's library, **When** they save the edit, **Then** it is refused like in User Story 1, scenario 4.
6. **Given** a request to change a starter exercise, another trainer's exercise or an exercise that does not exist, **When** it is made, **Then** the answer is "not found", identical in all three cases, and nothing changes.
7. **Given** a save that fails for a reason the form cannot fix (connection lost, unexpected error), **When** the dialog is open, **Then** it stays open with the trainer's entries and shows a message by the error's code (generic when unknown), announced as an alert.
8. **Given** the same rules as User Story 1 scenarios 3, 4, 6 and 8, **When** an edit breaks one, **Then** the same messages and codes apply.

---

### User Story 3 - Each trainer sees only their own exercises, and nobody deletes (Priority: P1)

The library shows a trainer the starter exercises and their own, and nothing of any other trainer. No exercise can be deleted.

**Why this priority**: What a trainer builds is theirs; showing it to others, or letting it vanish from plans, is a defect.

**Independent Test**: With two trainers who each added an exercise, each library holds the starter set plus only that trainer's own; a delete request for an own exercise is refused and the exercise stays.

**Acceptance Scenarios**:

1. **Given** trainer A added "Goblet squat", **When** trainer B opens the library, searches for "goblet" or counts the exercises, **Then** nothing of A's appears, and the "of M" total does not include it.
2. **Given** a trainer with own exercises, **When** they open the library, **Then** their own and the starter exercises are listed together, ordered by name like before, filterable by category and name, with the total counting both.
3. **Given** the library, **When** the trainer looks for a way to remove an exercise, **Then** none is offered, and a delete request for any exercise is refused with the standard "method not allowed" answer and removes nothing.
4. **Given** a signed-in client, or a person who is not signed in, **When** they ask to create or change an exercise, **Then** they are refused like for the listing (forbidden for a client, unauthenticated for a person who is not signed in) and nothing is created or changed.

---

### Edge Cases

- Name, equipment and cues are saved trimmed; the name is compared without regard to letter case, and "Plank" and " plank " are the same name. Polish letters match as typed ("ł" is not "l"). Inner spaces are kept as typed.
- A name equal to a starter exercise's name in either language is refused whichever language the UI shows ("Deska" is refused in the English UI).
- An own exercise is written in the language the trainer typed it; the other UI language shows the same text, as spec #13 fixed. The name's uniqueness is checked against that text.
- Names may be at most 100 characters, equipment 50, cues 500; longer text is refused with a per-field code (the fields also stop typing at the limit).
- Sets accept 1 to 20, the amount 1 to 3600 (repetitions or seconds; the starter exercise bike is 1200 s), rest 0 to 600 seconds. Whole numbers only; a decimal, empty or non-numeric value is refused with the field's message.
- Two identical "create" requests at the same moment from one trainer leave exactly one exercise; the other is refused as a duplicate name.
- A trainer saves an edit in two browser tabs: the last save wins; there is no merge. Two saves of one exercise in the very same instant may end in the generic error for one of them; saving again works.
- Names, equipment and cues accept any text, including line breaks; the fields are single-line or plain text and are always shown as text, never as markup.
- Equipment left empty shows "Not set" in the list and the panel; cues left empty show no cues area.
- The measure of an exercise is chosen when it is added and cannot change afterwards, because a changed measure would change what the amounts in the trainer's plans and the clients' logs mean.
- Adding or editing never changes a starter exercise; a starter exercise added to the product later with the same name as an own one leaves both in the library.
- Edit when the exercise is no longer the trainer's (not found): the dialog closes, the list refreshes, and "This exercise can't be edited." is announced.
- Closing the dialog with unsaved entries discards them; there is no confirmation.
- While saving, the "Save exercise" button is disabled, shows a busy indicator and keeps its label, so a second request cannot be sent.

## Requirements *(mandatory)*

### Functional Requirements

**Own exercise** *(task #48)*

- **FR-001**: Only a signed-in trainer MUST be able to create or change an exercise, and only one they own. An exercise is owned by the trainer who created it; ownership never changes.
- **FR-002**: Creating an exercise MUST take a name, category, measure, equipment, default sets, default amount, rest and cues, and give back the exercise as the listing shows it. Equipment and cues are optional.
- **FR-003**: Changing an exercise MUST take the same fields except the measure, which cannot be changed, and replace all of them.
- **FR-004**: The name MUST be required (spaces only counts as empty), at most 100 characters, and unique among the exercises the trainer sees: the starter exercises in both languages and their own. Letter case and surrounding spaces do not matter. An exercise MUST NOT clash with itself.
- **FR-005**: The category MUST be one of the nine of spec #13. Sets MUST be 1 to 20, the amount 1 to 3600, rest 0 to 600, equipment at most 50 characters and cues at most 500.
- **FR-006**: A name that is already in use MUST be refused with its own stable code, and any other rule with the code of the faulty field, as the other features do. A value the API cannot read at all (an unknown category, text where a number belongs) is refused with the generic invalid-request code. Every error is a problem detail with a stable code and no server internals.
- **FR-007**: Changing a starter exercise, another trainer's exercise or one that does not exist MUST answer "not found", identically in every case, never "forbidden", and change nothing.
- **FR-008**: The system MUST NOT offer any way to delete an exercise; a delete request MUST be refused and remove nothing.
- **FR-009**: Starter exercises MUST remain unchangeable by everyone (FR-005 of spec #13).
- **FR-010**: Creating the same name twice at the same time MUST leave one exercise.
- **FR-024**: A trainer MUST NOT own more than 200 exercises; creating the 201st is refused with its own stable code (`exercise.limit-reached`) and the message "You've reached the limit of 200 own exercises.", shown in the dialog as an alert. Starter exercises and other trainers' exercises do not count; editing is never limited. The library is returned in one response, so the cap keeps it small.

**Listing** *(task #48, completing spec #13 FR-010)*

- **FR-011**: The library listing MUST contain the starter exercises and the signed-in trainer's own, and never another trainer's. Filters, order, language and the "of M" total follow spec #13 and count both kinds.
- **FR-012**: Each listed exercise MUST say whether it is a starter exercise, so the screen knows which ones can be edited. Nothing else about ownership is exposed.
- **FR-013**: An own exercise MUST show the cues, equipment and name exactly as saved, in both UI languages (spec #13 edge case on exercises without a Polish text).

**Form** *(task #49)*

- **FR-014**: The page MUST offer "Add exercise" in its header and in the empty state of a filter that matches nothing, next to "Clear filters". The panel MUST offer "Edit" for an own exercise and MUST NOT for a starter one; a starter exercise is marked "Starter" in the panel.
- **FR-015**: Add and Edit MUST use one dialog with a visible label above every field: Name, Category, Measure, Equipment (optional), Sets, Reps or Seconds, Rest with a unit "s", and Cues (optional, a text area). The dialog has the title "Add exercise" or "Edit exercise", the actions "Cancel" and "Save exercise", and in Edit the measure is shown read-only.
- **FR-016**: Defaults of Add: Category "Legs", Measure "Repetitions", 3 sets, 10 repetitions, 90 s. Choosing the other measure keeps the typed numbers and relabels the amount.
- **FR-017**: The dialog MUST check the rules of FR-004 and FR-005 before sending, with one message per rule, the same whether the dialog or the server found the problem: "Enter a name.", "An exercise with this name is already in the library.", "Use 100 characters or fewer.", "Enter 1 to 20 sets.", "Enter 1 to 3600." for the amount, "Enter 0 to 600 seconds.", "Use 50 characters or fewer.", "Use 500 characters or fewer.". Editing a field clears its error until the next save.
- **FR-018**: After a successful save the dialog MUST close, filters reset (category "All", empty search), the saved exercise be chosen, the list and panel show the saved values without a page reload, and a toast say "{name} added to the library." or "{name} saved.". A save that fails leaves the dialog open and the entries kept; an error not tied to a field is shown in the dialog as an alert by its code, generic when unknown, never server text.
- **FR-019**: While saving, the save button MUST be disabled, show a busy indicator and keep its label.
- **FR-020**: The dialog MUST trap focus, close on Escape and on "Cancel", return focus to the control that opened it, start with focus on Name, move focus to the first invalid field after a failed save, set `aria-invalid` and link each message to its field.
- **FR-021**: The screens MUST follow the board "Trainer · exercise library", built from Kinetiq tokens and shadcn components, in English and Polish, in light and dark, checked at 1280px. Every new user-facing string exists in both languages; Polish uses natural gym language and the plural forms Polish needs.
- **FR-022**: At narrower widths the dialog fits the screen and scrolls inside when it is taller than the screen; trainer phone layout stays story #179.

**Demo data**

- **FR-023**: Local-only demo data MUST give the demo trainer a few own exercises (one measured in seconds, one without equipment or cues) and add a second confirmed demo trainer with one own exercise, so isolation can be checked by hand.

### Public endpoints

None. Creating and changing are trainer-only and the listing stays trainer-only (spec #13 FR-017); nothing is opened to make the form work.

### Key Entities

- **Exercise**: As in spec #13. This story fills in the owning trainer: set when a trainer creates the exercise, never changed, empty for a starter exercise. Name, equipment and cues are stored as the trainer typed them in one language.
- **Trainer**: The account that owns zero or more exercises and sees them, with the starter exercises, as their library.
- **Exercise library**: The starter exercises plus the signed-in trainer's own.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A trainer adds an exercise with only a name in under 20 seconds from opening the dialog, and it is selected in the library at once.
- **SC-002**: 100% of requests to change a starter, a foreign or a missing exercise answer "not found", shown by tests with a second trainer.
- **SC-003**: 0 own exercises of one trainer appear in another trainer's listing, search, total count or name-clash check, shown by tests.
- **SC-004**: 100% of names equal to an exercise in the trainer's library (any letter case or surrounding spaces) are refused with the same message.
- **SC-005**: 0 exercises are deletable: no control exists and every delete request is refused.
- **SC-006**: Every user-facing string of the dialog, its errors and toasts exists in English and Polish; switching the language changes all of them.
- **SC-007**: The dialog and the changed page pass WCAG 2.2 AA checks for contrast, labels, focus order and visible focus, in light and dark, at 1280px.
- **SC-008**: No error response of this feature contains server-internal text, shown by tests that force invalid input and an unexpected failure.

## Assumptions

- The board's "Add exercise" dialog shows only Name, Category, Sets, Reps and Rest. The story also needs a measure (Plank is timed), equipment and cues, which the board's panel shows and its "Add cues in Edit." text implies; one dialog with all fields serves Add and Edit (task #49). The board seeds a new exercise with the text "Add cues in Edit." as cues; this is left out, because a placeholder is not a coaching instruction.
- The ranges of FR-005 (20 sets, 3600 amount, 600 s rest) are product defaults chosen so a typo cannot store an absurd value; they can be widened later without migration of existing data. Existing starter exercises fit them (largest: 1 × 1200 s, rest 90 s, 6 sets).
- The measure is fixed once saved (see Edge Cases). Plans and logs do not exist yet; locking it now is cheaper than repairing meaning later.
- An own exercise has only its base text (the mandatory name, equipment and cues fields), whatever language the trainer typed it in; the Polish fields stay empty and both UI languages show the base text. For starter exercises the base text is English. Spec #13's "the English text is always present" means "the base text is always present".
- Names are unique per trainer's visible library, not globally and not per language; the check is case-insensitive over the English and Polish name of starter exercises and over the typed name of own ones.
- The "Starter" marker in the panel is not on the board; without it the missing "Edit" would be unexplained.
- Exercises are never deleted and never archived in this story; the story forbids deletion, and a hide or archive option is a separate story if wanted.
- The count "In plans" stays 0 until plans exist (story #68); editing an own exercise will then change it everywhere it is used, which story #68 may want to revisit.
- This story supersedes spec #13 FR-026 (the page showed no "Add exercise" or "Edit" until now).
- The cap of 200 (FR-024) is a product default, about five times the starter set; simultaneous creates may overshoot it by a few, which is accepted.
- Authentication, CSRF protection, the trainer role check and the problem-detail format already exist (stories #2 and #18, spec #13) and are reused unchanged.
- The second demo trainer is local-only and never shipped.
- Task split: #48 covers FR-001 to FR-013 and FR-023 (API, listing scope, tests, regenerated API types); #49 covers FR-014 to FR-022 (dialog, panel, empty state, translations, browser check). One commit per task, ending with the task number.
