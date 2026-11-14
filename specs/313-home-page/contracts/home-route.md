# Contract: the `/` route

## Landing

| Who opens `/` | Result |
|---|---|
| Signed-in trainer | Redirect to `/trainer` → `/trainer/clients` |
| Signed-in client | Redirect to `/app` → `/app/today` (or the quiz, by the client area's own rules) |
| Visitor (401) | The home page |
| Check fails (network, 5xx) | The home page |

## Links on the page

| Link (accessible name) | Count | Target |
|---|---|---|
| Sign in | 1 (header) | `/sign-in` |
| Create trainer account | 2 (hero, closing) | `/sign-up` |
| Privacy notice / Read the privacy notice | 2 (closing, footer) | `/privacy` |

## Controls

| Control | Options | Effect |
|---|---|---|
| Language (toggle group) | English, Polski | `i18n.changeLanguage`, remembered in browser storage |
| Theme (toggle group) | Light, Dark, System | `setTheme`, remembered in browser storage |

## Landmarks and headings

`banner` (header) · `main` with one `h1` and an `h2` per section · `contentinfo` (footer). Each mock-up is one `img` with an accessible name.
