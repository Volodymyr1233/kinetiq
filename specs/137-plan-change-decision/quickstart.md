# Quickstart: validate the plan change decision end to end

Details of endpoints are in [contracts/api.md](contracts/api.md), tables in [data-model.md](data-model.md).

## Prerequisites

- Docker running. The local compose database has a stale Flyway V22 (see project memory): for browser checks start a **throwaway PostgreSQL** and point `bootRun` at it, then drop it afterwards.
- `kinetiq.plan-drafting.api-key` empty is fine: plan changes then use the rule's fixed transformation.
- Demo data (profile `local`) has a trainer, clients, and (after this story) plan changes in the states pending, accepted by the trainer, applied and declined.

## Automated gates

```bash
cd backend && ./gradlew check
cd frontend && npm run check
```

## Scenarios (each in the browser, light and dark, English and Polish)

1. **Client accepts, trainer sees it** (375px, then 1280px)
   - Client: Plan or Today shows the plan change card → open it → read old → new and the chart → "Accept change" (not lime) → "You accepted. Waiting for <trainer>" with "Undo".
   - Trainer: Approvals → "Waiting for you" shows it; open it; the detail says the client accepted. Accept → "You both accepted. Takes effect Mon …".
2. **Undo**: after one accepts, "Undo" returns to "Waiting for both"; after both accepted there is no Undo.
3. **Keep current plan, with reason**: as either person open "Keep current" → no reason preselected, the button disabled → choose "Schedule", add a note → confirm. Check: the plan change is declined; the other person's list shows it under "Decided"; the conversation of both shows one message from the decliner with the title, reason and note; repeat the request (browser back, resend) → still one message.
4. **Approvals**: tabs "Waiting for you", "Waiting for client", "Decided" with counts; drafted plans appear next to plan changes; nav count falls after a decision; "Decided" shows only the last 30 days; the client page lists the open one and the last 5 ended.
5. **Chart**: each rule's chart shows one point per week against the dashed threshold; the accessible name reads every value.
6. **Errors**: sign in as another trainer and open the plan change URL → "not found". Edit the effective week in the database to this week → accept shows the week-begun message. Stop the backend → the screen shows the translated network error and keeps the last confirmed state.

## Security checks

- As a client, call the trainer endpoints, and as a trainer the client ones → `403 request.not-allowed`.
- A client with another client's plan change id → `404 plan-change.not-found` on every endpoint.
- Missing CSRF header on `POST`/`DELETE` → rejected with a code.
- Response bodies carry no stack trace or exception text; the note is shown as plain text (try `<b>x</b>`).

## Expected outcome

All five states of the story's state machine are reachable except `expired` (story #143); no plan changed; the demo plan stays as it was.
