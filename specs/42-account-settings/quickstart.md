# Quickstart: Trainer and client change their settings

How to see the story working and prove it. The rules are in spec.md; the contract is in contracts/api.md.

## Prerequisites

- Node 24 and the frontend's dependencies (`npm install` in `frontend/`).
- Docker running and the backend started (`./gradlew bootRun` in `backend/`), and the frontend (`npm run dev` in `frontend/`).
- The demo accounts with the demo password from the README: `demo.trainer@example.com` (trainer) and `demo.client@example.com` (client).

## Automated checks

- `./gradlew check` in `backend/` (tasks #43, #44).
- `npm run check` in `frontend/` (all tasks).

## Browser check, task #43

Task #43 has no screen to change settings yet; this check proves that the account's settings win over what the device used before.

1. Open `http://localhost:5173/` at 1280 px, choose Polski and Dark on the start page.
2. Open `/sign-in` and sign in as the demo trainer (settings: English, system): the trainer area shows English and follows the system theme.
3. Sign out: the sign-in page stays in English and the system theme, the settings the device last used.
4. Reload: still English and the system theme (the language is remembered on the device).
5. Repeat 1–4 as the demo client at 375 px: the client app shows English and the system theme.
6. The console shows no errors.

## Browser check, tasks #44, #45 and #46

Refined when those tasks start:

- #44: as the demo trainer, open Settings, change the name, units, language and appearance, save: "Settings saved.", the sidebar shows the new name, the app switches language and theme; sign in from another browser: the same settings.
- #45: as the demo client, open the settings sheet from the header, choose Polski, Dark and lb: each applies at once; sign in from another browser: the same settings.
- #46: as the demo client with lb, answer the quiz's body weight in lb (for example 137.8): the review shows 137.8 lb; choose kg in the settings sheet and open the review again: 62.5 kg.
