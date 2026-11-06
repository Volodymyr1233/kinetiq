# Feature Specification: First plan is drafted from the quiz

**Feature Branch**: `feat/79-plan-drafting`

**Created**: 2026-11-05

**Status**: Draft

**Input**: Story #79 "First plan is drafted from the quiz" and its tasks #80 (LLM client), #81 (generator port and adapters), #82 (prompt), #83 (validation, retry and failure state) and #84 (background drafting). Screens: Kinetiq Screens, boards "Quiz · building the plan" and "Trainer · plan editor". Design: the Kinetiq design system. The story, the tasks and the boards are starting points, not the source of truth; every place this spec departs from them is listed under "Departures from the story and the boards". Evidence for the provider decisions is in `research.md`.

## Clarifications

### Session 2026-11-05

- Q: Do the client's notes go to the drafting service? → A: Yes, cleaned first: the client's and trainer's names, email addresses and phone numbers are replaced, and a notice on the quiz's review step tells the client.
- Q: How do clients agree to their answers going to an external AI service? → A: The review-step notice for everyone, and the join consent sentence names the service for clients who join from now on. There is no opt-out.
- Q: What does the plan badge on the client list and client page say after drafting ends? → A: An untouched generated draft shows "Plan to review", a failed draft "Draft failed", and a draft after the trainer's first save "Draft".
- Q: Is drafting limited to providers that don't store prompts or train on them? → A: No, it uses the full free pool for better plans.
- Q: Cleaned notes can still hold a name the cleaner misses, while the constitution forbade names in prompts. Which gives way? → A: The constitution is amended (1.2.0): free text may go once known identifiers are removed and the person was told. Cleaning also catches Polish inflected forms of the stored names.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - A submitted quiz becomes a draft (Priority: P1)

A client submits the quiz and goes straight to the waiting screen. In the background a first plan is drafted from their answers and the trainer's exercise library, and stored as a draft only the trainer can see.

**Why this priority**: This is the point of the story: no new client waits for a trainer to build everything by hand.

**Independent Test**: Submit a quiz with the drafting service replaced by a stand-in that answers slowly: the submit returns at once, and once the stand-in answers, the client has a draft holding the expected sessions, while the client's own status still says waiting.

**Acceptance Scenarios**:

1. **Given** a client who has not submitted the quiz, **When** they submit it, **Then** the answer comes without waiting for the drafting service, and the client's status says a plan is being drafted.
2. **Given** drafting has started, **When** a valid draft comes back, **Then** it is stored as a draft for that client, with the goal from the quiz, 12 weeks, as many sessions as the client's days per week, each on its own day as FR-013 describes.
3. **Given** a stored draft, **When** the client asks for their status or their plan, **Then** they see that they are waiting for their trainer and nothing of the draft.
4. **Given** a stored draft, **When** the client's trainer opens the Plan tab, **Then** the editor opens with it, marked as built automatically, and every usual edit, save and publish works as in spec 68.
5. **Given** a client who submitted the quiz before this story, **When** the story ships, **Then** nothing is drafted for them and their trainer still sees "Build plan".

---

### User Story 2 - A bad or missing answer never reaches anyone (Priority: P1)

The drafting service is outside our control and may answer with nonsense, answer late or not at all. Every answer is checked; a rejected answer gets one more attempt, and after that the trainer gets an empty draft saying so.

**Why this priority**: Training load affects injury risk, and the free service used here fails often enough that the failure path is a normal path.

**Independent Test**: With a stand-in that always gives an answer naming an exercise outside the library: two attempts are made, the second names the reasons of the first, and an empty draft marked as failed is stored.

**Acceptance Scenarios**:

1. **Given** an answer that breaks any check of FR-015 to FR-017, **When** it arrives, **Then** nothing of it is stored and a second attempt is made, carrying the reasons of the first rejection.
2. **Given** a first attempt that fails for any other reason (time limit, refusal, too many requests, an unreadable answer), **When** it fails, **Then** a second attempt is made after a short pause.
3. **Given** two failed attempts, **When** the second one fails, **Then** an empty draft marked as failed is stored, and the trainer sees that drafting failed and can build the plan in the editor.
4. **Given** a first failed attempt and a valid second one, **When** the second arrives, **Then** the draft is stored and nothing shows that a retry happened.
5. **Given** no connection to the drafting service is configured, **When** a quiz is submitted, **Then** no attempt is made, the empty failed draft is stored at once, and the application itself runs normally.
6. **Given** the application stops or restarts during drafting, **When** it is running again, **Then** that client's drafting ends with the empty failed draft within the limit of FR-005, so nobody waits forever.

