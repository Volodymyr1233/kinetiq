# Research: Client sees their trainer and a draft summary while waiting for a plan

Decisions taken while planning. Each: what, why, what else was considered.

## R1. Where the trainer's name comes from

**Decision**: From `name` in `GET /api/v1/conversations`, read in `app/quiz-area.tsx` with the existing `conversationsQuery` and passed to the waiting page as a prop. The avatar is initials (`AccountAvatar`), so the name is all it needs.

**Why**: For a client the list already holds their trainer, whether or not a quiz, a plan or a message exists (spec 58), so #248 changes no API. `features/quiz` must not import `features/messaging`, and the app already joins them for the message button. The read has no refresh interval, so a closed sheet polls nothing; an open sheet shares the same cache entry, so a name that failed at first arrives there too.

**Rejected**: A trainer field on the quiz status or the account (an API change for data the client can already read); a read inside `features/quiz` (a second way to the same data).

## R2. The message button keeps no name

**Decision**: The button stays "Message your trainer" and "Napisz do trenera", in both languages, unlike the board's "Message Marek".

**Why**: Polish needs the genitive after "do", and an account holds one name, with no declined form and no gender. The app declines no name anywhere, and the text just above the button names the trainer, so the name on the button adds little. Keeping it out means no code at all on the button, and no wrong form of a person's name.

**Rejected**: A genitive worked out from the name's ending (about 40 lines and a long test for one label on a screen a client sees only before their first plan, and still wrong for some names); a table of Polish names (names outside it lose the form, and each new one is a code change); a declined form stored on the account (a migration and new fields in sign-up and settings, for one button); "Napisz do: Marek" (reads poorly on a button); the name in English only (two languages behaving differently for no gain, and a Polish text that silently drops a value).

## R3. One `firstName`

**Decision**: `firstName` moves from `conversation.tsx` and `join-page.tsx` to `lib/first-name.ts`, and both import it.

**Why**: The waiting page needs it too. `frontend/CLAUDE.md`: "A feature imports from `components/`, `lib/` and its own folder. It does not import from another feature; shared code moves up." The behaviour of both screens stays as it is.

## R4. Address and answers of the summary

**Decision**: `GET /api/v1/plan/draft-summary`, `200` with `trainingDays`, `weeks` and `goal`. A failed draft, a draft without sessions, no plan and a published plan all throw `PlanNotFoundException` (`plan.not-found`, 404), before anything of the plan is read for the answer.

**Why**: `/api/v1/plan/**` already needs role CLIENT and takes the client from the sign-in, and 404 with this code is how the plan answers "nothing for you" (spec 75, FR-003). A 404 is not retried (`isWorthRetrying`), and the screen treats it like a failed read: no card.

**Rejected**: `204` (a second way to say "nothing" beside the plan's 404); a field on the quiz status (spec 79, FR-003 keeps the status free of the draft's content); a new use case port (`ViewOwnPlanUseCase` is the client's read of their own plan, and `backend/CLAUDE.md` allows a port to group closely related operations).

## R5. Where the card lives

**Decision**: `DraftCard` in `features/plan/components`, with `useDraftSummary` in `features/plan/api/client-plan.ts`. `app/quiz-area.tsx` passes it to `WaitingPage` as a slot, shown only on the reviewing state.

**Why**: The summary, the day names and the goal's words belong to the plan side, and a feature does not import another feature. Because the slot renders only on the reviewing state, the card mounts and reads the summary at the move from building to reviewing (FR-017), and needs no flag for it. The card shows nothing until it has data, so loading, no summary and a failed read look the same (FR-016).

**Rejected**: Reading the summary in `app/quiz-area.tsx` (it would run during the building state too, when no draft exists); a card in `features/quiz` (it would need the plan's helpers).

## R6. No refresh of the card

**Decision**: The summary is read on mount and when the query library reads again by itself, such as on return to the tab. It has no refresh interval.

**Why**: Checking stops once the reviewing state shows (spec 79, FR-027). A trainer's edit is rare, and the card already says the values can change.
