# Quickstart: validating the story

## Prerequisites

- Docker running.
- `OPENROUTER_API_KEY` set for the real-service scenarios. Scenario 3 runs without it.

## Quality gates

Run in `backend/`:

```bash
./gradlew check
```

Run in `frontend/`:

```bash
npm run check
```

## Run the app

Start the `backend` and `frontend` configurations in `.claude/launch.json`, then open `http://localhost:5173`. The demo accounts are listed in `README.md`. Use `demo.client@example.com` for the scenarios that submit a quiz; a submit happens once per account, so reset the local database to run them again.

## Scenarios

### 1. A submitted quiz becomes a draft

1. As a demo client without answers, finish the quiz. The review step shows the notice about the AI service.
2. Submit. The answer is immediate, and the waiting screen shows the building state.
3. Without touching the page, it changes to "Your trainer is reviewing your plan". Keyboard focus stays where it was.
4. As that client's trainer, open the client list: the badge says "Plan to review".
5. Open the Plan tab: the editor shows the sessions on the usual days of FR-013 (or others the notes ask for), 12 weeks, the quiz's goal, and the "built" note.
6. Change a target and save: the note goes, and the badge says "Draft".
7. Publish. When the client opens the app again, they land on the plan-ready screen, and "View my plan" shows the generated sessions on the Plan tab.

### 2. During drafting

While the client sees the building state, open that client's Plan tab as the trainer. It shows the drafting state, with no "Build plan", and moves on to the editor by itself. The badge says "Plan being drafted" until then.

### 3. No key

Start the backend with the key empty. It starts and logs one warning. Submit a quiz: drafting ends at once, the client sees the reviewing state, the badge says "Draft failed", and the editor shows the failed note and "Add session".

### 4. Restart during drafting

Submit a quiz and stop the backend while the building state shows. Start it again: within 6 minutes of the submit the client sees the reviewing state and the trainer gets a failed draft.

### 5. Access

As another trainer, open the client's Plan tab: not found. As the client, call `GET /api/v1/clients/{id}/plan`: refused by role. As a client with a built or failed draft, call `GET /api/v1/plan`: `404` `plan.not-found`.

### 6. Prompt quality

Run the evaluation matrix in [contracts/prompt.md](contracts/prompt.md) and report each draft against SC-005.

## Screens to check

The waiting screen (building and reviewing), the review notice, the Plan tab's drafting state, both notes and the badges, each at 375 px and 1280 px. Light and dark, English and Polish, keyboard focus, and the console free of errors.