---

### User Story 3 - The client sees progress, then waits for their trainer (Priority: P2)

While the draft is built the client sees that their plan is being built, and the screen moves on by itself to "Your trainer is reviewing your plan".

**Why this priority**: The client has just answered a long quiz; a moving, honest state tells them it worked. The plan still works without it, so it follows the two stories above.

**Independent Test**: Submit a quiz with a slow stand-in: the building state shows, then changes by itself to the reviewing state, in English and Polish, at 375 px.

**Acceptance Scenarios**:

1. **Given** a client whose plan is being drafted, **When** they open the waiting screen, **Then** they see the building state with a spinner and a skeleton, a line saying it can take a few minutes and that they may close the app, and the action to message their trainer.
2. **Given** the building state, **When** drafting ends, whether in a draft or in failure, **Then** the screen changes by itself to the reviewing state, within about 5 seconds, and the change is announced to assistive technology without moving keyboard focus or closing what the client has open.
3. **Given** drafting ended in failure, **When** the client looks at the screen, **Then** it looks the same as after a valid draft: a failed draft is the trainer's business.
4. **Given** the client closes the app during drafting, **When** they return, **Then** they land on the state that is true by then.
5. **Given** a status check that fails during polling, **When** it fails, **Then** the shown state stays and checking continues.

---

### User Story 4 - The trainer sees what the system did (Priority: P2)

The trainer can tell a plan is still being drafted, that a draft was built automatically and needs checking, or that drafting failed.

**Why this priority**: A generated plan must never look as if a person wrote it; the trainer's review is what makes it safe.

**Independent Test**: Open the Plan tab during drafting, after a valid draft and after a failed one: three different messages show, each in English and Polish, and the marks go after the first save.

**Acceptance Scenarios**:

1. **Given** a client's plan is being drafted, **When** the trainer opens the Plan tab, **Then** they see a line saying so with a skeleton and no "Build plan"; the tab moves on to the editor by itself when drafting ends.
2. **Given** a client's plan is being drafted, **When** the trainer looks at the client list or the client's page, **Then** the plan state says it is being drafted; once drafting ends it says "Plan to review" for a generated draft and "Draft failed" for a failed one, and "Draft" after the trainer's first save.
3. **Given** an automatically built draft, **When** the editor shows it, **Then** a note says it was built from the quiz answers, that the exercises and loads should be checked, and that the client sees it only after publishing.
4. **Given** a failed draft, **When** the editor shows it, **Then** a note says it could not be built automatically and that the trainer can build it here, and the empty sessions state offers "Add session".
5. **Given** either note, **When** the trainer saves the plan successfully, **Then** the note is gone for good: the plan is now the trainer's.
6. **Given** a failed draft, **When** the trainer tries to publish it empty, **Then** it is refused as any incomplete draft (spec 68, FR-016).
7. **Given** a stale tab or a hand-made request, **When** a first save arrives during drafting, **Then** it is refused with a stable code and a message saying the plan is still being drafted; nothing is stored.

---

### User Story 5 - Personal data stays out of the prompt (Priority: P1)

What goes to the drafting service holds what is needed to plan training and nothing that says who the person is.

**Why this priority**: The constitution requires it, and the free service may log what it receives.

**Independent Test**: Build a prompt for a client whose name, email and trainer's name are distinctive strings, also written into the notes, with a trainer's own exercise holding the name too: none of those strings or any id appears in the prompt.

**Acceptance Scenarios**:

1. **Given** any client, **When** the prompt is built, **Then** it contains no name, email, phone number or id of a person held as data, and no exercise id.
2. **Given** notes holding the client's name, also in an inflected form, the trainer's name, an email address or a phone number, **When** the prompt is built, **Then** those are replaced by the mark `[removed]` before anything leaves the system, and the rest of the notes goes to the service as written.
3. **Given** a trainer's own exercise, **When** the catalogue is built, **Then** it holds only its name, category, equipment, measure and usual targets, the name and equipment cleaned the same way; its cues stay out.
4. **Given** any attempt, **When** it is logged, **Then** the log holds the outcome, the model used, the duration and a rejection code, and never the prompt, the answer, the notes or who it was for.
5. **Given** the review step of the quiz, **When** the client is about to submit, **Then** a line tells them their answers go to an external AI service to draft the plan and asks them not to write names or contact details in the notes.

