# Feature Specification: Trainer reviews and publishes a drafted plan

**Feature Branch**: `feat/85-draft-review`

**Created**: 2026-11-07

**Status**: Draft

**Input**: Story #85 "Trainer reviews and publishes a drafted plan" and its tasks #86 (approvals API: drafted plans), #87 (approvals screen: drafted plans) and #88 (draft panel in the editor). Screens: Kinetiq Screens, boards "Trainer · approvals" and "Trainer · plan editor". Design: the Kinetiq design system. The story, the tasks and the boards are starting points, not the source of truth; every place this spec departs from them is listed under "Departures from the story and the boards". Builds on spec 79 (drafting) and spec 68 (plan editor).

## Clarifications

### Session 2026-11-07

- Q: While a rebuild runs, does the editor show the previous draft? → A: Yes, read-only, under the drafting state; every editing control, Save and Publish are disabled.
- Q: Does a draft the trainer saved by hand stay in the Approvals list? → A: Yes. Every unpublished draft is listed (built, failed and saved), each labelled by kind, and all are counted in the navigation.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Drafted plans wait in one place (Priority: P1)

The trainer opens Approvals and sees every client whose plan is still a draft, the one waiting longest first, and opens any of them in the plan editor.

**Why this priority**: Today a draft is found only by opening each client's Plan tab. Without this list a new client can wait for days because nobody knew a plan was ready.

**Independent Test**: A trainer with three clients holding drafts of different ages, one client with a published plan and one client of another trainer with a draft: Approvals lists exactly the three, oldest first; the other two never appear.

**Acceptance Scenarios**:

1. **Given** clients whose plans are drafts, **When** the trainer opens Approvals, **Then** each shows as one row with the client's name, how long the draft has waited, the kind of draft (to review, failed, or saved by the trainer) and the plan's length and sessions per week; the oldest draft is first.
2. **Given** a row, **When** the trainer chooses it, **Then** the client's plan opens in the editor.
3. **Given** an automatically built draft, an empty failed draft and a draft the trainer saved, **When** they are listed, **Then** each carries the same wording as the client's plan state (spec 79, FR-024): "Plan to review", "Draft failed", "Draft".
4. **Given** a client whose first plan is still being drafted, **When** the trainer opens Approvals, **Then** that client is not listed: there is nothing to open yet. A client whose draft is being rebuilt stays listed, because the previous draft exists.
5. **Given** a plan that was published, **When** the trainer opens Approvals, **Then** its client has left the list.
6. **Given** no drafts, **When** the trainer opens Approvals, **Then** an empty state says nothing waits, with a way back to the clients.
7. **Given** the navigation, **When** drafts wait, **Then** Approvals shows how many.
8. **Given** a trainer, **When** they ask for the list, **Then** they get only their own clients; another trainer's drafts, and a client's own sign-in, never reach it.

---

### User Story 2 - The trainer reviews a draft next to what the client said (Priority: P1)

Beside a draft the editor shows the client's quiz answers and notes, so the trainer can judge the exercises and loads without leaving the plan. Publishing works exactly as in spec 68.

**Why this priority**: The draft was built from those answers; reviewing it blind defeats the review that makes an automatic plan safe.

**Independent Test**: Open a built draft: a panel next to the sessions lists goal, experience, level, age, height, weight, likes, days per week and the notes. Publish it: the client's plan appears, the panel is gone and the client has left Approvals.

**Acceptance Scenarios**:

1. **Given** a draft of any kind, **When** the editor shows it, **Then** a panel next to the sessions shows the client's quiz answers and notes as the client gave them, and the draft's note from spec 79.
2. **Given** a failed draft, **When** the panel shows it, **Then** it carries the failed-draft message and the Build again action; the note above the sessions does not repeat it.
3. **Given** a client with no notes, **When** the panel shows, **Then** it says the client wrote none.
4. **Given** a published plan, **When** the editor shows it, **Then** there is no panel: the answers served their purpose.
5. **Given** a draft, **When** the trainer publishes it, **Then** it behaves as spec 68 describes and the client leaves Approvals.
6. **Given** a narrow screen, **When** the editor shows a draft, **Then** the panel stacks under the sessions and nothing scrolls sideways.

