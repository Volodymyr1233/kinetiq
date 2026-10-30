# Feature Specification: Trainer sees their clients

**Feature Branch**: `feat/36-client-list-api`

**Created**: 2026-10-30

**Status**: Draft

**Input**: Story #35 "Trainer sees their clients" and its tasks #36 (client list API), #37 (all-clients table with search) and #38 (client page frame with tabs). Screens: Kinetiq Screens, board Trainer → "All clients" and "Client overview" (frame only). Builds on spec `30-client-join` (each client has one trainer) and spec `54-quiz-review-submit` (the quiz answers, the goal). Later stories fill the columns that are empty here: plans (#68), logs (#92).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Trainer sees only their own clients (Priority: P1)

A signed-in trainer asks for their clients and gets each one with the name, the goal the client gave in the quiz, and where the client stands with the plan, in alphabetical order by name.

**Why this priority**: The list is the trainer's way into every client; the table (#37) and the client page (#38) are built on it, and it carries the ownership rule of the whole story.

**Independent Test**: Sign in as the demo trainer; ask for the clients: the demo clients of this trainer appear by name, the client who answered the quiz shows a goal and "plan to review", the one who did not shows no goal and "no quiz yet", and no client of the second demo trainer appears.

**Acceptance Scenarios**:

1. **Given** a trainer with clients, **When** they ask for their clients, **Then** they get each client with name, goal and plan state, ordered by name without regard to letter case.
2. **Given** a client who has submitted the quiz, **When** the list is read, **Then** it shows the goal from the quiz and the plan state "plan to review" (the plan is drafted from the quiz, and the trainer reviews it).
3. **Given** a client who has not submitted the quiz, **When** the list is read, **Then** it shows no goal and the plan state "no quiz yet".
4. **Given** another trainer's clients, **When** a trainer asks for their clients, **Then** none of them appears.
5. **Given** a trainer with no clients, **When** they ask, **Then** they get an empty list, not an error.
6. **Given** a client or a person who is not signed in, **When** they ask for the list, **Then** a client is refused and a visitor is asked to sign in.

### User Story 2 - Trainer finds a client by name (Priority: P2)

On the all-clients screen the trainer types part of a name and the table narrows to the clients whose name contains it. Built in task #37.

**Independent Test**: With three clients, type "ko": only the clients whose name contains "ko" remain, whatever the letter case.

### User Story 3 - Trainer opens a client (Priority: P2)

Choosing a client opens the client page with its header (name, goal, plan state) and the tabs Overview, Plan, Logs and Messages, empty until those features land. A client who is unknown, or another trainer's, shows "not found". Built in task #38.

**Independent Test**: Open a client of the trainer: the page shows the header and the four tabs. Open the address of a client of another trainer: "not found", the same as for an address that does not exist.

### Edge Cases

- Two clients with the same name both appear; their order is then fixed, not random.
- A client's name with capital letters from Polish ("Łukasz", "Ola") sorts with other names without regard to case.
- A client who joined but has not confirmed the email still appears; the list is of everyone linked to the trainer.
- The list never contains the quiz notes, the body data or anything beyond the columns of FR-002.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: A signed-in trainer MUST be able to read the list of their own clients; nobody else's clients MAY appear, and a client MUST NOT read the list.
- **FR-002**: For each client the list MUST give the client's id, name, goal (when the client has submitted the quiz) and plan state, ordered by name without regard to letter case, then by id.
- **FR-003**: The plan state MUST be "no quiz yet" while the client has not submitted the quiz, and "plan to review" once they have, until plans exist (#68). Plan week, plan weeks and last log are not in the list until the stories that produce them; the screen shows those columns empty.
- **FR-004**: The list MUST be answered with the trainer's whole client list in one response; paging is not needed for the number of clients a trainer has.
- **FR-005**: The all-clients screen MUST list the clients, search by name (a part of the name, any letter case), and show an empty state with the invite action when there are no clients (task #37).
- **FR-006**: Opening a client MUST show the client page frame with the header and four tabs; an unknown client and another trainer's client MUST both show "not found" (task #38).
- **FR-007**: Every user-facing text of the screens MUST exist in English and Polish, and the screens MUST follow the boards at 1280 px, in light and dark, with keyboard operation (tasks #37, #38).

### Key Entities

- **Client list entry**: What a trainer sees of one client in the list: id, name, goal, plan state. Read from the client's account and the quiz answers; it is not stored.

### Public endpoints

None. Reading the list needs a signed-in trainer.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A trainer sees all their clients and no one else's, in every case.
- **SC-002**: A trainer finds a client by name in under 10 seconds with 50 clients.
- **SC-003**: The list is the same whether read once or repeatedly; reading it changes nothing.

## Assumptions

- The goal shown is the one in the quiz (`BUILD_MUSCLE` and the rest); the screen translates it. A plan's own goal (spec 68) may replace it later.
- Plan states "week N of M" and "plan finished" arrive with the plan stories; the list gains them when plans can be published, by a change of the API contract in that story.
- Search by name is done on the screen over the list already read, because a trainer has few clients; the API has no search parameter.
- The client page's own read (a single client by id) is needed by task #38 and is added there; this story's first task adds only the list.
