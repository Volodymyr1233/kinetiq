# Research: Client answers the quiz

Each entry: decision, reason, alternatives.

## R1 — Frontend only, feature `quiz`

**Decision**: Tasks #52 and #51 touch only `frontend/`, in a new feature folder `features/quiz/`. The backend feature of task #55 will use the same name. Task #53 reads the quiz status from the API of story #54 and adds no endpoint of its own.

**Why**: The spec keeps answers in the browser until story #54 submits them. The folder name follows `CONTEXT.md`'s term "Quiz".

## R2 — One react-hook-form form for the whole quiz, checked by zod

**Decision**: The page holds one `useForm` for all answers: `mode: 'onTouched'`, `resolver: zodResolver(answersSchema)`, `defaultValues` from the stored draft (R3). Fields of steps not on screen keep their values (the default `shouldUnregister: false`). The question patterns are wired with `Controller`. Continue checks `isStepValid(step, values)` on the watched values. The draft is saved by `subscribe({ formState: { values: true } })` on every change. `reached` lives beside the form in `useState` and is saved with the draft.

**Why**: `frontend/CLAUDE.md` names react-hook-form and zod as the project's form libraries, and the sign-up form uses them. `onTouched` gives the board's message timing: shown after the field is first left, then updated on every change. The form keeps each field's touched state while the client moves between steps (US2 scenario 3). After a reload, invalid values are dropped (R3), so no message has to be restored. `subscribe` is the library's documented way to react to changes without re-rendering; the `watch` callback is superseded.

**Alternatives**: plain React state with zod (less code, but departs from the project's form convention); a form per step (answers copied between forms and storage on every step change).

## R3 — Draft kept in `sessionStorage`, invalid fields dropped on load

**Decision**: The draft is stored under the key `kinetiq.quiz` in `sessionStorage`, written on every change and read once when the page mounts. Reading parses it through `draftSchema`, the rules of `answersSchema` with `.catch()` on every field, so an invalid or unknown value falls back to empty on its own and the rest survive (spec, Clarifications). If storage throws (private mode, quota), the quiz keeps working in memory.

**Why**: The spec keeps answers only in the tab (Clarifications). `sessionStorage` survives a reload and ends with the tab. zod 4's `.catch()` gives the per-field fallback without hand-written checks.

**Alternatives**: `localStorage` (outlives the tab, rejected in the spec); keeping only valid answers in storage (a half-typed value on step 4 would vanish on Back, against US2 scenario 3).

**Later**: Tying the draft to the signed-in account (FR-020) and removing it on sign-out (FR-019) need the current-account API (task #22) and the sign-out action (task #24). Both land in task #53, see plan "Open items".

## R4 — One URL per step, Back follows browser history

**Decision**: Steps live at `/app/quiz/1` to `/app/quiz/7`. `/app/quiz` and any step the client may not open yet send them, with `replace`, to the step they may open: the furthest step reached, or the first earlier step without a valid answer. Continue pushes the next step with `state: { fromStep }`. The quiz's Back calls `navigate(-1)` when the current entry came from the previous step, otherwise it replaces the URL with the previous step.

**Why**: The design system says "Back is always the browser back. Every client, session and plan change has its own URL." A reload keeps the URL, so the current step survives without being stored (FR-019). The browser's back works without extra code (FR-021). The `fromStep` check keeps Back inside the quiz when the client arrived by a typed address.

**Alternatives**: step only in state (the device's back would leave the quiz); step in a query parameter (works the same, but a path reads better and matches other client URLs).

## R5 — The quiz route sits outside the client layout

**Decision**: `/app/quiz/:step?` is a top-level route next to `/app`, loaded lazily, so it does not render the tab bar of `ClientArea`. React Router ranks it above `/app`'s children.

**Why**: The board's quiz screens have no tab bar (FR-028). A layout flag inside `ClientArea` would mix two layouts in one component.

## R6 — Question patterns from shadcn components

**Decision**:
- Single choice: shadcn `RadioGroup` (added with the CLI) in the "choice card" form, a `FieldLabel` wrapping a horizontal `Field` with `FieldTitle`, `FieldDescription` and `RadioGroupItem`. The group is named by the step heading through `aria-labelledby`. Cards are at least 64 px high (design system 6.1).
- Multiple choice: the same card with `Checkbox`, inside a group named by the heading.
- Number with unit: `InputGroup` with the unit in an `InputGroupAddon` at the end, linked to the input by `aria-describedby`. Height `--control-h-touch`, text 16 px.
- Stepper: two outline `Button`s, 48 px, with `aria-label`s, around an `<output aria-live="polite">` with the number. At a bound the button is marked `aria-disabled` and ignores the press; `disabled` would drop the keyboard focus of the person pressing it.
- Free text with a count: `Textarea` with `maxLength` and a counter in `FieldDescription`, linked by `aria-describedby`.

**Why**: Constitution VII and `frontend/CLAUDE.md`: shadcn first, tokens only. The choice card is shadcn's documented pattern for radio and checkbox cards, and `field.tsx` already carries its styles. No shadcn stepper exists, and two buttons with an output are the smallest accessible version.

**Alternatives**: `ToggleGroup` for single choice (no radio mark; the board shows one); a `role="spinbutton"` stepper (arrow-key semantics nobody asked for).

## R7 — Numbers typed as text and parsed by hand

**Decision**: Number fields are `type="text"` with `inputMode="numeric"` (age, height) or `"decimal"` (weight). Parsing trims, then accepts `^\d+$` for age and height and `^\d+([.,]\d)?$` for weight, with a comma read as a dot. Bounds are inclusive. The draft keeps what was typed. When a field's message shows is decided by the form (R2); the number field shows the message it is given.

**Why**: `type="number"` rejects a Polish comma in some browsers and changes the value on scroll. The design system asks for `inputmode="decimal"` with a unit inside the field. Keeping the typed text satisfies US2 scenario 3.

**Later**: Weight in lb arrives with task #46 (story #42).

## R8 — Lengths counted in UTF-16 units on both sides

**Decision**: The notes limit, the counter and `maxLength` use JavaScript string length. Task #55 checks the same limit with Java's `String.length()`.

**Why**: Both count UTF-16 units, so a note the screen accepts is never refused for its length (spec, Edge Cases). The sign-up form already follows this rule (`sign-up-schema.ts`).

## R9 — Focus moves to the step heading

**Decision**: The heading is an `h1` with `tabIndex={-1}`. An effect focuses it whenever the step changes.

**Why**: Spec, Clarifications and FR-015: a screen reader reads the new question.

## R10 — Option codes

**Decision**: Answers are stored as upper-case codes, listed in data-model.md. Labels come from the locale files by code.

**Why**: FR-002: switching the language never changes an answer. This is the same style as the exercise categories (spec 13, R10), and task #55 uses the same codes.

## R11 — Seeing the patterns of #52 in a browser

**Decision**: Task #52 builds components that no screen uses yet. They are checked in the browser through a throwaway local page that renders every pattern. It is never committed, and `git status` is checked before the commit. The whole flow is checked again in task #51.

**Why**: The Definition of Done asks for a browser check of every UI change. A committed demo route would be code that exists only to be removed.

## R12 — Continue on step 7 before the review step exists

**Decision**: In task #51, Continue on step 7 points to `/app/quiz/review`. Task #56, the next task in order, adds that route. Until then the address falls back to step 7, so the button has no visible effect, except that the device's back needs one extra press to leave step 7.

**Why**: Spec assumption: until task #56, step 7 is the end of the flow. No client can sign in before story #18, so nobody meets the gap outside development.
