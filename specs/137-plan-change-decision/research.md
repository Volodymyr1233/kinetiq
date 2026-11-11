# Research: Trainer and client decide on a plan change

No `NEEDS CLARIFICATION` was left in the spec. These are the design decisions the code reading turned up.

## 1. The state machine lives in the `PlanChange` aggregate

- **Decision**: `PlanChange` gets `accept(Side, now)`, `undo(Side)` and `decline(Side, reason, note, now)`, each returning a new `PlanChange` (the class is immutable). Who accepted is two nullable instants (`trainerAcceptedAt`, `clientAcceptedAt`); the state is derived on write: none → `PENDING`, one → `ACCEPTED_BY_ONE`, both → `APPLIED`. `DECLINED` stores `declinedBy`, `reason`, `note`. `EXPIRED` stays untouched (story #143).
- **Rationale**: the issue asks for "state machine in the domain"; two timestamps carry "who", so undo needs no extra column, and the state cannot disagree with them (a database check enforces it too).
- **Alternatives**: a single `accepted_by` enum (cannot hold both acceptors); a decision log table (more than the story needs, no second use).

## 2. Concurrency: lock the row, not a version

- **Decision**: decisions load the plan change with `PESSIMISTIC_WRITE` (`findForUpdate`), then apply the transition. Two simultaneous accepts serialise; the second sees `ACCEPTED_BY_ONE` by the other and ends `APPLIED`.
- **Rationale**: the row is tiny and decisions are rare; a lock gives the spec's "no lost acceptance" without retry code. No `@Version` column exists on `plan_changes`.
- **Alternatives**: optimistic version plus retry (more code, same result).

## 3. A decision refuses once the effective Monday has begun

- **Decision**: the service compares "today in the client's time zone" with `effectiveWeek` (`Clock` is injected, as in the detection service); `today >= effectiveWeek` → `plan-change.week-begun` (409). The state is not changed; moving it to `EXPIRED` is story #143.
- **Rationale**: spec scenario 1.8 without doing #143's work. Client time zone comes from the account, as the detection service already does.

## 4. Decline message is posted through the messaging port, in the same transaction

- **Decision**: the decline calls `ExchangeMessagesUseCase.send(...)` of the `messaging` feature (a `domain.input` port, allowed by the architecture test; `messaging` does not depend on `planchange`, so there is no cycle). `TransactionConfig` wraps each `*UseCase` call in a transaction; the inner call joins the outer one, so the decline and the message commit or roll back together.
- **Idempotency**: the message id is derived from the plan change id (`UUID.nameUUIDFromBytes("plan-change-declined:" + id)`). `send` returns the stored message when the id was sent before, and a decline of an already ended plan change is refused before it posts anything, so a retry cannot duplicate.
- **Text**: built by the domain in the decliner's language from the title, the reason and the note (`DeclineMessageWording`, next to `PlanChangeWording`). Plain text. The note is limited to 500 characters so the whole message stays under the 2000 of `MessageText`.
- **Alternatives**: an output port with a messaging adapter inside `planchange` (the architecture rules allow feature-to-feature calls only through `domain.input`, so the direct port is simpler); a domain event (no second consumer).

## 5. Notification = derived state, no new store

- **Decision** (confirmed by the user): there is no notification table or email. The other person learns from: the Approvals list and its count, the open plan change card on client Plan and Today, the state shown on both screens, and, for a decline, the message in the conversation (which also raises the unread count).
- **Consequence**: withdrawing a notice on undo is automatic. The trainer nav count becomes "drafted plans waiting + plan changes waiting for the trainer".

## 6. Who may read and decide, and the extra read the client needs

- **Trainer**: existing `ViewClientsUseCase.getClient(trainer, client)` proves ownership (404 otherwise), as `PlanChangeViewService` does.
- **Client**: may read and decide only plan changes whose `client_id` is their own id; any other id → `plan-change.not-found`. The client also needs the trainer's first name for "Waiting for Marek"; it comes from `CurrentAccountUseCase.find(client).getTrainerId()` and a second `find`. No new port.
- **Routes** (role rule first, then ownership): trainer on `/api/v1/plan-changes/**` and `/api/v1/approvals/**` (already `TRAINER` in `SecurityConfig`); client on `/api/v1/plan/changes/**` (already `CLIENT` through `/api/v1/plan/**`). No `SecurityConfig` change.

## 7. Approvals and nav count

- **Decision**: a new `GET /api/v1/approvals/plan-changes` returns the trainer's open plan changes plus those ended in the last 30 days, with enough to place a row in a tab (state, who accepted). The frontend merges it with the drafted plans of the existing endpoint and builds the three tabs (the spec's tabs are a UI grouping, not backend vocabulary). The nav count is computed from the same two queries.
- **Rationale**: keeps `plan` and `planchange` features separate (no feature imports another feature on the frontend; the page that merges them is in `features/plan`, which already owns Approvals, and gets plan changes through a hook in its own `api/`).
- **Alternatives**: one backend endpoint joining both (couples the two features' domains).

## 8. Client page history

- `GET /api/v1/clients/{id}/plan-changes/ended` returns the last 5 ended plan changes (applied, declined, expired) of the trainer's own client, newest first; the existing open-plan-change endpoint stays. Each row links to the existing detail.

## 9. Frontend feature boundaries

- The detail for the trainer, the client screen and the decision hooks live in `features/planchange`. The client **Plan** and **Today** cards need "the client's open plan change"; they sit in `features/plan`, which may not import `features/planchange`. So `features/plan/api/client-plan-change.ts` holds its own `useOpenPlanChange` hook for the cards (a thin query of the same endpoint); the shared `PlanChangeCard` is already in `components/`.
- The mockup's chart on the client screen is a small bar chart; the existing `RuleChart` is reused with a compact variant instead of a second chart (spec US6: same numbers, same chart rules).
- Reason pills and the drawer (client, 375px) / dialog (trainer, 1280px): one `KeepPlanForm` inside `Sheet` (bottom) or `Dialog`, picked by area. Check the installed shadcn components (`sheet`, `dialog`, `toggle-group`) first; add through the CLI only what is missing.

## 10. Inconsistencies found, left to #143 (not fixed here)

- `PlanChangeRepository.hasOpen` is documented "an accepted one ends when its effective week begins", but `PlanChangeState.isOpen()` is only `PENDING`/`ACCEPTED_BY_ONE`, and the database's one-open-per-client index excludes `APPLIED`. So between "applied" and its Monday a client has no open plan change and the next weekly check may create a second one. #143 owns the applied-but-not-yet-effective period, so it decides whether an applied plan change blocks detection. Here `ended_at` is set to the decision time for applied and declined, so the existing cool-down (`lastEndedAt`) still counts from the decision.
- The effective Monday passing while the plan change is still open leaves it open forever until #143 adds the expiry; decisions on it are refused (item 3).

## 11. Error codes

| Code | Status | When |
|---|---|---|
| `plan-change.not-found` | 404 | unknown id, or not the caller's (existing) |
| `plan-change.ended` | 409 | accept/undo/decline on applied, declined or expired |
| `plan-change.nothing-to-undo` | 409 | undo by someone who has not accepted, or after both accepted (the latter is `plan-change.ended`) |
| `plan-change.week-begun` | 409 | effective Monday reached |
| `request.invalid` with `errors[{field:"reason", code}]` | 400 | missing/unknown reason, note over 500 characters (existing Bean Validation path); this is the spec's "reason required" |
