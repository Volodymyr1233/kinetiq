# Feature Specification: Trainer browses the exercise library

**Feature Branch**: `feat/13-exercise-library`

**Created**: 2026-10-20

**Status**: Draft

**Input**: Story #13 "Trainer browses the exercise library" and its tasks #14–#17. Screens: Kinetiq Screens, board Trainer → "Trainer · exercise library". Design: the Kinetiq design system.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Trainer sees the exercise library (Priority: P1)

A trainer opens "Exercise library" in the sidebar and sees every exercise they can use in a plan: the starter exercises (their own join them with story #47). The list shows each exercise's name, category, equipment, default and how many plans use it. One exercise is selected, and a panel beside the list shows its details.

**Why this priority**: Without a library to look at there is nothing to search, and the plan editor (story #68) has nothing to pick from.

**Independent Test**: Open the library as a signed-in trainer; about 40 starter exercises are listed and the first one is selected with its details shown.

**Acceptance Scenarios**:

1. **Given** a trainer who has added no exercises, **When** they open the library, **Then** the list shows all starter exercises, the heading line reads "N of N exercises" with N the number of starter exercises, and the first exercise in the list is selected.
2. **Given** the library, **When** the trainer looks at a row, **Then** it shows name, category, equipment, default (for example "3 × 5" for repetitions or "3 × 45 s" for seconds) and "In plans".
3. **Given** the library, **When** the trainer chooses another row, **Then** the panel shows that exercise: category, name, equipment, default, rest in seconds, "In plans" and cues. The chosen row is marked in a way that does not rely on colour alone.
4. **Given** an exercise used in no plan, **When** it is shown in the list or in the panel, **Then** "In plans" reads "Not used" in the list and "Not used yet" in the panel instead of "0 plans".
5. **Given** the library is loading, **When** the response has not arrived, **Then** the page shows a loading state, and **when** the request fails, **Then** it shows the error's message in the UI's language, chosen by the error's code and generic when the code is unknown, with a way to try again.

---

### User Story 2 - Trainer narrows the list by category and name (Priority: P1)

The trainer picks a category chip, types part of a name, or both, and the list shows only exercises that match both.

**Why this priority**: A plan is built from many picks; scrolling 40 or more rows for each one is the pain this story removes.

**Independent Test**: Choose "Legs", then type "squat"; only legs exercises whose name contains "squat" remain, and the heading line reads "N of 40 exercises".

**Acceptance Scenarios**:

1. **Given** the library, **When** the trainer chooses a category, **Then** only exercises of that category are listed, and "All" shows every category again. Exactly one chip is chosen at a time, "All" at the start.
2. **Given** the library, **When** the trainer types "SQUAT " (any letter case, surrounding spaces), **Then** every exercise whose name contains "squat" is listed, in any category.
3. **Given** a category and a search text, **When** both are set, **Then** an exercise is listed only if it matches both.
4. **Given** the selected exercise is filtered out, **When** the list changes, **Then** the first remaining exercise becomes selected; **when** nothing remains, **then** the panel shows no exercise.
5. **Given** a filter that matches nothing, **When** the list is empty, **Then** an empty state says that no exercises match and offers "Clear filters", which restores "All" and an empty search.
6. **Given** the search field, **When** the trainer clears it, **Then** the full list for the chosen category returns.
7. **Given** filters are set, **When** the page is reloaded, **Then** they are reset; filters are not kept anywhere.

---

### User Story 3 - Trainer knows which exercises are in use (Priority: P2)

The panel and the list tell the trainer in how many of their plans an exercise is used.

**Why this priority**: It tells the trainer what changing or avoiding an exercise would affect. Plans do not exist yet (story #68), so until then every exercise is unused; the number is real from the day plans exist.

**Independent Test**: With no plans, every exercise shows "Not used". Counting real plans is tested when plans exist (story #68).

**Acceptance Scenarios**:

1. **Given** no plans exist, **When** the library is shown, **Then** every exercise reads "Not used" (list) and "Not used yet" (panel).
2. **Given** a count of 3, **When** an exercise is shown, **Then** "In plans" reads "3 plans"; a count of 1 reads "1 plan" (Polish plural forms apply in Polish).

---

### User Story 4 - The library needs a signed-in person (Priority: P1)

Nobody who is not signed in can read the library.

**Why this priority**: Every endpoint is closed unless deliberately opened (constitution III); the library is not a reason to open one.

**Independent Test**: Ask for the library without a session; the request is refused and no exercise data is returned. Ask with a session; the list is returned.

**Acceptance Scenarios**:

1. **Given** a person who is not signed in, **When** they ask for the library, **Then** they are refused as unauthenticated and no exercise data is returned.
2. **Given** a signed-in person, **When** they ask for the library, **Then** they get the list.

---

### User Story 5 - A starter set every trainer starts with (Priority: P1)

Every trainer, new or existing, sees the same starter exercises, in English or in Polish to match the UI. Nobody can change or remove them.

**Why this priority**: A trainer who signs up must be able to build a plan on day one without typing 40 exercises.

**Independent Test**: Sign up two new trainers; both see the same 40 starter exercises, each with all fields filled.

**Acceptance Scenarios**:

1. **Given** a fresh installation, **When** the library is read, **Then** there are about 40 starter exercises, every one of the nine categories (Chest, Back, Legs, Shoulders, Arms, Core, Full body, Cardio, Mobility) has at least three, and at least three are measured in seconds, Plank among them.
2. **Given** a starter exercise, **When** it is read, **Then** it has a name, category, equipment, measure (repetitions or seconds), default sets, default repetitions or seconds, rest and cues; name, equipment and cues exist in both English and Polish.
3. **Given** a starter exercise, **When** any trainer looks at it, **Then** it is the same for everyone and is not owned by anyone.
4. **Given** a trainer who signed up before the starter exercises were added, **When** they open the library, **Then** they see them too.
5. **Given** the UI in Polish, **When** the library is shown, **Then** names, equipment and cues are the Polish ones, the list is ordered by the Polish names, and the heading, chips and column headings are Polish too; in English everything is English.
6. **Given** the UI in Polish, **When** the trainer types an English name such as "plank" or a Polish one such as "deska", **Then** the exercise is found by either.

---

### Edge Cases

- Search text is trimmed and compared without regard to letter case; a search of only spaces counts as empty. Wildcard characters typed by the person (`%`, `_`) are plain characters and match only themselves.
- The search field accepts at most 100 characters (the longest a name can be). A request with a longer text that bypasses the field is refused with a per-field error code, like an invalid category.
- Names are matched as a contiguous part of the English or the Polish name; "squat back" does not match "Back squat". Polish letters match as typed ("ł" does not match "l").
- An unknown category value in a request (altered outside the UI) is rejected with a field error and no data; the UI offers only the nine categories.
- The list is ordered by the name shown, ignoring letter case and following the alphabet of the UI language (in Polish "ł" sorts after "l"), so the order is the same on every load.
- Rapid changes of filters show only the result of the latest choice; an older, slower response never replaces a newer one.
- An exercise without a Polish text (none among the starter ones) shows its English text in the Polish UI; the same for each of name, equipment and cues on its own.
- Rest of 0 seconds is valid and shows as "0 s".
- An exercise with no cues shows an empty cues area without a stray label or error.

## Requirements *(mandatory)*

### Functional Requirements

**Exercise**

- **FR-001**: An exercise MUST have a name, a category, an equipment label, a measure (`reps` or `seconds`), default sets, a default amount (repetitions, or seconds when the measure is seconds), a rest in seconds, cues, and optionally one owning trainer. Name, equipment and cues each exist in English and may also exist in Polish; the English text is always present. An exercise with no owner is a starter exercise.
- **FR-002**: The category MUST be one of Chest, Back, Legs, Shoulders, Arms, Core, Full body, Cardio, Mobility.
- **FR-003**: The system MUST refuse to create an exercise with an empty English name (spaces only counts as empty), a name of more than 100 characters in either language, default sets below 1, a default amount below 1, or a negative rest.
- **FR-004**: The default of an exercise MUST be shown as sets × repetitions, or sets × seconds with an "s" unit (for example "3 × 5", "3 × 45 s").
- **FR-005**: Starter exercises MUST be read-only for everyone; no operation of this story changes or removes any exercise.

**Starter set**

- **FR-006**: The system MUST come with about 40 starter exercises, every category with at least three, at least three timed ones (Plank among them), each with all fields filled.
- **FR-007**: Name, equipment label and cues of every starter exercise MUST exist in both English and Polish, and the Polish text MUST be natural gym language a Polish trainer would use, not a word-for-word translation. Equipment is a short label (for example Barbell/Sztanga, Dumbbells/Hantle, Machine/Maszyna, Cable/Wyciąg, Bodyweight/Masa ciała).
- **FR-008**: Starter exercises MUST be available to every trainer account, including accounts created before they were added, and MUST NOT be copied per trainer.
- **FR-009**: Nothing in this story creates exercises owned by a trainer, so no demo data is added; the starter exercises are the demo data.

**Listing**

- **FR-010**: The system MUST list the starter exercises. Owned exercises appear in the library only once story #47 lets trainers create them, and that story MUST show each trainer only their own (see Assumptions).
- **FR-011**: The list MUST be filterable by one category and by a name text, together (both must match), and an unfiltered request MUST return everything in the library.
- **FR-012**: Name matching MUST ignore letter case and surrounding spaces and match any contiguous part of the English or the Polish name.
- **FR-013**: The list MUST be ordered by the name in the requested language (the English name when it has no Polish one), ignoring letter case and following that language's alphabet, then in a fixed secondary order so equal names keep their place.
- **FR-014**: Every listed exercise MUST carry everything the detail panel shows (FR-021), so choosing a row needs no further request. Name, equipment and cues come in the requested language, falling back to English per field. The requested language is the one the UI is shown in.
- **FR-015**: Each listed exercise MUST say in how many plans it is used, counting a plan once however many times the exercise is in it. Until plans exist (story #68) the number MUST be 0; story #68 decides whose plans are counted.
- **FR-016**: The response MUST also give the total number of exercises in the library, regardless of filters, for the "N of M exercises" line.
- **FR-017**: The listing MUST require a signed-in person; a person who is not signed in gets the standard unauthenticated refusal and receives no data. Errors follow the problem-detail rules of the project (stable `code`, no server internals).
- **FR-018**: An invalid category, or a name text longer than 100 characters, in a request MUST be refused with a per-field error code.

**Screen**

- **FR-019**: The "Exercise library" page MUST be reachable from the trainer sidebar entry of that name and follow the board "Trainer · exercise library" at 1280px: a heading with the "N of M exercises" line, category chips ("All" plus nine) above, a table with a search field on the left (Exercise, Category, Equipment, Default, In plans), and a detail panel on the right.
- **FR-020**: Choosing a chip, typing in the search field or choosing a row MUST update the list and panel without a full page reload. While the person types, the list MUST settle with one result per pause, not flicker per keystroke, and show the final result within 1 second after typing stops (SC-002).
- **FR-021**: The detail panel MUST show category, name, equipment, default, rest in seconds, "In plans" and cues, in the board's order.
- **FR-022**: The page MUST show a loading state while the first list is awaited, an error state with "Try again" if it fails, and the empty state of User Story 2 when a filter matches nothing. The error state shows a translated message chosen by the error's code (for example "Sign in to continue." for an unauthenticated request) and a generic one for an unknown code, a missing code or no response; it never shows server text.
- **FR-023**: The chips MUST work as a single-choice group with the chosen one announced, the search field MUST have a visible or accessible label, the result count MUST be announced to assistive technology when it changes, and every row MUST be reachable and choosable with the keyboard with a visible focus. The chosen row MUST be exposed to assistive technology.
- **FR-024**: The screen MUST be built from Kinetiq tokens and shadcn components, in English and Polish, in light and dark. All category names, column headings, labels, states and messages exist in both languages, and so do the exercises' names, equipment and cues (FR-007, FR-014).
- **FR-025**: At 1280px all content and actions MUST be visible without horizontal scrolling of the page; at narrower widths the panel wraps under the table and the page stays usable (phone layout for trainers is story #179).
- **FR-026**: The page MUST NOT show "Add exercise" or "Edit"; those belong to story #47. The empty state offers "Clear filters" instead of "Add exercise".

### Public endpoints

None. The listing requires a signed-in person (FR-017). It is not opened to make the screen work before sign-in (story #18) exists.

### Key Entities

- **Exercise**: A movement that can be prescribed, such as a back squat. Name, category, equipment, measure (repetitions or seconds), default sets, default repetitions or seconds, rest, cues, and an optional owning trainer. Name, equipment and cues exist in English and, optionally, Polish. Without an owner it is a **starter exercise** every trainer sees and nobody can edit. The owner is stored now so story #47 does not need to change the model; nothing sets it yet.
- **Exercise library**: The starter exercises (and, from story #47, a trainer's own).
- **Plan** *(exists only as a term until story #68)*: Used only to count in how many plans an exercise appears.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A trainer finds a named exercise among 40 or more in under 10 seconds from opening the library: one chip or a few typed letters, one click.
- **SC-002**: After the trainer stops typing or picks a chip, the list shows the matching result within 1 second on a normal connection.
- **SC-003**: 0 requests without a session receive exercise data.
- **SC-004**: 100% of starter exercises have every field filled in both languages, and every category has at least three exercises.
- **SC-005**: Every user-facing string of the screen exists in English and Polish; switching the language changes all of them, including error messages.
- **SC-006**: The screen passes WCAG 2.2 AA checks for contrast, labels, focus order and visible focus, in light and dark, at 1280px.
- **SC-007**: No error response of this feature contains server-internal text, shown by tests that force an invalid category and an unexpected failure.

## Assumptions

- Sign-in is story #18 and is not built yet, so the app has no signed-in trainer to scope to. This story therefore builds the starter library only: it needs no knowledge of who asks beyond "signed in". Per-trainer scoping (a trainer never sees another's own exercises, FR-010) and a trainer-only check are delivered with the first owned exercises in story #47, on top of sign-in; the criterion "another trainer's own exercises never appear" of task #16 moves there, and it is the place for the test that another trainer's exercise is absent. Nothing in this story can create an owned exercise, so nothing can leak meanwhile.
- Until sign-in exists the endpoint answers 401 to the browser. It is proven with tests that act as a signed-in person; the screen is checked in a browser with a throwaway local change that lets the request through, which is reverted before the commit and never committed.
- "Add exercise" and "Edit" on the board are story #47 (tasks #48, #49). This story builds only the viewing side; the board's buttons are left out rather than shown disabled.
- Plans do not exist before story #68. This story defines the count (FR-015) and returns 0; the real count is supplied when plans exist, and that story adds the test that a plan raises the number. The list and panel already format it (User Story 3).
- The "In plans" label of the board's panel says "In client plans"; this spec uses "In plans" in both places, as in the list column and the story, because a plan always belongs to one client (see the glossary).
- The whole library (about 40 starter exercises, later plus a trainer's own) is returned in one response; no paging. If the number of own exercises grows large, paging is a later change.
- The category set is fixed in the product; trainers cannot add categories. Equipment is a free label, not filtered in this story.
- All data a row and the panel need comes with the listing, so no operation fetches a single exercise by id; there is nothing to probe by id.
- The "another person's resource answers 404" rule has nothing to apply to here: the starter exercises are no one's, and no operation addresses an exercise by id.
- Filters and the chosen exercise live only in the open page; they are not in the address bar and not stored.
- Exercise names, equipment and cues are content, but the app is bilingual, so starter exercises carry both languages (decided after the issue was written; the issue's "names and cues are in English" is superseded for the Polish UI). A trainer's own exercise (story #47) is written in one language; the other UI language shows that same text.
- The UI language is sent with the request, as it is for emails in story #2; the server picks the texts.
