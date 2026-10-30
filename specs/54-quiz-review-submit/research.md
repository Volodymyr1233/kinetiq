# Research: Client reviews and submits the quiz

Each entry: decision, reason, alternatives. Entries for tasks #55 and #57 are refined when their blockers are merged; the frontend of task #56 is final.

## R1 — The review is its own route and page

**Decision**: `/app/quiz/review` is a static route beside `/app/quiz/:step?`, loading a `ReviewPage` in `features/quiz/pages/`. React Router ranks the static segment above the dynamic one. The page reads the draft with `loadDraft()` (spec 50, research R3) and does not hold a form.

**Why**: The review shows answers; it edits nothing. The draft in the tab is the single source of the answers, already saved on every change by the step flow, so a second form would only copy it. A static route also ends spec 50's fallback of `/app/quiz/review` to step 7 (spec 50, research R12).

**Alternatives**: the review as an eighth "step" inside `QuizPage` (mixes a read-only screen into the form page); a query parameter (`/app/quiz/7?review`) (an address that does not say what it shows).

## R2 — Which drafts may open the review

**Decision**: `quiz-schema.ts` gets `isComplete(draft)`: `reached` is 7 and steps 1–6 are valid (step 7 is always valid). When it is false, the review page redirects with `replace` to `allowedStep(undefined, draft)`, the step the quiz may open.

**Why**: Spec FR-005 reuses spec 50's rule (FR-021); `allowedStep` already computes the step to open, so the review adds one boolean and no new rule.

## R3 — Change, Continue and Back between the review and a step

**Decision**: "Change" is a `Link` to `/app/quiz/<n>` with `state: { fromReview: true }`. On a step opened that way, Continue and Back both call `navigate(-1)`, returning to the review entry in the browser history; Continue stays disabled while the step is invalid. Back on the review calls `navigate(-1)` when it came from step 7 (`state.fromStep === 7`), otherwise replaces the address with `/app/quiz/7`.

**Why**: Spec FR-003 and FR-004, and the design system's rule that Back is the browser back. Popping the history keeps the device's back and the on-screen Back the same, so neither leaves a duplicate review entry behind.

**Alternatives**: pushing `/app/quiz/review` on Continue (a second review entry in history, so the device's back would show the step again).

## R4 — How the review shows the answers

**Decision**: A description list (`dl`): each row a `dt` label and a `dd` with the value and the "Change" link. Option titles come from the existing `quiz.<step>.options.<CODE>.title` keys. "About you" is one translated pattern whose numbers use i18next's `number` format (`Intl.NumberFormat` for the UI language), as the exercise library's counts do, so the weight reads "62.5" or "62,5". Likes are joined in screen order with a comma, as on the board ("Weights, Yoga and Pilates"). Empty notes show `quiz.review.noNotes`. Each link has its own translated accessible name ("Change goal").

**Why**: Spec FR-002 and User Story 1, scenario 2; `frontend/CLAUDE.md` asks for numbers formatted for the locale and interpolation instead of concatenated sentences. Option titles contain "and" themselves ("Yoga and Pilates", "HIIT and circuits"), so a list joined with a conjunction would read "Weights and Yoga and Pilates", as if there were three; a comma separates list items in both languages.

**Alternatives**: `Intl.ListFormat` (adds that conjunction, "and" or "i").

## R5 — "Build my plan" before task #57

**Decision**: Task #56 renders "Build my plan" disabled. Task #57 gives it its action.

**Why**: Submitting needs the API of task #55. A visible but disabled button keeps the screen as on the board without a control that silently does nothing.

**Alternatives**: hiding the button until #57 (the screen would lack its one forward action); an enabled button with no action (looks broken).

## R6 — One resource, "my quiz", for submit and status (task #55)

**Decision**: `GET /api/v1/quiz` answers the signed-in client's status; `POST /api/v1/quiz` submits the answers and answers `201` with the same status body. The status is one enum: `NOT_SUBMITTED`, `WAITING_FOR_PLAN`, `PLAN_PUBLISHED`. Until plans exist the service never returns `PLAN_PUBLISHED` (spec FR-011). Shape in contracts/api.md.

**Why**: The quiz is always the signed-in client's own (spec FR-010), so the address needs no id and nothing can address another client's quiz. One enum is easier for tasks #57 and #53 than two booleans whose combinations need explaining.

## R7 — Only clients reach the quiz, refused by role (task #55)

**Decision**: `SecurityConfig` requires the role `CLIENT` for `/api/v1/quiz/**`, the way the sign-in story (#18) requires `TRAINER` for `/api/v1/exercises/**`. Another role gets `403` with code `request.not-allowed`; no session gets `401`. The use case reads the client from the session's principal, as `CurrentAccountController` of that story does.

**Why**: The rule "answer 404 for another person's resource" (`CLAUDE.md`) applies to a resource of another person; a trainer asking for "my quiz" asks for an endpoint of another role, which the project answers by role (spec FR-010).

## R8 — "Already submitted" as a conflict (task #55)

**Decision**: `QuizAlreadySubmittedException` is a `DomainException` of kind `CONFLICT` with code `quiz.already-submitted`, so it answers `409`. The frontend reacts to the code, not the status.

**Why**: `DomainException.Kind` has `CONFLICT` (`409`), and the refusals of something that already exists use it: `exercise.name-taken` (story #47) and an invite for an existing account (story #26). An existing submission is the same kind of refusal. The code is what the contract promises (spec FR-009).

**Alternatives**: kind `INVALID` answering `400` (chosen when this story was planned, before `common` had a conflict kind).

## R9 — One submission per client, enforced by the database (task #55)

**Decision**: Table `quiz_submissions` has a unique constraint on `client_id`. The service refuses a client who already has a submission; when two submits race past that check, the adapter turns the unique-constraint violation into `QuizAlreadySubmittedException`. Likes are a child table with a primary key on (submission, like), so a like appears once.

**Why**: Spec FR-009 and Clarifications: storage itself allows one submission per client. A check in code alone loses a race between two requests.

## R10 — Status read and refetch (task #57)

**Decision**: `useQuizStatus()` is a TanStack Query query; its defaults refetch when the window regains focus, which gives spec FR-016's "check again when the client returns" without code. No `refetchInterval`.

**Why**: Spec Clarifications: re-check on reload and return, no polling.

## R11 — Draft removal after submit (task #57)

**Decision**: `quiz-draft.ts` gets `clearDraft()`, called in the submit mutation's `onSuccess` before navigating to the waiting state.

**Why**: Spec FR-012 and spec 50's contract ("Task #57 removes it after a successful submit").

## R12 — The quiz pages follow the quiz status (task #57)

**Decision**: The quiz steps, the review and the waiting page sit inside one status check in `features/quiz/`, built on `useQuizStatus()`. A client who submitted is sent from a step or the review to `/app/quiz/waiting`; a client who has not is sent from the waiting page to `/app/quiz`; a published plan sends either to `/app`. While the status loads, a neutral loading state shows; on failure, `t(errorMessageKey(error))` and "Try again". Task #53 checks the client area and the landing after sign-in.

**Why**: Spec FR-016 and User Story 3, scenario 3: a client who submitted gets the waiting state instead of the quiz or the review. With the check in this task, that holds when story #54 closes, not only after task #53 of story #50. The quiz feature owns the three pages, and the check shows a failed status read by its code (FR-016).

**Alternatives**: a route loader in `app/guards.ts`, like the role check (its failure shows the area's error page, which always shows the generic message); leaving every check to task #53 (story #54 would close without User Story 3, scenario 3).
