# Research: Client sees what comes next on a rest day

## R1. Where the next session is found

**Decision**: In `PlanViewService.today`, for a rest day: walk the days after today up to the Sunday of the plan's last week and take the first with a session (a plan has at most one session a day). The result is the session and its date.

**Why**: The week model is "sessions repeat every week" (spec `68-plan-editor`), so the date follows from the weekday and today; the plan's last day bounds it. A loop over at most 7 x 16 days is trivial.

**Alternatives**: Asking the Plan tab's week endpoint from the screen (several requests and a client-side search across weeks); rejected.

## R2. Where the estimated minutes are computed

**Decision**: On the screen, with the plan editor's formula extracted from `sessionTotals` into `estimatedMinutes` and used by both. The answer carries the next session's entries.

**Why**: The spec says "the way the plan editor estimates a session"; a second formula on the server would drift. The entries are in the same shape as the session of a session day, so the mapper is reused.

**Alternatives**: Minutes on the server (a duplicate of `SECONDS_PER_REP` and the formula); rejected.

## R3. The count of done sessions

**Decision**: Reuse `useSessionStates()` (current week) which Today already calls, and `useOwnPlan()` for the number of sessions in a week; the line reads "<done> of <planned> sessions done this week". It is hidden when the states fail.

**Why**: No new request or endpoint; the Plan tab shows the same count (`plan.client.weekDone`), so the string and plural forms are reused where they fit.

## R4. "See this week"

**Decision**: A `Button asChild` with a `Link` to `/app/plan`.

**Why**: The Plan tab opens on the current week without a parameter.

## R5. The check-in line

**Decision**: Plain text "No session today. No check-in either." The check-in itself is story #109.
