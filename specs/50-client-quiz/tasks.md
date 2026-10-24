---
description: "Tasks for story #50, Client answers the quiz"
---

# Tasks: Client answers the quiz

**Input**: `specs/50-client-quiz/` (spec.md, plan.md, research.md, data-model.md, contracts/routes.md, quickstart.md)

**Tests**: Required by the Definition of Done (`CLAUDE.md`) and principle V. Tests sit in the same phase as the code they cover.

**Organization**: Phases follow the GitHub tasks, not the spec's user stories, because each task is its own pull request, squash merged (`CLAUDE.md`, Workflow). #52 builds the question patterns (US1), #51 the step flow (US1, US2), #53 the entry routing (US3). Story labels show which spec story a task serves.

**Paths**: `F` = `frontend/src`, `Q` = `frontend/src/features/quiz`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: spec user story (US1 answer seven steps, US2 answers kept on Back and reload, US3 sent to the quiz until submitted)

---

## Phase 1: Setup

- [X] T001 Commit `specs/50-client-quiz/` (spec, checklist, plan, research, data model, contract, quickstart, tasks) with the code of task #52, so the spec is reviewed in that pull request. `CONTEXT.md` needs no new term: "Quiz" is already defined

---

## Phase 2: task #52, quiz question components (US1)

**Goal**: Five accessible, controlled question patterns, each with component tests. No screen uses them yet; they show what they are given and hold no quiz rules.

