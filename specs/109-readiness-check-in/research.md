# Research: Client checks in and sees readiness

## R1. A feature of its own

**Decision**: A backend feature `readiness` (aggregate `CheckIn`, value objects `Answers` and `Readiness`) and a frontend feature `readiness` (check-in card, readiness card, their API hooks).

**Why**: `CONTEXT.md` puts check-in, readiness, band and adjustment under their own heading, and `backend/CLAUDE.md` names `readiness` as a feature. Story #117 (adjustments) and #122 (the trainer's view) grow it.

**Rejected**: Putting the check-in in `sessionlog` (a check-in is given before a session exists and is not a log) or in `plan` (the plan is the trainer's prescription).

## R2. Dependency direction

**Decision**: `readiness` depends on `plan` through `ViewOwnPlanUseCase.today(client)` (today's date and state) and on `account` for `AccountId`. Whether a set is logged today is asked through an output port that `readiness` owns, `TrainingProgress.hasLoggedSet(client, date)`, implemented in `sessionlog.infrastructure.output` by `SessionLogTrainingProgress`. So `sessionlog → readiness`, and `readiness` never depends on `sessionlog`.

**Why**: Story #117 lets a session start with today's adjusted targets (#119, #120), so `sessionlog` will read `readiness`. If `readiness` called a `sessionlog` input port now, #117 would close a cycle that `HexagonalArchitectureTest.featuresAreFreeOfCycles` refuses. A port owned by the side that needs the fact keeps the one direction that the next story needs. The ArchUnit rules allow it: `sessionlog`'s infrastructure is used only by `sessionlog` (Spring injects it into `readiness`'s service by the port type).

**Rejected**: A query method on `LogSessionUseCase` (cycle in #117); a native query on `set_logs` from `readiness` (reads another feature's tables behind its back).

## R3. Endpoints

**Decision**: `GET /api/v1/check-ins/today` and `PUT /api/v1/check-ins/today`, role CLIENT. Both answer the same `TodayCheckInResponse`: `date`, `open` (a check-in can still be saved today), and `checkIn` (answers, score, band, `savedAt`) or none. `PUT` takes all three answers, creates or replaces today's check-in, and answers `200`.

**Why**: "Today" is computed from the sign-in and time zone, as `/api/v1/today` does, so no id or date is taken from the request (FR-006) and no other person's check-in can be named. `PUT` on one resource per day is idempotent by nature: a repeat stores the same answers (FR-004). A `GET` is needed because the Today tab must know, on load, whether to show the check-in, the card, or nothing. A rest day, a finished plan and a published plan with a logged set all read as `open: false`; the screen only asks on a session day.

**Rejected**: Adding the check-in to `GET /api/v1/today` (`plan` would depend on `readiness`, and `readiness` depends on `plan`: a cycle); `POST` plus `PATCH` (two operations for one action, and a repeat of the `POST` would need idempotency handling).

## R4. When saving is refused

**Decision**: The service reads `today(client)`: no published plan stays `PlanNotFoundException` (`404 plan.not-found`); `REST` and `FINISHED` are `NoSessionTodayException` (`409 check-in.no-session-today`); a logged set today is `TrainingStartedException` (`409 check-in.training-started`). Checked in that order, then the answers are stored.

**Why**: The spec (FR-005) and the boards: no check-in on a rest day, none after the first set. The code names the reason, `training-started`: `CONTEXT.md` keeps "Locked" for a session past its 24 hours (story #101) and avoids "Closed", so the check-in uses neither. The rules of "today" stay in `plan` (spec 89). `readiness` has its own code for the rest day so the frontend can translate it in readiness terms.

**Note**: A set logged at the same instant as a save can let both through. `// ponytail:` comment in the service; the window is milliseconds, the client is one person, and #117 recomputes on change. A lock or a constraint across features is not worth it.

## R5. Score and band

**Decision**: `Readiness.of(Answers)` in `readiness.domain.model`: `sum = (sleep − 1) + (5 − soreness) + (5 − stress)` (0–12), `score = Math.round(sum * 100 / 12.0)`, band `READY` ≥ 70, `TAKE_IT_EASIER` 45–69, `RECOVERY_DAY` < 45. Derived on every read; not stored.

**Why**: The mean of three quarters times 100 equals `100 × sum / 12`; `sum / 12 × 100` is never x.5, so the rounding mode does not matter (spec, Edge Cases). A pure function needs no storage, and nothing in this story or #122 needs a past score to survive a change of the rule (spec, Assumptions). The screen shows what the server computed (FR-009).

**Rejected**: Storing score and band (two columns that can disagree with the answers); computing in the frontend (two copies of a rule).

## R6. Validation

**Decision**: `SaveCheckInRequest` has `@NotNull @Min(1) @Max(5) Integer` for `sleep`, `soreness`, `stress` → `400 validation.failed` with `errors[{field, code}]` (`not-null`, `min`, `max`). The domain's `Answers` record checks 1–5 again and throws `InvalidValueException`. A body that cannot be read is `400 request.invalid`.

**Why**: The project's boundary rule (Bean Validation on bodies, invariants in domain constructors). Fractions such as `2.5` give `request.invalid`: Jackson would cut them to a whole number, so `spring.jackson.deserialization.accept-float-as-int` is off for the whole API.

## R7. Storage

**Decision**: Migration `V21__create_check_ins.sql`: table `check_ins` (`id`, `client_id` → `accounts`, `check_in_date`, `sleep`, `soreness`, `stress`, `saved_at`, `created_at`, `updated_at`, `version`), unique `(client_id, check_in_date)`, checks 1–5 on each answer. The repository port has `findByClientAndDate` and `save`.

**Why**: One row per client per day (FR-002); the constraint catches a race of two first saves. Same column conventions as `session_logs`.

**Note**: Two first saves racing for the same day: the second fails on the unique constraint and answers `500`. `// ponytail:` comment; one person, two tabs, the same millisecond. Retry on conflict if it is ever seen.

## R8. The Today tab composes two features

**Decision**: `features/plan/pages/today-page.tsx` takes an optional `beforeSession: ReactNode` and renders it above the session card when the state is `SESSION`. A new `app/today-route.tsx` renders `<TodayPage beforeSession={<ReadinessSection />} />`, and the router points `/app/today` at it. `ReadinessSection` (in `features/readiness`) loads `GET /check-ins/today` itself and shows the check-in, the skipped line, the card, or nothing.

**Why**: `frontend/CLAUDE.md` forbids a feature importing another; `app/` may compose features. The section needs nothing from the Today data: the server answers `open` and the check-in for today.

**Rejected**: Moving the Today page to `app/` (it is the plan's page); duplicating today's state in `readiness`.

## R9. Three radio groups where the arrows only move

**Decision**: shadcn `ToggleGroup` with `type="single"` per question, with `role="radiogroup"` and `aria-labelledby` on the root. Radix renders its single-type items as `role="radio"` with `aria-checked` and gives the group a roving tab stop: Tab enters on the chosen value (or the first), arrows move focus, Space or Enter chooses. An empty `onValueChange` (clicking the chosen value) is ignored, so an answer cannot be cleared by a second tap. Each item is 48 × 48 px (`--control-h-touch`), the chosen one shows the pressed style and `aria-checked`, and its name is its number; the scale ends are a visible line linked by `aria-describedby`.

**Why**: FR-012 and the clarification: arrows must not choose, because the first check-in saves on the third choice. Radix `RadioGroup` checks on arrow focus (the quiz uses it, where nothing saves on choice). Verify Radix's roles and key handling with the shadcn docs and in the browser before building on it.

**Rejected**: `RadioGroup` with intercepted arrow keys (fights the primitive); native `<input type="radio">` (also checks on arrows).

## R10. Saving

**Decision**: A first check-in calls the save mutation from the click handler that sets the third answer (not from an effect). While pending, the choices keep their values and stay focusable but are marked `aria-disabled` and ignore input (a `disabled` button that has focus would drop focus to the page), and the card shows "Saving…" (`role="status"`); on error the answers stay, an `Alert` shows `t(errorMessageKey(error))` and "Try again". Changing opens with the saved answers chosen and saves on "Done". On success the mutation writes the response into the `['check-in', 'today']` query.

**Why**: FR-011, FR-018. Saving from the event handler sends one request per completed check-in, not one per render.

## R11. Skip

**Decision**: `localStorage['kinetiq.checkIn.skippedOn'] = <date from the GET>`; skipped when it equals today's `date`. Every read and write is in `try/catch`; without storage the skip lasts until the page is left.

**Why**: The clarification (on the device until the day ends, nothing on the server). The server's `date` is the client's day, so the skip ends with it. A stale key is harmless.

## R12. Focus and announcements

**Decision**: When the check-in turns into the card (or back) because of the client, focus moves to the new card's heading (`tabIndex={-1}`); on first load focus is left alone; the score is read out through the card's `aria-live="polite"` region. "Change answers" sends focus to the check-in's title.

**Why**: FR-019; WCAG 2.4.3 (focus order) and 4.1.3 (status messages). The element that had focus disappears, so focus must be placed.

## R13. Freshness after logging

**Decision**: The check-in query uses the default `staleTime` (0), so returning to Today after logging a set in the logger refetches and shows `open: false`. A save refused with `check-in.training-started` invalidates the query.

**Why**: Scenarios 3.4–3.6 and the edge case of a set logged in another tab.

## R14. Copy and translation

**Decision**: Keys under `readiness.*` in `en.json` and `pl.json`: title "How do you feel today?", questions and scale ends, "Skip", "Saves after the third answer", "Saving…", "Done", "Cancel", "Change answers", "Check in", skipped line, "Readiness", the three band names and lines, the answers line with interpolation, the fixed-answers line, and errors `check-in.training-started` and `check-in.no-session-today`. Polish written by hand, not machine-translated; numbers formatted with `Intl.NumberFormat`.

## R15. Demo data

**Decision**: `R__demo_check_ins.sql` in `db/dev-data`: check-ins for Demo Client Daily on the three days before the first run (one per band, e.g. 4/2/1, 3/3/3, 2/4/3), none for today, `on conflict do nothing`.

**Why**: FR-023 and the Definition of Done. Today stays open for trying; past rows are what #122 will show.
