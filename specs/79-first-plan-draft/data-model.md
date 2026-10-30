# Data Model: First plan is drafted from the quiz

## Settings (task #80)

No table is added by #80. The language-model connection is four settings:

| Setting | Source | Default | Meaning |
|---|---|---|---|
| `kinetiq.llm.api-key` | environment variable `OPENROUTER_API_KEY` | empty | The key for the provider. Empty, missing or only spaces means drafting by a language model is off |
| `kinetiq.llm.base-url` | property | `https://openrouter.ai/api/v1` | The provider's address; a test points it at a local server |
| `kinetiq.llm.model` | property, or environment variable `KINETIQ_LLM_MODEL` | `openrouter/free` | The model name sent with every request |
| `kinetiq.llm.timeout` | property | `30s` | How long one request may take before it counts as failed |

The key is never logged, shown or committed; see [contracts/configuration.md](contracts/configuration.md).

## Entities of the whole story (tasks #81 to #84, planned)

Not built on this branch. Their shape follows the plan model (#69), which does not exist yet; they are confirmed when those tasks start.

- **Plan generator** (port in the plan feature's domain): turns quiz answers and the exercise library into a draft plan, or fails. Two adapters: language model (needs the `ChatModel` bean of this task) and fixed template (no outside call). The language model adapter is chosen when the bean exists.
- **Draft** (a plan the client cannot see yet, `CONTEXT.md`): belongs to the client's plan and is readable only by the client's trainer. Two kinds: a plan that passed the checks, and an empty one marked "Couldn't build automatically. Try again or build it yourself."
- **Attempt**: not stored; at most two per quiz submission, counted while drafting.
- **Quiz answers**: spec 50 and 54; read, never changed.

### Rules the checks apply (from story #68, #70)

- every exercise is in the trainer's library;
- 4 to 16 weeks, 2 to 6 sessions on distinct weekdays, at least 1 set and at least 1 rep or second per entry, RPE 5 to 10.

A draft that breaks a rule is rejected; two rejections or errors give the empty marked draft.