- [X] T002 Fetch the docs with `npx shadcn@latest docs radio-group`, `checkbox`, `field`, `input-group`, `textarea` and `button`, then add the radio group in `frontend/` with `npx shadcn@latest add radio-group`. Read `F/components/ui/radio-group.tsx` as added and do not edit it
- [X] T003 [P] Add `quiz.characterCount` to `F/locales/en.json` ("{{count}} of {{max}} characters") and `F/locales/pl.json` ("{{count}} z {{max}} znaków")
- [X] T004 [P] [US1] Create `Q/components/single-choice.tsx`: `SingleChoice<T extends string>({ labelledBy, options: { value: T; title: string; description?: string }[], value?: T, onChange(value: T) })`. shadcn `RadioGroup` with `aria-labelledby={labelledBy}`; each option a choice card (research R6): `FieldLabel` wrapping `Field orientation="horizontal"` with `FieldContent` (`FieldTitle`, optional `FieldDescription`) and a `RadioGroupItem` with its own `id`; whole card clickable, at least 64 px high (`min-h-16`), chosen card marked by the filled radio and a stronger border (not colour alone). Tokens only
- [X] T005 [P] [US1] Write `Q/components/single-choice.test.tsx`: the group is a `radiogroup` named by the labelling element; options are `radio`s with their titles and descriptions; clicking a card checks it and unchecks the previous one, calling `onChange` with the code; nothing is checked when `value` is undefined. Arrow keys are Radix's own behaviour (`frontend/CLAUDE.md`: no tests of library behaviour), so they are checked in the browser (T014), not here
- [X] T006 [P] [US1] Create `Q/components/multiple-choice.tsx`: `MultipleChoice<T extends string>({ labelledBy, options, value: T[], onChange(value: T[]) })`. A `div role="group" aria-labelledby={labelledBy}` with the same choice cards as T004 but a `Checkbox`; toggling adds or removes the code, keeping the options' order
- [X] T007 [P] [US1] Write `Q/components/multiple-choice.test.tsx`: a `group` named by the labelling element holds one `checkbox` per option; options toggle independently; clearing the last one calls `onChange([])`. Space on a checkbox is Radix's own behaviour, checked in the browser (T014)
- [X] T008 [P] [US1] Create `Q/components/number-field.tsx`: `NumberField({ id, label, unit, placeholder, inputMode: 'numeric' | 'decimal', value: string, onChange(value: string), onBlur(), message?: string, note?: string })`, shaped to take a react-hook-form `Controller` field (research R2). `Field` with `FieldLabel`; `InputGroup` (height `--control-h-touch`, text 16 px) with `InputGroupInput type="text"` and `inputMode`, and the unit in an `InputGroupAddon align="inline-end"` with an id, listed in the input's `aria-describedby`. When `message` is given: `FieldError` with it, `aria-invalid="true"`, its id added to `aria-describedby`; otherwise `note` as `FieldDescription` when given. The field decides nothing about when a message shows (research R7)
- [X] T009 [P] [US1] Write `Q/components/number-field.test.tsx`: the textbox is named by its label and described by the unit; a given message is shown, linked by `aria-describedby`, with `aria-invalid="true"`; without a message the note shows and `aria-invalid` is absent; typing calls `onChange` with the text as typed; leaving the field calls `onBlur`
- [X] T010 [P] [US1] Create `Q/components/stepper.tsx`: `Stepper({ labelledBy, value, min, max, unit, decreaseLabel, increaseLabel, onChange(value: number) })`. A `div role="group" aria-labelledby={labelledBy}`; two outline `Button`s, 48 px square, with `aria-label`s `decreaseLabel` and `increaseLabel` and Lucide `Minus` and `Plus` icons (`aria-hidden`), marked `aria-disabled` at `min` and `max` with the press ignored there (not `disabled`: a button that disables itself would drop the keyboard focus of the person pressing it); between them the number in an `<output aria-live="polite">` with the `num` class, large, and `unit` below it
- [X] T011 [P] [US1] Write `Q/components/stepper.test.tsx`: the buttons carry their accessible names; + and − call `onChange` with one more or less; − is marked disabled at `min` and + at `max`, the number does not change past them, and the pressed button keeps focus; the number sits in a live `status` region
- [X] T012 [P] [US1] Create `Q/components/notes-field.tsx`: `NotesField({ id, label, placeholder, value, maxLength, onChange(value: string) })`. `Field` with `FieldLabel`, `Textarea` with `maxLength` and `aria-describedby` pointing to a `FieldDescription` showing `t('quiz.characterCount', { count: value.length, max: maxLength })`. A paste longer than the limit keeps the first `maxLength` units (the browser's `maxLength` does this; cut in `onChange` too, so a value set by script never exceeds it)
- [X] T013 [P] [US1] Write `Q/components/notes-field.test.tsx`: the textbox is named by its label and described by the counter; the counter follows typing ("9 of 500 characters"); typing stops at the limit; pasting a longer text keeps the first `maxLength` characters
- [X] T014 [US1] Verify in the browser per research R11: add a throwaway page rendering each pattern with sample props (single choice of five, multiple choice of five, the three number fields with and without a message, the stepper at 2, 3 and 6, the notes field) on a temporary route; check 375 px, light and dark, keyboard and focus rings, 48 px and 64 px targets, console; take screenshots; remove the page and the route and confirm with `git status` that neither is left. Done in two passes; the second covered the full list: goal (five, with descriptions) and experience (six, without), likes (five), age, height and weight each shown invalid after leaving the field and cleared at once when fixed ("62,5" valid, "62,55" not), steppers from 3 and from 6 driven by keyboard to both bounds with focus kept, notes typed and given 600 characters (500 kept), English and Polish counter, light and dark, no horizontal scroll, cards 66 px or more, fields and step buttons 48 px, no console errors after a reload
- [X] T015 Run `npm run check` in `frontend/`; open the pull request for #52 after the user agrees (title `feat(frontend): add quiz question components`, `Closes #52`)

---

## Phase 3: task #51, quiz step flow (US1, US2)

**Goal**: `/app/quiz/1` … `/app/quiz/7` show the seven steps as on the board, with the rules of data-model.md, the draft kept in the tab, Back and the device's back between steps, and the step guard.

- [X] T016 [P] [US1] [US2] Create `Q/quiz-schema.ts`: the code lists of data-model.md "Option codes" as `as const` arrays with their types; `parseWholeNumber(text)` (trim, `^\d+$`, else undefined) and `parseWeight(text)` (trim, `^\d+([.,]\d)?$`, comma read as a dot, else undefined); field rules verbatim from data-model.md "Quiz draft": age "trimmed, digits only, 16–99", height "trimmed, digits only, 120–230 (cm)", weight "trimmed, digits with at most one decimal after `.` or `,`, 30–250 (kg)", likes "at least one, each code once", days "2–6", notes "at most 500 UTF-16 units"; `answersSchema` (zod) with those rules, each failing with the translation key of its message (`quiz.about.age.error` and so on), for the form's resolver; `draftSchema`, the same fields with `.catch()` on every field to its empty value (`goal`/`experience`/`level` absent, numbers `""`, `likes` `[]`, `days` 3, `notes` `""`, `reached` 1, reached limited to 1–7), and `emptyDraft`; `isStepValid(step, answers)` per data-model.md "Steps and validity"; `allowedStep(requested, draft)`: the requested step, at most `reached`, at most the first earlier step that is not valid; anything not a whole number 1–7 counts as `reached`
- [X] T017 [P] [US1] [US2] Write `Q/quiz-schema.test.ts`: age 15, 16, 99, 100 and "29.5"; height 119, 120, 230, 231; weight "29.9", "30", "62,5", "62.5", "62.55", "250", "250.1", " 62 "; likes empty, one, duplicate, unknown code; days 1, 2, 6, 7; notes of 500 and 501 units, notes of spaces valid; `draftSchema` keeps valid fields and drops an unknown goal code, a likes list with an unknown code, an out-of-range age and a non-number days one by one; a non-object gives `emptyDraft` (broken JSON is read by `quiz-draft.ts`, so `quiz-page.test.tsx` covers it); `allowedStep` caps at `reached`, stops at the first invalid earlier step, and treats "review", "0" and "8" as `reached`
- [X] T018 [P] [US2] Create `Q/quiz-draft.ts`: `loadDraft()` reads `sessionStorage.getItem('kinetiq.quiz')`, `JSON.parse`s it and parses it with `draftSchema`; any thrown error gives `emptyDraft`. `saveDraft(draft)` writes it with `JSON.stringify`, ignoring a thrown error (research R3: storage may be unavailable)
- [X] T019 [P] [US1] Add `quiz.*` to `F/locales/en.json` and `F/locales/pl.json`, same keys in both, English from the board (spec FR-001), Polish natural and informal: `stepOf` ("Step {{step}} of {{total}}"), `back` ("Back"), `continue` ("Continue"), `skipAndContinue` ("Skip and continue"), `answersStay` ("Your answers stay if you go back."); `goal.title`, `goal.hint`, `goal.options.<CODE>.title` and `.description`; `experience.title`, `.hint`, `.options.<CODE>.title`; `level.title`, `.hint`, `.options.<CODE>.title` and `.description`; `about.title`, `about.hint`, `about.age.{label,unit,placeholder,error}` ("Age", "years", "e.g. 29", "Enter an age between 16 and 99."), `about.height.{…}` ("Height", "cm", "e.g. 168", "Enter a height between 120 and 230 cm."), `about.weight.{…, note}` ("Body weight", "kg", "e.g. 62.5", "Enter a weight between 30 and 250 kg.", "Switch to lb later in settings."); `likes.title`, `.hint`, `.options.<CODE>.title` and `.description`; `days.title`, `.hint`, `.unit` ("days a week"), `.range` ("At least 2, at most 6."), `.fewer` ("Fewer days"), `.more` ("More days"); `notes.title`, `.hint`, `.label` ("Notes for your trainer"), `.placeholder` ("e.g. Left knee gets stiff on deep squats"). Polish examples use a decimal comma ("np. 62,5"). Sentence case, no exclamation marks
- [X] T020 [US1] [US2] Create `Q/pages/quiz-page.tsx`: read `loadDraft()` once; one `useForm` (research R2) with `mode: 'onTouched'`, `resolver: zodResolver(answersSchema)` and `defaultValues` from the draft's answers; `reached` in `useState` from the draft; `saveDraft` on every change through `form.subscribe({ formState: { values: true }, callback })` in an effect that returns the unsubscribe, and when `reached` changes; each pattern wired with `Controller`, a number field getting its translated `fieldState.error` message only while the field holds text (spec, Edge Cases: an empty field shows no message, although `onTouched` validates it when it is left); the step from `useParams`; when `allowedStep` differs from the requested step, `<Navigate replace>` to it. Layout per spec FR-028: column up to 640 px, header with "Step N of 7" and, from step 2, a ghost Back button; seven-part progress bar (`aria-hidden`; done `bg-primary`, current `bg-brand`, still to come `bg-muted`); `h1` heading with `id` and `tabIndex={-1}`, focused in an effect whenever the step changes (research R9); hint; the step's pattern from T004–T012 labelled by the heading; Continue (`size="touch"`, default variant, full width) fixed at the bottom with "Your answers stay if you go back." under it. Continue is disabled unless `isStepValid(step, values)` on the values from `useWatch`; it raises `reached` to the next step and pushes `/app/quiz/<next>` with `state: { fromStep: step }`; on step 7 it reads "Skip and continue" while the trimmed notes are empty and goes to `/app/quiz/review` (research R12). Back calls `navigate(-1)` when `location.state?.fromStep === step - 1`, else replaces with the previous step (research R4). Step 6 uses min 2, max 6
- [X] T021 [US1] Add a top-level lazy route `/app/quiz/:step?` loading `QuizPage` (`HydrateFallback: BlankWhileLoading`) in `F/app/router.ts`, beside `/app` so the tab bar is not shown (research R5); extend `F/app/router.test.tsx` if it lists the routes
- [X] T022 [US1] [US2] Write `Q/pages/quiz-page.test.tsx` (render through a `MemoryRouter` with the route, `sessionStorage` cleared before each test): step 1 shows "Step 1 of 7", five options, no Back, Continue disabled; choosing enables Continue; Continue on step 1 shows step 2 and moves focus to its heading; Back on step 2 shows step 1 with its choice; step 4: an empty field left shows no message; invalid age shows no message while first typed and its message after leaving the field, the message goes once the age is valid, it is still shown after Back and Continue back to step 4, Continue disabled until all three are valid, "62,5" accepted; step 5 starts with nothing chosen; step 6 starts at 3 with Continue enabled; step 7 reads "Skip and continue", then "Continue" after typing; a remount (reload) on step 5 shows step 5 with every answer; `/app/quiz/7` with `reached` 5 shows step 5; `/app/quiz/7` after steps 1–5 answered but step 6 never reached shows step 6; a stored unknown goal code is dropped and step 1 shows nothing chosen; in Polish the heading and the weight example ("np. 62,5") are Polish
- [X] T023 [US1] [US2] Verify in the browser per `quickstart.md` steps 1–10 at 375 px, light and dark, English and Polish, keyboard only, console; take screenshots
- [X] T024 Run `npm run check` in `frontend/`; open the pull request for #51 after the user agrees (title `feat(frontend): add quiz step flow`, `Closes #51`)

---

## Phase 4: task #53, client entry routing (US3)

**Blocked by**: task #25 (route guards), the quiz status read of story #54 (task #55 or #57, settled in that story's spec), task #57 (the waiting state it sends clients to), task #22 (current account, for T027), task #24 (sign-out, for T027) and the agreed way to remove the draft on sign-out (plan, "Open items"). Only #25 is linked as a blocker on GitHub; the others are recorded here. Re-read them when merged and refine T025–T029 to their actual code before starting.

**Goal**: A signed-in client without a submitted quiz gets the quiz from every client page; a client who submitted gets the waiting state; a client with a published plan gets the app; loading and failure as spec FR-026.

- [ ] T025 [P] [US3] Create `Q/api/quiz.ts` with a `useQuizStatus()` query over the status read of story #54 (`api.GET` typed by `F/lib/api/schema.d.ts`), throwing the problem detail on error as `F/features/exercise/api/exercises.ts` does
- [ ] T026 [US3] Add the entry check in `F/app/` around the client routes and the quiz route, on top of the guards of task #25: while the status is pending, a neutral loading state and nothing else; on failure, `t(errorMessageKey(error))` from `F/lib/api/problem.ts` and a "Try again" button that refetches; not submitted → `/app/quiz`; submitted without a published plan → the waiting state of task #57; published plan → the requested client page, and the quiz sends them into the app
- [ ] T027 [US3] Tie the draft to the signed-in account (spec FR-020) with the current-account API of task #22: store the account id with the draft and treat a draft of another account as empty; remove the draft on sign-out in the way settled in plan, "Open items". The quiz gets the account id from `F/app/` (the entry check of T026 knows the signed-in client) or from shared code in `F/lib/`, never by importing `features/account` (`frontend/CLAUDE.md`: a feature does not import from another feature)
- [ ] T028 [US3] Write tests for each case of US3 (scenarios 1–4) and for FR-020: not submitted, submitted without a plan, published plan, pending, failure with an unknown and a known code, "Try again", a draft of another account not shown
- [ ] T029 Run `npm run check` (and `./gradlew check` if the backend was touched); verify in the browser at 375 px, light and dark; open the pull request for #53 after the user agrees (title `feat(frontend): send clients to the quiz until submitted`, `Closes #53` and `Closes #50` as the story's last task)

---

## Phase 5: Convergence

- [ ] T030 Run `/speckit-converge` on the branch of task #53, the story's last task, and walk through `quickstart.md` once more

---

## Dependencies

- T001 with #52. #52 (T002–T015) → #51 (T016–T024) → #53 (T025–T029), which also waits for tasks #25, #57, #22 and #24, the quiz status read of story #54 and the agreed way to remove the draft on sign-out. T030 last.
- Inside #52: T002 before T004; T003–T013 are independent of each other except that each test follows its component; T014 needs T002–T013.
- Inside #51: T016–T019 are independent; T017 follows T016; T020 needs T016, T018 and T019 and the components of #52; T021 needs T020; T022 needs T020 and T021; T023 needs everything before it.
- Inside #53: T026 needs T025; T028 needs T026 and T027.

## Parallel examples

- #52: T004, T006, T008, T010 and T012 (five component files) at once, then their tests T005, T007, T009, T011 and T013.
- #51: T016, T018 and T019 at once.

## Implementation strategy

Build in order and open one pull request per task after its gate (`npm run check`) and the user's approval. #52 gives tested building blocks; #51 is the MVP of the story, a client can answer the whole quiz; #53 completes the story once sign-in and the quiz API exist.
