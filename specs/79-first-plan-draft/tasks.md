---
description: "Tasks for story #79, First plan is drafted from the quiz"
---

# Tasks: First plan is drafted from the quiz

**Input**: `specs/79-first-plan-draft/` (spec.md, plan.md, research.md, data-model.md, contracts/configuration.md, quickstart.md), building on `specs/50-client-quiz`, `specs/54-quiz-review-submit` and `specs/13-exercise-library`

**Tests**: Required by the Definition of Done (`CLAUDE.md`) and principle V. Tests sit in the same phase as the code they cover.

**Organization**: Phases follow the GitHub tasks, not the spec's user stories, because each task is its own pull request, squash merged (`CLAUDE.md`, Workflow). #80 builds the connection to the language model (US1), #81 the plan generator and its two adapters (US3), #82 the prompt (US2), #83 the checks, the retry and the empty marked draft (US2, US4), #84 the background run on quiz submit (US2). **Only #80 is built on the branch `feat/80-spring-ai-openrouter`**; the phases of #81 to #84 stay unchecked and are carried out in their own pull requests when their blockers are closed (#81 waits for #80 and #70, #82 for #81 and #55, #83 for #81, #84 for #82 and #83).

**Paths**: `B` = `backend/src/main/java/com/kinetiq`, `BT` = `backend/src/test/java/com/kinetiq`, `R` = `backend/src/main/resources`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: spec user story (US1 backend ready for a language model, US2 trainer gets a draft, US3 drafting without a key, US4 failed draft becomes an empty marked draft)

---

## Phase 1: Setup

- [X] T001 Commit `specs/79-first-plan-draft/` (spec, checklist, plan, research, data model, contract, quickstart, tasks) with the code of task #80, so the spec is reviewed in that pull request. `CONTEXT.md` needs no new term: Draft is defined

---

## Phase 2: task #80, Spring AI for OpenRouter (US1)

**Goal**: A `ChatModel` pointed at OpenRouter, created from settings and only when a key is set; the backend starts without one.

**Independent Test**: `./gradlew test --tests '*LlmConfigTest' --tests '*LlmChatModelTest' --tests '*KinetiqBackendApplicationIT'` passes with no `OPENROUTER_API_KEY` in the environment.

- [X] T002 [US1] Add the dependencies to `backend/build.gradle.kts`: `implementation(platform("org.springframework.ai:spring-ai-bom:$springAiVersion"))` with `val springAiVersion = "2.0.1"` next to the other version values, and `implementation("org.springframework.ai:spring-ai-openai")`; not the starter (research.md: it fails to start without a key)
- [X] T003 [US1] Add the settings to `R/application.yaml` under `kinetiq.llm`: `api-key: ${OPENROUTER_API_KEY:}`, `base-url: https://openrouter.ai/api/v1`, `model: openrouter/free`, `timeout: 30s`, with a one-line comment on why the key default is empty (FR-001, FR-003; data-model.md)
- [X] T004 [US1] Create `B/common/infrastructure/config/LlmConfig.java`: `@Configuration(proxyBeanMethods = false)` with one `@Bean ChatModel` guarded by `@ConditionalOnExpression("!'${kinetiq.llm.api-key:}'.isBlank()")`, built with `OpenAiChatModel.builder().options(OpenAiChatOptions.builder().apiKey(..).baseUrl(..).model(..).timeout(..).maxRetries(0).build()).build()` (maxRetries 0 because the two attempts are counted in the domain, research.md); the key goes nowhere else (FR-001, FR-002, FR-003, FR-005)
- [X] T005 [P] [US1] Create `.env.example` in the repository root listing `OPENROUTER_API_KEY=` and a commented `# KINETIQ_LLM_MODEL=openrouter/free`, each with a one-line comment and no value (FR-004; `.gitignore` already allows `.env.example`)
- [X] T006 [P] [US1] Add to `README.md` a short section "Drafting with a language model": the two variables, that without a key Kinetiq starts and uses the fixed template, that the backend does not read `.env` by itself, and that the key is never committed
- [X] T007 [US1] Test `BT/common/infrastructure/config/LlmConfigTest.java` with `ApplicationContextRunner` and `LlmConfig`: no key, an empty key and a key of spaces give a context that starts and has no `ChatModel` (FR-002); a key gives exactly one (FR-003)
- [X] T008 [US1] Test `BT/common/infrastructure/config/LlmChatModelTest.java` with a local `com.sun.net.httpserver.HttpServer` on a free port answering like an OpenAI-compatible service: the model built by `LlmConfig` with `kinetiq.llm.base-url` set to the server sends `POST /chat/completions` with `Authorization: Bearer <key>` and the configured model in the body and reads the answer back (FR-003); an answer of `401` raises an error whose message does not contain the key; `toString()` of the model and of its options does not contain the key (FR-005, SC-006); a server that does not answer within `kinetiq.llm.timeout` fails the call instead of hanging (edge case)

