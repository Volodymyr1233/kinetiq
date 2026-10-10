# Kinetiq frontend

Single-page app. React 19, TypeScript, Vite, Tailwind CSS 4, shadcn/ui (Radix primitives, Lucide icons), React Router, TanStack Query, react-i18next, Vitest with Testing Library. npm.

Libraries chosen but installed with the first feature that needs them: `react-hook-form` and `zod` for forms, shadcn `chart` (Recharts) for charts.

## Structure

```
src/
├─ app/                router, providers, app shell
├─ features/<feature>/ one folder per backend feature
│  ├─ api/             queries and mutations for this feature
│  ├─ components/      components used only by this feature
│  └─ pages/           route components
├─ components/ui/      shadcn components, added through the CLI
├─ components/         shared components composed from ui/
├─ lib/                api client, i18n, theme, utilities
├─ locales/            en.json, pl.json
└─ test/               test setup
```

- A feature imports from `components/`, `lib/` and its own folder. It does not import from another feature; shared code moves up.
- Import with the `@/` alias.
- Routes: `/trainer/*` for the trainer (designed at 1280px, sidebar layout), `/app/*` for the client (designed at 375px, centred column up to 640px). Load each group lazily.

## Design system

- Design system: https://claude.ai/artifact/ER36LGiPnfasj7Q3mFNdEM
- Screens: https://claude.ai/artifact/TRSarkkjnNWQFoJ4NsCQjh

Open the matching screen before building a page, and the design system when a component, state or piece of copy is unclear.

- Tokens live in `src/index.css`. It is the only place colours, radii, fonts and motion are defined.
- Use semantic utilities only: `bg-primary`, `text-muted-foreground`, `bg-brand`, `text-warning`, `bg-ready-low-muted`. Never palette classes (`bg-lime-300`), hex values or manual `dark:` colour overrides.
- Ink (`primary`) is the default action colour. Lime (`brand`) marks the one forward action of a view, at most once per view, always with `text-brand-foreground`.
- Low readiness and high RPE are not errors: never red. Use `ready-*` and `rpe-*`.
- Measured values (kg, reps, RPE, %, bpm) use the `num` class: Geist Mono with tabular figures.
- Client screens use 48px touch targets. Trainer tables set `data-density="compact"`.
- No state is shown by colour alone: add text, an icon or a line style.

## Components

- Look for an existing shadcn component before writing markup: `npx shadcn@latest search -q <term>`, then `npx shadcn@latest add <name>`. Read the files it adds.
- Before using a component, get its docs with `npx shadcn@latest docs <name>`.
- Compose with full structure (`Card` with `CardHeader`/`CardContent`, items inside their group, a title in every dialog). Use `gap-*` not `space-*`, `size-*` for squares, `cn()` for conditional classes.
- Variants the design system defines (for example `Card` `data-variant="metric"`) are added to the component in `components/ui/`. Check upstream changes with `npx shadcn@latest add <name> --diff` before updating one that was customised; never `--overwrite` it.
- Icons are Lucide only.

## Data

- Call the backend only through `api` from `@/lib/api/client`, typed by `src/lib/api/schema.d.ts`. Never write request or response types by hand and never edit `schema.d.ts`.
- After the backend API changes: start the backend, run `npm run gen:api`, commit the result in the same pull request.
- Server state belongs to TanStack Query: queries and mutations in `features/<feature>/api/`, one hook per operation. No server data in component state or context.
- No global state library. Local state first, then URL state, then context for app-wide concerns such as theme.
- The backend returns problem details with a `code`. Show `t('errors.<code>')`; never show backend text.
- State-changing requests send the `X-XSRF-TOKEN` header with the value of the `XSRF-TOKEN` cookie.

## Text

- Every user-facing string goes through `t()` and exists in both `en.json` and `pl.json`. No hardcoded copy, no string concatenation; use interpolation and plural forms (Polish has three).
- Use the words from `CONTEXT.md`: session, set, plan, log, trainer, client, plan change, readiness.
- Sentence case, no exclamation marks, no emoji. Buttons are verb + object. Errors say how to fix the problem.
- Numbers, dates and units are formatted for the active locale (`82.5 kg` / `82,5 kg`).

## Accessibility

- Semantic elements first; every interactive element is reachable and operable by keyboard with a visible focus ring.
- Inputs have labels; icon-only buttons have `aria-label`; invalid fields set `aria-invalid` and link their message.
- Respect `prefers-reduced-motion`.

## Tests

- Test behaviour a user would notice: what is rendered for given data, what happens on interaction, edge cases in logic. Query by role and label, interact with `userEvent`.
- Do not test implementation details, styling classes or library behaviour. No snapshot tests. No tests written to raise coverage.
- Test files sit next to the code: `*.test.ts(x)`.

## Done means verified in the browser

Before saying a UI change is done:

1. Start the app (`frontend` in `.claude/launch.json`; start the backend too when the change needs data).
2. Open the changed screen in the built-in browser and use the feature end to end: the happy path, an error, an empty state.
3. Check light and dark theme, and the viewport of the audience: 375px for client screens, 1280px for trainer screens.
4. Check the console for errors. Take a screenshot when it helps the review.

If the browser is not available, say the change is not verified. Never claim it was.

## Commands

Run in `frontend/`.

- `npm run dev`: dev server on `:5173`, proxies `/api` to the backend on `:8080`.
- `npm run check`: ESLint, Prettier check, tests, type check, production build. Must pass before a commit.
- `npm run format`: apply Prettier.
- `npm run test`: tests only.
- `npm run gen:api`: regenerate API types from the running backend.
