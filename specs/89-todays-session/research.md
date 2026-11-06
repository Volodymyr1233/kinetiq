# Research: Client sees today's session

## R1. Where the read lives

**Decision**: A `today(client)` method on the existing `ViewOwnPlanUseCase`, implemented in `PlanViewService`, with its own controller and response.

**Why**: It needs the same three things the week view reads: the client's published plan, their time zone, and their trainer's library for names. A second use case would repeat all three. The date helper is shared with `view()`.

**Rejected**: A new `ViewTodayUseCase` (a port and a service for one method); a `today` query on the week endpoint (the week view and Today answer different questions and have different states).

## R2. Address

**Decision**: `GET /api/v1/today`, covered by the role CLIENT rule for `/api/v1/today/**`.

**Why**: Same reasoning as `/api/v1/plan` (spec 75, R2): it is the signed-in client's own, so no id is in the path.

## R3. States

**Decision**: The answer has the date and a `state` of `SESSION`, `REST` or `FINISHED`. `SESSION` carries the session; the others carry none. A published plan always yields one of the three; a draft and no plan are `plan.not-found` (404), before anything is read.

**Why**: The screen draws exactly three things; each is a state with no extra flags. Returning the session for a finished plan would invite the screen to show it.

**Rejected**: A nullable session with a boolean `finished` (two flags that can disagree).

## R4. What is today

**Decision**: `clock.instant()` in the client's time zone, converted to a date; the session is the plan's session on that weekday; finished is `currentWeek > weeks`, as in the week view.

**Why**: Weeks are Monday to Sunday in the client's time zone (`CONTEXT.md`). The last Sunday of the last week is still a normal day, so the plan ends the Monday after. A fixed clock tests the boundary.

## R5. The waiting state

**Decision**: Not drawn on the Today tab. A `plan.not-found` from this endpoint renders the tab's "no plan" line, the same as the Plan tab, and the area's routing (spec 50) decides where the client belongs.

**Why**: A client without a published plan never reaches the tabs; the answer is only defensive.

## R6. "Start session"

**Decision**: A `Button` (size `touch`, the one lime action of the view) that is `disabled`, with a visible line under it saying starting arrives with logging.

**Why**: The logger is task #95 of another story. A link would lead nowhere; hiding the button would hide where training starts. Story #92 replaces `disabled` with a link.

**Rejected**: Hiding the action; linking to a stub page.

## R7. The entry row

**Decision**: Extract the exercise name and targets line from `session-card.tsx` into `entry-row.tsx` and use it in both places.

**Why**: The same entry must read the same on both screens (seconds, no load, unit conversion). One component keeps them from drifting.

## R8. Refresh

**Decision**: No timers. TanStack Query refetches on window focus, which is enough to move to the next day when the client returns to the tab.

**Why**: Smallest thing that satisfies the spec (edge case: not live).

## R9. Demo data

**Decision**: Add `Demo Client Daily`, whose plan has a session on Monday to Saturday, so Today shows a session on every day but Sunday. The seen client (Tuesday and Friday) shows a rest day on most days, and the finished client shows the finished state.

**Why**: Repeatable demo SQL cannot depend on the date it is read, and a plan needs at least two sessions on distinct days. Six sessions give a session on six days of seven.
