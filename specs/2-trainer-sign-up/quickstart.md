# Quickstart: verify Trainer signs up

## Prerequisites

Docker running. Backend: `cd backend && ./gradlew bootRun`. Frontend: `cd frontend && npm run dev`. Open http://localhost:5173.

## Gates

| Side | Command | Expect |
|---|---|---|
| backend | `./gradlew check` | Pass, including `HexagonalArchitectureTest` |
| frontend | `npm run check` | Pass |
| contract | `npm run gen:api` with the backend running | `git diff src/lib/api/schema.d.ts` is empty after the commit |

## Scenarios

| # | Do | Expect |
|---|---|---|
| 1 | `/sign-up`, submit empty | Four field messages; nothing sent (Network tab) |
| 2 | Password `kinetiq-2026` | Password help turns into an error: upper-case missing |
| 3 | Valid form, new email | "Check your email" names the address; backend log has a `confirm-email` line with a link |
| 4 | Open the logged link | "Email confirmed" with "Sign in"; the address bar no longer shows the token |
| 5 | Open the same link again | "This link no longer works" |
| 6 | On that state, enter the email, "Send new link" | "Check your email"; no log line (the account is confirmed) |
| 7 | Sign up again with the email from 3 in other letter case | Same "Check your email"; log line `account-exists`; no second account |
| 8 | New email, sign up, "Resend link" twice | Button disabled for 60 s; one new log line; the first link now fails |
| 9 | `curl` sign-up with `"adult": false` (cookie and header from step 3) | `400`, `validation.failed`, `errors` has `adult` / `assert-true`, no server text |
| 10 | Switch the UI to Polish (`i18n.changeLanguage('pl')` in the console), repeat 1, 3 and 5 | All text in Polish, including the log subject `Potwierdź konto w Kinetiq` |
| 11 | Repeat 1–5 in dark theme, at 375px and 1280px, and once at 200% zoom | Layout holds without horizontal scrolling, focus ring visible, labels read by the accessibility tree |
| 12 | Sign up with `Marek@Example.com ` (trailing space) | Accepted; account stored as `marek@example.com` |

## Demo accounts (local only)

`demo.trainer@example.com` (confirmed) and `demo.unconfirmed@example.com`, password `Kinetiq-demo-1`. They become useful for sign-in in story #18.
