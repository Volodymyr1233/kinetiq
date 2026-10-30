# Research: First plan is drafted from the quiz

Findings for task #80, checked against Spring AI's documentation (context7, Spring AI 2.0.1) and by experiment in this repository on 2026-10-30. Tasks #81 to #84 are researched when they start.

## Spring AI version and artifact

- **Decision**: Spring AI **2.0.1**, imported with its BOM (`org.springframework.ai:spring-ai-bom`) and the library artifact **`spring-ai-openai`**, not the Spring Boot starter.
- **Rationale**: 2.0.x is the line for Spring Boot 4; it resolved and compiled in this project (Boot 4.1.1, Java 25). The starter `spring-ai-starter-model-openai` adds auto-configuration for six model kinds (chat, embedding, image, audio speech, transcription, moderation) that all build a client when the application starts.
- **Alternatives considered**: The starter (rejected, see next section); the Spring AI 1.1 line (rejected: built for Boot 3).

## The application does not start without a key when the starter is used

- **Experiment**: With the starter and no key, `KinetiqBackendApplicationIT` failed to load the context: `IllegalStateException: At least one credential source must be specified: credential (apiKey), workloadIdentity, or adminApiKey`. Spring AI 2.x builds its OpenAI client at startup and refuses a missing key.
- **Decision**: Use `spring-ai-openai` without auto-configuration and create the chat model in Kinetiq's own configuration, only when a key is set.
- **Rationale**: The acceptance criterion of #80 is that the backend starts without the key. Alternatives to keep the starter and still start:
  - `spring.ai.model.chat=none` and five more `none` switches: starts, but the switch cannot depend on the key, so the model would never be created from a key alone (SC-002 would need a second setting).
  - A dummy key when none is set: an empty environment variable is not replaced by a placeholder default, so an empty key would still fail; a dummy would also send a wrong key to the provider.
  - An environment post-processor that switches the model off when the key is blank: works, but is hidden machinery that surprises the next developer.
- **Alternatives considered**: all three above (rejected for the reasons given).

## How the model is created

- **Decision**: One `@Bean` of type `ChatModel` in `common.infrastructure.config`, guarded by `@ConditionalOnExpression` on `kinetiq.llm.api-key` being not blank, built with `OpenAiChatModel.builder().options(OpenAiChatOptions.builder().apiKey(..).baseUrl(..).model(..).timeout(..).maxRetries(0).build()).build()` (API read from the 2.0.1 jar).
- **Rationale**: The bean exists only when there is a key; feature code that needs a model asks for `ChatModel` and the drafting code decides between it and the fixed-template generator (#81). It lives in `common` because plan drafting (#81) and plan-change drafting (#133) both use it, and `common` must not depend on a feature.
- **Retries**: Spring AI has its own retry settings. This story fixes "two attempts" in the domain (spec FR-010), so the client is built without hidden retries (`maxRetries(0)`), otherwise one counted attempt could make several calls.
- **Alternatives considered**: Putting the bean in a `plan` feature package (rejected: #133 belongs to another feature and `common` is where shared technical wiring lives, like `SecurityConfig`).

## Settings

- **Decision**: Properties under `kinetiq.llm`: `api-key` (from `OPENROUTER_API_KEY`, empty by default), `base-url` (`https://openrouter.ai/api/v1`), `model` (`openrouter/free`), `timeout` (`30s`). Environment variables work by relaxed binding too: `KINETIQ_LLM_MODEL` sets the model. `.env.example` lists `OPENROUTER_API_KEY` and `KINETIQ_LLM_MODEL`.
- **Rationale**: The issue names `OPENROUTER_API_KEY` and "model from a property". Keeping all four under one prefix makes them easy to find and keeps `spring.ai.*` out of the file, which is only meaningful with the starter. Base URL and timeout are settings so tests and later hosting can change them without code (spec FR-003, edge case on slow answers).
- **Alternatives considered**: `spring.ai.openai.*` names (rejected: they would do nothing without the starter and would mislead).

## Default model

- **Decision**: `openrouter/free`.
- **Rationale**: OpenRouter's public model list on 2026-10-30 holds 456 models, 19 of them free, and free models change often. `openrouter/free` is OpenRouter's own router: it picks a free model for each request, accepts a 200 000-token context and is listed with support for `response_format` and structured outputs, which the draft needs (#82, #83). It never has to be renamed when a free model is retired. The cost is that quality varies; the draft checks, the second attempt and the fixed fallback (spec FR-008, FR-010) exist for this.
- **Alternatives considered**: A named free model such as `google/gemma-4-31b-it:free` (listed with `response_format`, 262 144 tokens): kept as the first alternative if the router's quality proves too uneven; the choice is one setting. Models without structured-output support (rejected: the draft is a JSON plan).

## Keeping the key out of logs and responses

- **Decision**: The key is read once into the model's options. No `toString`, log line, health detail or response contains it; `management.endpoints.web.exposure.include` stays `health`, and no Spring AI observation that logs prompts or completions is switched on.
- **Rationale**: Spec FR-005, SC-006 and constitution III. A test checks that the key is absent from the model's and options' `toString` and from the message of an error caused by a refused key.
- **Alternatives considered**: Wrapping the key in a type whose `toString` hides it (not needed if the test shows no leak; added only if it does).

## Testing the connection without the provider

- **Decision**: Tests start a local HTTP server (the JDK's `com.sun.net.httpserver`) that answers like an OpenAI-compatible service, point `kinetiq.llm.base-url` at it, call the model, and check the request that arrived: path `/chat/completions`, header `Authorization: Bearer <key>`, the configured model in the body, and the answer read back. Another test answers `401` and checks that nothing leaks.
- **Rationale**: Proves the real wiring end to end with no network, no key and no new dependency; the quality gate stays independent of a free external service (spec SC-001).
- **Alternatives considered**: A mock-server library (rejected: a dependency for one test, constitution VI); calling OpenRouter in the build (rejected: needs a key, is flaky and costs).
- **Live check**: a manual step in `quickstart.md` for someone who has a key. It is not part of `./gradlew check`.

## Spring Boot 4.1 and Spring AI 2.0 compatibility

- **Decision**: Accept 2.0.1 with Boot 4.1.1, and confirm with the whole quality gate.
- **Rationale**: Spring AI 2.0 targets Boot 4.0; 4.1 is a minor release of the same line. The dependency resolved and the application context got as far as Spring AI's own validation in the experiment. `./gradlew check` is the proof; if it fails for a compatibility reason, the plan changes before more is built.
- **Alternatives considered**: Waiting for a Spring AI release that names Boot 4.1 (rejected: the story cannot start without a client).

## What stays open for later tasks

- **#81**: the plan-generator port, the language-model adapter and the fixed-template adapter; which is chosen when the key is blank follows from this story's bean being absent.
- **#82**: the prompt; its "no names, emails or ids" rule is tested there by building the prompt from quiz answers that contain such values.
- **#83**: the two attempts, the checks against library and plan rules, the empty marked draft.
- **#84**: running the draft in the background after the quiz is submitted.
