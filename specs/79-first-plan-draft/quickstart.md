# Quickstart: validating task #80 (Spring AI for OpenRouter)

Run in `backend/` with Docker running. No key and no network are needed for steps 1 to 4.

## 1. The backend starts without a key

```bash
./gradlew test --tests '*KinetiqBackendApplicationIT'
```

Expected: passes with no `OPENROUTER_API_KEY` in the environment. (With the Spring Boot starter instead of the library this test failed with "At least one credential source must be specified"; see `research.md`.)

## 2. The model exists only with a key

```bash
./gradlew test --tests '*LlmConfigTest'
```

Expected: passes. With no key, an empty key and a key of spaces the context starts and has no `ChatModel`; with a key it has one.

## 3. The model calls the configured address with the key and model

```bash
./gradlew test --tests '*LlmChatModelTest'
```

Expected: passes. A local stand-in server receives `POST /chat/completions` with `Authorization: Bearer <key>` and the configured model in the body, and the model reads its answer. A refused key (`401`) produces an error that does not contain the key, and the key is absent from the model's text form.

## 4. The whole quality gate

```bash
./gradlew check
```

Expected: `BUILD SUCCESSFUL`, including `HexagonalArchitectureTest`.

## 5. A test that bites

Make the bean unconditional (remove the condition) and run step 2: the "no key" tests MUST fail. Print the key in a log line inside the bean method and run step 3: the leak test MUST fail. Restore the code.

## 6. Live check with a real key (manual, not part of `check`)

1. Set `OPENROUTER_API_KEY` in the environment of the process that starts the backend: in the shell (`export OPENROUTER_API_KEY=...`, or `$env:OPENROUTER_API_KEY = "..."` in PowerShell) or in the IDE's run configuration. The backend does not read a `.env` file by itself; `.env.example` only names the variables. Never paste the key into a file that is committed.
2. Start the backend: `./gradlew bootRun`.
3. Expected: it starts, the log shows no key, and `GET /actuator/health` answers `UP`. Calling the model is part of #81 and later; until then nothing sends a request.

Tasks #81 to #84 add their own validation steps to this file when they start.
