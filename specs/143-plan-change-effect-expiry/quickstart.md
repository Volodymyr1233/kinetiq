# Quickstart: validate plan change takes effect or expires

Run after implementation. Backend and frontend are separate builds (see `CLAUDE.md`). `backend/CLAUDE.md` explains that `./gradlew bootRun` starts PostgreSQL; when the local database has a stale Flyway history, use a throwaway PostgreSQL for the browser checks.

## 1. Quality gates

```bash
cd backend && ./gradlew check
cd frontend && npm run check
```

Both pass, `HexagonalArchitectureTest` included. After an API change, with the backend running: `cd frontend && npm run gen:api` and commit `schema.d.ts`.

## 2. Backend behaviour (fixed clock tests)

The unit and integration tests prove, with a fixed clock:

1. A plan change accepted by both is not in the plan before its Monday, is after it, and earlier weeks keep the old targets.
2. A pending plan change, and one accepted by one person, expire on the Monday and leave the plan identical, with cause `UNDECIDED`.
3. An accepted change whose touched entry the trainer edited meanwhile expires with cause `PLAN_EDITED` and applies nothing; an unrelated edit does not block it.
4. Running the step twice, or three days late, gives the same result as once.
5. The Monday of a client in another time zone is that client's Monday.
6. An acceptance in flight at the boundary gives exactly one outcome.
7. Another person's plan change or client answers `404` on every new endpoint.

## 3. Real database and API

Start the backend with the demo data and sign in with the demo accounts from `R__demo_accounts.sql`. Use a plan change from `R__demo_plan_changes.sql`.

```bash
# as the client, in the browser: GET /api/v1/plan/changes/upcoming → 200 with the content
# after the Monday passes (move the demo effective week into the past, or set the clock in a test profile): 404 plan-change.not-found, and the plan holds the new values
```

Check in the database that the plan has one revision for the applied change, valid until its Monday, and that `plan_changes` has `took_effect_at` set (or `expiry_cause` for an expired one).

## 4. Browser checks (built-in browser)

Client at 375px, trainer at 1280px, light and dark, English and Polish, keyboard only for one pass:

1. Client Plan on the effective week of an upcoming change: the new values are marked "takes effect Mon …" with the old value readable; the weeks before show old targets and no mark.
2. The same plan after the Monday: new values as the plan, with the "changed by a plan change" note linking to it.
3. Client plan change screen: applied (upcoming, then in effect), expired (undecided), expired (plan edited): the sentences read word for word, no action offered.
4. Trainer Approvals: counts and tabs drop when a change expires; "Decided" shows the right labels and Monday.
5. Trainer plan editor: upcoming banner before the Monday; ordinary edit of a week in effect still works and stays a plan edit.
6. Console clean, no layout break, labels and focus visible on every control.

## 5. Security pass

- Unauthenticated request to each new endpoint: `401`.
- Client token on a trainer path and the reverse: refused by role.
- Another trainer's client id, another client's plan change id, an unknown id: `404`.
- No endpoint accepts a request body; no response contains stack traces, SQL or class names.
- Logs of a failed step carry no names, emails or note text.
