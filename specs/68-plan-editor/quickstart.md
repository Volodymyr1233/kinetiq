# Quickstart: validating the plan editor

## Prerequisites

Docker running. Backend: `./gradlew bootRun` in `backend/`. Frontend: `npm run dev` in `frontend/`. Sign in as `demo.trainer@example.com`; passwords are in `R__demo_accounts.sql`.

## Quality gates

- `./gradlew check` in `backend/`
- `npm run check` in `frontend/`

## Browser check

At 1280 px and 375 px, light and dark, English and Polish. At 375 px and 320 px the table scrolls inside its box and the page does not scroll sideways.

| # | Do | Expect |
|---|---|---|
| 1 | Open Demo Client's Plan tab (no quiz) | One line saying the quiz is not answered; no "Build plan" |
| 2 | Open Demo Client Waiting's Plan tab | "Build plan" |
| 3 | Choose it, add two sessions, add exercises with search and a category filter | Goal from the quiz, 12 weeks; rows carry defaults, RPE 8, empty load; a timed exercise shows seconds |
| 4 | Edit a target, switch session and back | The edit stays; "Unsaved changes" shows |
| 5 | Choose another client tab, then Stay | Question appears, focus on Stay, edits still there |
| 6 | Reload | The browser's own warning |
| 7 | Enter RPE 11, leave the field, then Save | Field invalid with a fix; the summary links to the field; session marked by more than colour |
| 8 | Fix it, move a row up and down with the keyboard | Focus stays on a control; the move is announced |
| 9 | Delete a row | Confirmation, focus on Cancel |
| 10 | Save, reload | Everything as saved; the client list shows Draft |
| 11 | Empty a session, choose Publish | Summary lists it; no dialog |
| 12 | Fix it, Publish, confirm | "Plan published"; week 1 of N; only "Save plan" remains |
| 13 | Sign in as Demo Client Waiting | Status is published, no waiting screen |
| 14 | Open the same plan in two tabs, save both | The second is refused with a message; its edits stay |
| 15 | On the published demo plan (week 6 or later), set length to 4 and save | Refused, with a message naming the current week |
| 15a | With unsaved changes, choose Sign out, then Stay | Question first; still signed in, edits still there |
| 16 | Open the library | "Plans" column shows real counts |
| 17 | As Demo Trainer Two, open a Demo Trainer client's plan address | Not found |
| 18 | Console | No errors except the expected network 404 of the empty state |

## API checks covered by tests

Foreign client 404 on all three operations, client role 403, publish twice 409, stale version 409, time zone boundary with a fixed clock, every rule of the data model's table.
