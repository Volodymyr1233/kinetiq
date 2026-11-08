# Feature Specification: Client sees what comes next on a rest day

**Feature Branch**: `feat/264-rest-day-next`

**Created**: 2026-11-08

**Status**: Draft

**Input**: Task #264 "Client sees what comes next on a rest day", under story #89 "Client sees today's session". Screen: Kinetiq Screens, board "Today · check-in" (the rest-day card). Builds on spec `89-todays-session` (the Today view and its three states), spec `98-finish-session` (session states per plan week) and spec `75-client-plan` (the client's plan and its weeks). The estimate of a session's time is the one the plan editor shows (spec `68-plan-editor`).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Client sees what comes next on a rest day (Priority: P1)

On a rest day the Today card says so with a "Rest" badge and the line "No session today. No check-in either.", and shows the next session of the plan by name, date and estimated time ("Next: Full body · Fri 9 Oct · 55 min"). The client knows what comes next without opening the Plan tab.

**Why this priority**: This is the point of the task: a rest day should answer "when do I train next?", not stop at "Nothing is planned".

**Independent Test**: Sign in as a client whose plan has no session today but has one later: the card shows the badge, the line and "Next: <name> · <date> · <minutes> min".

**Acceptance Scenarios**:

1. **Given** a plan with no session today and one on a later weekday of this week, **When** the client opens Today, **Then** the card shows the "Rest" badge, "No session today. No check-in either." and the next session's name, date and estimated minutes.
2. **Given** the last session of this week has passed, **When** the client opens Today on a rest day, **Then** the next session is the first one of the following week, with its date.
3. **Given** no session is left in the plan after today, **When** the client opens Today, **Then** the next-session line is absent and nothing else on the card breaks.
4. **Given** a session's entries, **When** its time is shown, **Then** it is the estimate the plan editor gives for the same session.
5. **Given** the UI language is Polish, **When** the card is shown, **Then** every text and the date are Polish.

---

### User Story 2 - Client jumps to the week from the rest day (Priority: P1)

The card has a "See this week" action that opens the Plan tab.

**Why this priority**: The next session is one session; the client may want the whole week.

**Independent Test**: On a rest day, choose "See this week": the Plan tab opens on the current week.

**Acceptance Scenarios**:

1. **Given** a rest day, **When** the client chooses "See this week", **Then** the Plan tab opens.
2. **Given** a session day or a finished plan, **When** Today is shown, **Then** there is no "See this week" action.

---

### User Story 3 - Client sees how the week is going (Priority: P2)

Under the card, a line says how many sessions of this week are done: "1 of 3 sessions done this week".

**Why this priority**: It rounds off the rest-day screen but does not block the rest of it.

**Independent Test**: On a rest day in a week with 3 sessions of which 1 is done, the line reads "1 of 3 sessions done this week"; in Polish it uses the right plural form.

**Acceptance Scenarios**:

1. **Given** a week with 3 planned sessions and 1 done, **When** Today shows a rest day, **Then** the line reads "1 of 3 sessions done this week".
2. **Given** the counts 1, 2, 5, 12 and 22, **When** the line is shown in Polish, **Then** each uses the correct plural form.
3. **Given** the session states cannot be read, **When** Today shows a rest day, **Then** the line is left out and the rest of the card is unaffected.

---

### User Story 4 - The rest day stays private (Priority: P1)

The next session is the signed-in client's own, with the same refusals as the rest of Today.

**Why this priority**: A plan is personal training data, and the project rule is that another person's resource answers 404.

**Independent Test**: As a client with a draft or no plan, ask for Today: "no plan", as before. As a trainer: refused by role.

**Acceptance Scenarios**:

1. **Given** a draft or no plan, **When** Today is asked for, **Then** the answer is "no plan" and nothing of the draft is revealed.
2. **Given** a trainer or a visitor who is not signed in, **When** they ask for Today, **Then** a trainer is refused by role and a visitor is asked to sign in.

