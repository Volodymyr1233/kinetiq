# Quickstart: verifying plan change content

Prerequisites: Docker running; backend and frontend dependencies installed. Memory note: the compose DB may hold a stale Flyway `V22`; use a throwaway PostgreSQL for the browser check if `bootRun` fails on it.

## 1. Automated gates

```bash
cd backend && ./gradlew check
cd frontend && npm run check
```

Both must pass. Highlights to see in the backend run: limits tests per rule (on and just beyond each limit), fixed transformation tests (rounding, exemption from the percentage limit), the prompt privacy test, the drafting policy test (valid first answer: one call; two invalid: fallback), `PlanChangeRepositoryImplIT` (one open plan change per client enforced), controller tests (`404` for another trainer's client).

## 2. End to end on the local stack

1. `cd backend && ./gradlew bootRun` (local profile loads demo data, including a pending plan change per rule).
2. `cd frontend && npm run dev`, sign in as the demo trainer (see `R__demo_accounts.sql`).
3. Open Clients → a client with a demo plan change → the client page shows the plan change card; follow its link.
4. The detail shows the title, the reason, the table with Now / New / Change, the rule's numbers chart and, for low completion, the removed session. Switch language (EN/PL), light/dark, keyboard-tab through the page.
5. Open the id of a plan change that belongs to another trainer's client (DevTools or `curl` with the session): expect `404` and `plan-change.not-found`.

## 3. Drafting without the LLM

Run with `OPENROUTER_API_KEY` unset (the default). Force a weekly check for a client whose logs make a rule fire (the demo logs do; use the sweep or the use case in an `*IT`). Expected: a plan change appears with the rule's fixed transformation; the log shows only the rule name and the failure class, never a prompt or an answer.

## 4. API contract

With the backend running: `cd frontend && npm run gen:api`; `git diff frontend/src/lib/api/schema.d.ts` shows exactly the two new operations and their schemas.