---

### User Story 3 - The trainer asks for a new automatic draft (Priority: P2)

When a draft is not good enough, or drafting failed, the trainer asks the system to build it again. After confirming, the current draft is replaced by a new automatic one; the editor shows that drafting is running and moves on by itself.

**Why this priority**: It saves building from nothing, but the trainer can still build by hand, so it follows the review.

**Independent Test**: With a stand-in drafting service that answers slowly, ask to build again: the editor shows the drafting state, Build again is unavailable and a second request is refused, then the editor shows the new draft by itself.

**Acceptance Scenarios**:

1. **Given** a draft, **When** the trainer chooses Build again, **Then** a confirmation says the current draft will be replaced if a new one can be built, and its edits lost; nothing happens until they confirm.
2. **Given** the confirmation, **When** the trainer confirms, **Then** the editor shows the drafting state of spec 79 (a line with a skeleton) above the previous draft, which stays readable but cannot be edited, saved or published; it moves on by itself, within about 5 seconds of drafting ending, to the new draft when one was built, or back to the previous draft with a message when it could not be.
3. **Given** drafting started by Build again, **When** it is running, **Then** it follows the rules of the first drafting: its attempts, checks, time limit and clean prompt (spec 79), and a result arriving later than its limit is dropped.
4. **Given** drafting is running for this client, **When** the trainer asks again, **Then** the action is unavailable on screen and a request sent anyway is refused with `409` and the code `plan.drafting`.
5. **Given** a trainer who has already built again as many times as the daily limit allows across all their clients, **When** they choose Build again, **Then** the action is unavailable and says when it can be used again; a request sent anyway is refused with `429` and a code of its own.
6. **Given** the limit, **When** a request is refused for drafting running or for another reason, **Then** it does not use up one of the day's builds.
7. **Given** a new draft that was built, **When** it arrives, **Then** it replaces the old one entirely.
7a. **Given** drafting fails twice, **When** it ends, **Then** the previous draft stays exactly as it was, with the same version, and the editor says a new draft could not be built and the previous one is kept; the day's count is not given back.
8. **Given** a published plan, **When** a request to build again arrives, **Then** it is refused as spec 68 refuses changes to a published plan, and the editor offers no such action.
9. **Given** a client of another trainer or an unknown client, **When** a request to build again arrives, **Then** it answers `404`, as every other plan operation.
10. **Given** a stale tab showing the replaced draft, **When** it saves, **Then** the save is refused as changed elsewhere (spec 68) and nothing is overwritten.
11. **Given** a rebuild is running, **When** the client opens their waiting screen, **Then** it still says their trainer is reviewing the plan; nothing of the rebuild shows.

---

### Edge Cases

