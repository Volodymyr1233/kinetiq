# Quickstart: Client joins through an invite

## Run

```bash
cd backend && ./gradlew bootRun
cd frontend && npm run dev
```

Demo accounts use the password from the README. Demo invite secrets are `demo-invite-<n>-email` and `demo-invite-<n>-link` (`R__demo_invites.sql`): 1 open (today), 2 open (last day), 3 expired, 4 revoked, 5 open (another trainer). Each works once; recreate the database for fresh ones, or invite a new email as `demo.trainer@example.com` and copy the link from the dialog or from the backend log.

## Scenarios

| # | Do | Expect |
|---|---|---|
| 1 | Open `http://localhost:5173/join#token=demo-invite-1-email` signed out, at 375px | "Demo Trainer invited you", "Join Demo on Kinetiq", `julia.nowicka@example.com` read-only |
| 2 | Submit empty | Name, password, age and consent messages; focus on name |
| 3 | Fill valid details, submit | "You're in, {first name}" with "Start the quiz"; `GET /api/v1/accounts/me` shows `CLIENT`, `emailConfirmed: true`, `units` of the demo trainer; no confirmation mail in the log |
| 4 | "Start the quiz" | Quiz first step |
| 5 | Open Today | No banner |
| 6 | Reload the join page from scenario 3 | "This link no longer works"; "Sign in instead" lands in `/app` |
| 7 | Open tokens `demo-invite-3-email`, `demo-invite-4-email`, `nonsense` | Same "This link no longer works" page each time |
| 8 | Invite a new email, copy the dialog's link, open it in a private window, join | "You're in"; mail log shows a `confirm-email` link to that address |
| 9 | Go to Today | Banner "Confirm your email: we sent a link to …" with "Resend"; tabs and settings all work |
| 10 | "Resend" | Button "Sent", disabled; screen reader hears "We sent a new link."; no second mail within 60 s |
| 11 | Open the confirmation link in another tab | "Email confirmed" with "Continue"; back on the app tab the banner is gone |
| 12 | Open the form of an open invite, revoke it as the trainer, then submit | "This link no longer works", nothing created |
| 13 | Join while signed in as the demo trainer | Trainer sign-in replaced by the new client's |
| 14 | Repeat 1–11 in Polish and dark theme; scenario 1 at 1280px | All copy in Polish, no horizontal scroll, visible focus |

## Checks

```bash
cd backend && ./gradlew check
cd frontend && npm run gen:api && npm run check
```

Contract: [contracts/api.md](contracts/api.md). Data: [data-model.md](data-model.md).
