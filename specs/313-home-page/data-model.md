# Data Model: Visitor sees what Kinetiq does and signs up

No stored data. The page reads only the current account (`GET /api/v1/accounts/me`), as the sign-in page does, to decide where the person lands.

| Concept | Source | Use |
|---|---|---|
| Visitor | No current account (401) or a failed check | Sees the page |
| Current account role | `CurrentAccount.role` | `TRAINER` → `/trainer/clients`, `CLIENT` → `/app/today` |
| Language | `kinetiq-language` in browser storage | Footer choice, as on the account pages |
| Theme | `kinetiq-theme` in browser storage | Footer choice, as on the account pages |

The mock-ups hold made-up example names and figures in the locale files; they are not data.
