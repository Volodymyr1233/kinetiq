# Feature Specification: Client sees their plan

**Feature Branch**: `feat/75-client-plan`

**Created**: 2026-11-02

**Status**: Draft

**Input**: Story #75 "Client sees their plan" and its tasks #76 (client plan API with week view), #77 (plan screen with week strip) and #78 (plan-ready screen and plan-finished state). Screens: Kinetiq Screens, board "Client · Plan" and "Client · Quiz plan ready". Builds on spec `68-plan-editor` (the plan, its publishing and the first week) and spec `50-client-quiz` (the waiting state). Later stories use it: #89 (today's session) reads the same plan, #92 (logging) starts from a session of it. The story, the tasks and the board are starting points; every place this spec departs from them is listed under "Departures from the story and the board".

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Client reads their plan, week by week (Priority: P1)

A client whose trainer has published a plan opens the Plan tab and sees the current week: a strip of the plan's sessions on their days of the week, each with its name. They can move to the previous and the next week of the plan. Choosing a session opens it to its exercises and targets.

**Why this priority**: This is the point of the story: the client knows what is coming. Today (#89) and logging (#92) build on the same plan.

**Independent Test**: Publish a plan with two sessions for a demo client, sign in as that client and open the Plan tab: the current week shows both sessions on their days, a session opens to its exercises with sets, repetitions or seconds, load, target RPE and rest, and next and previous move through the weeks of the plan.

**Acceptance Scenarios**:

1. **Given** a client with a published plan in plan week 3 of 12, **When** they open the Plan tab, **Then** they see "Week 3 of 12" with the Monday-to-Sunday dates of that week and a strip of seven days, where each of the plan's sessions stands on its day with its name and the other days are marked as rest.
2. **Given** the plan week strip, **When** the client chooses next week, **Then** it shows week 4 with the same sessions on the dates of that week; **when** they choose previous week, **then** it shows week 2.
3. **Given** week 1, **When** it is shown, **Then** previous week is unavailable; **given** the last week of the plan, **then** next week is unavailable.
4. **Given** the week strip, **When** the client chooses a session, **Then** its card opens to the session's exercises in order, each with its name, sets, repetitions or seconds, load (or no load), target RPE and rest; **when** they choose it again, **then** it closes.
5. **Given** an exercise measured in seconds, **When** its targets are shown, **Then** the amount reads in seconds, never as repetitions.
6. **Given** a load, **When** it is shown, **Then** it is in the client's own unit (kilograms or pounds); **and** an exercise with no load says so in words, not as "0".
7. **Given** the client opens the Plan tab, **When** the week shown is not the current one, **Then** a way back to the current week is offered.
8. **Given** a plan week in the past or the future, **When** it is shown, **Then** it shows the plan's sessions as they are now; the plan's sessions repeat every week (change history belongs to story #143).
9. **Given** a phone at 375 px, **When** the client reads the strip, **Then** nothing scrolls sideways and a day and its session are readable at once.

---

### User Story 2 - Client sees their plan is ready, once (Priority: P1)

The first time a client opens the app after their trainer publishes the plan, they see a "your plan is ready" screen instead of landing on a tab. One action takes them to the Plan tab. After that the screen never shows again for that plan.

**Why this priority**: It is the moment the client's waiting ends, and it must not repeat on every visit.

**Independent Test**: Publish a plan for a client who waits for it, sign in as that client: the "plan is ready" screen shows with the trainer's goal for them; choose "View my plan": the Plan tab opens; sign out and in again, or open another device: the screen does not show again.

**Acceptance Scenarios**:

1. **Given** a client whose plan was published and who has not seen the plan-ready screen, **When** they open the app, **Then** they see the plan-ready screen with a short line about the plan (its goal, its number of weeks and its number of sessions per week) and the action "View my plan".
2. **Given** the plan-ready screen, **When** the client chooses "View my plan", **Then** the Plan tab opens and the plan is marked as seen.
3. **Given** a plan marked as seen, **When** the client opens the app again, on this or another device, **Then** the plan-ready screen does not show.
4. **Given** a client who opens the Plan tab by its address before seeing the plan-ready screen, **When** the tab opens, **Then** the plan is marked as seen, so the screen does not show afterwards.
5. **Given** a trainer who edits a published plan, **When** the client has already seen the plan-ready screen, **Then** it does not show again; it shows once per published plan, not once per save.
6. **Given** a client without a published plan, **When** they open the plan-ready address, **Then** they are sent to where their state belongs (the quiz or the waiting screen), not shown a plan they do not have.

---

### User Story 3 - Client sees a plan that has finished (Priority: P2)

After the last week of the plan, the Plan tab says "Your trainer is preparing your next plan." instead of a week strip.

**Why this priority**: Every plan ends; the client must not see an empty or broken week.

**Independent Test**: Take a demo client whose published plan is past its last week and open the Plan tab: the message shows, with no week strip.

**Acceptance Scenarios**:

1. **Given** a plan whose current plan week is above its number of weeks, **When** the client opens the Plan tab, **Then** they see "Your trainer is preparing your next plan." and the plan's number of weeks as finished, with no week strip.
2. **Given** a finished plan, **When** the trainer lengthens it so that the current week falls inside it again, **Then** the client sees the week strip again on the next visit.
3. **Given** the last week of a plan, **When** it is shown, **Then** it is a normal week; the finished state starts only with the Monday after it.

---

### User Story 4 - A plan is private to its client (Priority: P1)

A client can read only their own published plan. A draft is never shown, not even a name or a number.

**Why this priority**: A plan is personal training data, and the project's rule is that another person's resource answers 404; the draft rule keeps a trainer's unfinished work from the client.

**Independent Test**: As a client with only a draft, ask for the plan: not found. As a trainer, ask for the client plan operation: refused by role. With no sign-in: the standard refusal.

**Acceptance Scenarios**:

1. **Given** a client whose plan is a draft, or who has none, **When** they ask for their plan, **Then** the answer is "no plan", the same for both, and nothing of the draft is revealed.
2. **Given** a trainer or a visitor who is not signed in, **When** they ask for the client's plan operation, **Then** a trainer is refused by role and a visitor is asked to sign in.
3. **Given** any request, **When** it names a client, **Then** the plan returned is always the signed-in client's own, taken from their sign-in; no request can name another client's plan.

---

### Edge Cases

- A client whose plan was published less than a week ago is in week 1; a client who moved to a time zone behind the one of publishing can for a moment be before week 1, and then still sees week 1, never week 0.
- Two sessions never share a day (the plan rules forbid it), so a day holds at most one session.
- A week asked for outside the plan (0, negative, or beyond the last week) is refused as a whole, not clamped to another week silently.
- A week asked for while the plan is finished is refused the same way; the finished state is a state of the plan, not a week.
- A session with many exercises scrolls within its card; nothing is cut.
- Long Polish exercise and session names wrap.
- A trainer saves a published plan while the client has it open: the client sees the saved content on their next load, never half of it.
- A client with a plan that was published but who has no sessions in a week (not possible for a published plan, which is complete) never sees an empty strip.
- No error response contains server internals.

## Requirements *(mandatory)*

### Functional Requirements

**Client plan API** (task #76)

- **FR-001**: A signed-in client MUST be able to read their own published plan for one plan week: its goal, number of weeks, the week shown with its first and last date, and every session with its name, its day of the week and its ordered exercise entries (the exercise's name in the language of the request, its measure, sets, amount, optional load, target RPE and rest).
- **FR-002**: The plan returned MUST always be the signed-in client's own, taken from their sign-in; the request MUST NOT take a client from its path or body.
- **FR-003**: A draft MUST never be returned: a client whose plan is a draft and a client with no plan MUST both get the same "no plan" answer.
- **FR-004**: The week MUST be asked for as a plan week number; it defaults to the current plan week in the client's time zone. A number below 1 or above the plan's weeks MUST be refused with a stable code, and nothing is returned.
- **FR-005**: The answer MUST also say the current plan week and whether the plan has finished (current plan week above the number of weeks); for a finished plan it MUST NOT return a week.
- **FR-006**: Loads MUST be returned in kilograms as stored, and the screen converts them to the client's own unit.
- **FR-007**: A trainer and a visitor MUST be refused: a trainer by role, a visitor with the standard unauthenticated answer. Tests prove each.

**Plan screen** (task #77)

- **FR-008**: The Plan tab of the client area MUST show the plan week asked for, with its label ("Week N of M"), its Monday-to-Sunday dates and a strip of seven days, each with its weekday and date, and the session standing on it or a rest-day mark.
- **FR-009**: The strip MUST offer previous week, next week and, away from the current week, a way back to the current week; the first and last weeks disable the matching control, and a live message announces the week shown.
- **FR-010**: Each session MUST open to a card with its exercises in order and each exercise's targets (FR-001), and close again; opening and closing work by pointer, touch and keyboard, with the open state exposed to assistive technology.
- **FR-011**: Loads MUST show in the client's own unit, an exercise measured in seconds MUST show its amount in seconds, and an exercise without a load MUST say so in words.
- **FR-012**: While the plan loads, the screen MUST show a skeleton of its layout; on failure a translated message and "Try again"; when the client has no published plan, the state of their account decides what they see (the quiz or the waiting screen), not an error.
- **FR-013**: The screen MUST be built from Kinetiq tokens and shadcn components, designed at 375 px, in light and dark, with visible focus; at 375 px and at 320 px (200% zoom) the page MUST NOT scroll sideways.

**Plan ready and finished** (task #78)

- **FR-014**: A client with a published plan they have not yet seen MUST see the plan-ready screen on opening the app instead of landing on a tab: a short line about the plan (goal, number of weeks, sessions per week) and the action "View my plan".
- **FR-015**: Whether a client has seen their plan MUST be kept with the plan, not in the browser, so that it counts across devices and a new browser. It MUST be set when the client chooses "View my plan" and also when they first open the Plan tab by any other way.
- **FR-016**: The plan-ready screen MUST show once per published plan: a later save of the plan by the trainer MUST NOT reset it.
- **FR-017**: A finished plan MUST show "Your trainer is preparing your next plan." on the Plan tab in place of the strip and show how many weeks it had.
- **FR-018**: A client without a published plan who opens the plan-ready address MUST be sent to the quiz or the waiting screen according to their state.

**Quality**

- **FR-019**: Every user-facing text MUST exist in English and Polish, with Polish plural forms; dates, weekdays and units follow the UI language.
- **FR-020**: Demo data MUST include a client whose plan is published and not yet seen, a client whose plan is published and seen, and a client whose published plan has finished.

### Public endpoints

None. Every operation needs a signed-in client.

### Key Entities

- **Plan**: Unchanged from spec `68-plan-editor`: a client's goal, number of weeks, first week once published, sessions. A client sees it only once it has a first week.
- **Week view**: What the client reads for one plan week: the plan's sessions placed on the dates of that week. It is derived from the plan and the week number; it is not stored.
- **Plan seen mark**: Whether the client has seen their published plan. One per client's plan; set once, never reset by a save.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A client finds the next session of their plan and its exercises within 10 seconds of opening the Plan tab.
- **SC-002**: In tests, a draft or a missing plan returns the same answer 100% of the time, and no request returns another client's plan.
- **SC-003**: The plan-ready screen shows exactly once per published plan, however many devices the client uses and however many times the trainer saves.
- **SC-004**: The week and its dates are correct on both sides of the Sunday-to-Monday boundary in the client's time zone, proven with a fixed clock.
- **SC-005**: The Plan tab passes WCAG 2.2 AA checks for contrast, labels, focus order and visible focus, in light and dark, at 375 px.
- **SC-006**: Every text of the screens exists in English and Polish.
- **SC-007**: No error response of this story contains server-internal text.

## Departures from the story and the board

| Source | It says | This spec | Why |
|---|---|---|---|
| #78 | "Plan-ready shows once per published plan" | Kept per plan on the server, set also when the Plan tab is opened first | The story does not say where it is kept; a browser-only flag would show it again on a new device and the project forbids storing state for a client's data in the browser. |
| Board | Previous and next weeks show different content | The same sessions every week, on the dates of that week | A plan's sessions repeat every week; weeks that differ arrive with story #143. |
| Board | Sessions marked done or missed on the strip | Not shown | Logging and finishing a session are stories #92 and #98. |

## Assumptions

- The client's unit and time zone come from their account settings (spec `42-account-settings`); the week and its dates are computed in the client's time zone.
- A client's plan is the one plan of the client (spec `68-plan-editor`, FR-001), so the plan is addressed by the signed-in client, not by an id.
- Routing a client past the quiz and the waiting screen once a plan is published is task #53 (open pull request #229); this story adds the plan-ready screen between them and the tabs. Until #229 merges, a client with a published plan can open the Plan tab and the plan-ready address by their address.
- "Today" is not highlighted on the strip beyond marking the current day; the session of the day and starting it belong to story #89.
- The plan's weeks are never edited per week; a trainer's change to a published plan applies from today and shows on every week (spec `68-plan-editor`, FR-012).
- The plan-ready screen's wording and the finished-plan wording follow the story ("your plan is ready", "Your trainer is preparing your next plan"); Polish is to be confirmed by a copywriter, as the design system says.
- A client's view of a past week is as the plan stands now; keeping earlier targets for history is story #143.
- Messages to the trainer from the plan-ready screen are not part of this story (task #230).