**Checkpoint**: US1 works when T002 to T008 pass; no API, no migration and no frontend change, so no `schema.d.ts`.

---

## Phase 3: task #81, plan-generator port and adapters (US3) — later pull request

**Waits for**: #80 and #70 (plan rules).

- [ ] T009 [US3] Re-plan #81 in this folder when it starts: confirm the port and the two adapters against the plan model (#69); extend `quickstart.md`
- [ ] T010 [US3] Port in the plan feature's `domain.output` (plan generator: quiz answers and library in, draft plan out, or a failure); fixed-template adapter (no outside call, same draft for the same answers, FR-011); language-model adapter using the `ChatModel` bean; chosen by whether the bean exists
- [ ] T011 [US3] Tests: the template gives the same draft twice and passes the plan rules; with no `ChatModel` bean the template is used and nothing is called; with a stand-in server the language-model adapter parses a JSON draft

---

## Phase 4: task #82, prompt from quiz answers and library (US2) — later pull request

**Waits for**: #81 and #55 (quiz answers model).

- [ ] T012 [US2] Build the prompt from the quiz answers and the library's exercises (FR-009); a test with answers whose free text contains an email-like string and a name-like string, and with account data in scope, proves the prompt contains no person's name, email or id other than what the client typed into free text (edge case in spec.md)

---

## Phase 5: task #83, draft validation, retry and failure state (US2, US4) — later pull request

**Waits for**: #81.

- [ ] T013 [US2] Check a draft against the library and the plan rules (data-model.md: every exercise in the trainer's library; 4 to 16 weeks, 2 to 6 sessions on distinct weekdays, at least 1 set and at least 1 rep or second per entry, RPE 5 to 10); reject anything else (FR-008)
- [ ] T014 [US4] At most two attempts; after two failures store an empty draft marked "Couldn't build automatically. Try again or build it yourself." in English and Polish (FR-010); tests with a fake generator that fails once, twice, returns an unknown exercise and times out

---

## Phase 6: task #84, draft in the background on quiz submit (US2) — later pull request

**Waits for**: #82 and #83.

- [ ] T015 [US2] Start drafting in the background when the quiz is submitted; the submission answers without waiting (FR-006); the draft is readable only by the client's trainer (FR-007); a failed draft never undoes the submission (US4/AC4); tests for each, including that another trainer's client's draft answers `404`

---

## Phase 7: Finish task #80

- [X] T016 Run `./gradlew test --tests '*KinetiqBackendApplicationIT'` with no `OPENROUTER_API_KEY` set (quickstart step 1); it passes
- [X] T017 Mutation checks (quickstart step 5): remove the condition on the bean, then `LlmConfigTest` MUST fail; log the key inside the bean method, then `LlmChatModelTest` MUST fail; restore the code afterwards
- [X] T018 Run `./gradlew check` in `backend/`; `BUILD SUCCESSFUL`, including `HexagonalArchitectureTest`; if it fails because Spring AI 2.0.1 does not fit Spring Boot 4.1.1, change the plan before building more (plan.md, Re-check after design)
- [X] T019 Run `/speckit-converge` for this task; commit the code, tests and spec locally as `feat(backend): add Spring AI client for OpenRouter`

---

## Dependencies and order

- #80 has no blocker and is built first (this branch). #81 uses its bean.
- Within #80: T002 → T003 → T004; T005 and T006 are independent; T007 and T008 after T004; T016 to T019 last. T001 is committed together with T019.
- #81 waits for #80 and #70; #82 for #81 and #55; #83 for #81; #84 for #82 and #83.

## Implementation strategy

MVP of this branch is US1 (#80): the connection, fully tested without the provider. Stop after T019; the pull request follows.