---

### Edge Cases

- A plan whose only session is on a later weekday of the last week: it is the next session; after it, there is none.
- On the plan's last day the next session is absent when today has no session.
- A finished plan keeps its finished message, with no "Rest" badge, next session, "See this week" or count.
- A session day and a finished plan are unchanged by this task.
- A long session name wraps; nothing is cut.
- A client in another time zone sees the next session's date in their own zone, the same day as "today".
- A failed request shows the usual translated message and "Try again".
- The check-in named in the line does not exist yet (story #109): the line is plain text, not a link.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: On a rest day, Today's answer MUST also give the next session of the plan: its name, its date in the client's time zone and its ordered entries; it MUST give none when the plan has no session after today.
- **FR-002**: The next session MUST be the first session strictly after today by date, in this week or a later one, and never a day beyond the plan's last week.
- **FR-003**: The rest-day card MUST show a "Rest" badge, the line "No session today. No check-in either.", the next session as "Next: <name> · <date> · <minutes> min", and a "See this week" action that opens the Plan tab.
- **FR-004**: The estimated minutes MUST be computed the way the plan editor estimates the same session.
- **FR-005**: The next-session line MUST be absent when the plan has no session after today.
- **FR-006**: Under the card, the screen MUST show how many sessions of this week are done out of those planned, with Polish plural forms; it MUST be left out when the states cannot be read.
- **FR-007**: A session day and a finished plan MUST look and behave as before; none of the new elements appears there.
- **FR-008**: The answer MUST be the signed-in client's own, taken from their sign-in; a draft and a missing plan answer "no plan"; a trainer is refused by role and a visitor with the standard unauthenticated answer. Tests prove each.
- **FR-009**: The screen MUST be built from Kinetiq tokens and shadcn components, designed at 375 px, in light and dark, with visible focus and keyboard use; the page MUST NOT scroll sideways at 375 px and at 320 px (200% zoom).
- **FR-010**: Every user-facing text MUST exist in English and Polish, with Polish plural forms; dates follow the UI language.
- **FR-011**: Demo data MUST let a rest day with a next session in this week, one with it in a later week, and one without a next session be seen in the browser.

### Public endpoints

None. Every operation needs a signed-in client.

### Key Entities

- **Today view** (spec `89-todays-session`): gains, on a rest day, the next session: the plan's session and the date it falls on. Derived from the plan and the clock; not stored.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: On a rest day a client sees when and what they train next without any tap.
- **SC-002**: In tests, the next session is correct across the week boundary, on the plan's last day and for a plan with no session left, with a fixed clock.
- **SC-003**: The minutes shown for the next session equal the plan editor's estimate for the same session in 100% of tested cases.
- **SC-004**: The rest-day card passes WCAG 2.2 AA checks for contrast, labels, focus order and visible focus, in light and dark, at 375 px.
- **SC-005**: Every text of the card exists in English and Polish, and the count line uses the correct Polish plural for 1, 2, 5, 12 and 22.
- **SC-006**: A draft or a missing plan returns the same answer 100% of the time in tests, and no request returns another client's plan.

## Departures from the story and the board

| Source | It says | This spec | Why |
|---|---|---|---|
| Board "Today · check-in" | A check-in on the rest-day card | Only the line "No check-in either." | The check-in is story #109, which builds on this screen. |

## Assumptions

- The estimate of a session's time is the plan editor's (`plan-draft.ts`); the answer carries the entries so the screen computes it the same way, rather than a second formula on the server.
- The count of done sessions uses the session states of the current plan week (spec `98-finish-session`) and the number of sessions the plan has in a week.
- "See this week" opens the Plan tab on its current week, which is where it opens by default.
- The week is Monday to Sunday in the client's time zone, and today is the client's date (spec `89-todays-session`).
- The screen reads the plan when it loads and is not refreshed while open.
