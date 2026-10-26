# Research: Trainer and client change their settings

Each entry: decision, reason, alternatives. Task #43 is final; the entries for tasks #44, #45 and #46 are refined when each task starts.

## R1 — Settings are three columns on the account (task #43)

**Decision**: The `Account` aggregate gets three enums: `WeightUnit` (`KG`, `LB`), `Language` (`EN`, `PL`) and `Theme` (`LIGHT`, `DARK`, `SYSTEM`), held together in a record `AccountSettings`. Migration `V6__add_account_settings.sql` adds the columns `units`, `language` and `theme` to `accounts`, each with a check constraint and a default (`KG`, `EN`, `SYSTEM`), which gives existing accounts their starting values (spec, Clarifications).

**Why**: Every account has exactly one value of each (spec FR-001); three columns are the smallest shape that the database can check. Enums stored as strings follow `backend/CLAUDE.md`.

**Alternatives**: a separate `account_settings` table (a one-to-one table with nothing to gain); one JSON column (the database could not check the values).

## R2 — The current account carries the settings (task #43)

**Decision**: `GET /api/v1/accounts/me` adds `units`, `language` and `theme` to its answer (spec 18, FR-018). Task #44 adds `email`, the first screen that shows it; task #45 adds it instead if it starts first. Shape in contracts/api.md.

**Why**: The guards already load the current account before every area page (spec 18), so the settings arrive with no extra request, and spec 18 already planned that task #43 adds them to this answer (spec 18, Assumptions).

## R3 — One partial update for the signed-in account (task #43)

**Decision**: `PATCH /api/v1/accounts/me` takes `units`, `language` and `theme`, each optional; only the fields sent change (spec, Clarifications: "Only the values changed on this screen"). It answers `200` with the current account as `GET` returns it, so the app can update what it holds. The values are strings checked with `@Pattern` (`KG|LB`, `EN|PL`, `LIGHT|DARK|SYSTEM`), so a wrong value gets `validation.failed` with that field and the code `pattern` (spec FR-005). Task #44 adds `name` to the same operation.

**Why**: The client sheet changes one value at a time and the trainer screen saves only what changed; one partial update serves both without overwriting a value changed on another device. Enum types in the request would answer an unknown value with `request.invalid` and no field.

**Alternatives**: one `PUT` per setting (the trainer screen's save would be several requests that can half succeed); `PUT` of all settings (overwrites another device's change).

## R4 — A new trainer starts with the UI language (task #43)

**Decision**: `Account.signUpTrainer` takes the starting language from the locale the sign-up already receives (`SignUpTrainerCommand.locale`, from the `Accept-Language` header the app sends, resolved to English or Polish by `WebConfig`); units start as `KG` and theme as `SYSTEM` (spec FR-002).

**Why**: The sign-up already knows the UI language for its email (spec 2, FR-019); no request field is added.

## R5 — The app applies the account's settings wherever it holds the account (task #43)

**Decision**: A component in `app/`, mounted once inside the query client, observes the current account with `useQuery({ ...currentAccountQuery, enabled: false })`: it never fetches, so public pages send no request, and it updates whenever the guards, sign-in or a save put an account into the cache. When the account's language or theme differs from what the app shows, it calls `i18n.changeLanguage` and the theme context's `setTheme`. `lib/i18n.ts` remembers every language change on the device (`localStorage` key `kinetiq-language`, next to the existing `kinetiq-theme`) and starts with it before falling back to the browser's language (spec FR-006). After sign-out the cache is cleared and the device keeps the last language and theme.

**Why**: Spec FR-006 and Clarifications. The cache is the one place every path to a signed-in page goes through (spec 18: guards, sign-in, reload), so one observer covers them all. `app/` may use `features/account` (it already does for the guards).

**Alternatives**: applying in the guard (a loader cannot reach the theme context); applying in each area (the quiz route sits outside the client area, so three places).

## R6 — A client cannot change a name (task #44)

**Decision**: A `name` sent by a client is refused by a domain exception of kind `NOT_ALLOWED` with the code `account.name-not-changeable` (`403`), and nothing changes. A trainer's name follows the sign-up rules (`Account.NAME_MAX_LENGTH`, the same Bean Validation as `SignUpRequest`).

**Why**: Spec FR-004: only a trainer changes their name. The check belongs to the account, which knows its role.

## R7 — Trainer settings screen (task #44, outline)

**Decision**: `/trainer/settings` loads a `SettingsPage` from `features/account/pages/`, built with react-hook-form and zod as the sign-up page is, with the name rules shared with `sign-up-schema.ts`. Units and appearance use the shadcn `ToggleGroup`; the language uses the shadcn `Select` (added with the CLI), as on the board. Saving sends only the changed fields (react-hook-form's dirty fields), then shows "Settings saved." in a status region (spec, Clarifications).

## R8 — Client settings sheet (task #45)

**Decision**: The sheet in `app/client-area.tsx` gets `DisplaySettings` from `features/account/components/` with three `ToggleGroup`s. A choice sends that one field and shows at once from the pending request (`useMutationState`); the server's answer goes into the cached account, so R5 applies language and theme. Updates share one mutation scope, so they reach the server in the order made and the last choice stays (spec, Edge Cases). On failure the pending choice disappears, so the stored value shows again, with a translated message (spec FR-012).

**Alternatives**: writing the choice into the cached account before the answer. The answer to an earlier choice would briefly undo a later one, and a failed language or theme would flip back after it had already applied.

## R9 — Weights in the chosen unit (task #46, outline)

**Decision**: `lib/weight.ts` holds the conversion (1 lb = 0.45359237 kg), formatting (at most one decimal, in the UI language) and parsing (a dot or a comma) for kg and lb, with unit tests (issue #46). The person's unit reaches features through a context in `lib/`, provided by `app/` from the current account, since a feature does not import another feature (`frontend/CLAUDE.md`). The quiz keeps the typed text in its draft and checks it against 30–250 kg after conversion; the review formats it; the submit of task #57 sends kg. The draft also keeps the unit the weight was typed in, so a draft read after the person switched units shows the weight converted (task #208, spec User Story 4, scenario 3); the submit of task #57 converts from that unit.

**Alternatives** (task #208): keeping the weight in kg in the draft (each keystroke would be rounded through a conversion while typing); converting where the weight is shown (step 4 and the review would each repeat it, and the tab would keep a value in the old unit).

**Why**: Spec FR-013 to FR-016 and Clarifications. One module is used by every screen with a weight, so the round trip of FR-014 is proven once.

## R10 — Migration number (task #43)

**Decision**: `V6` is the next free number on `master`. If another task merges a `V6` first, this task renumbers its own migration before merging; a merged migration is never edited (`backend/CLAUDE.md`).