- The trainer opens Approvals while a draft is replaced: the row shows what is true at the moment of the request; opening it shows the drafting state.
- Two clients' drafts have the same age: a stable order, so the list does not shuffle between visits.
- A very large number of drafts: the list stays readable on desktop at 1280 px (the trainer's audience) and does not break on a narrow screen.
- Build again is requested twice at once: exactly one starts and one is refused with `plan.drafting`; the day's count rises by one.
- The limit is reached while a drafting is running: the running one finishes; only new requests are refused.
- The drafting service is not configured: Build again ends at once as a failure, keeping the previous draft (FR-015); it counts toward the limit like any request, which keeps the rule uniform.
- A client's quiz answers are gone (account state changed): the panel states the answers are unavailable instead of failing the editor.
- The trainer's client was reassigned during drafting: as in spec 79, the result is dropped and nothing is stored.
- A very long note from the client: shown in full, wrapped, as plain text only.
- The drafting service is down for a long time: the day's builds are spent on failures; the trainer is told when they can try again, not that the service failed.

## Requirements *(mandatory)*

### Functional Requirements

**Approvals list** (tasks #86, #87)

- **FR-001**: The system MUST list the signed-in trainer's clients whose plan is a draft, oldest first by when the draft was created, with a stable tiebreak.
- **FR-002**: Each entry MUST carry the client's identity and name, the kind of draft (built, failed or saved by the trainer), the draft's age, and the plan's length in weeks and sessions per week.
- **FR-003**: A client whose first plan is still being drafted MUST NOT be listed, and neither MUST a client with a published plan or no plan. A client whose draft is being rebuilt stays listed.
- **FR-004**: The list MUST contain only the signed-in trainer's own clients. It requires a trainer's sign-in; a client's sign-in or no sign-in is refused as everywhere else.
- **FR-005**: The Approvals page MUST show the drafts as one section, each entry opening the client's plan in the editor, and an empty state when none wait, in English and Polish.
- **FR-006**: The navigation entry for Approvals MUST show the number of drafts waiting (all kinds, as listed), and it MUST NOT claim waiting plan changes, which do not exist yet.
- **FR-007**: The page MUST be usable by keyboard and screen reader: the list is a list, each entry has one link with the client's name, and the kind of draft is conveyed by text, not colour alone.

**Draft panel** (task #88)

- **FR-008**: The system MUST make the client's quiz answers and notes readable to that client's trainer only, as the client submitted them.
- **FR-009**: The editor MUST show, beside a draft, a panel holding the answers (goal, experience, level, age, height, weight, likes, days per week), the notes, and, for a failed draft, the failed-draft message with the Build again action. The panel is absent for a published plan.
- **FR-010**: The note above the sessions that spec 79 introduced (FR-026) MUST be kept for a built draft and MUST NOT duplicate the panel's failed-draft message.
- **FR-011**: Notes and answers MUST be shown as plain text. Weight MUST follow the trainer's units setting, as loads do in the editor; height stays in centimetres.

**Building again** (task #88)

- **FR-012**: Building again MUST be available only for a draft of one's own client; a published plan, a plan being drafted, a foreign client and an unknown client are refused (`409` with `plan.already-published`, `409` with `plan.drafting`, `404` and `404`).
- **FR-013**: The request MUST return without waiting for the drafting service, and MUST start drafting the way a quiz submit does (spec 79, FR-001, FR-002, FR-005): recorded first, run in the background, ended within 6 minutes in every case.
- **FR-013a**: A rebuild MUST NOT change what the client sees: their quiz status stays "waiting for plan" and the draft stays invisible to them throughout (spec 79, FR-003 and FR-029).
- **FR-014**: While drafting runs for a client, the plan MUST NOT be editable or publishable, though it stays readable; a save or publish is refused with `plan.drafting`, as spec 79 refuses a first save.
- **FR-015**: When a drafting started by Build again ends with a built draft, it MUST replace the previous draft completely, with a new version so a stale tab cannot save over it. When it ends in failure, nothing MUST change: the previous draft, its mark and its version stay, and an empty failed draft is never stored over an existing plan. The previous draft is kept until a new one arrives.
- **FR-015a**: When the editor sees a Build again end in failure, it MUST say so ("Couldn't build a new draft. Your previous one is kept.") in a message announced to assistive technology, and offer Build again once more, subject to the limit. The message is not stored: a trainer who was away just sees the previous draft.
- **FR-016**: The prompt, checks, attempts and personal-data rules of spec 79 apply unchanged. The answers and notes used are those from the quiz, never the previous draft's content.
- **FR-017**: A trainer MUST be able to start at most **10** builds per rolling 24 hours across all their clients, counted per trainer account and kept through a restart. A request refused for any other reason MUST NOT be counted.
- **FR-018**: Beyond the limit, the request MUST be refused with `429` and a stable code of its own, carrying the time from which a new build is allowed, so the screen can say when.
- **FR-019**: The screen MUST show the remaining builds only as the moment the action is next available once the limit is reached; it MUST NOT expose a count to the trainer before that.
- **FR-020**: Build again MUST require a confirmation that names what will be lost, with focus starting on the safe option; the action is unavailable while drafting runs or the limit is reached.
- **FR-021**: While drafting runs, the editor MUST show the drafting state of spec 79 (FR-025) above the previous draft, shown read-only, and check about every 5 seconds only while that state is on screen, and move on by itself. The change MUST be announced to assistive technology without moving keyboard focus.

**Access and safety**

- **FR-022**: No new public endpoint. Each new operation requires a trainer's sign-in and checks that the client is that trainer's own; another trainer's client answers `404` exactly as an unknown one, and tests prove it for each operation.
- **FR-023**: Mutating requests MUST be covered by the same CSRF protection as every other write.
- **FR-024**: No response MUST contain text from the drafting service, an exception message, SQL or a class name. The frontend translates by code and does not display backend text.
- **FR-025**: The limit is the trainer's, not the client's or the request's origin; a client cannot use or spend it.
- **FR-026**: Nothing about the trainer's requests, answers or notes is logged beyond the outcome code, as in spec 79.

**Quality**

- **FR-027**: Every new text exists in English and Polish; the Polish uses the informal "ty" and is flagged for copywriter review as in spec 79.
- **FR-028**: Screens are built from Kinetiq tokens and shadcn components, checked in light and dark at 1280 px (trainer), and the panel checked at a narrow width. The drafting state respects reduced motion.
- **FR-029**: An API change comes with regenerated types in the same pull request; demo data holds drafts of each kind and a trainer close to the limit.
- **FR-030**: Automated tests MUST NOT call the real drafting service. Before the story is done, the finished flow is run once against the real service and by hand in the browser, and the outcome reported, including a check for personal data in the responses and a foreign-client attempt on each operation.

### Public endpoints

None.

### Key Entities

- **Drafted plan**: A client's plan while it is a draft: built, failed or saved by the trainer. Shown as one entry of the Approvals list.
- **Quiz answers**: What the client answered, read by the trainer beside a draft (spec 50, spec 54); unchanged by this story.
- **Build request**: One use of Build again by a trainer, remembered for 24 hours to enforce the limit. It holds the trainer and the moment, and nothing about the client or the content.
- **Drafting** (spec 79): Now also started by the trainer, not only by a quiz submit.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A trainer finds every waiting draft on one screen and opens any of them in the editor with one action.
- **SC-002**: A trainer reaches the answer to "what did the client say?" for a draft without leaving the editor, in 0 additional clicks.
- **SC-003**: In tests, the list holds 0 clients of another trainer, 0 published plans and 0 plans still being drafted, and the oldest draft is always first.
- **SC-004**: In tests, every operation of this story answers `404` for another trainer's client and for an unknown client alike.
- **SC-005**: Build again returns in under 1 second regardless of how long drafting takes; the editor reaches the next state without a reload in 100% of cases.
- **SC-006**: In tests, a second request during drafting answers `409` with `plan.drafting` and the count does not rise; the eleventh request in 24 hours answers `429`, and a request after the window passes is accepted, proven with a fixed clock.
- **SC-007**: In tests, a failed rebuild leaves the previous draft and its version untouched; a build never replaces a published plan, and a replaced draft's stale version can never be saved over the new one.
- **SC-008**: Every new text exists in English and Polish, and the screens pass WCAG 2.2 AA checks for contrast, labels, focus and live announcements, in light and dark.
- **SC-009**: In tests, a client's quiz status is the same before, during and after a rebuild.

## Departures from the story and the boards

| Source | It says | This spec | Why |
|---|---|---|---|
| Approvals board | Tabs "Waiting for you", "Waiting for client", "Decided", plan changes and counts | One section of drafted plans; no tabs | Plan changes are a later story. Tabs with nothing behind them would invite clicks that lead nowhere. They return when the first plan change exists. |
| Approvals board | "Build again" and "Build it yourself" on a failed row | Rows only open the editor | Story #85 puts the action in the editor's panel (#88). One place for an action that costs budget and replaces work, with its confirmation and limit message, is safer than two. |
| Approvals board | Bell button and unread dot in the top bar | Not built | Notifications are story #150. |
| Editor board | "Notes from Bartek" inside the info alert | Notes live in the panel, the alert says how the draft was made | Notes can be long; an alert is for a short message. |
| Editor board | "Build it yourself" next to "Build again" on a failed draft | Not a separate action; the empty editor already offers "Add session" | The trainer is already in the editor; the action would only dismiss the panel. |
| Story #88 | Rebuild moves on "to a built or a failed draft" | A failed rebuild keeps the previous draft and shows a message (FR-015a) | A failure would otherwise destroy good work the trainer had reviewed or edited. |
| Editor board | Confirmation absent | Confirmation required (FR-020) | Story #88 requires it; the action replaces the trainer's work. |

## Texts

Main English and Polish texts. Polish uses informal "ty" and should be confirmed by a copywriter.

| Where | English | Polish |
|---|---|---|
| Approvals, title | Approvals | Do akceptacji |
| Approvals, intro | New plans wait here for your review. | Tutaj czekają nowe plany do Twojego przeglądu. |
| Section | Drafted plans | Szkice planów |
| Empty | Nothing to approve. Drafted plans show up here. | Nic do akceptacji. Szkice planów pojawią się tutaj. |
| Empty action | View clients | Zobacz klientów |
| Row age | {n} h ago | {n} godz. temu |
| Panel title | {name}'s answers | Odpowiedzi: {name} |
| Panel, no notes | {name} wrote no notes. | {name} nie napisał(a) uwag. |
| Panel, failed | Couldn't build this plan automatically. Try again or build it yourself. | Nie udało się zbudować tego planu automatycznie. Spróbuj ponownie albo zbuduj go sam. |
| Action | Build again | Zbuduj ponownie |
| Confirm, title | Build {name}'s plan again? | Zbudować plan ({name}) ponownie? |
| Confirm, text | The current draft, including your edits, will be replaced by a new automatic one if it can be built. | Obecny szkic, razem z Twoimi zmianami, zostanie zastąpiony nowym, automatycznym, jeśli uda się go zbudować. |
| Confirm, accept | Replace draft | Zastąp szkic |
| Limit reached | You've used today's automatic builds. You can build again from {time}. | Wykorzystano dzisiejszy limit automatycznych budowań. Możesz zbudować ponownie od {time}. |
| Rebuild failed | Couldn't build a new draft. Your previous one is kept. | Nie udało się zbudować nowego szkicu. Poprzedni został zachowany. |
| Drafting (editor) | Building a new draft from {name}'s answers. This can take a few minutes. | Budujemy nowy szkic z odpowiedzi ({name}). To może potrwać kilka minut. |

## Assumptions

- Story title/scope: this covers issue #85 and tasks #86–#88. The task "Folded in from #243" is the rate limit; no separate work.
- "A drafted plan" means every plan that is still a draft, including one the trainer already saved, because it still waits to be published. A client being drafted for the first time has no plan yet, so does not appear.
- Age is measured from when the client's plan row was first created. A rebuild does not reset it: the client has waited since the first draft, so the plan stays where its waiting puts it.
- The fixed number is 10 builds per rolling 24 hours, counted per trainer and persisted so a restart does not reset it. It protects the paid model's budget; if the value proves wrong it is a change of one constant, not of design.
- A failed rebuild keeps the previous draft, a departure from the story's "moves on to a built or a failed draft" agreed with the story's assignee: a failure must not destroy a good draft. An empty failed draft still appears only for a first drafting (spec 79).
- The quiz answers are health-related data already visible to the trainer elsewhere (spec 54); showing them in the editor adds no new reader.
- The trainer's units setting already governs loads in the editor; the panel's weight and height follow the same setting.
- Plan changes, notifications and the "Waiting for client" and "Decided" views belong to later stories; the Approvals page grows into them without rework of this list.
- Spec 79's drafting job, sweep and storage are reused as they are; this story only starts a drafting from a different place and replaces instead of creating.
