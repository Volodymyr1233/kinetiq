# Feature Specification: Client checks in and sees readiness

**Feature Branch**: `feat/111-readiness-score`

**Created**: 2026-10-30

**Status**: Draft

**Input**: Story #109 "Client checks in and sees readiness" and its tasks #110 (check-in model and API), #111 (readiness score and band), #112 (check-in on the Today screen) and #113 (readiness card). Screens: Kinetiq Screens, board Client → "Today before check-in" and "Today ready". Design: the Kinetiq design system. Builds on spec `specs/18-sign-in-out` (the signed-in client, the time zone). Plan changes and the lightening of today's targets on a recovery day are other stories (#117, #125) and are not part of this one.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Readiness from three answers (Priority: P1)

Three answers about last night's sleep, muscle soreness and stress become one number from 0 to 100 and one of three bands, the same way every time.

**Why this priority**: Every other part of the story, and the later adjustment of a recovery day, depends on this number being right. It has no screen of its own and can be proven without one.

**Independent Test**: Give the calculation sleep 2, soreness 4, stress 3 and read 33; give it sleep 4, soreness 2, stress 1 and read 83 (the issue's examples). Try all 125 combinations of answers: every score is from 0 to 100, and a better answer never lowers the score.

**Acceptance Scenarios**:

1. **Given** sleep, soreness and stress, each answered from 1 to 5, **When** readiness is calculated, **Then** the score is the mean of sleep (s − 1) / 4, soreness (5 − x) / 4 and stress (5 − x) / 4, times 100, rounded to a whole number.
2. **Given** sleep 2, soreness 4 and stress 3, **When** readiness is calculated, **Then** it is 33; **Given** sleep 4, soreness 2 and stress 1, **Then** it is 83.
3. **Given** the best answers (sleep 5, soreness 1, stress 1), **When** readiness is calculated, **Then** it is 100; **Given** the worst (sleep 1, soreness 5, stress 5), **Then** it is 0.
4. **Given** a score, **When** its band is asked for, **Then** 70 and above is Ready, 45 to 69 is Take it easier, and below 45 is Recovery day.
5. **Given** an answer outside 1 to 5, **When** readiness is calculated, **Then** it is refused and no score exists.

---

### User Story 2 - Client checks in before training (Priority: P1)

Before the day's first session, the Today screen asks how the client slept and how they feel. Three taps are the whole check-in.

**Why this priority**: The acceptance criterion of the story and of tasks #110 and #112.

**Independent Test**: Sign in as a client with a session today and no check-in yet; open Today, answer sleep, soreness and stress: the check-in is stored on the third answer with no button, and the screen shows the readiness card.

**Acceptance Scenarios**:

1. **Given** a client who has not checked in today and has not logged a set today, **When** they open Today, **Then** it asks for sleep, soreness and stress, each as a choice from 1 to 5, as on the board "Today before check-in".
2. **Given** the check-in, **When** the client has answered all three, **Then** it is stored at once, with no submit button, and Today shows their readiness.
3. **Given** a client who has answered one or two questions, **When** they leave Today, **Then** nothing is stored.
4. **Given** the client chooses to skip, **When** they skip, **Then** nothing is stored, no readiness shows and nothing else in Kinetiq changes; they may still check in later the same day, until their first set is logged.
5. **Given** a stored check-in, **When** the client changes an answer before their first set of the day is logged, **Then** the check-in and the readiness are replaced by the new ones; there is still only one check-in for the day.
6. **Given** the client has logged their first set of the day, **When** they open Today, **Then** the check-in can no longer be answered or changed, and the stored one (if any) is shown.

---

### User Story 3 - Client sees their readiness (Priority: P1)

After the check-in, the client sees their readiness and what it means for today.

**Why this priority**: Acceptance criterion of the story and of task #113.

**Independent Test**: Check in with the answers of the three bands in turn; each shows its score, its band name and, for Take it easier, one line of advice, and none of them in red.

**Acceptance Scenarios**:

1. **Given** a check-in, **When** Today shows it, **Then** the card shows the score from 0 to 100, the band name (Ready, Take it easier or Recovery day) and the three answers, as on the board "Today ready".
2. **Given** a readiness in Take it easier, **When** the card shows it, **Then** it shows one line of advice in plain words.
3. **Given** any band, **When** the card shows it, **Then** it is never red and the band is told by its name and an icon, not by colour alone.
4. **Given** a readiness in Recovery day, **When** the card shows it, **Then** it says so and changes nothing else; lightening today's targets is the story "Today's targets are lightened on a recovery day" (#117).

---

### Edge Cases

- "Today" is the day in the client's own time zone (spec 18); a check-in made at 23:50 belongs to that day and not to the next.
- A client without a session today may check in as well; the check-in is then stored for the day with no session to affect.
- A client who has logged a set today without a check-in cannot check in for that day.
- Two quick taps on the same answer, or the same check-in sent twice (for instance from a second tab), leave one check-in for the day with the last answers.
- A client who answers the three questions in any order gets the same result.
- A request with an answer outside 1 to 5, or a missing answer, is refused with a code for the field and stores nothing.
- A client sees and changes only their own check-in; another person's answers answer "not found".
- The answers are about health: they are stored with the client, shown only to the client and, later, to their trainer (story #122), and never sent to an LLM with a name, an email or an id.

## Requirements *(mandatory)*

### Functional Requirements

**Readiness** (task #111)

- **FR-001**: Readiness MUST be a whole number from 0 to 100 calculated from sleep, soreness and stress, each from 1 to 5, as the mean of sleep (s − 1) / 4, soreness (5 − x) / 4 and stress (5 − x) / 4, times 100, rounded to the nearest whole number.
- **FR-002**: The band MUST be Ready from 70, Take it easier from 45 to 69, and Recovery day below 45.
- **FR-003**: A calculation with an answer outside 1 to 5 MUST be refused.
- **FR-004**: The same answers MUST always give the same readiness and band.

**Check-in** (task #110)

- **FR-005**: A client MUST be able to store one check-in for a day: sleep, soreness and stress, each from 1 to 5. The check-in is stored once all three are answered, with no submit step.
- **FR-006**: A client MUST have at most one check-in per day, the day being the one in their time zone. A second check-in for the day replaces the first.
- **FR-007**: A check-in MUST be changeable until the first set of that day is logged, and not after.
- **FR-008**: The server MUST refuse an answer outside 1 to 5 or a missing one, with a code per field. Nothing changes on a refusal.
- **FR-009**: A client MUST be able to read their own check-in of today, with its readiness and band; nobody else's.
- **FR-010**: Skipping MUST store nothing and change nothing else.

**Today screen** (tasks #112 and #113)

- **FR-011**: Today MUST show the check-in questions to a client who has no check-in for today and has not logged a set today, and the readiness card to a client who has one, as on the boards "Today before check-in" and "Today ready".
- **FR-012**: The card MUST show the score, the band name, the three answers and, for Take it easier, one line of advice. No band is shown in red; the band is told by its name and an icon too.
- **FR-013**: Every question MUST be reachable and operable by keyboard, with its choices labelled and the chosen one shown by more than colour; touch targets are 48 px (constitution VII).
- **FR-014**: Every text MUST exist in English and Polish.

### Key Entities

- **Check-in**: A client's daily answers about sleep, soreness and stress, given before the first session of the day. Belongs to one client and one day; at most one per client per day.
- **Readiness**: The 0 to 100 score derived from a check-in. It is calculated from the answers and not stored separately.
- **Readiness band**: Ready, Take it easier or Recovery day, derived from the score.

### Public endpoints

None. Storing and reading a check-in need a signed-in client (spec 18). A trainer or an unauthenticated caller cannot use them.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: For all 125 combinations of answers, the score is from 0 to 100, and improving one answer by one step never lowers the score.
- **SC-002**: The issue's examples hold: sleep 2, soreness 4, stress 3 gives 33; sleep 4, soreness 2, stress 1 gives 83.
- **SC-003**: A client completes a check-in in three taps, with no further action.
- **SC-004**: A client has never more than one check-in for a day, however many times they answer.
- **SC-005**: No band is shown in red, and each band is told apart without colour.

## Assumptions

- The score and the band are only calculated, never stored; they follow from the three answers, so a later change of a threshold would not leave old values behind.
- The one line of advice for Take it easier is written with the card (#113); this story fixes only that it is one line in plain words and not alarming. No medical advice is given.
- "Before the day's first session" means: no set logged yet today, whether or not a session is planned. Logging itself is the story "Client logs sets" (#92); until it exists, a check-in can always be changed.
- What a Recovery day changes in the session (lightening targets) belongs to story #117; this story only shows the band.
- Trainers see check-ins in the client overview (story #122); this story does not show them.
- A skipped check-in leaves no record; the client may still answer later the same day.
- The bands and thresholds are the working numbers recorded in the team's decisions and in the story's criteria; they are product settings that may change later without a new design.
