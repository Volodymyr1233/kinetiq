# Quickstart: Client sees their trainer and a draft summary while waiting for a plan

How to prove the story works end to end. Every demo account uses the password `Kinetiq-demo-1`.

## Prerequisites

- Docker running. Backend: `./gradlew bootRun` in `backend/`. Frontend: `npm run dev` in `frontend/`; open http://localhost:5173.
- Demo clients of `demo.trainer@example.com` who submitted the quiz: `demo.client.waiting` (no plan), `demo.client.draft` (a draft with sessions, saved by the trainer), `demo.client.built` (an automatic draft) and `demo.client.failed` (a failed draft), all `@example.com`.
- The demo trainer is called "Demo Trainer".

## 1. The trainer (#248)

1. Sign in as `demo.client.waiting`, with the UI in English. The reviewing state shows the avatar "DT" above the heading "Your trainer is reviewing your plan", the text "Demo checks it before you see it. You get a notice here when it's ready." and the button "Message your trainer".
2. Choose the button: the conversation opens in a sheet. Close it: focus returns to the button.
3. Switch the UI to Polish: the text starts with "Demo sprawdza plan" and the button reads "Napisz do trenera".
4. In the browser's developer tools, block `/api/v1/conversations` and reload. The text says "your trainer", there is no avatar and no error, and the page does not scroll sideways at 375 px.

## 2. The draft card (#250)

1. Sign in as `demo.client.built`. Between the text and the button a card shows "Draft", "Waiting for your trainer", the training days, "12 weeks", "Build muscle" and "Your trainer can still change these."
2. Switch the UI to Polish: the weekdays, "12 tygodni" and the goal are in Polish.
3. Sign in as `demo.client.draft`: the card shows that draft's values.
4. Sign in as `demo.client.failed` and as `demo.client.waiting`: no card, and the rest of the screen is the same.
5. In the browser's developer tools, block `/api/v1/plan/draft-summary` and reload as `demo.client.built`: no card and no error.
6. Check light and dark, 375 px and 320 px, keyboard (Tab, Enter) and focus, and an empty console.

## 3. Privacy (#249)

With the browser's developer tools or curl:

- As `demo.client.built`: `GET /api/v1/plan/draft-summary` is `200` with `trainingDays`, `weeks` and `goal` only, and `GET /api/v1/plan` is still `404 plan.not-found`.
- As `demo.client.failed`, `demo.client.waiting` and `demo.client.published`: the summary is `404 plan.not-found`, the same for all three.
- As the trainer: the summary is `403 request.not-allowed`. Without a sign-in: `401 request.unauthenticated`.

## Automated

- `backend/`: `./gradlew check`; `frontend/`: `npm run check`.
- `PlanViewServiceTest` proves what answers alike; `ClientPlanControllerTest` proves the roles and that the answer holds only the three fields.
