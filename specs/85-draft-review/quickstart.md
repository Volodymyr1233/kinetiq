# Quickstart: validating the story

Prerequisites: Docker running (PostgreSQL starts with the backend); `OPENROUTER_API_KEY` only for the one real-service run. Demo data (local profile) holds, for the demo trainer: a built draft, a failed draft, a draft the trainer saved, a client being drafted, a published plan, a second trainer with a draft, and a trainer close to the limit.

```bash
cd backend && ./gradlew check
cd frontend && npm run check
cd backend && ./gradlew bootRun
cd frontend && npm run dev
```

## Scenarios

1. **List.** Sign in as the demo trainer, open Approvals: the drafts are listed oldest first, each labelled "Plan to review", "Draft failed" or "Draft"; the published plan, the client being drafted for the first time and the other trainer's client are absent; the sidebar shows the count. With no drafts the empty state shows.
2. **Review.** Open a built draft: the panel shows the client's answers and notes; the note above the sessions is present; publishing removes the panel and the row.
3. **Build again.** Choose Build again on a built draft, confirm: the drafting line shows above the read-only draft and controls are disabled; after the stand-in answers (or the real service), the editor shows the new draft by itself and the client leaves "Plan being drafted".
4. **Failed rebuild.** With the service unconfigured or a failing stand-in: the editor returns to the previous draft, unchanged and with the same version, and says a new draft could not be built.
5. **Refusals.** A second request while drafting answers `409 plan.drafting`; the eleventh within 24 hours answers `429 plan.rebuild-limit-reached` with `availableAt`, and the button says when; another trainer's client answers `404` on all three operations.
6. **Stale tab.** Open the plan in two tabs, rebuild in one: saving in the other is refused as changed elsewhere.
7. **Security pass.** Call the three new endpoints without a sign-in (`401`), with a client's sign-in (`403`), and the `POST` without the CSRF header; check the JSON for names or text that should not be there; search the logs of a rebuild for client names, notes and prompts.
8. **Visual pass.** Approvals and the editor with the panel, light and dark, at 1280 px and at a narrow width; keyboard order and focus in the confirmation; screen reader announcement when the drafting ends.

Contracts: [contracts/api.md](contracts/api.md). Data: [data-model.md](data-model.md).
