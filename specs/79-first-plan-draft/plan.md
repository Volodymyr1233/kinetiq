# Implementation Plan: First plan is drafted from the quiz

**Branch**: `feat/80-spring-ai-openrouter` | **Date**: 2026-10-30 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/79-first-plan-draft/spec.md`

## Summary

Kinetiq drafts a client's first plan in the background when the quiz is submitted, with a language model reached through OpenRouter, a fixed-template generator when there is no key, and an empty marked draft when two attempts fail. The story has five tasks. This branch builds **#80 only**: Spring AI's OpenAI client, pointed at OpenRouter, created from settings and only when a key exists, so that the backend starts without one. #81 to #84 are planned here at the level needed to keep #80's design compatible with them.

The approach for #80: add the `spring-ai-openai` library (not the starter, which refuses to start without a key; see [research.md](research.md)), and create one `ChatModel` bean in `common.infrastructure.config`, guarded by "key is not blank". Settings live under `kinetiq.llm`.

## Technical Context

**Language/Version**: Java 25, Spring Boot 4.1.1

**Primary Dependencies**: Spring AI 2.0.1 (`spring-ai-bom`, `spring-ai-openai`); no new test dependency

**Storage**: none for #80. #81 to #84 store a draft with the plan model (#69), planned there

**Testing**: JUnit 5 and AssertJ; `ApplicationContextRunner` for the conditional bean; a local JDK HTTP server standing in for the provider; the existing `KinetiqBackendApplicationIT` for starting without a key

**Target Platform**: the Kinetiq backend; local development, tests and later the hosted server (story #63)

**Project Type**: web application, backend change only

**Performance Goals**: none for #80; the client's request is never delayed by drafting (spec SC-003, built in #84)

**Constraints**: starts without a key (spec FR-002); the key never appears in logs, responses, health or configuration output (FR-005); no call to an outside service in the quality gate (SC-001); domain code does not depend on Spring AI (constitution I)

**Scale/Scope**: one bean, four settings, one template file; a handful of tests

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Result | Note |
|---|---|---|
| I. Hexagonal Boundaries | Pass | Spring AI types stay in `common.infrastructure`; later adapters (#81) are in `plan.infrastructure.output`. The domain sees only a port. `HexagonalArchitectureTest` runs in `./gradlew check` |
| II. One Contract | Pass | No API change, so no `schema.d.ts` change |
| III. Secure by Default | Pass | The key comes from the environment only, never committed; `.env.example` has no value; no log, response, health or configuration output shows it (tested); prompts without identifiers are built in #82 |
| IV. Explainable, Approved Plan Changes | Pass | Only the first plan is drafted by a language model, and a trainer reviews it before the client sees it; no rule decision and no today-only adjustment uses a model (spec FR-012) |
| V. Tests Verify Behaviour | Pass | Tests prove behaviour: no key, no bean; a key, a bean that calls the configured address with the key and model; no leak. No test only reads a getter |
| VI. Simplicity | Pass | One bean, no abstraction over the model: Spring AI's `ChatModel` is already the port for the adapters; the dependency is added by the first task that needs it |
| VII. One Design System | Not applicable | No UI in #80 |

No violations; Complexity Tracking is empty.

## Phase 0: Research

See [research.md](research.md). All unknowns are resolved: the version and artifact, why the starter does not work, how the model is built, the settings, the default model, how to keep the key out of logs, and how to test without the provider.

## Phase 1: Design

- Settings and the entities of the whole story: [data-model.md](data-model.md)
- The configuration contract (variables and properties): [contracts/configuration.md](contracts/configuration.md)
- How to prove the task works: [quickstart.md](quickstart.md)

**Re-check after design**: unchanged. The one point to confirm in implementation is that the whole quality gate passes with Spring AI 2.0.1 on Spring Boot 4.1.1; if it does not, the plan changes before more is built.

## Project Structure

### Documentation (this feature)

```text
specs/79-first-plan-draft/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── configuration.md
├── checklists/
│   └── requirements.md
└── tasks.md
```

### Source Code (repository root)

```text
.env.example                                            # #80: lists OPENROUTER_API_KEY and the model setting, no values
README.md                                               # #80: how to switch drafting on
backend/build.gradle.kts                                # #80: Spring AI BOM and spring-ai-openai
backend/src/main/resources/application.yaml             # #80: kinetiq.llm settings
backend/src/main/java/com/kinetiq/common/infrastructure/config/
└── LlmConfig.java                                      # #80: the ChatModel bean, only with a key
backend/src/test/java/com/kinetiq/common/infrastructure/config/
├── LlmConfigTest.java                                  # #80: bean present or absent by key
└── LlmChatModelTest.java                               # #80: request to a local stand-in server, no leak

# planned, built by later tasks:
#   #81  plan/domain/{input,output,model,service}: the plan-generator port; plan/infrastructure/output: LLM adapter (uses ChatModel)
#        and fixed-template adapter; chosen by whether a ChatModel bean exists
#   #82  plan/domain/service or output: prompt from quiz answers and the library, without identifiers
#   #83  plan/domain/service: two attempts, checks against library and plan rules, the empty marked draft
#   #84  plan/infrastructure/config: background run on quiz submit (virtual threads)
```

**Structure Decision**: The language-model connection is shared technical wiring, so it lives in `common.infrastructure.config` next to `SecurityConfig` and `TransactionConfig`. Feature adapters ask for `ChatModel` and stay in their own feature (`plan`).

## Complexity Tracking

No violations to justify.
