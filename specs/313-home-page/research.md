# Research: Visitor sees what Kinetiq does and signs up

## R1. Redirect of a signed-in person

- **Decision**: Give `/` the loader `redirectIfSignedIn` (`app/guards.ts`). It reads the current account and redirects to `landingFor(role)`, whose area index redirects on to `/trainer/clients` or `/app/today`; any failure (401, network) returns `null` and the page shows.
- **Rationale**: The same rule as the sign-in page (FR-006), already tested there; a loader runs before the page renders, so a signed-in person never sees it flash.
- **Alternatives**: A check inside the page with `useQuery` and `<Navigate>`: renders the page first, then jumps. A new guard: a second copy of the same rule.

## R2. Lazy chunk

- **Decision**: Load `HomePage` with `lazy`, with `HydrateFallback: BlankWhileLoading`, like every other page.
- **Rationale**: The page is the largest static page in the app; the areas need not carry it.

## R3. The lime action

- **Decision**: Add variant `brand` to `components/ui/button.tsx`: `bg-brand text-brand-foreground hover:bg-brand/80`, the classes the join and today pages pass by hand today. Use `size="touch"` (48 px).
- **Rationale**: The design system defines the brand button (`btn-brand`), and `frontend/CLAUDE.md` asks for its variants in `components/ui/`. The existing hand-written uses stay as they are (out of scope).

## R4. Motion

- **Decision**:
  - Hero: `tw-animate-css` classes `animate-in fade-in motion-safe:slide-in-from-bottom-3 fill-mode-both duration-200 ease-out` with `delay-*` steps of 80 ms.
  - Sections: a `reveal` utility in `src/index.css`, inside `@supports (animation-timeline: view())`, running a fade-up keyframe on `view()` over `entry 0% entry 40%`; under `prefers-reduced-motion` it runs a fade only.
  - Readiness: an effect that fills the three check-in answers at 500, 750 and 1000 ms, then counts 0 → 83 over 600 ms with `requestAnimationFrame`; with reduced motion it starts at the final state. Timers are cleared on unmount.
- **Rationale**: No new dependency; the design system's motion: opacity and transform only, ease-out, nothing loops. Browsers without scroll timelines show sections as they are.
- **Note**: The global reduced-motion rule in `index.css` shortens every animation to 1 ms, so with reduced motion the hero's fade is effectively instant; that satisfies FR-008 (no movement).

## R5. Display text size

- **Decision**: Tokens `--text-display-sm` (2.5rem / 2.75rem) and `--text-display` (3.25rem / 3.5rem) in the `@theme inline` type scale; the headline uses `text-display-sm md:text-display`.
- **Rationale**: The boards' headline is 40/44 on a phone and 52/56 from 768 px; Tailwind's scale has no such pair, and the type scale lives only in `index.css`.

## R6. Tests

- **Decision**: `home-page.test.tsx` with `renderAppAt`: a trainer lands on `/trainer/clients`, a client on `/app/today`, a visitor stays on `/` and sees the main heading; the visitor's links point to `/sign-up` (two), `/sign-in` and `/privacy`; choosing Polish in the footer switches the page.
- **Rationale**: Behaviour a visitor notices (spec SC-002, SC-003); no snapshot or style tests (constitution V).

## R7. Accessibility

- **Decision**: Header `banner`, `main`, footer `contentinfo`; one `h1`, an `h2` per section, `h3` in steps and sides. Each mock-up is a `figure` with `role="img"` and an `aria-label` from the locale files; its contents are static spans, never buttons or links. Language and theme are `SettingToggleGroup`s labelled by visible titles. The how-it-works steps are an `ol`.
- **Rationale**: FR-012 and WCAG 2.2 AA (1.1.1, 1.3.1, 2.1.1, 2.4.7, 2.5.8).