---

### Edge Cases

Drafting and storage:

- The trainer already has a plan for this client when the draft arrives (a demo row, a save racing the end of drafting): the draft is dropped, never stored over the trainer's work, and that is not a failure. Storage allows one plan per client, so two arrivals at once keep one.
- The client's trainer was changed or the account removed during drafting: the result is dropped without any error shown to anyone, and nothing is stored. The client then shows as a client with answers and no plan, and the new trainer can build one.
- A second submit is already refused (spec 54); it starts nothing.
- The trainer deletes an own exercise between the prompt and the answer: the answer fails the library check and goes to the second attempt.
- Many clients submit at once: each drafts independently; the service's limits show up as failed attempts, and the failure path holds. The daily free allowance running out ends in empty failed drafts, not in errors.

Answers from the service:

- Empty answer, an answer cut off at its length limit, text around the JSON, JSON in a code fence, or valid JSON of another shape: a fence is removed; everything else counts as a failed attempt.
- Schema-valid nonsense, such as a session list holding one fragment: fails the checks.
- A loaded exercise measured in seconds, such as a farmer's carry: allowed. Load depends on the exercise, not on its measure.
- The same exercise twice in one session: refused for generated drafts, though a trainer may do it by hand.
- Session names that collide, that hold a link or an email address, or that are only spaces: refused.
- Notes telling the model to break its rules, reveal its text or output something else: treated as data. The answer's shape allows nothing but a plan, and it passes the same checks regardless.

Answers from the client, which the prompt must handle sensibly (checked against the real service before the story is done):

- 2 and 6 sessions a week; each goal; each level.
- Level and experience that disagree (athlete, under 3 months of training): the more cautious of the two wins.
- Age 16–17 and 60 or above: controlled movements and RPE at most 8.
- Body weight at both ends of the allowed range.
- Likes the catalogue covers poorly; only cardio, or only yoga and pilates, with a goal of building muscle: the goal comes first, and the closest exercises are used.
- Notes in Polish or English, empty notes, injuries, medical conditions, equipment limits and limits on a specific day. The service picks the days, so "busy on Mondays" moves a session off Monday and "only dumbbells on Fridays" shapes the Friday session.
- A library of 240 exercises (40 starter plus 200 own): the prompt stays bounded and exercises keep their short keys.

## Requirements *(mandatory)*

### Functional Requirements

