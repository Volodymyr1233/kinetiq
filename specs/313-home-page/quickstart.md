# Quickstart: Visitor sees what Kinetiq does and signs up

## Run

1. `frontend/`: `npm run dev` (`frontend` in `.claude/launch.json`). For the signed-in redirects, also run the backend: `backend/`: `./gradlew bootRun` (JDK 25).
2. Open `http://localhost:5173/` signed out.

## Check

| Scenario | Expected |
|---|---|
| Signed out, open `/` | The home page; no console error |
| Create trainer account (hero, closing) | `/sign-up` |
| Sign in | `/sign-in` |
| Privacy notice links | `/privacy` |
| Footer: Polski, then Dark | The page in Polish and dark at once; kept after a reload |
| Signed in as a demo trainer, open `/` | `/trainer/clients` |
| Signed in as a demo client, open `/` | `/app/today` |
| 375 px and 1280 px, light and dark, EN and PL | Layout as on the Home boards; no sideways scroll |
| Scroll down | Each section fades up once; the phone fills its answers and counts to 83 once |
| Keyboard: Tab through the page | Header link, hero button, closing button, privacy links, toggle groups; visible focus; no stop inside a mock-up |

## Tests

`frontend/`: `npm run test -- home-page`, then `npm run check`.
