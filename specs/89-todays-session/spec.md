# Feature Specification: Client sees today's session

**Feature Branch**: `feat/89-todays-session`

**Created**: 2026-11-06

**Status**: Draft

**Input**: Story #89 "Client sees today's session" and its tasks #90 (Today API) and #91 (Today screen). Screen: Kinetiq Screens, board "Client · Today". Builds on spec `75-client-plan` (the client's published plan, its weeks and the plan-ready screen) and spec `50-client-quiz` (a client without a published plan is sent to the quiz or the waiting screen). Later stories use it: #109 (readiness check-in on this screen, #112), #92 (the logger behind "Start session") and #98 (finishing a session, the session states on Today). The story, the tasks and the board are starting points; every place this spec departs from them is listed under "Departures from the story and the board".

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Client sees what to train today (Priority: P1)

A client opens the app and the Today tab shows the session their plan has for today's weekday: its name and each exercise with its targets. Today is the date in the client's own time zone.

**Why this priority**: This is the point of the story: the client opens the app and knows what to do. #112 and the logger build on the same screen.

**Independent Test**: Sign in as a client whose published plan has a session on today's weekday: the Today tab shows that session with every exercise in order, with sets, repetitions or seconds, load, RPE and rest.

**Acceptance Scenarios**:

1. **Given** a client whose plan has a session on today's weekday, **When** they open Today, **Then** they see the date, the session's name and its exercises in plan order, each with its name and its targets (sets, repetitions or seconds, load or "No load", target RPE, rest).
2. **Given** an exercise measured in seconds, **When** its targets are shown, **Then** the amount reads in seconds, never as repetitions.
3. **Given** a load, **When** it is shown, **Then** it is in the client's own unit, and an exercise without a load says so in words, not as "0".
4. **Given** a session with many exercises, **When** it is shown, **Then** nothing is cut: the list grows and the page scrolls, and a long name wraps.
5. **Given** a client in Los Angeles at 23:30 on Sunday, when it is already Monday in UTC and in Warsaw, **When** they open Today, **Then** it is Sunday's: today is the date where the client is.
6. **Given** a trainer saved a change to the published plan, **When** the client opens Today, **Then** they see the session as saved; the change applies from today.

---

### User Story 2 - Client sees a rest day or a finished plan (Priority: P1)

When the plan has no session for today, Today says it is a rest day. When the plan has ended, Today says the trainer is preparing the next plan.

**Why this priority**: Most days of a week are not training days, and every plan ends; the tab must never look empty or broken.

**Independent Test**: Open Today for a client whose plan has no session today: "Rest day". Open it for a client whose plan has finished: "Your trainer is preparing your next plan."

**Acceptance Scenarios**:

1. **Given** a plan with no session on today's weekday, **When** the client opens Today, **Then** they see that today is a rest day and the date, with no session card and no "Start session".
2. **Given** a plan whose last week has passed, **When** the client opens Today, **Then** they see "Your trainer is preparing your next plan." and no session card, even on a weekday that has a session in the plan.
3. **Given** the last day of the plan's last week, **When** it falls on a training day, **Then** Today shows that session as usual; the plan finishes the Monday after.
4. **Given** the trainer lengthens a finished plan so that today falls inside it again, **When** the client opens Today, **Then** they see today's session or a rest day again.

---

### User Story 3 - Client can start the session (Priority: P2)

A day with a session shows "Start session". Starting and logging sets is story #92, so until the logger exists the action is shown but cannot be used.

**Why this priority**: The story's acceptance criterion names it, but the logger belongs to story #92; this story provides the place for it, and a follow-up task under #89 turns it on.

**Independent Test**: On a day with a session, "Start session" is present and has an accessible name; on a rest day and for a finished plan it is not.

**Acceptance Scenarios**:

1. **Given** a day with a session, **When** Today is shown, **Then** "Start session" is shown once, as the one forward action of the screen, large enough to tap on a phone.
2. **Given** the logger does not exist yet, **When** the client looks at "Start session", **Then** it is disabled and says, in words, that starting arrives with logging; it does not lead to a blank or missing page.
3. **Given** the logger exists, **When** the client chooses "Start session", **Then** the logger opens (a task under #89, blocked by #94 and #95, adds this link; this story leaves the place and the label ready).

---

### User Story 4 - Today is private to the client (Priority: P1)

A client sees only their own session; a draft and a missing plan show nothing of a plan.

**Why this priority**: A plan is personal training data, and the project's rule is that another person's resource answers 404.

**Independent Test**: As a client whose plan is a draft, ask for Today: "no plan", the same as for a client with none. As a trainer, ask for it: refused by role. With no sign-in: the standard refusal.

**Acceptance Scenarios**:

1. **Given** a client whose plan is a draft, or who has none, **When** Today is asked for, **Then** the answer is "no plan", the same for both, and nothing of the draft is revealed.
2. **Given** a trainer or a visitor who is not signed in, **When** they ask for Today, **Then** a trainer is refused by role and a visitor is asked to sign in.
3. **Given** any request, **When** it is made, **Then** the session returned is the signed-in client's own, taken from their sign-in; no request can name another client.

---

### Edge Cases

- A plan has at most one session a day, so Today never shows two.
- A client who changes their time zone sees the day of the new zone on the next load.
- The screen is not live: a day that ends while it is open shows the old day until the client reloads or returns to the tab (live refresh is out of scope).
- A client without a published plan never sees the Today tab; the quiz or the waiting screen stands in (spec `50-client-quiz`), and a published plan not yet seen shows the plan-ready screen first (spec `75-client-plan`).
- A plan published today for a weekday that has a session shows that session today.
- A failed request shows a translated message and "Try again", never server text.
- No error response contains server internals.

## Requirements *(mandatory)*

### Functional Requirements

**Today API** (task #90)

- **FR-001**: A signed-in client MUST be able to read what they train today: today's date in their time zone, and one of three states: a session, a rest day, or a finished plan.
- **FR-002**: For a session, the answer MUST give its name and its ordered exercise entries, each with the exercise's name in the language of the request, its measure, sets, amount, optional load, target RPE and rest.
- **FR-003**: The state MUST be a rest day when the plan has no session on today's weekday, and a finished plan when the current plan week is above the plan's weeks; a finished plan MUST NOT return a session.
- **FR-004**: Today's date MUST be computed in the client's time zone, not the server's or the trainer's; a fixed clock across a time-zone boundary MUST give the client's own day.
- **FR-005**: A draft and a missing plan MUST both answer "no plan" with the same code, and nothing of a draft is returned.
- **FR-006**: The session returned MUST be the signed-in client's own, taken from their sign-in; the request takes no client from its path or body.
- **FR-007**: A trainer and a visitor MUST be refused: a trainer by role, a visitor with the standard unauthenticated answer. Tests prove each.
- **FR-008**: Loads MUST be returned in kilograms as stored; the screen converts them to the client's own unit.

**Today screen** (task #91)

- **FR-009**: The Today tab of the client area MUST show the date and, by state: the session card (name, exercises with targets, "Start session"), a rest-day message, or the finished-plan message.
- **FR-010**: Loads MUST show in the client's own unit, an exercise measured in seconds MUST show its amount in seconds, and an exercise without a load MUST say so in words.
- **FR-011**: "Start session" MUST be shown for a session day only, once, as the one forward action of the screen, with a touch target of 48 px; until the logger exists it MUST be disabled and say why, in words.
- **FR-012**: While Today loads, the screen MUST show a skeleton of its layout; on failure a translated message and "Try again"; when the client has no published plan, the area's routing decides what they see, not an error on this tab.
- **FR-013**: The screen MUST be built from Kinetiq tokens and shadcn components, designed at 375 px, in light and dark, with visible focus and keyboard use; at 375 px and at 320 px (200% zoom) the page MUST NOT scroll sideways.

**Quality**

- **FR-014**: Every user-facing text MUST exist in English and Polish, with Polish plural forms; the date and units follow the UI language and the client's settings.
- **FR-015**: Demo data MUST let each state be seen in the browser: a client with a session today, a client with a rest day and a client whose plan has finished.

### Public endpoints

None. Every operation needs a signed-in client.

### Key Entities

- **Today view**: What the client sees for one day: the date in their time zone and one state: session, rest day or finished. For a session, it carries the plan's session with its entries. It is derived from the plan and the clock; it is not stored.
- **Session, Exercise entry, Target**: Unchanged from the plan (spec `68-plan-editor`).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A client sees today's session, or that it is a rest day, within 2 seconds of opening the Today tab on an ordinary connection.
- **SC-002**: A client finds the first exercise of today's session and its targets without any tap.
- **SC-003**: In tests, a draft or a missing plan returns the same answer 100% of the time, and no request returns another client's session.
- **SC-004**: Today's date is the client's on both sides of the Sunday-to-Monday boundary and across a time-zone offset, proven with a fixed clock.
- **SC-005**: The Today tab passes WCAG 2.2 AA checks for contrast, labels, focus order and visible focus, in light and dark, at 375 px.
- **SC-006**: Every text of the screen exists in English and Polish.
- **SC-007**: No error response of this story contains server-internal text.

## Departures from the story and the board

| Source | It says | This spec | Why |
|---|---|---|---|
| #89 | "Start session opens the logger" | The action is shown and disabled until the logger exists | The logger is task #95 of story #92, built by someone else; a link to a page that does not exist would break the screen. A task under #89, blocked by #94 and #95, turns it on, so the criterion is not lost when #89 closes. |
| #89 | The screen shows "the waiting state" | Not drawn on the Today tab | A client waiting for a plan is sent to the waiting screen by the client area (spec `50-client-quiz`) and never reaches the tab. |
| Board | Session states (started, done, missed) on Today | Not shown | Starting, finishing and missing a session are stories #92 and #98 (task #100). |
| Board | Readiness check-in above the session | Not shown | That is story #109 (task #112), which builds on this screen. |

## Assumptions

- The client's time zone and unit come from their account settings (spec `42-account-settings`); today is computed in that time zone.
- The plan's sessions repeat every week, so today's session is the plan's session on today's weekday (spec `68-plan-editor`, FR-012 and the week model).
- The Today tab keeps its place in the client area and its title; this story replaces its placeholder.
- The screen reads the plan as it is when it loads and is not refreshed while it stays open.
- "Start session" is disabled with a visible line, not hidden, so the client sees where training starts once logging arrives.
- A client who has not yet seen their plan is sent to the plan-ready screen before Today (spec `75-client-plan`); this story does not change that.
- The first exercise and targets are shown in full, not collapsed, because the screen is for training, not for browsing.