**Starting and ending** (task #84)

- **FR-001**: Submitting the quiz MUST record, as part of the submit itself, that drafting is due for this client, and MUST answer without waiting for the drafting service.
- **FR-002**: Drafting MUST start only after the submit is safely stored, so a refused or failed submit starts nothing.
- **FR-003**: The client's quiz status MUST gain a state for a plan being drafted, between "not submitted" and "waiting for plan". It says nothing about the draft's content. Drafting that ended, either way, gives "waiting for plan".
- **FR-004**: A draft MUST arise only from a first submit in this story. Asking for a new automatic draft is story #85.
- **FR-005**: Drafting MUST end within 6 minutes of the submit in every case, including a restart of the application: drafting found older than that ends with an empty failed draft, checked at least once a minute. A result arriving after that is dropped.
- **FR-006**: The application MUST start and work without a connection to the drafting service. Without one, a submit stores the empty failed draft at once, and the application logs a warning at start.
- **FR-007**: The service's model MUST be changeable through configuration alone, and its secret MUST come from an environment variable, listed without a value in `.env.example`.

**The prompt** (task #82)

- **FR-008**: The prompt MUST contain the goal, experience, level, age, height, weight, likes, days per week and the usual days of FR-013, the language for session names, the catalogue, and the rules of FR-015 to FR-017 in words. It MUST NOT contain a name, email, phone number or id of a person held as data, or an exercise id.
- **FR-008a**: The client's notes MUST go into the prompt cleaned: each word of the client's and the trainer's stored names (whole words, ignoring case and Polish diacritics, including Polish inflected forms such as "Anny" for "Anna" and "Marka" for "Marek"), and anything shaped like an email address or a phone number, is replaced by `[removed]`. A trainer's own exercise names and equipment are cleaned the same way. Empty notes are sent as `none`.
- **FR-009**: Exercises MUST be listed with short keys that mean something only within one prompt (E1, E2, …), with name, category, equipment, measure and usual sets, amount and rest. The answer's keys are mapped back; an unknown key is a rejection.
- **FR-010**: The prompt MUST give guidance for the choices the checks cannot make: the split by sessions per week, exercise order within a session, repetition ranges and rest by goal, volume and RPE by level, caution by age, how likes weigh against the goal, session length of about 45 to 75 minutes, conservative starting loads for someone with no logged history, and how to respect injuries and equipment limits. Where level and experience disagree, the more cautious wins.
- **FR-011**: Client-written text in the prompt MUST be fenced and declared to be data, with instructions to ignore anything in it that asks for something else.
- **FR-012**: The answer MUST be requested in a fixed machine-readable shape holding only sessions (name, day, entries) and entries (exercise key, sets, amount, load or none, RPE, rest). The shape has no free-text field other than the session name, so the service cannot pass advice or a message to anyone.

**What the system decides** (task #81)

- **FR-013**: The service MUST NOT decide what code can. The goal is the quiz's goal, the length is 12 weeks, and the days per week fix the number of sessions. The service picks each session's day, because only it reads the notes: it uses these usual days unless the notes rule a day out or name the days that suit:

  | Sessions | Usual days |
  |---|---|
  | 2 | Mon, Thu |
  | 3 | Mon, Wed, Fri |
  | 4 | Mon, Tue, Thu, Fri |
  | 5 | Mon, Tue, Wed, Fri, Sat |
  | 6 | Mon, Tue, Wed, Thu, Fri, Sat |

- **FR-014**: Session names MUST be asked for in the client's language at the time of the submit, English or Polish.

**Checks** (task #83)

- **FR-015**: A draft MUST pass every plan rule of spec 68 (FR-005) and the rules for a complete plan (FR-006), through the same domain rules a trainer's save goes through.
- **FR-016**: Every exercise MUST be in this trainer's library, known to the prompt that asked for it.
- **FR-017**: A generated draft MUST also pass these narrower limits, because the plan rules allow values no first plan should have. Each rejection has a stable code that can be named in the second attempt:
  - exactly as many sessions as the client's days per week, each on a different day;
  - 4–10 entries per session, with no exercise twice in one session;
  - session names unique within the plan, at most 40 characters after trimming, holding no link or email address;
  - 1–8 sets;
  - 1–50 repetitions, or 5–3600 seconds, as the exercise measures;
  - RPE of 5–9, or 5–8 for a client aged 16–17 or 60 and over;
  - rest of 0–300 seconds;
  - a load that is empty, or above 0 and at most twice the client's body weight.
- **FR-018**: Nothing of a rejected answer MUST be stored, shown or logged.

**Attempts** (task #83)

- **FR-019**: There MUST be at most 2 attempts per drafting, whatever the failure, each with a time limit of 150 seconds and a pause of a few seconds between them. No other retry layer may add requests, since the free allowance counts each one.
- **FR-020**: After a rejection the second attempt MUST name the reasons, by rule and never by quoting the client's text.
- **FR-021**: After 2 failed attempts the system MUST store an empty draft with the quiz's goal and 12 weeks, marked as failed.
- **FR-022**: If the client has a plan by the time a draft or failed draft would be stored, nothing MUST be stored; that plan stays as it is.

**Marks on a plan**

- **FR-023**: A draft MUST carry whether it was built automatically or failed to be. The first successful save by the trainer removes the mark. A plan built by hand, and any published plan, has none. Publishing a draft that still carries the "built" mark is allowed and removes it.

**Trainer screens**

- **FR-024**: The plan state on the client list and the client's page MUST add "Plan being drafted" and "Draft failed", translated, and follow the plan's mark: a draft marked as built shows "Plan to review", a draft marked as failed shows "Draft failed", and an unmarked draft shows "Draft". A client who submitted the quiz before this story and has no plan keeps "Plan to review".
- **FR-025**: The Plan tab MUST show the drafting state of User Story 4, and move on by itself when drafting ends, checking about every 5 seconds only while that state is on screen. A first save during drafting is refused with a stable code. Only the client's trainer can see any of it: a foreign or unknown client answers 404 as in spec 68.
- **FR-026**: The editor MUST show the note of the plan's mark above the sessions, using the design system's alert, with an icon and text, not colour alone. The note MUST NOT claim anything the story does not deliver.

**Client screens**

- **FR-027**: The waiting screen MUST show the building state of User Story 3 while the status says drafting, checking about every 5 seconds only while that state is on screen, and stopping on any other. The action to message the trainer stays available in both states.
- **FR-028**: The quiz's review step MUST show the notice of User Story 5, scenario 5. The join form's consent sentence MUST also name the external AI service and what goes to it; accounts that already agreed are not asked again.

**Access and safety**

- **FR-029**: No new public endpoint. The client's status answers only for the signed-in client; a client still cannot read a plan that is not published, and a built or failed draft answers like no plan (spec 75, FR-003). Tests prove each.
- **FR-030**: No error response MUST contain text from the drafting service, and session names from the service are shown as plain text only.

**Quality**

- **FR-031**: Every text MUST exist in English and Polish.
- **FR-032**: The screens MUST be built from Kinetiq tokens and shadcn components, in light and dark, checked at 375 px and 1280 px. The building state respects reduced motion.
- **FR-033**: Demo data MUST include a client with an automatically built draft and a client with a failed draft.
- **FR-034**: Automated tests MUST NOT call the real service. Before the story is done, the edge cases under "Answers from the client" are run once against the real service and the outcome reported.

### Public endpoints

None. Drafting has no endpoint of its own; what it changes is read through the signed-in client's status and the trainer's existing plan operations.

### Key Entities

- **Drafting**: The record that a client's first plan is being drafted, from the submit until a draft or a failed draft is stored. Its absence means nothing is being drafted, so clients from before this story are untouched.
- **Plan** (spec 68): gains its mark: built automatically, failed, or none.
- **Generated draft**: What an accepted answer becomes: an ordinary draft plan, with nothing about it stored beyond the mark.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A client's submit is answered in under 1 second regardless of how long drafting takes.
- **SC-002**: 100% of drafting attempts end in a draft or a failed draft within 6 minutes, including after a restart, proven with a fixed clock.
- **SC-003**: In tests, every check of FR-015 to FR-017 rejects a breaking answer, and 0 rejected answers leave anything stored.
- **SC-004**: In tests, 0 distinctive names, emails, phone numbers or ids of a person reach the prompt, including through notes, inflected forms and a trainer's own exercises.
- **SC-005**: In 10 real runs (the 9 cases of the evaluation matrix, case 2 repeated), at least 8 produce a valid draft within 2 attempts, and each draft is reported with its split and exercises for a person to judge.
- **SC-006**: A client on the building screen and a trainer on the Plan tab's drafting state reach the correct next state without reloading the page, in 100% of cases.
- **SC-007**: A trainer's plan is never replaced by a draft, proven by a test with a plan arriving during drafting.
- **SC-008**: Every new text exists in English and Polish, and the screens pass WCAG 2.2 AA checks for contrast, labels, focus and live announcements, in light and dark.

## Departures from the story and the boards

| Source | It says | This spec | Why |
|---|---|---|---|
| Story, #81 | A fixed-template generator when no key is set | No template generator. Without a key the empty failed draft is stored at once; tests use a hand-written stand-in of the port | A template in production would pass a canned plan off as drafted from the client's answers if the key went missing. Local development has a key, and everything still works without one because the failure path exists anyway. |
| Story, #83 | Empty draft message "Try again or build it yourself" | The note says only that the trainer can build it here until #88 adds "Build again" | A note must not offer an action that does not exist yet. #88 owns the panel and the action. |
| #84 comment, spec 68 | A trainer may start a plan during drafting; the draft is then dropped | No "Build plan" during drafting; drafting ends within 6 minutes | A trainer's first save would collide with the arriving draft: either their unsaved work is refused as changed elsewhere, or the draft is lost. Waiting a few minutes is cheaper than either. The drop rule stays as a safeguard. |
| Building board | "Building your plan from 7 answers… starting weights for 3 days a week" | Says it is building, for how long, and that the client may leave | The count of answers says nothing, and the wait can reach minutes. |
| Building board | Draft summary card on the client's waiting screen | Not shown | The story says a draft is seen only by the trainer; length and sessions could still change. |
| Editor board | Client notes and quiz answers next to a draft; "Build again" | Left to #88 | They are that task's scope. |
| Editor board | "gets a notice", "16 weeks" | Not said; 12 weeks | Notifications are story #150; 12 weeks is the editor's starting length. |
| Plan rules | RPE up to 10, 20 sets, 3600 repetitions | Narrower limits for generated drafts only (FR-017) | A valid plan is not necessarily a sensible first plan; a trainer may still go beyond them by hand. |

## Texts

The main English and Polish texts. Polish uses informal "ty" and should be confirmed by a copywriter, as the design system says (8.5).

| Where | English | Polish |
|---|---|---|
| Client, building | Building your plan | Budujemy Twój plan |
| Client, building text | Sessions, exercises and starting weights from your answers. This can take a few minutes, and you can close the app. | Sesje, ćwiczenia i obciążenia startowe na podstawie Twoich odpowiedzi. To może potrwać kilka minut, a aplikację możesz zamknąć. |
| Client, review notice | To draft your plan we send your answers to an external AI service. Please don't write names or contact details in your notes. | Aby przygotować szkic planu, wysyłamy Twoje odpowiedzi do zewnętrznej usługi AI. Nie wpisuj w uwagach imion ani danych kontaktowych. |
| Trainer, drafting | Drafting a plan from {name}'s quiz answers. This can take a few minutes. | {name}: szkic planu powstaje z odpowiedzi w quizie. To może potrwać kilka minut. |
| Plan states | Plan being drafted · Draft failed | Szkic w przygotowaniu · Szkic nieudany |
| Join consent | I agree that Kinetiq processes my health data (sleep, soreness, stress, body weight and training logs) so {trainerFirst} can plan my training, and sends my quiz answers to an external AI service to draft my first plan. | Zgadzam się, aby Kinetiq przetwarzał moje dane o zdrowiu (sen, bolesność mięśni, stres, masa ciała i zapisy treningów), dzięki czemu {trainerFirst} zaplanuje mój trening, oraz wysyłał moje odpowiedzi z quizu do zewnętrznej usługi AI, aby przygotować szkic pierwszego planu. |
| Note, built | Built automatically from the quiz answers. Check the exercises and loads. {name} sees the plan only after you publish it. | Zbudowano automatycznie z odpowiedzi w quizie. Sprawdź ćwiczenia i obciążenia. {name}: plan będzie widoczny dopiero po publikacji. |
| Note, failed | Couldn't build this plan automatically. You can build it here. | Nie udało się zbudować tego planu automatycznie. Możesz zbudować go tutaj. |
| Refused save | This plan is still being drafted. Try again in a few minutes. | Ten plan jest jeszcze w przygotowaniu. Spróbuj ponownie za kilka minut. |

## Assumptions

- Story title: the request named #79 "Trainer builds, edits and publishes a plan"; that title belongs to the closed #68. Issue #79 and tasks #80–#84 are what this spec covers.
- Every generated draft is reviewed by a trainer before any client sees it (spec 68 publishing); that review is what makes a free, varying model acceptable, and it is why quality is measured by SC-005 and not guaranteed per answer.
- Drafting is recorded next to the submit, so a restart cannot lose it. A restart does not resume drafting; it ends in the failed draft, because that cannot use up the free allowance on a restart loop.
- The client's language at the submit is the right one for session names, because the client trains with them; the trainer can rename them.
- Starting loads come from the service because the board promises them and an empty load means "no added weight" (spec 68), which would misdescribe a barbell exercise. They are estimates; the "built" note asks the trainer to check them.
- A trainer's own exercise names are in the prompt because the story asks for the library, cleaned as in User Story 5.
- Cleaning notes cannot catch everything: the name of a third person, a nickname or an unusual inflected form can remain. Constitution III (1.2.0) allows this for free text, because the notice on the review step tells clients and asks them not to write names. FR-008a is what a test can prove.
- The status check stays as it is once the client reaches the reviewing state: published plans are picked up as they are today, and the client then lands on the plan-ready screen and the Plan tab of spec 75, for a generated plan as for any other. Notifying the client is story #150.
- The Approvals list and the panel with the quiz answers are story #85; until then the trainer finds a draft through the client's Plan tab and its plan state.
- The notice and the consent sentence are plain-language disclosure, not a legal review. Clients who joined before this story see only the notice, and submitting the quiz is their choice to go on.
