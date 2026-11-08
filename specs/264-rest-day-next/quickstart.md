# Quickstart: Client sees what comes next on a rest day

Prerequisites: backend running (`./gradlew bootRun` in `backend/`) and frontend (`npm run dev` in `frontend/`); demo data is loaded locally.

1. Sign in as a demo client whose plan has no session today but one later this week. Today shows the "Rest" badge, "No session today. No check-in either.", "Next: <name> · <date> · <minutes> min", "See this week", and "<n> of <m> sessions done this week" under the card.
2. Sign in as the demo client whose next session is in a later week: the date is in that week.
3. Sign in as the demo client with no session left: no "Next:" line, the rest of the card unchanged.
4. Choose "See this week": the Plan tab opens.
5. Switch the language to Polish: all texts and the date are Polish.
6. Check light and dark at 375 px, keyboard focus on "See this week".

Checks: `./gradlew check` in `backend/`, `npm run check` in `frontend/`.
