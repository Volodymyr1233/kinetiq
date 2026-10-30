# Contract: configuration of the language-model connection (task #80)

Kinetiq has no new endpoint in this task. Its interface to the operator is configuration.

## Environment variables

| Variable | Required | Meaning |
|---|---|---|
| `OPENROUTER_API_KEY` | No | The OpenRouter key. Not set, empty or only spaces: Kinetiq starts and drafts from the fixed template. Set: Kinetiq can draft with the language model |
| `KINETIQ_LLM_MODEL` | No | Overrides the model name (default `openrouter/free`) |

Both are read from the process environment of the backend (shell, IDE run configuration or server). They are never committed: `.env` is ignored by git, and `.env.example` in the repository root only names the variables, with no value; the backend does not read a `.env` file by itself.

## Properties

| Property | Default | Notes |
|---|---|---|
| `kinetiq.llm.api-key` | `${OPENROUTER_API_KEY:}` | Do not set a value in a committed file |
| `kinetiq.llm.base-url` | `https://openrouter.ai/api/v1` | Must be an OpenAI-compatible base address (it ends in `/v1`) |
| `kinetiq.llm.model` | `openrouter/free` | OpenRouter's router for free models; a named model such as `google/gemma-4-31b-it:free` also works |
| `kinetiq.llm.timeout` | `30s` | One request; a request that takes longer fails |

## Behaviour

- With a blank key there is no `ChatModel` bean and nothing is called; every other feature works.
- With a key the bean calls `POST <base-url>/chat/completions` with the header `Authorization: Bearer <key>` and the configured model.
- The key does not appear in a log line, an error response, the health report or any configuration output. The health endpoint stays the only exposed actuator endpoint.
- The provider receives only what a draft needs (quiz answers and library exercises, built in #82). Nothing in this task sends a name, an email or an id.
