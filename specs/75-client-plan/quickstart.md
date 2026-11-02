# Quickstart: Client sees their plan

How to prove the story works end to end. Every demo account uses the password `Kinetiq-demo-1`.

## Prerequisites

- Docker running. Backend: `./gradlew bootRun` in `backend/`. Frontend: `npm run dev` in `frontend/`; open http://localhost:5173.
- Demo data (the `local` profile) includes clients of `demo.trainer@example.com`: one with a published plan that is not yet seen, one that has seen it, and one whose plan has finished (see the README table).

## 1. Plan ready, once (#78)

1. Sign in as the client with the unseen published plan. The "your plan is ready" screen shows with the goal, the number of weeks and sessions per week.
2. Choose "View my plan". The Plan tab opens.
3. Sign out, sign in again, or use a private window. The plan-ready screen does not show; the app opens on Today.
4. As the trainer, save a change to that client's plan. As the client, reload: still no plan-ready screen.

## 2. Week by week (#77)

1. As the client, open the Plan tab: "Week N of M" with the Monday-to-Sunday dates, seven days with the sessions on their days and rest days marked.
2. Choose next and previous week; the dates change, the sessions stay. Week 1 has no previous; the last week has no next. Away from the current week, "Current week" returns to it.
3. Open a session: its exercises with sets, repetitions or seconds, load in your unit (change the unit in settings), RPE and rest. A plank shows seconds; an exercise without load says "no load".
4. At 375 px the page does not scroll sideways. Check light and dark, keyboard (Tab, Enter) and focus.

## 3. Finished plan (#78)

1. Sign in as the client whose plan has finished. The Plan tab shows "Your trainer is preparing your next plan." and no week strip.
2. As the trainer, lengthen that plan; as the client, reload: the strip is back.

## 4. Privacy (#76)

- With the browser's DevTools or curl as the draft client (`demo.client.draft`): `GET /api/v1/plan` is `404 plan.not-found`, the same as for a client with no plan.
- As the trainer: `GET /api/v1/plan` is `403 request.not-allowed`.
- As the client: `GET /api/v1/plan?week=0` and `?week=99` are `400 plan.week-out-of-range`.

## Automated

- `backend/`: `./gradlew check`; `frontend/`: `npm run check`.
- `PlanViewServiceTest` proves the draft and no plan answer alike, the week bounds, the finished state and the Sunday-to-Monday boundary with a fixed clock; `PlanRepositoryIT` proves the seen mark survives a save and does not raise the version.
