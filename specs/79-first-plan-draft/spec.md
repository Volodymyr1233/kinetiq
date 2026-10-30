# Feature Specification: First plan is drafted from the quiz

**Feature Branch**: `feat/80-spring-ai-openrouter`

**Created**: 2026-10-30

**Status**: Draft

**Input**: Story #79 "First plan is drafted from the quiz" and its tasks #80 (add Spring AI for OpenRouter), #81 (plan-generator port and adapters), #82 (prompt from quiz answers and library), #83 (draft validation, retry and failure state) and #84 (draft in the background on quiz submit). Builds on spec `specs/50-client-quiz` and `specs/54-quiz-review-submit` (the quiz and its submission), spec `specs/13-exercise-library` (the exercise library) and story #68 (plan rules). Decision record: `docs/adr/0002-plan-changes-rules-decide-llm-drafts.md` and the team's decisions (the LLM provider is OpenRouter, reached through Spring AI; free models; the model is chosen when this story starts). Showing the draft to the trainer is story #85, and the client's waiting message is part of the quiz stories; neither is part of this one.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - The backend is ready to ask a language model (Priority: P1)

Whoever runs Kinetiq (a developer, the team, later the hosting setup) can switch on drafting by giving the backend a key and a model name, without changing code. Without a key, Kinetiq still starts and works.

**Why this priority**: Task #80 and the first step of the story: every later task needs the connection to exist, and no developer may be blocked from starting the app by a missing key.

**Independent Test**: Start the backend with no key: it starts and every existing feature works. Start it again with a key and a model name set in the environment: it starts and reports that drafting by a language model is configured, without ever showing the key.

**Acceptance Scenarios**:

1. **Given** no key is set, **When** the backend starts, **Then** it starts normally and drafting by a language model is not available.
2. **Given** a key and a model name are set outside the code, **When** the backend starts, **Then** it starts and drafting by a language model is available through the provider's address and the chosen model.
3. **Given** a repository checkout, **When** a developer looks for what to set, **Then** a template file lists the variable and its purpose, with no real value in it.
4. **Given** the key, **When** the backend logs, reports its health or shows its configuration, **Then** the key never appears.

---

### User Story 2 - The trainer gets a draft when the client submits the quiz (Priority: P1)

When a client submits the quiz, Kinetiq drafts a first plan in the background from the answers and the exercise library. The result is a draft that only the trainer sees.

**Why this priority**: The acceptance criteria of the story ("starts drafting in the background", "a draft only the trainer sees").

**Independent Test**: Submit a quiz as a client: the submission answers at once, and shortly after a draft plan exists for that client with the trainer as its only reader.

**Acceptance Scenarios**:

1. **Given** a client submits the quiz, **When** the submission is accepted, **Then** the client's request is answered without waiting for the draft, and drafting starts in the background.
2. **Given** drafting finishes, **When** the result is stored, **Then** it is a draft that only the client's trainer can see; the client does not see it until the trainer publishes it.
3. **Given** a draft from the language model, **When** it is checked, **Then** it uses only exercises from the library and passes the plan rules; a draft that breaks either is rejected and not stored as the result.
4. **Given** the quiz answers and the library, **When** the request to the language model is built, **Then** it contains the answers and the library's exercises, and never a name, an email or an id of any person.

---

### User Story 3 - Drafting works without a key (Priority: P2)

Without a key, Kinetiq drafts a first plan from a fixed template, so that local development, tests and demos behave like the real thing without a network and without cost.

**Why this priority**: Acceptance criterion of the story ("Without `OPENROUTER_API_KEY` a fixed-template generator is used"); it keeps every developer and every test independent of a free external service.

**Independent Test**: With no key set, submit a quiz: a draft appears, built from a fixed template, and no request leaves Kinetiq.

**Acceptance Scenarios**:

1. **Given** no key is set, **When** a client submits the quiz, **Then** the draft comes from a fixed template, passes the same checks as any draft, and no call is made to an outside service.
2. **Given** the same answers, **When** the template builds a draft twice, **Then** both drafts are the same.

---

### User Story 4 - A failed draft becomes an empty draft with a message (Priority: P2)

When the language model does not deliver a usable draft after two attempts, the trainer gets an empty draft marked "Couldn't build automatically. Try again or build it yourself."

**Why this priority**: Acceptance criterion of the story; a free model fails often, and the client must never be left without a plan to review.

**Independent Test**: Make the language model fail twice (an error or a draft that breaks the rules): the trainer's draft is empty and carries the message; the client's submission is not undone.

**Acceptance Scenarios**:

1. **Given** the first attempt fails (no answer, an unreadable answer or a draft that breaks the library or the plan rules), **When** drafting retries, **Then** it asks once more.
2. **Given** the second attempt fails as well, **When** drafting gives up, **Then** an empty draft is stored for the trainer with the message "Couldn't build automatically. Try again or build it yourself." (also in Polish), and nothing is shown to the client.
3. **Given** a failed draft, **When** the trainer looks at it, **Then** they can try again or build the plan themselves (story #85).
4. **Given** any failure, **When** the client's quiz was already submitted, **Then** the submission stays; a failed draft never undoes or delays it.

---

### Edge Cases

- The key is set to an empty value or only spaces: treated as no key.
- The language model service is slow or does not answer: the attempt fails after a time limit, counts as one of the two attempts and never blocks the client's request.
- The language model answers with a draft that names an exercise that is not in the library, or an exercise of another trainer: rejected, as any draft that breaks the rules.
- Quiz answers contain free text (notes, limits): the text is part of the answers and is sent; it is not treated as a name or an email and not cleaned, but the quiz's own rules apply (spec 50).
- A client submits the quiz again: a new draft replaces the earlier draft that nobody published; a published plan is never replaced.
- The backend restarts while a draft is being built: the draft is built again or reported as failed when the trainer next looks; the client's submission stays either way.
- No error response and no log line contains the key, the prompt, a quiz answer or a person's name or email.

## Requirements *(mandatory)*

### Functional Requirements

**Connection to the language model** (task #80)

- **FR-001**: The key for the language model service MUST come from the environment variable `OPENROUTER_API_KEY` and the model name from a setting outside the code; neither is written in the repository.
- **FR-002**: The backend MUST start and all its other features MUST work when no key is set, or when it is empty or only spaces.
- **FR-003**: When a key is set, the backend MUST be able to reach the language model service at the provider's address with the chosen model; the address is a setting, not code.
- **FR-004**: The repository MUST contain a template file listing `OPENROUTER_API_KEY` and the model setting with what they are for and no value; the file with real values is never committed.
- **FR-005**: The key MUST NOT appear in a log line, an error response, the health report or any shown configuration.

**Drafting** (tasks #81 to #84, built after #80)

- **FR-006**: Submitting the quiz MUST start drafting in the background; the submission's answer MUST NOT wait for the draft (spec 54).
- **FR-007**: The result MUST be a draft that only the client's trainer can see; the client sees a plan only after the trainer publishes it (story #85).
- **FR-008**: A draft MUST use only exercises from the exercise library the trainer can use and MUST pass the plan rules (story #68); any other draft is rejected.
- **FR-009**: The request to the language model MUST contain the quiz answers and the library's exercises and MUST NOT contain a name, an email or an id of a person (constitution III).
- **FR-010**: Drafting MUST ask the language model at most twice; when neither attempt gives an accepted draft, it MUST store an empty draft for the trainer marked "Couldn't build automatically. Try again or build it yourself." in English and Polish.
- **FR-011**: Without a key, drafting MUST use a fixed-template generator that makes no outside call and gives the same draft for the same answers; its draft passes the same checks (FR-008).
- **FR-012**: Whether a plan change is due, and any adjustment of today's targets, MUST NOT be decided by a language model (constitution IV); drafting a first plan is the one use covered here.

### Key Entities

- **Plan generator**: The one place that turns quiz answers and the library into a draft plan. It has a language-model version and a fixed-template version; the rest of Kinetiq does not know which one answered.
- **Draft**: A plan the client cannot see yet (`CONTEXT.md`), here the first one for a client, built from the quiz. It is either a plan that passed the checks or an empty one marked as not built.
- **Quiz answers**: What the client submitted (spec 50, 54); the only personal input of drafting, and sent without identifiers.

### Public endpoints

None. Drafting is started by the quiz submission of a signed-in client (spec 54); the language model service is called by the backend, never by the browser.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: With no key set, a clean checkout starts and passes its whole quality gate without any outside service.
- **SC-002**: Setting a key and a model name in the environment is enough to switch drafting to the language model; no file in the repository changes.
- **SC-003**: A client's quiz submission is answered in the same time whether drafting takes one second or fails; drafting never adds waiting for the client.
- **SC-004**: In every test and sample of the request to the language model, no name, email or id of a person occurs.
- **SC-005**: Every draft that reaches a trainer, whatever generator built it, uses only library exercises and passes the plan rules, or is the empty marked draft; no other result exists.
- **SC-006**: The key occurs nowhere in logs, responses, health reports or shown configuration in any test.

## Assumptions

- The language model service is OpenRouter, reached through Spring AI's OpenAI-compatible client, and its free models are used; this is the team's recorded decision. Which free model is used is decided in the plan of this story (`research.md`) and is only a setting, so it can change without code.
- Free models may log what they receive, which is why FR-009 is strict; the quiz holds health-adjacent data (weight, limits).
- A time limit for one attempt and the number of attempts (two) are fixed by this story; the time limit is a setting with a sensible default.
- The first task (#80) builds only the connection and its settings (US1); the drafting itself (US2 to US4) is built by #81 to #84 in their own pull requests, when their blockers (#70, #55, #80, #81) are closed.
- The empty marked draft needs the plan model (#69) to exist; its text is stored with the draft and shown by the trainer's screens (story #85).
- Production secrets reach the backend only through the server's environment (story #63); this story only reads the variable.
