# Implementation Plan: Client answers the quiz

**Branch**: `feat/52-quiz-question-components` (first task; later tasks use their own branches) | **Date**: 2026-10-23 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/50-client-quiz/spec.md`

## Summary

A seven-step quiz for new clients, entirely in the frontend. Five question patterns built from shadcn components (task #52). A page that shows one step per address, holds the answers in one react-hook-form form checked by zod, and keeps the draft in the tab's `sessionStorage` (task #51). Entry routing that sends a client without a submitted quiz to it (task #53), once the route guards (task #25) and the quiz API (task #55) exist. One pull request per task, squash merged; the first one also carries the Spec Kit files.

## Technical Context

**Language/Version**: TypeScript 6

**Primary Dependencies**: React 19, React Router 8, TanStack Query 5 (task #53 only), react-i18next, react-hook-form 7 with `@hookform/resolvers`, zod 4, shadcn/ui. No new dependency: the form libraries are already installed for sign-up. New shadcn component added through the CLI: `radio-group`.

**Storage**: Browser `sessionStorage`, key `kinetiq.quiz` (data-model.md). No server storage in this story.

**Testing**: Vitest + Testing Library + `userEvent`, jsdom's `sessionStorage`

**Target Platform**: Browser SPA, client screens at 375 px

**Project Type**: Web application; this story touches `frontend/` only

**Performance Goals**: Step changes are instant (local state only); SC-001: seven steps in under 2 minutes

**Constraints**: No answer leaves the browser before story #54 submits it. No answer in the address bar, only the step number. Kinetiq tokens only; 48 px controls, 64 px option cards.

**Scale/Scope**: 1 route, 5 question components, 1 page, about 70 strings per language

## Constitution Check

| Principle | Status |
|---|---|
| I. Hexagonal boundaries | Not affected: no backend code. |
| II. One contract | Pass. Tasks #52 and #51 call no API. Task #53 uses the types generated from task #55 and shows errors by `code` only (FR-026). |
| III. Secure by default | Pass. No endpoint. Health data (weight, notes) stays in the tab's storage, never in the address or in logs, and is removed on submit. Signing out, signing in and joining empty the tab; task #226 also empties a tab that another account used (Open items). |
| IV. Plan changes | Not affected. |
| V. Tests verify behaviour | Pass. The test list below covers the rules, every pattern's behaviour and the flow, not markup or styles. |
| VI. Simplicity | Pass. No new library: the quiz uses the project's form libraries, react-hook-form and zod (R2). No state library, no committed demo page (R11). |
| VII. One design system | Pass. shadcn components and tokens; EN and PL; browser check at 375 px, light and dark. |

Re-checked after design: unchanged.

### Definition of Done items that do not apply

- *Another person's resource answers 404*: the story has no endpoint, and the draft lives in the client's own tab.
- *New entities get demo data*: nothing is stored on the server.

## Project Structure

### Documentation (this feature)

```text
specs/50-client-quiz/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/routes.md
├── checklists/requirements.md
└── tasks.md             (/speckit-tasks)
```

### Source Code

```text
frontend/src/
├── app/router.ts                                  /app/quiz/:step? → quiz page (#51)
├── components/ui/radio-group.tsx                  shadcn CLI (#52)
├── features/quiz/
│   ├── quiz-schema.ts (+ .test.ts)                codes, rules, number parsing, draft loading, which step may open (#51)
│   ├── components/
│   │   ├── choice-card.tsx                        the card shared by single and multiple choice (#52)
│   │   ├── single-choice.tsx (+ .test.tsx)        radio choice cards (#52)
│   │   ├── multiple-choice.tsx (+ .test.tsx)      checkbox choice cards (#52)
│   │   ├── number-field.tsx (+ .test.tsx)         number with unit and range message (#52)
│   │   ├── stepper.tsx (+ .test.tsx)              − / number / + (#52)
│   │   └── notes-field.tsx (+ .test.tsx)          free text with a character count (#52)
│   ├── quiz-draft.ts                              read and write the draft in sessionStorage (#51)
│   └── pages/quiz-page.tsx (+ .test.tsx)          steps, progress, Back, Continue, focus, step guard (#51)
└── locales/en.json, pl.json                       quiz.* (#52 for the character count, #51 for the steps)
```

Task #53 adds a quiz status hook in `features/quiz/api/` and a check in `app/` around the client routes. Its exact files depend on task #25 and the quiz status read of story #54, and are planned when they are merged.

Task #226 (FR-020) ties the tab's storage to the signed-in account in the area guard (`app/guards.ts`, with `claimTabStorage` beside `clearTabStorage` in `features/account/api/account.ts`), so the quiz feature needs no account id.

Task #216 (FR-031) moves the client's settings sheet from `app/client-area.tsx` to `app/settings-sheet.tsx` and adds `app/quiz-area.tsx`, which renders the quiz pages with the sheet's button: a feature imports neither `app/` nor another feature (`frontend/CLAUDE.md`), so the quiz pages take an optional `settings` node for their header. The quiz steps and the review are keyed by the weight unit there, so a unit changed in the sheet reloads the draft, converted (story #42), instead of keeping a number typed in the old unit.

**Structure Decision**: One feature, `quiz`, as `CONTEXT.md` names it. The question patterns are used only by the quiz, so they stay in the feature, not in `components/`.

## Work per task (one pull request each, squash merged)

| Task | Pull request title | Contents |
|---|---|---|
| #52 | `feat(frontend): add quiz question components` | Spec Kit files of story #50; `radio-group` from the CLI; five components with tests; their own string (the character count) in EN and PL; browser check through a throwaway page (R11). The components only show what they are given; the rules come with #51 |
| #51 | `feat(frontend): add quiz step flow` | `quiz-schema.ts` with tests, `quiz-draft.ts`, `quiz-page.tsx` with tests, the route, every step's strings in EN and PL, browser check per quickstart.md |
| #53 | `feat(frontend): send clients to the quiz until submitted` | Status hook, entry check with loading and error states, tests per case of US3 |
| #216 | `feat(frontend): add settings to the quiz and the waiting state` | The settings sheet in its own `app/` file, `quiz-area.tsx` and its routes, the `settings` place in the quiz pages' headers, tests through the app's routes, browser check per quickstart.md |
| #226 | `feat(frontend): tie quiz answers to the signed-in account` | The tab's storage tied to the account in the area guard (Open items), a guard test for FR-020, browser check per quickstart.md |
| #228 | `docs(docs): bring the quiz's quickstart and data model up to date` | The findings of the story's convergence check (`tasks.md` Phase 8): the quickstart's check of the step flow and the data model's lifecycle follow the merged quiz |

Task order: #52, #51, then #216 once #57 is merged, then #53 once #25, #57, #216 and the quiz status read of story #54 are merged; #226 at any time after #22, #24 and #25; #228 after the convergence check that found it.

## Tests

| Test | Covers |
|---|---|
| `quiz-schema.test.ts` | Age and height bounds 16/99 and 120/230 inclusive, decimals refused; weight 30–250 with one decimal, comma or dot, two decimals refused; spaces trimmed; likes need one and refuse duplicates and unknown codes; days 2–6; notes trimmed, 500 units allowed, 501 refused; loading keeps valid fields and drops each invalid one; which step may open |
| `single-choice.test.tsx` | Radio group named by the heading; choosing marks one and clears the other; nothing chosen at first; descriptions shown (arrow keys are Radix's behaviour, checked in the browser) |
| `multiple-choice.test.tsx` | Checkbox group named by the heading; options toggle independently |
| `number-field.test.tsx` | Label and unit announced; a given message is shown, linked by `aria-describedby` with `aria-invalid`; the note shows when there is no message; leaving the field reports it (`onBlur`) |
| `stepper.test.tsx` | − and + change by one; marked disabled at both bounds and no change past them, focus kept; new value announced; accessible names |
| `notes-field.test.tsx` | Counter follows the text; typing stops at the limit; pasting longer text keeps the first 500 |
| `router.test.tsx` (#226) | Quiz answers left in the tab by another account are not shown; the own account's answers are |
| `quiz-area.test.tsx` (#216) | On a quiz step and on the waiting state, Settings opens the sheet and "Sign out" ends the sign-in, clears the draft and opens sign-in; a unit changed in the sheet on step 4 shows the typed weight in the new unit |
| `quiz-page.test.tsx` | Step 1 with no Back and Continue disabled; Continue enabled only when valid; a number field's message appears after leaving it, disappears once valid and is still there after going back and returning; answers kept through Continue, Back and remount (reload); addresses beyond the furthest step or past an invalid step go back to the allowed step; step 6 cannot be skipped by address; focus on the new heading; step 7 button text; stored invalid answer dropped; broken stored data gives a fresh start; Polish strings and number example |

## Open items for later tasks

| Item | Task | How it is settled |
|---|---|---|
| Tie the draft to the signed-in account (FR-020) | #226 | Settled when #53 started, as task #226: the area guard keeps the account id in the tab and empties a tab that another account used (`claimTabStorage`), before any page reads the quiz draft; the quiz feature imports nothing from `features/account` |
| Remove the draft on sign-out (FR-019) | #24 | Settled by task #24: signing out empties the tab's storage, as signing in and joining (task #33) do |
| Endpoint and fields of the quiz status, and whether #55 or #57 builds it | Story #54 | Defined in the spec of story #54 |

## Complexity Tracking

None.
